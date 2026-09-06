package com.cfmc.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 统一日志门面 — 走 SLF4J (Fabric Loader 自带), tag 统一为 [CFMC]
 */
public final class CFMCLogger {
    private CFMCLogger() {}

    public static final Logger LOGGER = LoggerFactory.getLogger("CFMC");

    public static void info(String msg) { LOGGER.info("[CFMC] {}", msg); }
    public static void warn(String msg) { LOGGER.warn("[CFMC] {}", msg); }
    public static void error(String msg, Throwable t) { LOGGER.error("[CFMC] {}", msg, t); }
}
