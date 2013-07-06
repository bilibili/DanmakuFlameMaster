package tv.light.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by ch on 13-7-5.
 */
public class TestSurfaceView extends SurfaceView implements SurfaceHolder.Callback{

    private SurfaceHolder mSurfaceHolder;
    private Paint paint;

    public TestSurfaceView(Context context) {
        super(context);
        init();
    }

    public TestSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TestSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setTextSize(50);
        setZOrderOnTop(true);
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
    }

    void drawText(Canvas canvas){

        canvas.drawText("hello",50,50,paint);

    }

    void drawCanvas(){
        Canvas canvas = mSurfaceHolder.lockCanvas();
        if(canvas!=null){
            drawText(canvas);
            mSurfaceHolder.unlockCanvasAndPost(canvas);
        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        drawCanvas();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        drawCanvas();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}
