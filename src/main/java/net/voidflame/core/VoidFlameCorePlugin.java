package net.voidflame.core;

import net.voidflame.core.api.ServiceRegistry;
import net.voidflame.core.config.CoreConfig;
import net.voidflame.core.logging.CoreLogger;
import net.voidflame.core.scheduler.CoreScheduler;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class VoidFlameCorePlugin extends JavaPlugin {
    private CoreConfig configuration;
    private CoreLogger coreLogger;
    private CoreScheduler scheduler;
    private ServiceRegistry services;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        configuration = new CoreConfig(getConfig());
    }

    @Override
    public void onEnable() {
        coreLogger = new CoreLogger(getLogger(), configuration.debug());
        scheduler = new CoreScheduler(this);
        services = new ServiceRegistry();

        getServer().getServicesManager().register(
                ServiceRegistry.class,
                services,
                this,
                ServicePriority.Highest
        );

        coreLogger.info("VoidFlame-Core enabled.");
    }

    @Override
    public void onDisable() {
        if (services != null) {
            services.clear();
        }
        if (scheduler != null) {
            scheduler.cancelAll();
        }
        if (coreLogger != null) {
            coreLogger.info("VoidFlame-Core disabled.");
        }
    }

    public CoreConfig configuration() {
        return configuration;
    }

    public CoreLogger coreLogger() {
        return coreLogger;
    }

    public CoreScheduler scheduler() {
        return scheduler;
    }

    public ServiceRegistry services() {
        return services;
    }
}
