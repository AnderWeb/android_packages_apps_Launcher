package com.android.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.FontMetrics;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CounterImageView extends ImageView {
    //ADW custom notifier counters
    private String mCounter=null;
    private int mCounterSize=0;
    private int mCounterPadding=0;
    private final Rect mRect2 = new Rect();
    private Paint mStrokePaint;
    private Paint mTextPaint;
    private FontMetrics fm;
    private int mBubbleColor=0xFF00FF00;
    private int mBubbleColor2=0xFFFF6666;
    public CounterImageView(Context context) {
        super(context);
        init();
    }

    public CounterImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CounterImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }
    private void init(){
        final float scale =getResources().getDisplayMetrics().density;
        final int fontSize = (int)(AlmostNexusSettingsHelper.getNotifSize(getContext()) * scale + 0.5f);

        mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setARGB(255, 255, 255, 255);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(fontSize);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setShadowLayer(2f, 1, 1, 0xFF000000);
        fm=mTextPaint.getFontMetrics();
        mCounterPadding=getContext().getResources().getDimensionPixelSize(R.dimen.counter_circle_padding);
    }
    public void setCounter(int counter, int color){
        if(color!=0 && color!=mBubbleColor){
            mBubbleColor=color;
            float[] hsv = new float[3];
            Color.colorToHSV(mBubbleColor, hsv);
            hsv[2]=.1f;
            mBubbleColor2=Color.HSVToColor(hsv);
        }
        if(counter>0){
            mCounter=String.valueOf(counter);
            mTextPaint.getTextBounds(mCounter, 0, mCounter.length(), mRect2);
            mCounterSize=(Math.max(mRect2.width(), mRect2.height())/2)+mCounterPadding;
            RadialGradient shader = new RadialGradient(0, -mCounterSize, mCounterSize*1.5f, mBubbleColor,mBubbleColor2, TileMode.MIRROR);
            mStrokePaint.setShader(shader);
        }else if(counter==-1){
            mCounter="?";
            mTextPaint.getTextBounds(mCounter, 0, mCounter.length(), mRect2);
            mCounterSize=(Math.max(mRect2.width(), mRect2.height())/2)+mCounterPadding;
            RadialGradient shader = new RadialGradient(0, -mCounterSize, mCounterSize*1.5f, mBubbleColor,mBubbleColor2, TileMode.MIRROR);
            mStrokePaint.setShader(shader);
        }else{
            mCounter=null;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        //OVERLAY COUNTERS
        if(mCounter!=null){
            canvas.save();
            canvas.translate(getScrollX()+getWidth()-(mCounterSize)-(mCounterPadding), getScrollY()+(mCounterSize)-(fm.top/2));
            mTextPaint.setAlpha(150);
            canvas.drawCircle(0, -mRect2.height()/2, mCounterSize+1, mTextPaint);
            canvas.drawCircle(0, -mRect2.height()/2, mCounterSize, mStrokePaint);
            mTextPaint.setAlpha(255);
            canvas.drawText(mCounter, 0, 0, mTextPaint);
            canvas.restore();
        }
    }
    
}
