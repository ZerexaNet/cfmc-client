package com.cfmc.common.platform;

import com.cfmc.common.world.CFMCChunkLoader;

import java.util.Map;

/**
 * ============================================================================
 * 世界桥接 — common → loader 层的"写入 Minecraft 世界"通道 (版本中立契约)
 * ============================================================================
 * 背景: common 模块零 MC 依赖, 收到服务端包 (JoinGame/ChunkData/BlockUpdate)
 * 后需要把这些数据真正灌进客户端世界 —— 这必须由 loader 层 (fabric/neoforge,
 * 各 MC 版本独立构建) 完成。本接口是两层之间唯一的下行通道, 模式与
 * {@link CFMCPlatform} (上行取版本/目录) 对称, 构成完整的双向桥。
 *
 * ⚠️ 线程契约: 实现方收到的所有回调都发生在 WebSocket IO 线程 (CFMCNetworkManager
 *    onBinaryMessage 直接分派), 实现内部必须自行切换到 MC 主线程
 *    (如 MinecraftClient.execute) 后再触碰世界/玩家/界面。
 *
 * 生命周期: loader 入口 init() 时 {@link CFMCWorldBridgeHolder#set} 注入;
 *    未注入时 holder 返回 null, 包 handle() 全部静默跳过 (网络层可独立运行)。
 */
public interface CFMCWorldBridge {

    /**
     * 加入游戏 (JoinGame 0x02)。
     * @param entityId 服务端分配的实体 id
     * @param gamemode 0=survival 1=creative 2=adventure 3=spectator
     * @param x/y/z    服务端恢复的或出生点坐标 (整列权威数据将围绕此点流式下发)
     */
    void onJoinGame(int entityId, int gamemode, double x, double y, double z);

    /**
     * 区块数据 (ChunkData 0x03)。
     * @param chunkX/chunkZ 区块坐标 (除以 16)
     * @param fullChunk     true = sections 覆盖整列权威数据 (未出现的 sectionY 全为空气,
     *                      实现方应清除本地残留); false = 增量, 只覆盖列出的 section
     * @param sections      sectionY(绝对序号, floor(y/16), 可为负) → 解码后的 16³ section
     */
    void onChunkData(int chunkX, int chunkZ, boolean fullChunk,
                     Map<Integer, CFMCChunkLoader.CesiumSection> sections);

    /** 单方块更新 (BlockUpdate 0x04) — 服务端权威的实时变化 */
    void onBlockUpdate(int x, int y, int z, String blockName);

    /** 玩家列表变化 (PlayerInfo 0x08) — action: 0=join 1=leave (PlayerInfoPacket 常量) */
    void onPlayerInfo(int action, String name);

    /** 聊天/系统消息 (ChatMessage 0x09) */
    void onChatMessage(String text);

    /** 服务端主动断开 (Disconnect 0x0A) — reason 已服务端本地化 */
    void onServerDisconnected(String reason);

    /**
     * 连接层异常关闭 (网络断开/认证失败被拒/服务端未部署等)。
     * 与 {@link #onServerDisconnected} 互斥触发 (见 CFMCNetworkManager 的去重)。
     * @param code   WebSocket 关闭码 (1000=正常; 1006=异常; 4xxx=服务端自定义如 4001 认证失败)
     * @param reason 关闭原因 (可能为空)
     * @param remote true=服务端关闭, false=本地/网络异常
     */
    void onConnectionClosed(int code, String reason, boolean remote);
}
