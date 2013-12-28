
package tv.cjump.jni;

import java.lang.reflect.Field;

import android.graphics.Bitmap;

public class NativeBitmapFactory {

    static Field nativeIntField = null;
    static Field nativeBitmapField;
    
    static {
        try {
            System.loadLibrary("ndkbitmap");
        } catch (Exception e) {
            e.printStackTrace();
        }
        initField();
    }


    static void initField() {
        try {
            nativeIntField = Bitmap.Config.class.getDeclaredField("nativeInt");
            nativeIntField.setAccessible(true);
            nativeBitmapField = Bitmap.class.getDeclaredField("mNativeBitmap");
            nativeBitmapField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            nativeIntField = null;
            e.printStackTrace();
        }
    }

    public static int getNativeConfig(Bitmap.Config config) {
        try {
            if (nativeIntField == null) {
                return 0;
            }
            int nativeInt = nativeIntField.getInt(config);
            return nativeInt;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public static int getNativeBitmap(Bitmap bitmap){
        try {
            if (nativeBitmapField == null) {
                return 0;
            }
            int nativeBitmap = nativeBitmapField.getInt(bitmap);
            return nativeBitmap;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Bitmap createBitmap(int width, int height, Bitmap.Config config) {
        return createBitmap(width, height, config, config.equals(Bitmap.Config.ARGB_8888));
    }
    
    public static void recycle(Bitmap bitmap){
        bitmap.recycle();
    }

    public static Bitmap createBitmap(int width, int height, Bitmap.Config config, boolean hasAlpha) {
        int nativeConfig = getNativeConfig(config);
        if (nativeConfig == 0) {
            return null;
        }
        return createBitmap(width, height, nativeConfig, hasAlpha);
    }

    private static native Bitmap createBitmap(int width, int height, int nativeConfig,
            boolean hasAlpha);

}
