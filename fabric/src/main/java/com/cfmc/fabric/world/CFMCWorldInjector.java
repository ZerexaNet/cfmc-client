package com.cfmc.fabric.world;

import com.cfmc.common.network.CFMCNetworkManager;
import com.cfmc.common.network.CFMCReconnectHandler;
import com.cfmc.common.platform.CFMCWorldBridge;
import com.cfmc.common.platform.CFMCWorldBridgeHolder;
import com.cfmc.common.protocol.packets.clientbound.PlayerInfoPacket;
import com.cfmc.common.util.CFMCLogger;
import com.cfmc.common.world.CFMCChunkLoader;
import com.cfmc.fabric.render.CFMCDisconnectScreen;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================================
 * CFMC 世界注入器 — 把服务端权威地形灌进客户端世界 (Phase 2 核心!)
 * ============================================================================
 * 解决的问题: CFMC 是 WebSocket-only, 服务端数据走我们自己的二进制协议;
 * 客户端在本地世界里按 P 连接后, 服务端的区块/方块/位置必须真正"落"到
 * 玩家眼前的世界中 —— 本类就是 [协议世界] 与 [渲染世界] 的桥梁。
 *
 * 模式 (与规格一致的"覆盖层"设计):
 *   - 玩家停留在本地世界 (单机存档/原版世界均可), P 键登录 CFMC 后:
 *     1. JoinGame → 把玩家传送到服务端恢复位置/出生点;
 *     2. 服务端围绕玩家流式下发区块 (4 区块/tick) → 逐列覆盖本地地形;
 *     3. 后续 BlockUpdate 实时覆盖单方块。
 *   - 原版区块流在 CFMC 会话期间被 ClientPlayNetworkHandlerMixin 拦截丢弃
 *     (防双数据源打架), 断开后自动恢复 → 本地存档本身不受任何影响
 *     (setBlockState 是客户端侧的, 不会写进存档)。
 *
 * 版本兼容 (1.20.1 ~ 1.21.4 全矩阵, 逐项经 yarn 映射核验):
 *   - Identifier.of(ns, path)         — 5 版本全部存在 (method_43902/60655)
 *   - World.setBlockState(pos,st,fl)  — ModifiableWorld 接口, 5 版本一致
 *   - BlockPos.Mutable.set(III)       — 5 版本一致
 *   - Registries.BLOCK / get(id)      — 5 版本一致
 *   - refreshPositionAndAngles(DDDFF) — Entity, 5 版本一致
 *   - inGameHud.getChatHud().addMessage — 5 版本一致
 *
 * 线程模型:
 *   - 桥接回调全部来自 WS IO 线程 → 统一经 MinecraftClient.execute 切主线程;
 *   - 区块不逐包立即灌入: 进 pendingChunks 队列 (同区块新数据覆盖旧数据),
 *     主线程 tick 里 drain(), 每 tick 最多 {@link #CHUNKS_PER_TICK} 个,
 *     与服务端 4 区块/tick 的流式预算对齐, 避免单帧 10 万次 setBlockState。
 */
public final class CFMCWorldInjector implements CFMCWorldBridge {

    /** 每 tick 最多灌入的区块数 (与服务端 CHUNK_SEND_BUDGET_PER_TICK=4 对齐) */
    private static final int CHUNKS_PER_TICK = 4;

    /**
     * setBlockState flags = Block.NOTIFY_LISTENERS(2):
     * 只通知渲染监听器重画区块, 不触发邻居方块更新 (避免水/沙物理连锁),
     * 光照照常增量重算。不用 FORCE_STATE(16) — 那会连重渲染也跳过。
     */
    private static final int SET_FLAG = 2;

    private static final BlockState AIR_STATE = Blocks.AIR.getDefaultState();

    /** 方块名 → 本版本 BlockState (跨区块高命中; ConcurrentHashMap 线程安全) */
    private static final Map<String, BlockState> STATE_CACHE = new ConcurrentHashMap<>();

    /** 待灌入区块队列: "cx,cz" → 数据 (同 key 后到覆盖先到; 插入序 = 服务端下发序) */
    private static final Map<String, ChunkApply> pendingChunks =
            Collections.synchronizedMap(new LinkedHashMap<>());

    /** HUD 计数器: 本会话已灌入区块数 */
    private static volatile int syncedChunks = 0;

    /** 单区块灌入任务 */
    private record ChunkApply(int cx, int cz, boolean fullChunk,
                              Map<Integer, CFMCChunkLoader.CesiumSection> sections) {}

    /* =========================================================================
     * 注册与生命周期
     * ======================================================================= */

    /** CFMCFabricClient.init 时调用一次 */
    public static void register() {
        CFMCWorldBridgeHolder.set(new CFMCWorldInjector());
    }

    /** HUD 用: 本会话已灌入区块数 */
    public static int syncedChunks() {
        return syncedChunks;
    }

    /**
     * 主线程每 tick 调用 (ClientTickEvents): 灌入积压区块。
     * 不在会话中/不在世界里时零开销直接返回。
     */
    public static void drain(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        if (!CFMCNetworkManager.getInstance().isActive()) return;

        int budget = CHUNKS_PER_TICK;
        while (budget-- > 0) {
            ChunkApply apply = pollPending();
            if (apply == null) break;
            try {
                applyChunk(client, apply);
                syncedChunks++;
            } catch (Throwable t) {
                // 单区块失败不拖垮整个队列 (错误方块名/越界等), 记日志便于排查
                CFMCLogger.error("区块 (" + apply.cx() + "," + apply.cz() + ") 灌入失败", t);
            }
        }
    }

    private static ChunkApply pollPending() {
        synchronized (pendingChunks) {
            Iterator<Map.Entry<String, ChunkApply>> it = pendingChunks.entrySet().iterator();
            if (!it.hasNext()) return null;
            ChunkApply a = it.next().getValue();
            it.remove();
            return a;
        }
    }

    /* =========================================================================
     * 桥接实现 (全部运行在 WS IO 线程 — 一律 execute 切主线程)
     * ======================================================================= */

    @Override
    public void onJoinGame(int entityId, int gamemode, double x, double y, double z) {
        pendingChunks.clear(); // 新会话: 丢弃上一场残留队列
        syncedChunks = 0;
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c.player == null || c.world == null) {
                // 主菜单直连场景: 无法传送/灌入; 进世界后位置上报会自然接管
                chat("§e[CFMC] 已加入服务器 — 进入任意世界后重新连接即可同步地形");
                return;
            }
            // 传送到服务端恢复位置/出生点 (+1 防卡地); 后续 20Hz 位置上报以新位置为准
            c.player.refreshPositionAndAngles(x, y + 1.0, z, 0.0f, 0.0f);
            c.player.setVelocity(Vec3d.ZERO);
            c.player.fallDistance = 0.0f;
            chat("§a[CFMC] 已连接, 服务器地形同步中...");
        });
    }

    @Override
    public void onChunkData(int chunkX, int chunkZ, boolean fullChunk,
                            Map<Integer, CFMCChunkLoader.CesiumSection> sections) {
        // IO 线程只做入队 (新数据覆盖同区块旧数据); 灌入走 drain()
        pendingChunks.put(chunkX + "," + chunkZ,
                new ChunkApply(chunkX, chunkZ, fullChunk, sections));
    }

    @Override
    public void onBlockUpdate(int x, int y, int z, String blockName) {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c.world == null) return;
            c.world.setBlockState(new BlockPos(x, y, z), toState(blockName), SET_FLAG);
        });
    }

    @Override
    public void onPlayerInfo(int action, String name) {
        MinecraftClient.getInstance().execute(() -> {
            if (action == PlayerInfoPacket.ACTION_JOIN) {
                chat("§e[CFMC] " + name + " 加入了服务器");
            } else {
                chat("§7[CFMC] " + name + " 离开了服务器");
            }
        });
    }

    @Override
    public void onChatMessage(String text) {
        MinecraftClient.getInstance().execute(() -> chat("[服务器] " + text));
    }

    @Override
    public void onServerDisconnected(String reason) {
        MinecraftClient.getInstance().execute(() -> {
            MinecraftClient c = MinecraftClient.getInstance();
            if (c.player != null && c.world != null) {
                chat("§c[CFMC] 与服务器断开: " + reason);
                return; // 世界内: 聊天提示即可, 不打断玩家
            }
            // 未进世界: 展示独立断线界面 (带重连按钮)
            c.setScreen(new CFMCDisconnectScreen(reason,
                    CFMCReconnectHandler.getInstance().lastUrl()));
        });
    }

    @Override
    public void onConnectionClosed(int code, String reason, boolean remote) {
        MinecraftClient.getInstance().execute(() -> {
            String detail = "(code=" + code
                    + (reason != null && !reason.isEmpty() ? ", " + reason : "") + ")";
            String msg = remote ? "§c[CFMC] 服务器关闭了连接 " + detail
                                : "§c[CFMC] 连接异常断开 " + detail + " — 请检查服务器地址/部署状态";
            chat(msg);
        });
    }

    /* =========================================================================
     * 区块灌入 (主线程)
     * ======================================================================= */

    /**
     * 把一个服务端区块覆盖到本地世界。
     * fullChunk=true (服务端恒为 true): sections 是整列权威数据 —
     * 包含的 section 逐方块覆盖, 未包含的 sectionY 全为空气 → 清除本地残留。
     */
    private static void applyChunk(MinecraftClient client, ChunkApply apply) {
        ClientWorld world = client.world;
        if (world == null) return;

        int wx0 = apply.cx() << 4;
        int wz0 = apply.cz() << 4;

        // 1) 调色板一次性解析 (名 → 本版本 BlockState; 未知方块按空气)
        Map<Integer, BlockState[]> resolved = new HashMap<>(apply.sections().size() * 2);
        for (Map.Entry<Integer, CFMCChunkLoader.CesiumSection> e : apply.sections().entrySet()) {
            resolved.put(e.getKey(), resolvePalette(e.getValue().palette));
        }

        int bottomY = world.getBottomY();
        int topY = bottomY + world.getHeight();          // exclusive
        int syMin = bottomY >> 4, syMax = (topY - 1) >> 4;

        BlockPos.Mutable pos = new BlockPos.Mutable();
        int changed = 0;

        // 2) 整列处理: 逐 section 对照服务端数据
        for (int sy = syMin; sy <= syMax; sy++) {
            CFMCChunkLoader.CesiumSection sec = apply.sections().get(sy);
            if (sec == null) {
                if (!apply.fullChunk()) continue;        // 增量模式: 未列出的不动
                // 整列模式: 服务器此 section 全空气 → 清本地残留 (避免存档地形"鬼影")
                for (int ly = 0; ly < 16; ly++) {
                    int wy = (sy << 4) | ly;
                    if (wy < bottomY || wy >= topY) continue;
                    for (int lz = 0; lz < 16; lz++) {
                        for (int lx = 0; lx < 16; lx++) {
                            pos.set(wx0 | lx, wy, wz0 | lz);
                            if (!world.getBlockState(pos).isAir()) {
                                world.setBlockState(pos, AIR_STATE, SET_FLAG);
                                changed++;
                            }
                        }
                    }
                }
                continue;
            }

            BlockState[] states = resolved.get(sy);
            short[] indices = sec.indices;
            for (int ly = 0; ly < 16; ly++) {
                int wy = (sy << 4) | ly;
                if (wy < bottomY || wy >= topY) continue;
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        // Cesium 索引布局: (y<<8)|(z<<4)|x — 与 CesiumSection.getBlock 一致
                        short pi = indices[(ly << 8) | (lz << 4) | lx];
                        BlockState target = (pi >= 0 && pi < states.length)
                                ? states[pi] : AIR_STATE;
                        pos.set(wx0 | lx, wy, wz0 | lz);
                        // 引用相等比较规范化 BlockState: 未变化的跳过 (省渲染重排)
                        if (world.getBlockState(pos) != target) {
                            world.setBlockState(pos, target, SET_FLAG);
                            changed++;
                        }
                    }
                }
            }
        }

        if (changed > 0) {
            CFMCLogger.info("区块 (" + apply.cx() + "," + apply.cz() + ") 已同步: " + changed + " 方块");
        }
    }

    /* =========================================================================
     * 方块名解析 (版本中立名 → 本版本 BlockState)
     * ======================================================================= */

    private static BlockState[] resolvePalette(String[] palette) {
        BlockState[] out = new BlockState[palette.length];
        for (int i = 0; i < palette.length; i++) {
            out[i] = toState(palette[i]);
        }
        return out;
    }

    /** "minecraft:stone[props...]" → 本版本 BlockState (未知/非法 → 空气) */
    public static BlockState toState(String name) {
        if (name == null || name.isEmpty()) return AIR_STATE;
        int br = name.indexOf('[');
        String base = br >= 0 ? name.substring(0, br) : name;
        BlockState cached = STATE_CACHE.get(base);
        if (cached != null) return cached;

        BlockState state = lookup(base);
        STATE_CACHE.put(base, state);
        return state;
    }

    private static BlockState lookup(String name) {
        try {
            int colon = name.indexOf(':');
            Identifier id = colon >= 0
                    ? Identifier.of(name.substring(0, colon), name.substring(colon + 1))
                    : Identifier.of("minecraft", name);
            // Registries.BLOCK 是 DefaultedRegistry: 未注册 id 返回 air 而非 null
            Block block = Registries.BLOCK.get(id);
            return block == null ? AIR_STATE : block.getDefaultState();
        } catch (Exception e) {
            return AIR_STATE; // 非法字符/未知命名空间 → 空气 (该方块本版本不存在)
        }
    }

    /* =========================================================================
     * 聊天工具
     * ======================================================================= */

    private static void chat(String legacyText) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.inGameHud == null) {
            CFMCLogger.info(legacyText);
            return;
        }
        c.inGameHud.getChatHud().addMessage(toText(legacyText));
    }

    /** § 色码 → Formatting (Text.literal 不解析 § 码, 手动映射常用色) */
    private static Text toText(String legacy) {
        String color = null;
        String body = legacy;
        if (body.startsWith("§") && body.length() >= 2) {
            color = switch (body.charAt(1)) {
                case 'a' -> "green";
                case 'e' -> "yellow";
                case 'c' -> "red";
                case '7' -> "gray";
                case 'b' -> "aqua";
                default -> null;
            };
            body = body.substring(2);
        }
        MutableText t = Text.literal(body); // literal 返回 MutableText, formatted 仅在其上存在
        if (color != null) {
            Formatting f = Formatting.byName(color);
            if (f != null) t = t.formatted(f);
        }
        return t;
    }
}
