
package tv.cjump.jni;

import java.lang.reflect.Field;

import android.graphics.Bitmap;
import android.util.Log;

public class NativeBitmapFactory {

    static Field nativeIntField = null;

    static Field nativeBitmapField;

    static boolean nativeLibLoaded = false;

    static {
        try {
            int sdkInt = android.os.Build.VERSION.SDK_INT;
            if (sdkInt >= 11 && sdkInt <= 13) {
                System.loadLibrary("ndkbitmap.11");
                nativeLibLoaded = true;
            } else if (android.os.Build.VERSION.SDK_INT == 14) {
                System.loadLibrary("ndkbitmap.14");
                nativeLibLoaded = true;
            } else if (android.os.Build.VERSION.SDK_INT == 15) {
                System.loadLibrary("ndkbitmap.15");
                nativeLibLoaded = true;
            } else if (android.os.Build.VERSION.SDK_INT == 16) {
                System.loadLibrary("ndkbitmap.16");
                nativeLibLoaded = true;
            } else if (android.os.Build.VERSION.SDK_INT == 17) {
                System.loadLibrary("ndkbitmap.17");
                nativeLibLoaded = true;
            } else if (android.os.Build.VERSION.SDK_INT == 18) {
                System.loadLibrary("ndkbitmap.18");
                nativeLibLoaded = true;
            } else if (android.os.Build.VERSION.SDK_INT == 19) {
                System.loadLibrary("ndkbitmap.19");
                nativeLibLoaded = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            nativeLibLoaded = false;
        } catch (Error e){
            e.printStackTrace();
            nativeLibLoaded = false;
        }
        if (nativeLibLoaded) {
            initField();
            boolean confirm = testLib();
            if (!confirm) {
                // 测试so文件函数调用失败
                nativeLibLoaded = false;
            }
        }        
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

    private static boolean testLib() {
        Bitmap bitmap = null;
        try {
            bitmap = createBitmap(2, 2, Bitmap.Config.ARGB_8888);
            boolean result = bitmap != null && bitmap.getWidth() == 2 && bitmap.getHeight() == 2;
            if(result && android.os.Build.VERSION.SDK_INT>=17){
                result = bitmap.isPremultiplied();
            }
            return result;
        } catch (Exception e) {
            return false;
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }
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

    public static int getNativeBitmap(Bitmap bitmap) {
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

    public static void recycle(Bitmap bitmap) {
        bitmap.recycle();
    }

    public static Bitmap createBitmap(int width, int height, Bitmap.Config config, boolean hasAlpha) {
        if (!nativeLibLoaded) {
            return Bitmap.createBitmap(width, height, config);
        }

        int nativeConfig = getNativeConfig(config);
        if (nativeConfig == 0) {
            return null;
        }
        return android.os.Build.VERSION.SDK_INT == 19 ? createBitmap19(width, height, nativeConfig, hasAlpha) : createBitmap(width, height, nativeConfig, hasAlpha);
    }

    // ///////////native methods//////////

    private static native Bitmap createBitmap(int width, int height, int nativeConfig,
            boolean hasAlpha);

    private static native Bitmap createBitmap19(int width, int height, int nativeConfig,
            boolean hasAlpha);

}
