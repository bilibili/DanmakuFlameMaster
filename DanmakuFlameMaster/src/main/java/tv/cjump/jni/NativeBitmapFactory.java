
package tv.cjump.jni;

import java.lang.reflect.Field;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class NativeBitmapFactory {

    static Field nativeIntField = null;    

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
                if (android.os.Build.VERSION.RELEASE.equals("4.4")
                        || android.os.Build.VERSION.RELEASE.equals("4.4.0")
                        || android.os.Build.VERSION.RELEASE.equals("4.4.1")
                        || android.os.Build.VERSION.RELEASE.equals("4.4.2")
                        || android.os.Build.VERSION.RELEASE.equals("4.4.3")) {
                    System.loadLibrary("ndkbitmap.19");
                } else {
                    System.loadLibrary("ndkbitmap.19-2");
                }
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
        
//Log.e("NativeBitmapFactory", "loaded" + nativeLibLoaded);
    }

    static void initField() {
        try {
            nativeIntField = Bitmap.Config.class.getDeclaredField("nativeInt");
            nativeIntField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            nativeIntField = null;
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    private static boolean testLib() {
        if (nativeIntField == null) {
            return false;
        }
        Bitmap bitmap = null;
        Canvas canvas = null;
        try {
            bitmap = createBitmap(2, 2, Bitmap.Config.ARGB_8888);
            boolean result = (bitmap != null && bitmap.getWidth() == 2 && bitmap.getHeight() == 2);
            canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            canvas.drawRect(0f, 0f, (float)bitmap.getWidth(), (float)bitmap.getHeight(), paint);
            if(result && android.os.Build.VERSION.SDK_INT>=17){
                result = bitmap.isPremultiplied();
            }
            return result;
        } catch (Exception e) {
            return false;
        } catch (Error e){
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

    public static Bitmap createBitmap(int width, int height, Bitmap.Config config) {
        return createBitmap(width, height, config, config.equals(Bitmap.Config.ARGB_8888));
    }

    public static void recycle(Bitmap bitmap) {
        bitmap.recycle();
    }

    public static Bitmap createBitmap(int width, int height, Bitmap.Config config, boolean hasAlpha) {
        if (nativeLibLoaded == false || nativeIntField == null) {
//Log.e("NativeBitmapFactory", "ndk bitmap create failed");
            return Bitmap.createBitmap(width, height, config);
        }
        int nativeConfig = getNativeConfig(config);
//Log.e("NativeBitmapFactory", "nativeConfig:"+nativeConfig);
        return android.os.Build.VERSION.SDK_INT == 19 ? createBitmap19(width, height, nativeConfig, hasAlpha) : createBitmap(width, height, nativeConfig, hasAlpha);
    }

    // ///////////native methods//////////

    private static native Bitmap createBitmap(int width, int height, int nativeConfig,
            boolean hasAlpha);

    private static native Bitmap createBitmap19(int width, int height, int nativeConfig,
            boolean hasAlpha);

}
