package com.example.t1dalert.Preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.t1dalert.Core.AppPrefsStore;


public final class EncryptedPreferencesRepository implements PreferencesRepository {
    private final SharedPreferences delegate;

    public EncryptedPreferencesRepository(Context context) {
        this.delegate = AppPrefsStore.get(context);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return delegate.getString(key, defaultValue);
    }

    @Override
    public void putString(String key, String value) {
        delegate.edit().putString(key, value).apply();
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return delegate.getBoolean(key, defaultValue);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        delegate.edit().putBoolean(key, value).apply();
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return delegate.getInt(key, defaultValue);
    }

    @Override
    public void putInt(String key, int value) {
        delegate.edit().putInt(key, value).apply();
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return delegate.getLong(key, defaultValue);
    }

    @Override
    public void putLong(String key, long value) {
        delegate.edit().putLong(key, value).apply();
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return delegate.getFloat(key, defaultValue);
    }

    @Override
    public void putFloat(String key, float value) {
        delegate.edit().putFloat(key, value).apply();
    }

    @Override
    public void apply() {
    }

    @Override
    public Editor edit() {
        return new EditorImpl(delegate.edit());
    }

    private static class EditorImpl implements Editor {
        private final SharedPreferences.Editor delegate;

        private EditorImpl(SharedPreferences.Editor delegate) {
            this.delegate = delegate;
        }

        @Override
        public Editor putString(String key, String value) {
            delegate.putString(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            delegate.putBoolean(key, value);
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            delegate.putInt(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            delegate.putLong(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            delegate.putFloat(key, value);
            return this;
        }

        @Override
        public void apply() {
            delegate.apply();
        }
    }
}
