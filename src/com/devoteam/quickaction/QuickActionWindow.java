
package com.devoteam.quickaction;

import com.android.launcher.R;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.PopupWindow;

/**
 * A class that can display, as a popup badge, a collection 
 * of QuickActionItems
 * 
 * Based on the great work done by Mohd Faruq
 *
 */
public class QuickActionWindow extends PopupWindow implements KeyEvent.Callback {
	private final Context mContext;
	private final LayoutInflater mInflater;
	private final WindowManager mWindowManager;
	
	View contentView;
	
	private int mScreenWidth;
	private int mShadowHoriz;
	private ImageView mArrowUp;
	private ImageView mArrowDown;
	private ViewGroup mTrack;
	private Animation mTrackAnim;

	private View mPView;
	private Rect mAnchor;
		
    /**
     * Creates a new Instance of the QuickActionWindow
     * 
     * @param context Context to use, usually your Appication or your Activity
     * @param pView The view you want to anchor the window on (the parent)
     * @param rect Rectangle defining the view area
     */
	public QuickActionWindow(Context context, View pView, Rect rect) {
		super(context);
		
		mPView = pView;
		mAnchor = rect;
		
		mContext = context;
		mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		mInflater = ((Activity)mContext).getLayoutInflater();
		
		contentView = mInflater.inflate(R.layout.quickaction, null);
		super.setContentView(contentView);
		
		mScreenWidth = mWindowManager.getDefaultDisplay().getWidth();
		
		setWindowLayoutMode(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		
		final Resources res = mContext.getResources();
		mShadowHoriz = res.getDimensionPixelSize(R.dimen.quickaction_shadow_horiz);
		
		setWidth(mScreenWidth + mShadowHoriz + mShadowHoriz);
		setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		
		setBackgroundDrawable(new ColorDrawable(0));
		
		mArrowUp = (ImageView) contentView.findViewById(R.id.arrow_up);
		mArrowDown = (ImageView) contentView.findViewById(R.id.arrow_down);
		
		mTrack = (ViewGroup) contentView.findViewById(R.id.quickaction);
		
		setFocusable(true);
		setTouchable(true);
		setOutsideTouchable(true);
		
		// Prepare track entrance animation
		mTrackAnim = AnimationUtils.loadAnimation(mContext, R.anim.quickaction);
		mTrackAnim.setInterpolator(new Interpolator() {
			public float getInterpolation(float t) {
				// Pushes past the target area, then snaps back into place.
				// Equation for graphing: 1.2-((x*1.6)-1.1)^2
				final float inner = (t * 1.55f) - 1.1f;
				return 1.2f - inner * inner;
			}
		});	
	}
	
	/**
	 * Adds an item to the QuickActionWindow
	 * 
	 * @param drawable Icon to be shown
	 * @param text Label to be shown below the drawable
	 * @param l Definition for the callback to be invoked when the view is cliked
	 */
	public void addItem(Drawable drawable, String text, OnClickListener l) {
		QuickActionItem view = (QuickActionItem) mInflater.inflate(R.layout.quickaction_item, mTrack, false);
		view.setChecked(false);
		view.setImageDrawable(drawable);
		view.setText(text);
		view.setOnClickListener(l);
		
		final int index = mTrack.getChildCount() - 1;
		mTrack.addView(view, index);
	}
	
	/**
	 * Adds an item to the QuickActionWindow
	 * 
	 * @param drawable Icon resource id to be shown
	 * @param text Label to be shown below the drawable
	 * @param l Definition for the callback to be invoked when the view is cliked
	 */
	public void addItem(int drawable, String text, OnClickListener l) {
		addItem(mContext.getResources().getDrawable(drawable), text, l);
	}
	
	/**
	 * Adds an item to the QuickActionWindow
	 * 
	 * @param drawable Icon to be shown
	 * @param text Label resource id to be shown below the drawable
	 * @param l Definition for the callback to be invoked when the view is cliked
	 */
	public void addItem(Drawable drawable, int resid, OnClickListener l) {
		addItem(drawable, mContext.getResources().getString(resid), l);
	}
	
	/**
	 * Adds an item to the QuickActionWindow
	 * 
	 * @param drawable Icon resource id to be shown
	 * @param text Label resource id to be shown below the drawable
	 * @param l Definition for the callback to be invoked when the view is cliked
	 */
	public void addItem(int drawable, int resid, OnClickListener l) {
		addItem(mContext.getResources().getDrawable(drawable), mContext.getResources().getText(resid).toString(), l);
	}

	/**
	 * Shows the correct call-out arrow based on a {@link R.id} reference.
	 */
	private void showArrow(int whichArrow, int requestedX) {
		final View showArrow = (whichArrow == R.id.arrow_up) ? mArrowUp : mArrowDown;
		final View hideArrow = (whichArrow == R.id.arrow_up) ? mArrowDown : mArrowUp;

		// Dirty hack to get width, might cause memory leak
		final int arrowWidth = mContext.getResources().getDrawable(R.drawable.quickaction_arrow_up).getIntrinsicWidth();

		showArrow.setVisibility(View.VISIBLE);
		ViewGroup.MarginLayoutParams param = (ViewGroup.MarginLayoutParams)showArrow.getLayoutParams();
		param.leftMargin = requestedX - arrowWidth / 2;

		hideArrow.setVisibility(View.INVISIBLE);
	}
	
	/**
	 * Shows the quick actions window
	 * 
	 * @param requestedX The X coordinate the arrow will point at
	 */
	public void show(int requestedX) {
		super.showAtLocation(mPView, Gravity.NO_GRAVITY, 0, 0);
		
		// Calculate properly to position the popup the correctly based on height of popup
		if (isShowing()) {
			int x, y, windowAnimations;
			this.getContentView().measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			final int blockHeight = this.getContentView().getMeasuredHeight();
			
			x = -mShadowHoriz;
			
			if (mAnchor.top > blockHeight) {
				// Show downwards callout when enough room, aligning bottom block
				// edge with top of anchor area, and adjusting to inset arrow.
				showArrow(R.id.arrow_down, requestedX);
				y = mAnchor.top - blockHeight;
				windowAnimations = R.style.QuickActionAboveAnimation;
	
			} else {
				// Otherwise show upwards callout, aligning block top with bottom of
				// anchor area, and adjusting to inset arrow.
				showArrow(R.id.arrow_up, requestedX);
				y = mAnchor.bottom;
				windowAnimations = R.style.QuickActionBelowAnimation;
			}
			
			setAnimationStyle(windowAnimations);
			mTrack.startAnimation(mTrackAnim);
			this.update(x, y, -1, -1);
		}
	}
	
	/**
	 * Shows the quick actions window
	 */
	public void show() {
		show(mAnchor.centerX());
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		//if the back key is presses, the window is dismissed
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			dismiss();
			return true;
		}

		return false;
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return false;
	}
}
