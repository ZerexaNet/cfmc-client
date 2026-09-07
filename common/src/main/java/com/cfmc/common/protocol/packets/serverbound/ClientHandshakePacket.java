package com.cfmc.common.protocol.packets.serverbound;

import com.cfmc.common.util.CFMCConstants;
import com.cfmc.common.version.CFMCVersionRegistry;
import com.cfmc.common.protocol.CFMCPacket;
import com.cfmc.common.protocol.PacketReader;
import com.cfmc.common.protocol.PacketWriter;

/**
 * ClientHandshake (0x10, C→S) — 握手请求 (连接后第一个包)
 *
 * payload (v2):
 *   protocolVersion(VarInt)   CFMC 协议版本
 *   playerName(String≤16)     玩家名
 *   mcVersion(String≤32)      MC 版本串 (如 "1.20.4") ← v2 新增
 *   mcProtocol(VarInt)        MC 协议号 (如 765)     ← v2 新增, 服务端据此选适配器
 *
 * 版本中立关键: MC 版本信息让服务端知道"我是谁", 但线上字段不依赖版本;
 * 未上报时 mcProtocol=-1, 服务端 generic 适配器兜底, 不拒连。
 *
 * 认证上下文: JWT 由 HTTP 升级请求头 Authorization: Bearer 携带
 * (见 CFMCNetworkManager.connect), 因此二进制握手只需版本协商。
 */
public class ClientHandshakePacket extends CFMCPacket {
    public String playerName;
    public String mcVersion;
    public int mcProtocol;

    public ClientHandshakePacket(String playerName) {
        super(CFMCConstants.SB_CLIENT_HANDSHAKE);
        this.playerName = playerName;
        // 从平台抽象读取运行时 MC 版本 (loader 注入), 全版本通用的来源
        this.mcVersion = com.cfmc.common.platform.CFMCPlatformHolder.get().mcVersion();
        this.mcProtocol = CFMCVersionRegistry.currentProtocol();
    }

    public ClientHandshakePacket() {
        super(CFMCConstants.SB_CLIENT_HANDSHAKE);
    }

    @Override
    public void encode(PacketWriter writer) {
        writer.writeVarInt(CFMCConstants.PROTOCOL_VERSION);
        writer.writeString(playerName, 16);
        writer.writeString(mcVersion == null ? "unknown" : mcVersion, 32);
        writer.writeVarInt(Math.max(mcProtocol, 0)); // -1(未知) 编码为 0 = 服务端 generic 兑底
    }

    @Override
    public void decode(PacketReader reader) { /* C2S 不解码 */ }

    @Override
    public void handle() { /* C2S */ }
}
