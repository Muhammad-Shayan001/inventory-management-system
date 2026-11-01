package shelflife.service;

import shelflife.model.AppSettings;
import shelflife.storage.DataStore;

/**
 * Implementation of SettingsService with atomic CSV synchronization.
 */
public class SettingsServiceImpl implements SettingsService {

    private final DataStore dataStore;
    private volatile AppSettings settings;

    public SettingsServiceImpl(DataStore dataStore) {
        if (dataStore == null) throw new IllegalArgumentException("DataStore cannot be null.");
        this.dataStore = dataStore;
        reload();
    }

    @Override
    public synchronized void reload() {
        try {
            this.settings = dataStore.loadSettings();
            if (this.settings == null) {
                this.settings = new AppSettings();
            }
        } catch (Exception e) {
            System.err.println("Error loading settings, using defaults: " + e.getMessage());
            this.settings = new AppSettings();
        }
    }

    @Override
    public AppSettings getSettings() {
        if (this.settings == null) {
            this.settings = new AppSettings();
        }
        return this.settings;
    }

    @Override
    public synchronized void updateSettings(AppSettings newSettings) throws Exception {
        if (newSettings == null) {
            throw new IllegalArgumentException("Settings cannot be null.");
        }
        this.settings = newSettings;
        dataStore.saveSettings(newSettings);
    }
}
