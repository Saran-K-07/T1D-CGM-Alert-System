package com.example.t1dalert.Preferences;

public interface PreferencesRepository {
    String getString(String key, String defaultValue);
    void putString(String key, String value);
    boolean getBoolean(String key, boolean defaultValue);
    void putBoolean(String key, boolean value);
    int getInt(String key, int defaultValue);
    void putInt(String key, int value);
    long getLong(String key, long defaultValue);
    void putLong(String key, long value);
    float getFloat(String key, float defaultValue);
    void putFloat(String key, float value);
    void apply();
    Editor edit();

    interface Editor {
        Editor putString(String key, String value);
        Editor putBoolean(String key, boolean value);
        Editor putInt(String key, int value);
        Editor putLong(String key, long value);
        Editor putFloat(String key, float value);
        void apply();
    }
}
