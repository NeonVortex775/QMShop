package com.qingming.shop.util;

import java.util.logging.Logger;

/** Logger holder, so config parsing works before the plugin is enabled. */
public final class Log {

    private static Logger logger = Logger.getLogger("QingMingShop");

    private Log() {
    }

    public static void init(Logger log) {
        if (log != null) {
            logger = log;
        }
    }

    public static void warn(String msg) {
        logger.warning(msg);
    }

    public static void info(String msg) {
        logger.info(msg);
    }
}
