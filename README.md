# CFMC Client

Cloudflare Minecraft Edge Server 的客户端 Mod —— **全版本支持**：一份代码构建覆盖 Fabric + NeoForge × MC 1.20.1 ~ 1.21.4（加一行即可扩展到更多版本）。

配合服务端 [cfmc-server](https://github.com/ZerexaNet/cfmc-server)（v2 协议，1.8~1.21.8 全协议接入）使用。

## 架构：为什么能做到全版本

```
┌─────────────────────────────────────────────────────────────┐
│                        common/  (纯 Java)                    │
│  协议编解码 · WebSocket 传输 · 四模式认证 · Cesium 区块解码   │
│  版本注册表 · 平台抽象 · 配置          【零 MC 依赖】         │
│  恒定 Java 17 字节码 → 所有版本共享同一份 core jar            │
└──────────────┬──────────────────────────────┬───────────────┘
               │ CFMCPlatform (注入点)         │
       ┌───────▼────────┐             ┌───────▼─────────┐
       │    fabric/     │             │   neoforge/     │
       │ yarn 映射 Mixin │             │ Mojmap Mixin    │
       │ HUD/键位/界面   │             │ 键位/界面        │
       │ (薄适配层)      │             │ (薄适配层)       │
       └────────────────┘             └─────────────────┘
```

**三步解耦**：

1. **协议层版本中立（CFMC v2）**——方块以命名空间名（`minecraft:stone`）传输，不用跨版本不稳定的数字 ID；握手上报 MC 版本让服务端选适配器。
2. **公共逻辑零 MC 依赖**——90% 的代码（网络/协议/认证）在 `common`，一份字节码全版本通用；版本差异收敛到 `CFMCPlatform` 接口。
3. **版本矩阵构建**——loader 适配层源码固定，构建时按 `versions.gradle` 版本表逐版本产出。

## 版本矩阵

| 加载器 | 1.20.1 | 1.20.4 | 1.20.6 | 1.21.1 | 1.21.4 |
|--------|:------:|:------:|:------:|:------:|:------:|
| Fabric | ✅ | ✅ | ✅ | ✅ | ✅ |
| NeoForge | —¹ | ✅ | ✅ | ✅ | ✅ |

¹ 1.20.1 无 NeoForge（该版本只有 Forge，可按同模式扩展 forge/ 模块）

CI（`.github/workflows/build.yml`）对上表每个组合独立构建，产物按 `cfmc-client-<loader>-<mc>-<version>.jar` 命名。

## 快速开始

```bash
# 构建（默认 1.20.4）
gradle :fabric:build

# 指定版本构建
gradle :fabric:build -Pmc_version=1.21.4
gradle :neoforge:build -Pmc_version=1.21.1

# 全版本矩阵构建（产物在 dist/）
./scripts/build-all.sh

# 本地运行调试
gradle :fabric:runClient -Pmc_version=1.20.4
```

**加一个新 MC 版本只需两步**：

1. `versions.gradle` 的 `versionsTable` 加一行（yarn/fabricApi/NeoForge 版本 + Java 要求）
2. `gradle.properties` 的 `supported_versions` 追加版本号

服务端、协议、common 全部零改动。

## 目录结构

```
cfmc-client/
├── common/                 # ★ 纯 Java 核心（禁止 import net.minecraft.*/net.fabricmc.*/net.neoforged.*）
│   └── src/main/java/com/cfmc/common/
│       ├── protocol/       # 包注册表/读写器/9 S2C + 6 C2S 包（与 server 端逐字对齐）
│       ├── network/        # WebSocket 客户端/状态机/心跳/重连/deflate
│       ├── auth/           # online/offline/skin_server/hybrid 四种认证策略
│       ├── version/        # CFMCVersionRegistry（MC 版本↔协议号, 与服务端镜像）
│       ├── platform/       # CFMCPlatform 抽象 + Holder（loader 注入点）
│       ├── world/          # Cesium 调色板区块解码（LongArray 位级互逆）
│       ├── config/         # Properties 配置
│       └── util/           # 常量/日志
├── fabric/                 # Fabric 适配层（yarn 映射）
│   └── src/main/java/com/cfmc/fabric/
│       ├── CFMCFabricClient.java    # 入口: 平台注入最早执行
│       ├── mixin/          # ClientConnection/MinecraftClient/PlayNetworkHandler
│       ├── render/         # HUD 覆盖层/断线界面
│       └── screen/         # 认证连接界面
├── neoforge/               # NeoForge 适配层（Mojang 官方映射, ModDevGradle）
│   └── src/main/java/com/cfmc/neoforge/
│       ├── CFMCNeoForgeClient.java
│       ├── mixin/          # Connection/Minecraft（Mojmap 名）
│       └── screen/
├── versions.gradle         # ★ 版本矩阵表（加版本的唯一改动点）
├── scripts/build-all.sh    # 本地全矩阵构建
└── .github/workflows/      # CI 矩阵（push 自动全版本构建）
```

## 使用方法

1. 下载对应版本组合的 jar（如 Fabric 1.21.4 → `cfmc-client-fabric-1.21.4-*.jar`），连同其依赖要求放入 `mods/`
2. 启动游戏，按 **P** 打开连接界面
3. 填入服务端地址（`ws://` 或 `wss://`）连接；认证模式支持 `online`（正版）/ `offline` / `skin_server`（外置皮肤站）/ `hybrid`
4. 配置文件：`.minecraft/config/cfmc-client.properties`

## 全版本支持的已知边界

- **Mixin 目标名**：fabric 侧用 yarn 名（`ClientConnection`）、neoforge 侧用 Mojmap 名（`Connection`），两个适配层各自维护；同一 loader 内 1.20.x~1.21.x 目标稳定。
- **版本级 API 差异**：极少数（如 NeoForge 1.20.x 与 1.21.x 的 HUD 事件签名不同）已通过 Mixin 注入绕开；若某版本构建失败，改动只局限在对应 loader 模块内。
- **1.8~1.19 服务端也支持**，但客户端 Mod 的 Mixin/UI 是按 1.20+ 写的；更老版本接入需按同一 common 模式新增适配层（common 零改动）。

## 与服务端的协议对齐

| 层 | 客户端 | 服务端 |
|----|--------|--------|
| 包注册表 | `common/protocol/CFMCPacketRegistry.java` | `src/protocol/packet-definitions.js` |
| 版本注册表 | `common/version/CFMCVersionRegistry.java` | `src/protocol/version-registry.js` |
| 常量 | `common/util/CFMCConstants.java`（CFMC v2） | `PROTOCOL_VERSION = 2` |

变更任何一端的包结构必须同步另一端并递增 `PROTOCOL_VERSION`。

## License

Apache-2.0
