/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher;

//import com.android.launcher.catalogue.CataGridView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class AllAppsGridView extends GridView implements
		AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
		DragSource, Drawer {

	private DragController mDragger;
	private Launcher mLauncher;
	private Paint mPaint;
	// ADW: Animation vars
	private final static int CLOSED = 1;
	private final static int OPEN = 2;
	private final static int CLOSING = 3;
	private final static int OPENING = 4;
	private int mStatus = CLOSED;
	private boolean isAnimating;
	private long startTime;
	private float mScaleFactor;
	private int mIconSize = 0;
	private int mBgAlpha = 255;
	private int mTargetAlpha = 255;
	private Paint mLabelPaint;
	private boolean shouldDrawLabels = false;
	private int mAnimationDuration = 800;
	private int mBgColor = 0xFF000000;
	private boolean mDrawLabels = true;
	private boolean mFadeDrawLabels = false;
	private float mLabelFactor;
    private int distH;
    private int distV;
    private float x;
    private float y;
    private float width;
    private float height;
    private Rect rl1=new Rect();
    private Rect rl2=new Rect();
    private float scale;
    private Rect r3=new Rect();
    private int xx;

	public AllAppsGridView(Context context) {
		super(context);
	}

	public AllAppsGridView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.gridViewStyle);
	}

	public AllAppsGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		mPaint = new Paint();
		mPaint.setDither(false);
		mLabelPaint = new Paint();
		mLabelPaint.setDither(false);
	}

	@Override
	public boolean isOpaque() {
		if (mBgAlpha >= 255)
			return true;
		else
			return false;
	}

	@Override
	protected void onFinishInflate() {
		setOnItemClickListener(this);
		setOnItemLongClickListener(this);
	}

	public void onItemClick(AdapterView parent, View v, int position, long id) {
		ApplicationInfo app = (ApplicationInfo) parent
				.getItemAtPosition(position);
		mLauncher.startActivitySafely(app.intent);
	}

	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		if (!view.isInTouchMode()) {
			return false;
		}

		ApplicationInfo app = (ApplicationInfo) parent
				.getItemAtPosition(position);
		app = new ApplicationInfo(app);

		mDragger.startDrag(view, this, app, DragController.DRAG_ACTION_COPY);
		if (!mLauncher.isDockBarOpen()) {
		    mLauncher.closeAllApplications();
		}

		return true;
	}

	public void setDragger(DragController dragger) {
		mDragger = dragger;
	}

	public void onDropCompleted(View target, boolean success) {
	}

	public void setLauncher(Launcher launcher) {
		mLauncher = launcher;
		setSelector(IconHighlights.getDrawable(mLauncher,
				IconHighlights.TYPE_DESKTOP));
	}

	/**
	 * ADW: easing functions for animation
	 */
	static float easeOut(float time, float begin, float end, float duration) {
		float change = end - begin;
		return change * ((time = time / duration - 1) * time * time + 1)
				+ begin;
	}

	static float easeIn(float time, float begin, float end, float duration) {
		float change = end - begin;
		return change * (time /= duration) * time * time + begin;
	}

	static float easeInOut(float time, float begin, float end, float duration) {
		float change = end - begin;
		if ((time /= duration / 2.0f) < 1)
			return change / 2.0f * time * time * time + begin;
		return change / 2.0f * ((time -= 2.0f) * time * time + 2.0f) + begin;
	}

	/**
	 * ADW: Override drawing methods to do animation
	 */
	@Override
	public void draw(Canvas canvas) {
		if (isAnimating) {
			long currentTime;
			if (startTime == 0) {
				startTime = SystemClock.uptimeMillis();
				currentTime = 0;
			} else {
				currentTime = SystemClock.uptimeMillis() - startTime;
			}
			if (mStatus == OPENING) {
				mScaleFactor = easeOut(currentTime, 3.0f, 1.0f, mAnimationDuration);
				mLabelFactor = easeOut(currentTime, -1.0f, 1.0f, mAnimationDuration);
			} else if (mStatus == CLOSING) {
				mScaleFactor = easeIn(currentTime, 1.0f, 3.0f, mAnimationDuration);
				mLabelFactor = easeIn(currentTime, 1.0f, -1.0f, mAnimationDuration);
			}
			if (mLabelFactor < 0)
				mLabelFactor = 0;
			if (currentTime >= mAnimationDuration) {
				isAnimating = false;
				if (mStatus == OPENING) {
					mStatus = OPEN;
				} else if (mStatus == CLOSING) {
					mStatus = CLOSED;
					mLauncher.getWorkspace().clearChildrenCache();
					setVisibility(View.GONE);
				}
			}
		}
		shouldDrawLabels = mFadeDrawLabels && mDrawLabels
				&& (mStatus == OPENING || mStatus == CLOSING);
		float porcentajeScale = 1.0f;
		if (isAnimating) {
			porcentajeScale = 1.0f - ((mScaleFactor - 1) / 3.0f);
			if (porcentajeScale > 0.9f)
				porcentajeScale = 1f;
			if (porcentajeScale < 0)
				porcentajeScale = 0;
			mBgAlpha = (int) (porcentajeScale * 255);
		}
		mPaint.setAlpha(mBgAlpha);
		if (getVisibility() == View.VISIBLE) {
			canvas
					.drawARGB((int) (porcentajeScale * mTargetAlpha), Color
							.red(mBgColor), Color.green(mBgColor), Color
							.blue(mBgColor));
			super.draw(canvas);
		}

	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		int saveCount = canvas.save();
		Drawable[] tmp = ((TextView) child).getCompoundDrawables();
		if (mIconSize == 0) {
			mIconSize = tmp[1].getIntrinsicHeight() + child.getPaddingTop();
		}
		if (isAnimating) {
			postInvalidate();
			//float x;
			//float y;
			distH = (child.getLeft() + (child.getWidth() / 2))
					- (getWidth() / 2);
			distV = (child.getTop() + (child.getHeight() / 2))
					- (getHeight() / 2);
			x = child.getLeft() + (distH * (mScaleFactor - 1)) * (mScaleFactor);
			y = child.getTop() + (distV * (mScaleFactor - 1)) * (mScaleFactor);
			width = child.getWidth() * mScaleFactor;
			height = (child.getHeight() - (child.getHeight() - mIconSize))
					* mScaleFactor;
			if (shouldDrawLabels)
				child.setDrawingCacheEnabled(true);
			if (shouldDrawLabels && child.getDrawingCache() != null) {
				// ADW: try to manually draw labels
				rl1.set(0, mIconSize, child.getDrawingCache()
						.getWidth(), child.getDrawingCache().getHeight());
				rl2.set(child.getLeft(),
						child.getTop() + mIconSize, child.getLeft()
								+ child.getDrawingCache().getWidth(), child
								.getTop()
								+ child.getDrawingCache().getHeight());
				mLabelPaint.setAlpha((int) (mLabelFactor * 255));
				canvas.drawBitmap(child.getDrawingCache(), rl1, rl2,
						mLabelPaint);
			}
			scale = ((width) / child.getWidth());
			r3 = tmp[1].getBounds();
			xx = (child.getWidth() / 2) - (r3.width() / 2);
			canvas.save();
			canvas.translate(x + xx, y + child.getPaddingTop());
			canvas.scale(scale, scale);
			tmp[1].draw(canvas);
			canvas.restore();
		} else {
			if (mDrawLabels) {
				child.setDrawingCacheEnabled(true);
				if (child.getDrawingCache() != null) {
					mPaint.setAlpha(255);
					canvas.drawBitmap(child.getDrawingCache(), child.getLeft(),
							child.getTop(), mPaint);
				} else {
					canvas.save();
					canvas.translate(child.getLeft(), child.getTop());
					child.draw(canvas);
					canvas.restore();
				}
			} else {
				r3 = tmp[1].getBounds();
				xx = (child.getWidth() / 2) - (r3.width() / 2);
				canvas.save();
				canvas.translate(child.getLeft() + xx, child.getTop()
						+ child.getPaddingTop());
				tmp[1].draw(canvas);
				canvas.restore();
			}
		}
		canvas.restoreToCount(saveCount);
		return true;
	}

	/**
	 * Open/close public methods
	 */
	public void open(boolean animate) {
		mBgColor = AlmostNexusSettingsHelper.getDrawerColor(mLauncher);
		mTargetAlpha = Color.alpha(mBgColor);
		mDrawLabels = AlmostNexusSettingsHelper.getDrawerLabels(mLauncher);
		mFadeDrawLabels = AlmostNexusSettingsHelper
				.getFadeDrawerLabels(mLauncher);
        if(getAdapter()==null)
        	animate=false;
        else if(getAdapter().getCount()<=0)
        	animate=false;
		if (animate) {
			if (mFadeDrawLabels && mDrawLabels) {
                ListAdapter adapter = getAdapter();
                if (adapter instanceof ApplicationsAdapter)
                    ((ApplicationsAdapter)adapter).setChildDrawingCacheEnabled(true);
			}
			mBgAlpha = 0;
			isAnimating = true;
			mStatus = OPENING;
		} else {
			mBgAlpha = mTargetAlpha;
			isAnimating = false;
			mStatus = OPEN;
		}
		startTime = 0;
		this.setVisibility(View.VISIBLE);
		invalidate();
	}

	public void close(boolean animate) {
        if(getAdapter()==null)
        	animate=false;
        else if(getAdapter().getCount()<=0)
        	animate=false;
		if (animate) {
			mStatus = CLOSING;
			isAnimating = true;
		} else {
			mStatus = CLOSED;
			isAnimating = false;
			mLauncher.getWorkspace().clearChildrenCache();
			setVisibility(View.GONE);
		}
		startTime = 0;
		invalidate();
	}
	public void setAnimationSpeed(int speed) {
		mAnimationDuration = speed;
	}

	public void updateAppGrp() {
		if(getAdapter()!=null){
			((ApplicationsAdapter) getAdapter()).updateDataSet();
		}
	}

	public void setAdapter(ApplicationsAdapter adapter) {
		setAdapter((ListAdapter)adapter);
	}

	public void setNumRows(int numRows) {}

	public void setPageHorizontalMargin(int margin) {}

}
