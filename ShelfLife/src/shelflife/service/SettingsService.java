package shelflife.service;

import shelflife.model.AppSettings;

/**
 * Service contract for managing application preferences, shop name,
 * tax rates, and alert thresholds.
 */
public interface SettingsService {

    AppSettings getSettings();

    void updateSettings(AppSettings settings) throws Exception;

    void reload();
}
