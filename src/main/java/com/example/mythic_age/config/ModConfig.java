package com.example.mythic_age.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模组配置类，管理调试输出和性能设置
 */
public class ModConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("MythicAge");
    
    // 调试开关
    public static final boolean DEBUG_MODE = false; // 生产环境设为false
    public static final boolean DEBUG_METEOR = false;
    public static final boolean DEBUG_TRADING = false;
    public static final boolean DEBUG_VILLAGE_GEN = false;
    public static final boolean DEBUG_TEMPLE_GUARDIAN = false;
    public static final boolean DEBUG_AI = false;
    
    // 性能设置
    public static final int MAX_METEORS_PER_SPELL = 5;
    public static final int PARTICLE_DISTANCE_THRESHOLD = 32; // 超过此距离减少粒子
    public static final int MAX_DEBUG_MESSAGES_PER_TICK = 10;
    
    // 调试计数器
    private static int debugMessageCount = 0;
    private static long lastResetTime = System.currentTimeMillis();
    
    /**
     * 安全的调试输出方法
     */
    public static void debugLog(String category, String message) {
        if (!DEBUG_MODE) return;
        
        // 防止调试消息过多
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastResetTime > 1000) { // 每秒重置
            debugMessageCount = 0;
            lastResetTime = currentTime;
        }
        
        if (debugMessageCount >= MAX_DEBUG_MESSAGES_PER_TICK) {
            return;
        }
        
        debugMessageCount++;
        LOGGER.info("[{}] {}", category, message);
    }
    
    /**
     * 陨石调试输出
     */
    public static void meteorDebug(String message) {
        if (DEBUG_METEOR) {
            debugLog("陨石", message);
        }
    }
    
    /**
     * 交易调试输出
     */
    public static void tradingDebug(String message) {
        if (DEBUG_TRADING) {
            debugLog("交易", message);
        }
    }
    
    /**
     * 村庄生成调试输出
     */
    public static void villageDebug(String message) {
        if (DEBUG_VILLAGE_GEN) {
            debugLog("村庄生成", message);
        }
    }
    
    /**
     * 神殿守护者调试输出
     */
    public static void guardianDebug(String message) {
        if (DEBUG_TEMPLE_GUARDIAN) {
            debugLog("神殿守护者", message);
        }
    }
    
    /**
     * AI调试输出
     */
    public static void aiDebug(String message) {
        if (DEBUG_AI) {
            debugLog("AI", message);
        }
    }
    
    /**
     * 错误日志
     */
    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }
    
    /**
     * 警告日志
     */
    public static void warn(String message) {
        LOGGER.warn(message);
    }
    
    /**
     * 信息日志
     */
    public static void info(String message) {
        LOGGER.info(message);
    }
}