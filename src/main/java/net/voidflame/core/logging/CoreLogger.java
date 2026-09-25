package net.voidflame.core.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class CoreLogger {
    private final Logger logger;
    private final boolean debug;

    public CoreLogger(Logger logger, boolean debug) {
        this.logger = logger;
        this.debug = debug;
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warning(String message) {
        logger.warning(message);
    }

    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    public void debug(String message) {
        if (debug) {
            logger.info("[DEBUG] " + message);
        }
    }
}
