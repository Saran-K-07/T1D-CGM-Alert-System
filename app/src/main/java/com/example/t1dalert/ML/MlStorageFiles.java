package com.example.t1dalert.ML;

import android.content.Context;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class MlStorageFiles {

    private static final String DIR_NAME = "ml_runtime";
    private static final String HISTORY_FILE = "history.json";

    private MlStorageFiles() {
    }

    static JSONArray readArray(Context context, String fileName) {
        File file = resolveFile(context, fileName);
        if (!file.exists()) {
            return new JSONArray();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.openFileInput(DIR_NAME + "/" + fileName), StandardCharsets.UTF_8))) {
            StringBuilder raw = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                raw.append(line);
            }
            if (raw.length() == 0) {
                return new JSONArray();
            }
            return new JSONArray(raw.toString());
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }

    static void writeArray(Context context, String fileName, JSONArray array) {
        File file = resolveFile(context, fileName);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            outputStream.write(array.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
        } catch (Exception ignored) {
        }
    }

    static File resolveHistoryFile(Context context) {
        return resolveFile(context, HISTORY_FILE);
    }

    private static File resolveFile(Context context, String fileName) {
        return new File(new File(context.getFilesDir(), DIR_NAME), fileName);
    }
}
