package tv.cjump.jni;

import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Debug;
import master.flame.danmaku.danmaku.util.SystemClock;
import android.test.InstrumentationTestCase;
import android.util.Log;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by ch on 15-6-12.
 */
public class NativeBitmapFactoryTest extends InstrumentationTestCase {

    private static final int DEFAULT_MESSAGE_SIZE = 1024;
    private static final int BYTES_IN_MEGABYTE = 1024 * 1024;

    private static final int BITMAP_WIDTH = 200;
    private static final int BITMAP_HEIGHT = 200;
    private static final String TAG = NativeBitmapFactoryTest.class.getSimpleName();

    public void testLoadLibs() {
        NativeBitmapFactory.loadLibs();
        boolean isInNativeAlloc = NativeBitmapFactory.isInNativeAlloc();
        Assert.assertTrue("NativeBitmapFactory is not supported on your OS", isInNativeAlloc);
    }

    public void testNativeBitmap() {
        Bitmap bitmap = NativeBitmapFactory.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
        accessBitmap(bitmap);
        bitmap.recycle();
        gcAndWait();
    }

    public void testDalvikBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
        accessBitmap(bitmap);
        bitmap.recycle();
        gcAndWait();
    }

    public void testNativeBitmaps() {
        StringBuilder sb = new StringBuilder(DEFAULT_MESSAGE_SIZE);
        appendValue(sb, "\n\n", "===== before create 50 NativeBitmap", "\n\n");
        updateHeapValue(sb);
        final String message = sb.toString();
        Log.i(TAG, message);
        sb = new StringBuilder();
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            Bitmap bitmap = NativeBitmapFactory.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
            accessBitmap(bitmap);
            bitmaps.add(bitmap);
        }
        updateHeapValue(sb);
        Log.d(TAG, sb.toString());
        for (Bitmap bitmap : bitmaps) {
            bitmap.recycle();
        }
        gcAndWait();
    }

    public Context getContext(){
        return getInstrumentation().getTargetContext();
    }

    private void updateHeapValue(StringBuilder sb) {

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long availableMegs = mi.availMem;
        final Runtime runtime = Runtime.getRuntime();
        final long heapMemory = runtime.totalMemory() - runtime.freeMemory();
        // When changing format of output below, make sure to sync "scripts/test_runner.py" as well.
        appendSize(sb, "System availMem:          ", availableMegs, "\n");
        appendSize(sb, "Java heap size:          ", heapMemory, "\n");
        appendSize(sb, "Native heap size:        ", Debug.getNativeHeapSize(), "\n");
    }

    public void testDalvikBitmaps() {
        StringBuilder sb = new StringBuilder(DEFAULT_MESSAGE_SIZE);
        appendValue(sb, "\n\n", "===== before create 50 DalvikBitmap", "\n\n");
        updateHeapValue(sb);
        final String message = sb.toString();
        Log.i(TAG, message);
        sb = new StringBuilder();
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            Bitmap bitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
            accessBitmap(bitmap);
            bitmaps.add(bitmap);
        }
        updateHeapValue(sb);
        Log.d(TAG, sb.toString());
        for (Bitmap bitmap : bitmaps) {
            bitmap.recycle();
        }
        gcAndWait();
    }

    public void testReleaseLibs() {
        NativeBitmapFactory.releaseLibs();
    }


    private void accessBitmap(Bitmap bitmap) {
        boolean result = (bitmap != null && bitmap.getWidth() == BITMAP_WIDTH && bitmap.getHeight() == BITMAP_HEIGHT);
        if (result) {
            if (android.os.Build.VERSION.SDK_INT >= 17 && !bitmap.isPremultiplied()) {
                bitmap.setPremultiplied(true);
            }
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setTextSize(20f);
            canvas.drawRect(0f, 0f, (float) bitmap.getWidth(), (float) bitmap.getHeight(),
                    paint);
            canvas.drawText("TestLib", 0, 0, paint);
        }
    }

    private void gcAndWait() {
        System.gc();
        SystemClock.sleep(10000);
//        try {
//            Debug.dumpHprofData("/sdcard/nbf_test_dump.hprof");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private static void appendSize(StringBuilder sb, String prefix, long bytes, String suffix) {
        String value = String.format(Locale.getDefault(), "%.2f", (float) bytes / BYTES_IN_MEGABYTE);
        appendValue(sb, prefix, value + " MB", suffix);
    }

    private static void appendTime(StringBuilder sb, String prefix, long timeMs, String suffix) {
        appendValue(sb, prefix, timeMs + " ms", suffix);
    }

    private static void appendNumber(StringBuilder sb, String prefix, long number, String suffix) {
        appendValue(sb, prefix, number + "", suffix);
    }

    private static void appendValue(StringBuilder sb, String prefix, String value, String suffix) {
        sb.append(prefix).append(value).append(suffix);
    }

}