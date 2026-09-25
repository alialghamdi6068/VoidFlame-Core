package net.voidflame.core;

import net.voidflame.core.api.ServiceRegistry;
import net.voidflame.core.config.CoreConfig;
import net.voidflame.core.logging.CoreLogger;
import net.voidflame.core.scheduler.CoreScheduler;
import net.voidflame.core.storage.DatabaseService;
import net.voidflame.core.storage.SqliteDatabaseService;
import net.voidflame.core.storage.StorageService;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class VoidFlameCorePlugin extends JavaPlugin {
    private CoreConfig configuration;
    private CoreLogger coreLogger;
    private CoreScheduler scheduler;
    private ServiceRegistry services;
    private DatabaseService database;
    private StorageService storage;

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

        try {
            database = new SqliteDatabaseService(getDataFolder());
            storage = new StorageService(database);
        } catch (RuntimeException ex) {
            coreLogger.error("Failed to initialize the VoidFlame database.", ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getServicesManager().register(ServiceRegistry.class, services, this, ServicePriority.Highest);
        getServer().getServicesManager().register(DatabaseService.class, database, this, ServicePriority.Highest);
        getServer().getServicesManager().register(StorageService.class, storage, this, ServicePriority.Highest);

        coreLogger.info("VoidFlame-Core enabled. Database: " + database.databasePath());
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregister(DatabaseService.class, this);
        getServer().getServicesManager().unregister(StorageService.class, this);
        getServer().getServicesManager().unregister(ServiceRegistry.class, this);
        if (services != null) services.clear();
        if (scheduler != null) scheduler.cancelAll();
        if (database != null) database.close();
        if (coreLogger != null) coreLogger.info("VoidFlame-Core disabled.");
    }

    public CoreConfig configuration() { return configuration; }
    public CoreLogger coreLogger() { return coreLogger; }
    public CoreScheduler scheduler() { return scheduler; }
    public ServiceRegistry services() { return services; }
    public DatabaseService database() { return database; }
    public StorageService storage() { return storage; }
}
