package com.example.t1dalert;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

final class MlAssetLoader {

    private MlAssetLoader() {
    }

    static String loadMetadataJson(Context context) {
        try (InputStream in = context.getAssets().open(AppConfig.ML_METADATA_ASSET_PATH);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new IllegalStateException("metadata_asset_missing", e);
        }
    }

    static File prepareModelFile(Context context, String modelFileName) {
        String assetPath = "ml/" + modelFileName;
        File target = new File(context.getFilesDir(), modelFileName);
        if (target.exists() && target.length() > 0) {
            return target;
        }

        try (InputStream in = context.getAssets().open(assetPath);
             FileOutputStream out = new FileOutputStream(target, false)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            out.flush();
            return target;
        } catch (Exception e) {
            throw new IllegalStateException("model_asset_missing", e);
        }
    }
}
