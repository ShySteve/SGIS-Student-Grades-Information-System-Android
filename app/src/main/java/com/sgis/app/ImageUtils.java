package com.sgis.app;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Helpers for the optional 1x1 student profile photo, stored in-memory as a Base64 JPEG string. */
public class ImageUtils {

    /** Loads a picked image, center-crops it to a square, and downsizes it for memory-friendly in-memory storage. */
    public static Bitmap loadSquareBitmap(ContentResolver resolver, Uri uri, int targetSize) throws IOException {
        Bitmap original;
        try (InputStream in = resolver.openInputStream(uri)) {
            original = BitmapFactory.decodeStream(in);
        }
        if (original == null) return null;

        int size = Math.min(original.getWidth(), original.getHeight());
        int x = (original.getWidth() - size) / 2;
        int y = (original.getHeight() - size) / 2;
        Bitmap square = Bitmap.createBitmap(original, x, y, size, size);
        if (square != original) original.recycle();

        if (size != targetSize) {
            Bitmap scaled = Bitmap.createScaledBitmap(square, targetSize, targetSize, true);
            if (scaled != square) square.recycle();
            return scaled;
        }
        return square;
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    public static Bitmap base64ToBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
