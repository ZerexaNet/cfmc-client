# CFMC Client

> Cloudflare Minecraft Edge Server 的配套客户端 Mod (Fabric 1.20.4)。

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![MC](https://img.shields.io/badge/Minecraft-1.20.4-green)](https://fabricmc.net)
[![Phase](https://img.shields.io/badge/Phase-1__wire__link-orange)](#开发状态)

配套服务端: [cfmc-server](https://github.com/ZerexaNet/cfmc-server)

---

## 项目简介

CFMC Client 用 **WebSocket 替代原版 TCP 网络层**，通过**自定义二进制协议**与运行在
Cloudflare 边缘上的 CFMC-Edge 服务器通信。不修改游戏逻辑，仅接管"连接"这一层。

| 能力 | 说明 |
|------|------|
| WebSocket 传输 | Java-WebSocket 库，WSS 加密，Header 携带 JWT |
| 自定义二进制协议 | VarInt + 帧格式 + 调色板区块解码，与服务端逐字节对齐 |
| 多模式认证 | 正版 / 离线 / 皮肤站 (Yggdrasil) / 混合降级链 |
| 心跳保活 | 10s 周期 KeepAlive（独立线程，卡顿不掉线） |
| 自动重连 | 指数退避 5s→10s→20s，可配置次数 |
| 按键直连 | 默认 `P` 键打开登录界面 |

## 快速开始

### 构建环境

- JDK 17
- （首次构建自动下载 Gradle Wrapper / Loom / Minecraft 映射）

```bash
# 生成 wrapper (仓库未附带 wrapper jar)
gradle wrapper --gradle-version 8.6

# 开发运行
./gradlew runClient

# 构建 mod jar → build/libs/cfmc-client-0.1.0.jar
./gradlew build
```

### 连接服务器

1. 安装 Fabric Loader 1.20.4 + 本 Mod（放进 `mods/`）
2. 修改 `.minecraft/config/cfmc-client.properties`：

```properties
serverAddress=ws://localhost:8787   # 或 wss://你的.workers.dev 域名
authMode=hybrid                     # online / offline / skin_server / hybrid
skinServerUrl=https://ely.by
```

3. 游戏内按 **P** → 输入用户名（皮肤站模式填密码）→ 连接
4. 聊天测试（对应服务端 Phase 1 验收标准）：两个客户端互连，互相可见加入通知

## 目录结构

```
src/main/java/com/cfmc/client/
├── CFMCClientMod.java            # 入口: 配置/HUD/按键/tick挂钩
├── CFMCConfig.java               # Properties 配置 (config/cfmc-client.properties)
├── network/
│   ├── CFMCNetworkManager.java   # 单例状态机: 连接/帧解析/包分派
│   ├── CFMCWebSocketClient.java  # Java-WebSocket 封装 (只管管道)
│   ├── CFMCHeartbeatManager.java # 10s 心跳保活
│   ├── CFMCReconnectHandler.java # 指数退避重连
│   └── CFMCCompression.java      # deflate-raw 解压 (服务端 nowrap 兼容)
├── protocol/
│   ├── CFMCBufferUtils.java      # VarInt/VarLong/String (与服务端逐字节一致)
│   ├── PacketReader/Writer.java  # BE 标量读写 + frame() 帧封装
│   ├── CFMCPacket.java           # 包基类 (encode/decode/handle)
│   ├── CFMCPacketRegistry.java   # 包ID ↔ 类映射
│   └── packets/
│       ├── clientbound/          # S2C: HandshakeAck/JoinGame/ChunkData/BlockUpdate/
│       │                         #      EntityMove/PlayerInfo/ChatMessage/Disconnect/KeepAlive
│       └── serverbound/          # C2S: ClientHandshake/PlayerPositionLook/
│                                 #      PlayerDigging/BlockPlace/ChatMessage/KeepAlive
├── auth/
│   ├── CFMCAuthService.java      # 策略接口
│   ├── AuthResult.java           # 认证结果载体
│   ├── CFMCAuthScreen.java       # 登录 GUI (P键打开)
│   └── impl/                     # Online / Offline / SkinServer / Hybrid
├── world/
│   └── CFMCChunkLoader.java      # Cesium 区块解码 (调色板+LongArray互逆)
├── mixin/
│   ├── ClientConnectionMixin.java          # 拦截原版连接
│   ├── ClientPlayNetworkHandlerMixin.java  # 拦截原版包处理
│   └── MinecraftClientMixin.java           # tick 挂钩
└── render/
    ├── CFMCHudOverlay.java       # 状态/区域 HUD
    └── CFMCDisconnectScreen.java # 断线界面
```

## 协议对齐清单（两端必须同步修改）

| 项 | 服务端 | 客户端 |
|----|--------|--------|
| VarInt 编码 | `packet-reader/writer.js` | `CFMCBufferUtils.java` |
| 帧格式 | `packet-writer.js frame()` | `PacketWriter.frame()` |
| 包 ID 表 | `packet-definitions.js` | `CFMCConstants.java` |
| ChunkData 调色板 | `cesium-reader/writer.js` | `CFMCChunkLoader.decodeBlockIndices` |
| 离线 UUID | `offline-uuid.js` | JDK `UUID.nameUUIDFromBytes` |
| 压缩 | `compression.js` (deflate-raw) | `CFMCCompression` (Inflater nowrap) |

**任何一端变更必须同步另一端并递增 `PROTOCOL_VERSION`。**

## 开发状态

### ✅ Phase 1（当前）

- [x] WebSocket 通道 + 二进制协议帧解析（多帧复用）
- [x] 握手/JoinGame/聊天/心跳/断开 九个 S2C + 六个 C2S 包
- [x] Cesium 区块解码（调色板 + LongArray 位级互逆已验证）
- [x] 四种认证策略 + 登录 GUI
- [x] Mixin 拦截点（连接/包处理/tick）

### 🔜 Phase 2

- [ ] 3D 世界渲染接入（ClientWorld 灌入 Cesium 区块）
- [ ] 第一人称移动 + 位置差量上报（相对坐标 flags）
- [ ] 实体插值渲染（CFMCInterpolationHelper）
- [ ] 客户端预测 + 服务端校正
- [ ] 相对方块状态 ID 映射表（1.20.4 全量生成脚本）

## 已知限制

1. **Mixin 方法签名**基于 yarn 1.20.4 编写，映射更新后需比对调整
2. 正版认证的 join→hasJoined 全流程在 Phase 3（当前 online 模式为占位）
3. 未实现压缩发送方向（服务端已就绪 deflate-raw，客户端解压方向已实现）
4. WS 回调在 IO 线程执行，包 handle() 中的世界操作需 `MinecraftClient.execute` 切线程（Phase 2 统一处理）

## License

Apache-2.0
