package com.sab.pawcare.util;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public final class PhotoStore {
    private PhotoStore() {}

    public static String copyIntoApp(Context context, Uri source) {
        File dir = new File(context.getFilesDir(), "pet_photos");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        File dest = new File(dir, UUID.randomUUID().toString() + ".jpg");
        try (InputStream in = context.getContentResolver().openInputStream(source);
             FileOutputStream out = new FileOutputStream(dest)) {
            if (in == null) return null;
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return dest.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
