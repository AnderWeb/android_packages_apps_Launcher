package com.android.launcher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class DesktopIndicator extends ViewGroup implements AnimationListener {
	private View mIndicator;
	public static final int INDICATOR_TYPE_PAGER=1;
	public static final int INDICATOR_TYPE_SLIDER_TOP=2;
	public static final int INDICATOR_TYPE_SLIDER_BOTTOM=3;
	private static final int INDICATOR_DEFAULT_COLOR=0x99FFFFFF;
	private int mIndicatorType=1;
	private int mItems=5;
	private int mCurrent=0;
	private int mIndicatorColor=INDICATOR_DEFAULT_COLOR;
	private int mVisibleTime=300;
	private Animation mAnimation;
	private Handler mHandler=new Handler();
	public DesktopIndicator(Context context) {
		super(context);
		loadThemeColors(context);
		initIndicator(context);
	}

	public DesktopIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);
		loadThemeColors(context);
		initIndicator(context);
	}

	public DesktopIndicator(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		loadThemeColors(context);
		initIndicator(context);
	}
	private void initIndicator(Context context){
		switch(mIndicatorType){
		case INDICATOR_TYPE_PAGER:
			mIndicator=new PreviewPager(context);
			((PreviewPager) mIndicator).setTotalItems(mItems);
			((PreviewPager) mIndicator).setCurrentItem(mCurrent);
			break;
		case INDICATOR_TYPE_SLIDER_TOP:
		case INDICATOR_TYPE_SLIDER_BOTTOM:
			mIndicator=new SliderIndicator(context);
			break;
		}
		addView(mIndicator);
	}
	public void setItems(int items){
		mItems=items;
		switch(mIndicatorType){
		case INDICATOR_TYPE_PAGER:
			((PreviewPager) mIndicator).setTotalItems(mItems);
			((PreviewPager) mIndicator).setCurrentItem(mCurrent);
		break;
		case INDICATOR_TYPE_SLIDER_TOP:
		case INDICATOR_TYPE_SLIDER_BOTTOM:
			((SliderIndicator)mIndicator).setTotalItems(mItems);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		int realHeight=SliderIndicator.INDICATOR_HEIGHT;
		switch(mIndicatorType){
		case INDICATOR_TYPE_PAGER:
			realHeight=20;
	        mIndicator.measure(getWidth(), realHeight);
			break;
		case INDICATOR_TYPE_SLIDER_BOTTOM:
		case INDICATOR_TYPE_SLIDER_TOP:
	        mIndicator.measure(getWidth(), realHeight);
			break;
		}
		int realHeightMeasurespec=MeasureSpec.makeMeasureSpec(realHeight, MeasureSpec.EXACTLY);
		super.onMeasure(widthMeasureSpec, realHeightMeasurespec);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		LinearLayout.LayoutParams params;
		switch(mIndicatorType){
		case INDICATOR_TYPE_PAGER:
	        params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
	        mIndicator.measure(getWidth(), 20);
	        mIndicator.setLayoutParams(params);
	        mIndicator.layout(0, 0, getWidth(), 20);
			break;
		case INDICATOR_TYPE_SLIDER_BOTTOM:
		case INDICATOR_TYPE_SLIDER_TOP:
	        params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
	        mIndicator.measure(getWidth(), SliderIndicator.INDICATOR_HEIGHT);
	        mIndicator.setLayoutParams(params);
	        mIndicator.layout(0, 0, getWidth(), SliderIndicator.INDICATOR_HEIGHT);
	        break;
		}
	}
	public void indicate(float percent){
		setVisibility(View.VISIBLE);
		int position=Math.round(mItems*percent);
		switch(mIndicatorType){
		case INDICATOR_TYPE_PAGER:
			((PreviewPager) mIndicator).setCurrentItem(position);
			break;
		case INDICATOR_TYPE_SLIDER_BOTTOM:
		case INDICATOR_TYPE_SLIDER_TOP:
			int offset=((int) (getWidth()*percent))-mIndicator.getLeft();
			((SliderIndicator)mIndicator).setOffset(offset);
			mIndicator.invalidate();
		}
        mHandler.removeCallbacks(mAutoHide);
        if(mVisibleTime>0)
        	mHandler.postDelayed(mAutoHide, mVisibleTime);
		mCurrent=position;
	}
	public void fullIndicate(int position){
		setVisibility(View.VISIBLE);
		switch(mIndicatorType){
		case INDICATOR_TYPE_PAGER:
			((PreviewPager) mIndicator).setCurrentItem(position);
			break;
		case INDICATOR_TYPE_SLIDER_BOTTOM:
		case INDICATOR_TYPE_SLIDER_TOP:
		}
        mHandler.removeCallbacks(mAutoHide);
        if(mVisibleTime>0)
        	mHandler.postDelayed(mAutoHide, mVisibleTime);
        mCurrent=position;
	}
	public void setType(int type){
		if(type!=mIndicatorType){
			FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(this.getLayoutParams());
			if(type==INDICATOR_TYPE_SLIDER_BOTTOM){
				lp.gravity=Gravity.BOTTOM;
			}else{
				lp.gravity=Gravity.TOP;
			}
			setLayoutParams(lp);
			mIndicatorType=type;
			removeView(mIndicator);
			initIndicator(getContext());
		}
	}
	public void setAutoHide(boolean autohide){
		if(autohide){
			mVisibleTime=300;
			setVisibility(INVISIBLE);
		}else{
			mVisibleTime=-1;
			setVisibility(VISIBLE);
		}
	}
	private Runnable mAutoHide = new Runnable() {
		   public void run() {
			   if(mAnimation==null){
				   mAnimation=AnimationUtils.loadAnimation(getContext(), R.anim.fade_out_fast);
				   mAnimation.setAnimationListener(DesktopIndicator.this);
			   }else{
				   try{
					   //This little thing seems to be making some androids piss off
					   if(!mAnimation.hasEnded())mAnimation.cancel();
				   }catch (NoSuchMethodError e) {
				   }
			   }
			   startAnimation(mAnimation);				   
		   }
		};	
	
	
	/**
	 * Simple line Indicator for desktop scrolling
	 * @author adw
	 *
	 */
	private class SliderIndicator extends View{
		public static final int INDICATOR_HEIGHT=4;
		private RectF mRect;
		private Paint mPaint;
		private int mTotalItems=5;
		public SliderIndicator(Context context) {
			super(context);
			mPaint=new Paint();
			mPaint.setColor(mIndicatorColor);
			mRect=new RectF(0, 0, 5, INDICATOR_HEIGHT);
		}
		@Override
		public void draw(Canvas canvas) {
			canvas.drawRoundRect(mRect, 2, 2, mPaint);
		}
		public void setTotalItems(int items){
			mTotalItems=items;
		}
		public void setOffset(int offset){
			int width=getWidth()/mTotalItems;
			mRect.left=offset;
			mRect.right=offset+width;
		}
	}


	public void onAnimationEnd(Animation animation) {
		setVisibility(View.INVISIBLE);
	}

	public void onAnimationRepeat(Animation animation) {
	}

	public void onAnimationStart(Animation animation) {
	}

	public void hide() {
		// TODO Auto-generated method stub
		setVisibility(View.INVISIBLE);
	}

	public void show() {
		// TODO Auto-generated method stub
		if(mVisibleTime<0){
			setVisibility(View.VISIBLE);
		}
	}
	private void loadThemeColors(Context context){
    	//ADW: Load the specified theme
    	String themePackage=AlmostNexusSettingsHelper.getThemePackageName(context, Launcher.THEME_DEFAULT);
    	Resources themeResources=null;
    	if(!themePackage.equals(Launcher.THEME_DEFAULT)){
        	PackageManager pm=context.getPackageManager();
	    	try {
				themeResources=pm.getResourcesForApplication(themePackage);
				int desktop_indicator_color_id=themeResources.getIdentifier("desktop_indicator_color", "color", themePackage);
				if(desktop_indicator_color_id!=0){
					mIndicatorColor=themeResources.getColor(desktop_indicator_color_id);
				}
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
			}
    	}
	}
}
