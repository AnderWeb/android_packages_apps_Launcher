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

import java.util.ArrayList;

import mobi.intuitit.android.widget.WidgetSpace;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;

import com.android.launcher.FlingGesture.FlingListener;

import android.app.Activity;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

/**
 * The workspace is a wide area with a wallpaper and a finite number of screens. Each
 * screen contains a number of icons, folders or widgets the user can interact with.
 * A workspace is meant to be used with a fixed width only.
 */
public class Workspace extends WidgetSpace implements DropTarget, DragSource, DragScroller,
												MultiTouchObjectCanvas<Object>, FlingListener {
    private static final int INVALID_SCREEN = -1;

    private int mDefaultScreen;

    private final WallpaperManager mWallpaperManager;

    private boolean mFirstLayout = true;

    //private int mCurrentScreen;
    private int mNextScreen = INVALID_SCREEN;
    private CustomScroller mScroller;
    private final FlingGesture mFlingGesture;

    /**
     * CellInfo for the cell that is currently being dragged
     */
    private CellLayout.CellInfo mDragInfo;

    /**
     * Target drop area calculated during last acceptDrop call.
     */
    private int[] mTargetCell = null;

    private float mLastMotionX;
    private float mLastMotionY;

    private final static int TOUCH_STATE_REST = 0;
    private final static int TOUCH_STATE_SCROLLING = 1;
    private final static int TOUCH_SWIPE_DOWN_GESTURE = 2;
    private final static int TOUCH_SWIPE_UP_GESTURE = 3;

    private int mTouchState = TOUCH_STATE_REST;

    private OnLongClickListener mLongClickListener;

    private Launcher mLauncher;
    private DragController mDragger;
    /**
     * Cache of vacant cells, used during drag events and invalidated as needed.
     */
    private CellLayout.CellInfo mVacantCache = null;

    private final int[] mTempCell = new int[2];
    private final int[] mTempEstimate = new int[2];

    //private boolean mAllowLongPress;
    private boolean mLocked;

    private int mTouchSlop;
    private int mMaximumVelocity;

    final Rect mDrawerBounds = new Rect();
    final Rect mClipBounds = new Rect();
    int mDrawerContentHeight;
    int mDrawerContentWidth;
    //ADW: Dots Indicators
    private Drawable mPreviousIndicator;
    private Drawable mNextIndicator;
    //rogro82@xda
    int mHomeScreens = 0;
    //int mHomeScreensLoaded = 0;
    //ADW: port from donut wallpaper drawing
    private Paint mPaint;
    private int mWallpaperWidth;
    private int mWallpaperHeight;
    private float mWallpaperOffset;
    private boolean mWallpaperLoaded;
    private boolean lwpSupport=true;
    private boolean wallpaperHack=true;
    private BitmapDrawable mWallpaperDrawable;
    //ADW: speed for desktop transitions
    private int mScrollingSpeed=600;
    //ADW: bounce scroll
    private int mScrollingBounce=50;
    //ADW: sense zoom constants
	private static final int SENSE_OPENING = 1;
	private static final int SENSE_CLOSING = 2;
	private static final int SENSE_OPEN = 3;
	private static final int SENSE_CLOSED = 4;
    //ADW: sense zoom variables
	private boolean mSensemode=false;
	private boolean isAnimating=false;
	private long startTime;
	private int mStatus=SENSE_CLOSED;
	private final int mAnimationDuration=400;
	private final int[][] distro={{1},{2},{1,2},{2,2},{2,1,2},{2,2,2},{2,3,2},{3,2,3},{3,3,3}};
	private float previewScale=1;
	//Wysie: Multitouch controller
	private MultiTouchController<Object> multiTouchController;
	// Wysie: Values taken from CyanogenMod (Donut era) Browser
	private static final double ZOOM_SENSITIVITY = 1.6;
	private static final double ZOOM_LOG_BASE_INV = 1.0 / Math.log(2.0 / ZOOM_SENSITIVITY);
	//ADW: we don't need bouncing while using the previews
	private boolean mRevertInterpolatorOnScrollFinish=false;
	//ADW: custom desktop rows/columns
	private int mDesktopRows;
	private int mDesktopColumns;
	//ADW: use drawing cache while scrolling, etc.
	//Seems a lot of users with "high end" devices, like to have tons of widgets (the bigger, the better)
	//On those devices, a drawing cache of a 4x4widget can be really big
	//cause of their screen sizes, so the bitmaps are... huge...
	//And as those devices can perform pretty well without cache... let's add an option... one more...
    	private boolean mTouchedScrollableWidget = false;
	private int mDesktopCacheType=AlmostNexusSettingsHelper.CACHE_LOW;
	private boolean mWallpaperScroll=true;
    //ADW: variable to track the proper Y position to draw the wallpaer when the wallpaper hack is enabled
    //this is to avoid the small vertical position change from the wallpapermanager one.
    private int mWallpaperY;
    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs The attribtues set containing the Workspace's customization values.
     */
    public Workspace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Used to inflate the Workspace from XML.
     *
     * @param context The application's context.
     * @param attrs The attribtues set containing the Workspace's customization values.
     * @param defStyle Unused.
     */
    public Workspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mWallpaperManager = WallpaperManager.getInstance(context);

        /* Rogro82@xda Extended : Load the default and number of homescreens from the settings database */
        mHomeScreens = AlmostNexusSettingsHelper.getDesktopScreens(context);
        mDefaultScreen = AlmostNexusSettingsHelper.getDefaultScreen(context);
        if(mDefaultScreen>mHomeScreens-1) mDefaultScreen=0;

        //ADW: create desired screens programatically
        LayoutInflater layoutInflter=LayoutInflater.from(context);
        for(int i=0;i<mHomeScreens;i++){
        	CellLayout screen=(CellLayout)layoutInflter.inflate(R.layout.workspace_screen, this, false);
        	addView(screen);
        }
        mFlingGesture = new FlingGesture();
        mFlingGesture.setListener(this);
        initWorkspace();
    }

    /**
     * Initializes various states for this workspace.
     */
    private void initWorkspace() {
        mScroller = new CustomScroller(getContext(), new ElasticInterpolator(5f));
        mCurrentScreen = mDefaultScreen;
        Launcher.setScreen(mCurrentScreen);
        mPaint=new Paint();
        mPaint.setDither(false);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        //Wysie: Use MultiTouchController only for multitouch events
        multiTouchController = new MultiTouchController<Object>(this, false);
        mDesktopRows=AlmostNexusSettingsHelper.getDesktopRows(getContext());
        mDesktopColumns=AlmostNexusSettingsHelper.getDesktopColumns(getContext());
        mDesktopCacheType=AlmostNexusSettingsHelper.getScreenCache(getContext());
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        /* Rogro82@xda Extended : Only load the number of home screens set */
        //if(mHomeScreensLoaded < mHomeScreens){
            //mHomeScreensLoaded++;
            super.addView(child, index, params);
        //}
    }

    @Override
    public void addView(View child) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, index);
    }

    @Override
    public void addView(View child, int width, int height) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, width, height);
    }

    @Override
    public void addView(View child, LayoutParams params) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, params);
    }

    /**
     * @return The open folder on the current screen, or null if there is none
     */
    Folder getOpenFolder() {
        CellLayout currentScreen = (CellLayout) getChildAt(mCurrentScreen);
        if(currentScreen==null)return null;
        int count = currentScreen.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = currentScreen.getChildAt(i);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
            if (lp.cellHSpan == mDesktopColumns && lp.cellVSpan == mDesktopRows && child instanceof Folder) {
                return (Folder) child;
            }
        }
        return null;
    }

    ArrayList<Folder> getOpenFolders() {
        final int screens = getChildCount();
        ArrayList<Folder> folders = new ArrayList<Folder>(screens);

        for (int screen = 0; screen < screens; screen++) {
            CellLayout currentScreen = (CellLayout) getChildAt(screen);
            int count = currentScreen.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = currentScreen.getChildAt(i);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
                if (lp.cellHSpan == mDesktopColumns && lp.cellVSpan == mDesktopRows && child instanceof Folder) {
                    folders.add((Folder) child);
                    break;
                }
            }
        }

        return folders;
    }

    boolean isDefaultScreenShowing() {
        return mCurrentScreen == mDefaultScreen;
    }

    /**
     * Returns the index of the currently displayed screen.
     *
     * @return The index of the currently displayed screen.
     */
    int getCurrentScreen() {
        return mCurrentScreen;
    }

    /**
     * Sets the current screen.
     *
     * @param currentScreen
     */
    void setCurrentScreen(int currentScreen) {
        clearVacantCache();
        mCurrentScreen = Math.max(0, Math.min(currentScreen, getChildCount() - 1));
        scrollTo(mCurrentScreen * getWidth(), 0);
        //ADW: dots
        indicatorLevels(mCurrentScreen);
        if(mLauncher.getDesktopIndicator()!=null){
        	mLauncher.getDesktopIndicator().fullIndicate(mCurrentScreen);
        	if(mLauncher.isEditMode() || mLauncher.isAllAppsVisible()){
        		mLauncher.getDesktopIndicator().hide();
        	}
        }
        invalidate();
    }

    /**
     * Adds the specified child in the current screen. The position and dimension of
     * the child are defined by x, y, spanX and spanY.
     *
     * @param child The child to add in one of the workspace's screens.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     */
    void addInCurrentScreen(View child, int x, int y, int spanX, int spanY) {
        addInScreen(child, mCurrentScreen, x, y, spanX, spanY, false);
    }

    /**
     * Adds the specified child in the current screen. The position and dimension of
     * the child are defined by x, y, spanX and spanY.
     *
     * @param child The child to add in one of the workspace's screens.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     * @param insert When true, the child is inserted at the beginning of the children list.
     */
    void addInCurrentScreen(View child, int x, int y, int spanX, int spanY, boolean insert) {
        addInScreen(child, mCurrentScreen, x, y, spanX, spanY, insert);
    }

    /**
     * Adds the specified child in the specified screen. The position and dimension of
     * the child are defined by x, y, spanX and spanY.
     *
     * @param child The child to add in one of the workspace's screens.
     * @param screen The screen in which to add the child.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     */
    void addInScreen(View child, int screen, int x, int y, int spanX, int spanY) {
        addInScreen(child, screen, x, y, spanX, spanY, false);
    }

    /**
     * Adds the specified child in the specified screen. The position and dimension of
     * the child are defined by x, y, spanX and spanY.
     *
     * @param child The child to add in one of the workspace's screens.
     * @param screen The screen in which to add the child.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     * @param insert When true, the child is inserted at the beginning of the children list.
     */
    void addInScreen(View child, int screen, int x, int y, int spanX, int spanY, boolean insert) {
        if (screen < 0 || screen >= getChildCount()) {
            /* Rogro82@xda Extended : Do not throw an exception else it will crash when there is an item on a hidden homescreen */
            return;
            //throw new IllegalStateException("The screen must be >= 0 and < " + getChildCount());
        }
        //ADW: we cannot accept an item from a position greater that current desktop columns/rows
        if(x>=mDesktopColumns || y>=mDesktopRows){
        	return;
        }
        clearVacantCache();

        final CellLayout group = (CellLayout) getChildAt(screen);
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
        if (lp == null) {
            lp = new CellLayout.LayoutParams(x, y, spanX, spanY);
        } else {
            lp.cellX = x;
            lp.cellY = y;
            lp.cellHSpan = spanX;
            lp.cellVSpan = spanY;
        }
        group.addView(child, insert ? 0 : -1, lp);
        if (!(child instanceof Folder)) {
            child.setOnLongClickListener(mLongClickListener);
        }
    }

    /*void addWidget(View view, Widget widget, boolean insert) {
        addInScreen(view, widget.screen, widget.cellX, widget.cellY, widget.spanX,
                widget.spanY, insert);
    }*/

    CellLayout.CellInfo findAllVacantCells(boolean[] occupied) {
        CellLayout group = (CellLayout) getChildAt(mCurrentScreen);
        if (group != null) {
            return group.findAllVacantCells(occupied, null);
        }
        return null;
    }

    CellLayout.CellInfo findAllVacantCellsFromModel() {
        CellLayout group = (CellLayout) getChildAt(mCurrentScreen);
        if (group != null) {
            int countX = group.getCountX();
            int countY = group.getCountY();
            boolean occupied[][] = new boolean[countX][countY];
            Launcher.getModel().findAllOccupiedCells(occupied, countX, countY, mCurrentScreen);
            return group.findAllVacantCellsFromOccupied(occupied, countX, countY);
        }
        return null;
    }

    private void clearVacantCache() {
        if (mVacantCache != null) {
            mVacantCache.clearVacantCells();
            mVacantCache = null;
        }
    }

    /**
     * Registers the specified listener on each screen contained in this workspace.
     *
     * @param l The listener used to respond to long clicks.
     */
    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        mLongClickListener = l;
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).setOnLongClickListener(l);
        }
    }

    private void updateWallpaperOffset() {
    	if(mWallpaperScroll){
    		updateWallpaperOffset(getChildAt(getChildCount() - 1).getRight() - (getRight() - getLeft()));
    	}
    }
    private void centerWallpaperOffset(){
		mWallpaperManager.setWallpaperOffsetSteps(0.5f, 0 );
		mWallpaperManager.setWallpaperOffsets(getWindowToken(), 0.5f, 0);
    }
    private void updateWallpaperOffset(int scrollRange) {
    	//ADW: we set a condition to not move wallpaper beyond the "bounce" zone
    	if(getScrollX()>0 && getScrollX()<getChildAt(getChildCount() - 1).getLeft()){
    		mWallpaperManager.setWallpaperOffsetSteps(1.0f / (getChildCount() - 1), 0 );
    		mWallpaperManager.setWallpaperOffsets(getWindowToken(), mScrollX / (float) scrollRange, 0);
    	}
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            updateWallpaperOffset();
            if(mLauncher.getDesktopIndicator()!=null){
                mLauncher.getDesktopIndicator().indicate((float)mScroller.getCurrX()/(float)(getChildCount()*getWidth()));
                if(mLauncher.isEditMode() || mLauncher.isAllAppsVisible()){
                    mLauncher.getDesktopIndicator().hide();
                }
            }
            postInvalidate();
        } else if (mNextScreen != INVALID_SCREEN) {
        	int lastScreen = mCurrentScreen;
            mCurrentScreen = Math.max(0, Math.min(mNextScreen, getChildCount() - 1));
            //ADW: dots
            //indicatorLevels(mCurrentScreen);
            Launcher.setScreen(mCurrentScreen);
            mNextScreen = INVALID_SCREEN;
            clearChildrenCache();
            if(mLauncher.getDesktopIndicator()!=null){
                mLauncher.getDesktopIndicator().fullIndicate(mCurrentScreen);
                if(mLauncher.isEditMode() || mLauncher.isAllAppsVisible()){
                    mLauncher.getDesktopIndicator().hide();
                }
            }
            //ADW: Revert back the interpolator when needed
            if(mRevertInterpolatorOnScrollFinish)setBounceAmount(mScrollingBounce);
			//ADW: use intuit code to allow extended widgets
			// notify widget about screen changed
			//REMOVED, seems its used for animated widgets, we don't need that yet :P
            /*if(mLauncher.isScrollableAllowed()){
	            View changedView;
				if (lastScreen != mCurrentScreen) {
					changedView = getChildAt(lastScreen); // A screen get out
				if (changedView instanceof WidgetCellLayout)
					((WidgetCellLayout) changedView).onViewportOut();
				}
				changedView = getChildAt(mCurrentScreen); // A screen get in
				if (changedView instanceof WidgetCellLayout)
				((WidgetCellLayout) changedView).onViewportIn();
			}*/
		}
    }

    @Override
    public boolean isOpaque() {
        //ADW: hack to use old rendering
        if(!lwpSupport && mWallpaperLoaded){
            //return !mWallpaper.hasAlpha();
        	return mWallpaperDrawable.getOpacity()==PixelFormat.OPAQUE;
        }else{
        	return false;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        boolean restore = false;
        //ADW: If using old wallpaper rendering method...
        if(!lwpSupport && mWallpaperDrawable!=null){
        	float x = getScrollX() * mWallpaperOffset;
    		if (x + mWallpaperWidth < getRight() - getLeft()) {
    			x = getRight() - getLeft() - mWallpaperWidth;
    		}
        	//ADW: added tweaks for when scrolling "beyond bounce limits" :P
    		if (mScrollX<0)x=mScrollX;
        	if(mScrollX>getChildAt(getChildCount() - 1).getRight() - (mRight - mLeft)){
        		x=(mScrollX-mWallpaperWidth+(mRight-mLeft));
        	}
        	//if(getChildCount()==1)x=getScrollX();
        	//ADW lets center the wallpaper when there's only one screen...
        	if(!mWallpaperScroll || getChildCount()==1)x=(getScrollX()-(mWallpaperWidth/2)+(getRight()/2));
        	final int y=mWallpaperY;
    		canvas.drawBitmap(mWallpaperDrawable.getBitmap(), x, y, mPaint);
        }
        if(!mSensemode){
	        // If the all apps drawer is open and the drawing region for the workspace
	        // is contained within the drawer's bounds, we skip the drawing. This requires
	        // the drawer to be fully opaque.
	        if((mLauncher.isAllAppsVisible()) || mLauncher.isEditMode()){
	        	return;
	        }
	        // ViewGroup.dispatchDraw() supports many features we don't need:
	        // clip to padding, layout animation, animation listener, disappearing
	        // children, etc. The following implementation attempts to fast-track
	        // the drawing dispatch by drawing only what we know needs to be drawn.

	        boolean fastDraw = mTouchState != TOUCH_STATE_SCROLLING && mNextScreen == INVALID_SCREEN;
	        // If we are not scrolling or flinging, draw only the current screen
	        if (fastDraw) {
	            drawChild(canvas, getChildAt(mCurrentScreen), getDrawingTime());
	        } else {
	            final long drawingTime = getDrawingTime();
	            // If we are flinging, draw only the current screen and the target screen
	            if (mNextScreen >= 0 && mNextScreen < getChildCount() &&
	                    Math.abs(mCurrentScreen - mNextScreen) == 1) {
	                drawChild(canvas, getChildAt(mCurrentScreen), drawingTime);
	                drawChild(canvas, getChildAt(mNextScreen), drawingTime);
	            } else {
	                // If we are scrolling, draw all of our children
	                final int count = getChildCount();
	                for (int i = 0; i < count; i++) {
	                    drawChild(canvas, getChildAt(i), drawingTime);
	                }
	            }
	        }
        }else{
    		long currentTime;
    		if(startTime==0){
    			startTime=SystemClock.uptimeMillis();
    			currentTime=0;
    		}else{
    			currentTime=SystemClock.uptimeMillis()-startTime;
    		}
    		if(currentTime>=mAnimationDuration){
    			isAnimating=false;
    			if(mStatus==SENSE_OPENING){
    				mStatus=SENSE_OPEN;
    			}else if(mStatus==SENSE_CLOSING){
    				mStatus=SENSE_CLOSED;
    				mSensemode=false;
    				clearChildrenCache();
    				unlock();
    				postInvalidate();
    			}
    		}else{
    			postInvalidate();
    		}
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                drawChild(canvas, getChildAt(i), getDrawingTime());
            }
        }
        float x = getScrollX();
        if (restore) {
            canvas.restore();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }
        // The children are given the same width and height as the workspace
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);
        heightSpecSize-=getPaddingTop()+getPaddingBottom();
        heightMeasureSpec=MeasureSpec.makeMeasureSpec(heightSpecSize, MeasureSpec.EXACTLY);
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }
        //ADW: measure wallpaper when using old rendering
    	if(!lwpSupport && mWallpaperDrawable!=null){
    		if (mWallpaperLoaded) {
    		    mWallpaperLoaded = false;
    		    mWallpaperHeight = mWallpaperDrawable.getIntrinsicHeight();
    		    mWallpaperWidth = mWallpaperDrawable.getIntrinsicWidth();
    		}

    		final int wallpaperWidth = mWallpaperWidth;
    		mWallpaperOffset = wallpaperWidth > width ? (count * width - wallpaperWidth) /
    		        ((count - 1) * (float) width) : 1.0f;
    	}
        if (mFirstLayout) {
            scrollTo(mCurrentScreen * width, 0);
            mScroller.startScroll(0, 0, mCurrentScreen * width, 0, 0);
            updateWallpaperOffset(width * (getChildCount() - 1));
            mFirstLayout = false;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childLeft = 0;
        final int mTop=getPaddingTop();
        final int mBottom=getPaddingBottom();
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                final int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, mTop, childLeft + childWidth, mTop+child.getMeasuredHeight()-mBottom);
                childLeft += childWidth;
            }
        }
        //ADW:updateWallpaperoffset
    	if(mWallpaperScroll)
    		updateWallpaperOffset();
    	else
    		centerWallpaperOffset();
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        int screen = indexOfChild(child);
        if (screen != mCurrentScreen || !mScroller.isFinished()) {
            if (!mLauncher.isWorkspaceLocked()) {
                snapToScreen(screen);
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if(!mLauncher.isAllAppsVisible() && getChildCount()>0){
            final Folder openFolder = getOpenFolder();
            if (openFolder != null) {
                return openFolder.requestFocus(direction, previouslyFocusedRect);
            } else {
                int focusableScreen;
                if (mNextScreen != INVALID_SCREEN) {
                    focusableScreen = mNextScreen;
                } else {
                    focusableScreen = mCurrentScreen;
                }
                if(focusableScreen>getChildCount()-1)focusableScreen=getChildCount()-1;
                getChildAt(focusableScreen).requestFocus(direction, previouslyFocusedRect);
            }
        }
        return false;
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (direction == View.FOCUS_LEFT) {
            if (getCurrentScreen() > 0) {
                snapToScreen(getCurrentScreen() - 1);
                return true;
            }
        } else if (direction == View.FOCUS_RIGHT) {
            if (getCurrentScreen() < getChildCount() - 1) {
                snapToScreen(getCurrentScreen() + 1);
                return true;
            }
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (!mLauncher.isAllAppsVisible()) {
            final Folder openFolder = getOpenFolder();
            if (openFolder == null) {
                try{
                    getChildAt(mCurrentScreen).addFocusables(views, direction);
                    if (direction == View.FOCUS_LEFT) {
                        if (mCurrentScreen > 0) {
                            getChildAt(mCurrentScreen - 1).addFocusables(views, direction);
                        }
                    } else if (direction == View.FOCUS_RIGHT){
                        if (mCurrentScreen < getChildCount() - 1) {
                            getChildAt(mCurrentScreen + 1).addFocusables(views, direction);
                        }
                    }
                }catch (Exception e){
                    //Adding focusables with screens not ready...
                }
            } else {
                openFolder.addFocusables(views, direction);
            }
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	if(mStatus==SENSE_OPEN){
    		if(ev.getAction()==MotionEvent.ACTION_DOWN){
    			findClickedPreview(ev.getX(),ev.getY());
    		}
    		return true;
    	}

        //Wysie: If multitouch event is detected
        if (multiTouchController.onTouchEvent(ev)) {
            return false;
        }

        if (mLocked || mLauncher.isAllAppsVisible()) {
            return true;
        }

        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onTouchEvent will be called and we do the actual
         * scrolling there.
         */

        /*
         * Shortcut the most recurring case: the user is in the dragging
         * state and he is moving his finger.  We want to intercept this
         * motion.
         */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mTouchState != TOUCH_STATE_REST)) {
            return true;
        }

        final float x = ev.getX();
        final float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                 * Locally do absolute value. mLastMotionX is set to the y value
                 * of the down event.
                 */
                final int xDiff = (int) Math.abs(x - mLastMotionX);
                final int yDiff = (int) Math.abs(y - mLastMotionY);

                final int touchSlop = mTouchSlop;
                boolean xMoved = xDiff > touchSlop;
                boolean yMoved = yDiff > touchSlop;
                if (xMoved || yMoved) {
                    // If xDiff > yDiff means the finger path pitch is smaller than 45deg so we assume the user want to scroll X axis
                    if (xDiff > yDiff) {
                        // Scroll if the user moved far enough along the X axis
                        mTouchState = TOUCH_STATE_SCROLLING;
                        enableChildrenCache();

                    }
                    // If yDiff > xDiff means the finger path pitch is bigger than 45deg so we assume the user want to either scroll Y or Y-axis gesture
                    else if (getOpenFolder()==null)
                    {
                    	// As x scrolling is left untouched (more or less untouched;)), every gesture should start by dragging in Y axis. In fact I only consider useful, swipe up and down.
                    	// Guess if the first Pointer where the user click belongs to where a scrollable widget is.
                		mTouchedScrollableWidget = isWidgetAtLocationScrollable((int)mLastMotionX,(int)mLastMotionY);
                    	if (!mTouchedScrollableWidget)
                    	{
	                    	// Only y axis movement. So may be a Swipe down or up gesture
	                    	if ((y - mLastMotionY) > 0){
	                    		if(Math.abs(y-mLastMotionY)>(touchSlop*2))mTouchState = TOUCH_SWIPE_DOWN_GESTURE;
	                    	}else{
	                    		if(Math.abs(y-mLastMotionY)>(touchSlop*2))mTouchState = TOUCH_SWIPE_UP_GESTURE;
	                    	}
                    	}
                    }
                    // Either way, cancel any pending longpress
                    if (mAllowLongPress) {
                        mAllowLongPress = false;
                        // Try canceling the long press. It could also have been scheduled
                        // by a distant descendant, so use the mAllowLongPress flag to block
                        // everything
                        final View currentScreen = getChildAt(mCurrentScreen);
                        currentScreen.cancelLongPress();
                    }
                }
                break;

            case MotionEvent.ACTION_DOWN:
                // Remember location of down touch
                mLastMotionX = x;
                mLastMotionY = y;
                mAllowLongPress = true;

                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't.  mScroller.isFinished should be false when
                 * being flinged.
                 */
                mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:

            	if (mTouchState != TOUCH_STATE_SCROLLING && mTouchState != TOUCH_SWIPE_DOWN_GESTURE && mTouchState != TOUCH_SWIPE_UP_GESTURE) {
                    final CellLayout currentScreen = (CellLayout) getChildAt(mCurrentScreen);
                    if (!currentScreen.lastDownOnOccupiedCell()) {
                        getLocationOnScreen(mTempCell);
                        // Send a tap to the wallpaper if the last down was on empty space
                        if(lwpSupport)
                        mWallpaperManager.sendWallpaperCommand(getWindowToken(),
                                "android.wallpaper.tap",
                                mTempCell[0] + (int) ev.getX(),
                                mTempCell[1] + (int) ev.getY(), 0, null);
                    }
                }
                // Release the drag
                clearChildrenCache();
                mTouchState = TOUCH_STATE_REST;
                mAllowLongPress = false;
                break;
        }

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mTouchState != TOUCH_STATE_REST;
    }
	void enableChildrenCache() {
        if(mDesktopCacheType!=AlmostNexusSettingsHelper.CACHE_DISABLED){
	    	final int count = getChildCount();
	        for (int i = 0; i < count; i++) {
	        	//ADW: create cache only for current screen/previous/next.
	        	if(i>=mCurrentScreen-1 || i<=mCurrentScreen+1){
	        		final CellLayout layout = (CellLayout) getChildAt(i);
	        		if(mDesktopCacheType==AlmostNexusSettingsHelper.CACHE_LOW)
	        			layout.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
	        		else
	        			layout.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);
	        		layout.setChildrenDrawnWithCacheEnabled(true);
	        		layout.setChildrenDrawingCacheEnabled(true);
	        	}
	        }
        }
    }

    void clearChildrenCache() {
        if(mDesktopCacheType!=AlmostNexusSettingsHelper.CACHE_DISABLED){
	    	final int count = getChildCount();
	        for (int i = 0; i < count; i++) {
	            final CellLayout layout = (CellLayout) getChildAt(i);
	            layout.setChildrenDrawnWithCacheEnabled(false);
	        }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //Wysie: If multitouch event is detected
        /*if (multiTouchController.onTouchEvent(ev)) {
            return false;
        }*/
        if (mLocked || mLauncher.isAllAppsVisible() || mSensemode) {
            return true;
        }

        mFlingGesture.ForwardTouchEvent(ev);

        final int action = ev.getAction();
        final float x = ev.getX();

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            /*
             * If being flinged and user touches, stop the fling. isFinished
             * will be false if being flinged.
             */
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }

            // Remember where the motion event started
            mLastMotionX = x;
            break;
        case MotionEvent.ACTION_MOVE:
            if (mTouchState == TOUCH_STATE_SCROLLING) {
                // Scroll to follow the motion event
                int deltaX = (int) (mLastMotionX - x);
                if(mScrollingBounce==0){
                    final int endLimit = getChildAt(getChildCount() - 1).getRight() - getWidth();
                    final int scrollpos=getScrollX();
                    if(scrollpos+deltaX<0)deltaX=-scrollpos;
                    if(scrollpos+deltaX>endLimit)deltaX=endLimit-scrollpos;
                }
                mLastMotionX = x;
                scrollBy(deltaX, 0);
                updateWallpaperOffset();
                if(mLauncher.getDesktopIndicator()!=null)mLauncher.getDesktopIndicator().indicate((float)getScrollX()/(float)(getChildCount()*getWidth()));
            }
            break;
        case MotionEvent.ACTION_UP:
            if (mTouchState == TOUCH_SWIPE_DOWN_GESTURE )
            {
            	mLauncher.fireSwipeDownAction();
            } else if (mTouchState == TOUCH_SWIPE_UP_GESTURE )
            {
            	mLauncher.fireSwipeUpAction();
            }
            mTouchState = TOUCH_STATE_REST;
            break;
        case MotionEvent.ACTION_CANCEL:
            mTouchState = TOUCH_STATE_REST;
        }

        return true;
    }

	@Override
	public void OnFling(int Direction) {
		if (mTouchState == TOUCH_STATE_SCROLLING) {
			if (Direction == FlingGesture.FLING_LEFT && mCurrentScreen > 0) {
				snapToScreen(mCurrentScreen - 1);
	        } else if (Direction == FlingGesture.FLING_RIGHT && mCurrentScreen < getChildCount() - 1) {
	        	snapToScreen(mCurrentScreen + 1);
	        } else {
				final int screenWidth = getWidth();
				final int nextScreen = (getScrollX() + (screenWidth / 2)) / screenWidth;
	            snapToScreen(nextScreen);
	        }
		}
	}


    void snapToScreen(int whichScreen) {
        //if (!mScroller.isFinished()) return;
        clearVacantCache();
        enableChildrenCache();

        whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
        boolean changingScreens = whichScreen != mCurrentScreen;

        mNextScreen = whichScreen;
        //ADW: dots
        indicatorLevels(mNextScreen);

        View focusedChild = getFocusedChild();
        if (focusedChild != null && changingScreens && focusedChild == getChildAt(mCurrentScreen)) {
            focusedChild.clearFocus();
        }


        final int screenDelta = Math.abs(whichScreen - mCurrentScreen);
        int durationOffset = 1;
		// Faruq: Added to allow easing even when Screen doesn't changed (when revert happens)
		//Log.d("Workspace", "whichScreen: "+whichScreen+"; mCurrentScreen: "+mCurrentScreen+"; getChildCount: "+(getChildCount()-1));
		if (screenDelta == 0) {
		durationOffset = 400;
		//Log.d("Workspace", "Increasing duration by "+durationOffset+" times");
		}
		final int duration = mScrollingSpeed + durationOffset;
        final int newX = whichScreen * getWidth();
        final int delta = newX - mScrollX;
        //mScroller.startScroll(mScrollX, 0, delta, 0, Math.abs(delta) * 2);
        if(!mSensemode)
        	mScroller.startScroll(getScrollX(), 0, delta, 0, duration);
        else
        	mScroller.startScroll(getScrollX(), 0, delta, 0, mAnimationDuration);
        invalidate();
    }

    void startDrag(CellLayout.CellInfo cellInfo) {
        View child = cellInfo.cell;

        // Make sure the drag was started by a long press as opposed to a long click.
        // Note that Search takes focus when clicked rather than entering touch mode
        /*if (!child.isInTouchMode() && !(child instanceof Search)) {
            return;
        }*/
        
        mDragInfo = cellInfo;
        mDragInfo.screen = mCurrentScreen;

        CellLayout current = ((CellLayout) getChildAt(mCurrentScreen));
        final ItemInfo info = (ItemInfo)child.getTag();
        mLauncher.showActions(info, child);

        current.onDragChild(child);
        mDragger.startDrag(child, this, child.getTag(), DragController.DRAG_ACTION_MOVE);
        invalidate();
        clearVacantCache();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final SavedState state = new SavedState(super.onSaveInstanceState());
        state.currentScreen = mCurrentScreen;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        try{
	    	SavedState savedState = (SavedState) state;
	        super.onRestoreInstanceState(savedState.getSuperState());
	        if (savedState.currentScreen != -1) {
	            mCurrentScreen = savedState.currentScreen;
	            Launcher.setScreen(mCurrentScreen);
	        }
        }catch (Exception e) {
			// TODO ADW: Weird bug http://code.google.com/p/android/issues/detail?id=3981
			//Should be completely fixed on eclair
			super.onRestoreInstanceState(null);
			Log.d("WORKSPACE","Google bug http://code.google.com/p/android/issues/detail?id=3981 found, bypassing...");
		}
    }

    void addApplicationShortcut(ApplicationInfo info, CellLayout.CellInfo cellInfo,
            boolean insertAtFirst) {
        final CellLayout layout = (CellLayout) getChildAt(cellInfo.screen);
        final int[] result = new int[2];

        layout.cellToPoint(cellInfo.cellX, cellInfo.cellY, result);
        onDropExternal(result[0], result[1], info, layout, insertAtFirst);
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        final CellLayout cellLayout = getCurrentDropLayout();
        if (source != this) {
            onDropExternal(x - xOffset, y - yOffset, dragInfo, cellLayout);
        } else {
            // Move internally
            if (mDragInfo != null) {
                boolean moved=false;
                final View cell = mDragInfo.cell;
                int index = mScroller.isFinished() ? mCurrentScreen : mNextScreen;
                if (index != mDragInfo.screen) {
                    final CellLayout originalCellLayout = (CellLayout) getChildAt(mDragInfo.screen);
                    originalCellLayout.removeView(cell);
                    cellLayout.addView(cell);
                    moved=true;
                }
                mTargetCell = estimateDropCell(x - xOffset, y - yOffset,
                        mDragInfo.spanX, mDragInfo.spanY, cell, cellLayout, mTargetCell);
                cellLayout.onDropChild(cell, mTargetCell);
                if(mTargetCell[0]!=mDragInfo.cellX || mTargetCell[1]!=mDragInfo.cellY)
                    moved=true;
                final ItemInfo info = (ItemInfo)cell.getTag();
                if(moved){
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell.getLayoutParams();
                    LauncherModel.moveItemInDatabase(mLauncher, info,
                            LauncherSettings.Favorites.CONTAINER_DESKTOP, index, lp.cellX, lp.cellY);
                //}else{
                    //mLauncher.showActions(info, cell);
                }
            }
        }
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
        clearVacantCache();
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
        clearVacantCache();
    }

    private void onDropExternal(int x, int y, Object dragInfo, CellLayout cellLayout) {
        onDropExternal(x, y, dragInfo, cellLayout, false);
    }

    private void onDropExternal(int x, int y, Object dragInfo, CellLayout cellLayout,
            boolean insertAtFirst) {
        // Drag from somewhere else
        ItemInfo info = (ItemInfo) dragInfo;

        View view;

        switch (info.itemType) {
        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
            if (info.container == NO_ID) {
                // Came from all apps -- make a copy
                info = new ApplicationInfo((ApplicationInfo) info);
            }
            view = mLauncher.createShortcut(R.layout.application, cellLayout,
                    (ApplicationInfo) info);
            break;
        case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
            view = FolderIcon.fromXml(R.layout.folder_icon, mLauncher,
                    (ViewGroup) getChildAt(mCurrentScreen), ((UserFolderInfo) info));
            break;
        case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
            view = LiveFolderIcon.fromXml(
                    R.layout.live_folder_icon, mLauncher,
                    (ViewGroup) getChildAt(mCurrentScreen),(LiveFolderInfo) info);
            break;
        default:
            throw new IllegalStateException("Unknown item type: " + info.itemType);
        }

        cellLayout.addView(view, insertAtFirst ? 0 : -1);
        view.setOnLongClickListener(mLongClickListener);
        mTargetCell = estimateDropCell(x, y, 1, 1, view, cellLayout, mTargetCell);
        cellLayout.onDropChild(view, mTargetCell);
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();

        final LauncherModel model = Launcher.getModel();
        model.addDesktopItem(info);
        LauncherModel.addOrMoveItemInDatabase(mLauncher, info,
                LauncherSettings.Favorites.CONTAINER_DESKTOP, mCurrentScreen, lp.cellX, lp.cellY);
    }

    /**
     * Return the current {@link CellLayout}, correctly picking the destination
     * screen while a scroll is in progress.
     */
    private CellLayout getCurrentDropLayout() {
        int index = mScroller.isFinished() ? mCurrentScreen : mNextScreen;
        final CellLayout layout = (CellLayout) getChildAt(index);
        if (layout!=null)
            return layout;
        else
            return (CellLayout) getChildAt(mCurrentScreen);
    }

    /**
     * {@inheritDoc}
     */
    public boolean acceptDrop(DragSource source, int x, int y,
            int xOffset, int yOffset, Object dragInfo) {
        final CellLayout layout = getCurrentDropLayout();
        final CellLayout.CellInfo cellInfo = mDragInfo;
        final int spanX = cellInfo == null ? 1 : cellInfo.spanX;
        final int spanY = cellInfo == null ? 1 : cellInfo.spanY;

        if (mVacantCache == null) {
            final View ignoreView = cellInfo == null ? null : cellInfo.cell;
            mVacantCache = layout.findAllVacantCells(null, ignoreView);
        }

        return mVacantCache.findCellForSpan(mTempEstimate, spanX, spanY, false);
    }

    /**
     * {@inheritDoc}
     */
    public Rect estimateDropLocation(int x, int y, int xOffset, int yOffset, Rect recycle) {
        final CellLayout layout = getCurrentDropLayout();

        final CellLayout.CellInfo cellInfo = mDragInfo;
        final int spanX = cellInfo == null ? 1 : cellInfo.spanX;
        final int spanY = cellInfo == null ? 1 : cellInfo.spanY;
        final View ignoreView = cellInfo == null ? null : cellInfo.cell;

        final Rect location = recycle != null ? recycle : new Rect();

        // Find drop cell and convert into rectangle
        int[] dropCell = estimateDropCell(x - xOffset, y - yOffset,
                spanX, spanY, ignoreView, layout, mTempCell);

        if (dropCell == null) {
            return null;
        }

        layout.cellToPoint(dropCell[0], dropCell[1], mTempEstimate);
        location.left = mTempEstimate[0];
        location.top = mTempEstimate[1];

        layout.cellToPoint(dropCell[0] + spanX, dropCell[1] + spanY, mTempEstimate);
        location.right = mTempEstimate[0];
        location.bottom = mTempEstimate[1];

        return location;
    }

    /**
     * Calculate the nearest cell where the given object would be dropped.
     */
    private int[] estimateDropCell(int pixelX, int pixelY,
            int spanX, int spanY, View ignoreView, CellLayout layout, int[] recycle) {
        // Create vacant cell cache if none exists
        if (mVacantCache == null) {
            mVacantCache = layout.findAllVacantCells(null, ignoreView);
        }

        // Find the best target drop location
        return layout.findNearestVacantArea(pixelX, pixelY, spanX, spanY, mVacantCache, recycle);
    }

    void setLauncher(Launcher launcher) {
        mLauncher = launcher;
        registerProvider();
        if(mLauncher.getDesktopIndicator()!=null)mLauncher.getDesktopIndicator().setItems(mHomeScreens);
    }

    public void setDragger(DragController dragger) {
        mDragger = dragger;
    }

    public void onDropCompleted(View target, boolean success) {
        // This is a bit expensive but safe
        clearVacantCache();
        if (success){
            if (target != this && mDragInfo != null) {
                final CellLayout cellLayout = (CellLayout) getChildAt(mDragInfo.screen);
                cellLayout.removeView(mDragInfo.cell);
                final Object tag = mDragInfo.cell.getTag();
                Launcher.getModel().removeDesktopItem((ItemInfo) tag);
            }
        } else {
            if (mDragInfo != null) {
                final CellLayout cellLayout = (CellLayout) getChildAt(mDragInfo.screen);
                cellLayout.onDropAborted(mDragInfo.cell);
            }
        }

        mDragInfo = null;
    }

    public void scrollLeft() {
        clearVacantCache();
        if(mNextScreen!=INVALID_SCREEN){
        	mCurrentScreen=mNextScreen;
        	mNextScreen=INVALID_SCREEN;
        }
        if (mNextScreen == INVALID_SCREEN && mCurrentScreen > 0) {
            snapToScreen(mCurrentScreen - 1);
        }
    }

    public void scrollRight() {
        clearVacantCache();
        if(mNextScreen!=INVALID_SCREEN){
        	mCurrentScreen=mNextScreen;
        	mNextScreen=INVALID_SCREEN;
        }
        if (mNextScreen == INVALID_SCREEN && mCurrentScreen < getChildCount() -1) {
            snapToScreen(mCurrentScreen + 1);
        }
    }

    public int getScreenForView(View v) {
        int result = -1;
        if (v != null) {
            ViewParent vp = v.getParent();
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                if (vp == getChildAt(i)) {
                    return i;
                }
            }
        }
        return result;
    }

    /**
     * Find a search widget on the given screen
     */
    /*private Search findSearchWidget(CellLayout screen) {
        final int count = screen.getChildCount();
        for (int i = 0; i < count; i++) {
            View v = screen.getChildAt(i);
            if (v instanceof Search) {
                return (Search) v;
            }
        }
        return null;
    }*/

    /**
     * Gets the first search widget on the current screen, if there is one.
     * Returns <code>null</code> otherwise.
     */
    /*public Search findSearchWidgetOnCurrentScreen() {
        CellLayout currentScreen = (CellLayout)getChildAt(mCurrentScreen);
        return findSearchWidget(currentScreen);
    }*/

    public Folder getFolderForTag(Object tag) {
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            CellLayout currentScreen = ((CellLayout) getChildAt(screen));
            int count = currentScreen.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = currentScreen.getChildAt(i);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
                if (lp.cellHSpan == mDesktopColumns && lp.cellVSpan == mDesktopRows && child instanceof Folder) {
                    Folder f = (Folder) child;
                    if (f.getInfo() == tag) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    public View getViewForTag(Object tag) {
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            CellLayout currentScreen = ((CellLayout) getChildAt(screen));
            int count = currentScreen.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = currentScreen.getChildAt(i);
                if (child.getTag() == tag) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Unlocks the SlidingDrawer so that touch events are processed.
     *
     * @see #lock()
     */
    public void unlock() {
        mLocked = false;
    }

    /**
     * Locks the SlidingDrawer so that touch events are ignores.
     *
     * @see #unlock()
     */
    public void lock() {
        mLocked = true;
    }

    /**
     * @return True is long presses are still allowed for the current touch
     */
    public boolean allowLongPress() {
        return mAllowLongPress;
    }

    /**
     * Set true to allow long-press events to be triggered, usually checked by
     * {@link Launcher} to accept or block dpad-initiated long-presses.
     */
    public void setAllowLongPress(boolean allowLongPress) {
        mAllowLongPress = allowLongPress;
    }

    void removeShortcutsForPackage(String packageName) {
        final ArrayList<View> childrenToRemove = new ArrayList<View>();
        final LauncherModel model = Launcher.getModel();
        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i);
            int childCount = layout.getChildCount();

            childrenToRemove.clear();

            for (int j = 0; j < childCount; j++) {
                final View view = layout.getChildAt(j);
                Object tag = view.getTag();

                if (tag instanceof ApplicationInfo) {
                    final ApplicationInfo info = (ApplicationInfo) tag;
                    // We need to check for ACTION_MAIN otherwise getComponent() might
                    // return null for some shortcuts (for instance, for shortcuts to
                    // web pages.)
                    final Intent intent = info.intent;
                    final ComponentName name = intent.getComponent();

                    if (Intent.ACTION_MAIN.equals(intent.getAction()) &&
                            name != null && packageName.equals(name.getPackageName())) {
                        model.removeDesktopItem(info);
                        LauncherModel.deleteItemFromDatabase(mLauncher, info);
                        childrenToRemove.add(view);
                    }
                } else if (tag instanceof UserFolderInfo) {
                    final UserFolderInfo info = (UserFolderInfo) tag;
                    final ArrayList<ApplicationInfo> contents = info.contents;
                    final ArrayList<ApplicationInfo> toRemove = new ArrayList<ApplicationInfo>(1);
                    final int contentsCount = contents.size();
                    boolean removedFromFolder = false;

                    for (int k = 0; k < contentsCount; k++) {
                        final ApplicationInfo appInfo = contents.get(k);
                        final Intent intent = appInfo.intent;
                        final ComponentName name = intent.getComponent();

                        if (Intent.ACTION_MAIN.equals(intent.getAction()) &&
                                name != null && packageName.equals(name.getPackageName())) {
                            toRemove.add(appInfo);
                            LauncherModel.deleteItemFromDatabase(mLauncher, appInfo);
                            removedFromFolder = true;
                        }
                    }

                    contents.removeAll(toRemove);
                    if (removedFromFolder) {
                        final Folder folder = getOpenFolder();
                        if (folder != null) folder.notifyDataSetChanged();
                    }
                }
            }

            childCount = childrenToRemove.size();
            for (int j = 0; j < childCount; j++) {
                layout.removeViewInLayout(childrenToRemove.get(j));
            }

            if (childCount > 0) {
                layout.requestLayout();
                layout.invalidate();
            }
        }
    }

    void updateShortcutFromApplicationInfo(ApplicationInfo info) {
    	final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i);
            int childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                final View view = layout.getChildAt(j);
                Object tag = view.getTag();
                if (tag instanceof ApplicationInfo) {
                	ApplicationInfo tagInfo = (ApplicationInfo)tag;
                    if (tagInfo.id == info.id)
                    {
                    	tagInfo.assignFrom(info);

                    	View newview = mLauncher.createShortcut(R.layout.application, layout, tagInfo);
                    	layout.removeView(view);
                    	addInScreen(newview, info.screen, info.cellX, info.cellY, info.spanX,
                    			info.spanY, false);
                    	break;
                    }
                }
            }
        }
    }

    void updateShortcutsForPackage(String packageName) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i);
            int childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                final View view = layout.getChildAt(j);
                Object tag = view.getTag();
                if (tag instanceof ApplicationInfo) {
                    ApplicationInfo info = (ApplicationInfo) tag;
                    // We need to check for ACTION_MAIN otherwise getComponent() might
                    // return null for some shortcuts (for instance, for shortcuts to
                    // web pages.)
                    final Intent intent = info.intent;
                    final ComponentName name = intent.getComponent();
                    if ((info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                            info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT)&&
                            Intent.ACTION_MAIN.equals(intent.getAction()) && name != null &&
                            packageName.equals(name.getPackageName())) {

                        final Drawable icon = Launcher.getModel().getApplicationInfoIcon(
                                mLauncher.getPackageManager(), info, mLauncher);
                        if (icon != null && icon != info.icon) {
                            info.icon.setCallback(null);
                            info.icon = Utilities.createIconThumbnail(icon, mContext);
                            info.filtered = true;
                            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null,
                                    info.icon, null, null);
                        }
                    }
                }else if (tag instanceof UserFolderInfo){
                	//TODO: ADW: Maybe there are icons inside folders.... need to update them too
                    final UserFolderInfo info = (UserFolderInfo) tag;
                    final ArrayList<ApplicationInfo> contents = info.contents;
                    final int contentsCount = contents.size();
                    for (int k = 0; k < contentsCount; k++) {
                        final ApplicationInfo appInfo = contents.get(k);
                        final Intent intent = appInfo.intent;
                        final ComponentName name = intent.getComponent();
                        if ((appInfo.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                                info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT)&&
                                Intent.ACTION_MAIN.equals(intent.getAction()) && name != null &&
                                packageName.equals(name.getPackageName())) {

                            final Drawable icon = Launcher.getModel().getApplicationInfoIcon(
                                    mLauncher.getPackageManager(), appInfo, mLauncher);
                            boolean folderUpdated=false;
                            if (icon != null && icon != appInfo.icon) {
                            	appInfo.icon.setCallback(null);
                            	appInfo.icon = Utilities.createIconThumbnail(icon, mLauncher);
                            	appInfo.filtered = true;
                            	folderUpdated=true;
                            }
                            if(folderUpdated){
                                final Folder folder = getOpenFolder();
                                if (folder != null) folder.notifyDataSetChanged();
                            }
                        }
                    }
                }
            }
        }
    }

    void moveToDefaultScreen() {
        snapToScreen(mDefaultScreen);
        getChildAt(mDefaultScreen).requestFocus();
    }

    public static class SavedState extends BaseSavedState {
        int currentScreen = -1;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            currentScreen = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(currentScreen);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
    /**************************************************
     * ADW: Custom modifications
     */

    /**
     * Pagination indicators (dots)
     */
    void setIndicators(Drawable previous, Drawable next) {
        mPreviousIndicator = previous;
        mNextIndicator = next;
    	indicatorLevels(mCurrentScreen);
    }
    void indicatorLevels(int mCurrent){
    	int numScreens=getChildCount();
    	mPreviousIndicator.setLevel(mCurrent);
    	mNextIndicator.setLevel(numScreens-mCurrent-1);
    }
    /**
     * ADW: Make a local copy of wallpaper bitmap to use instead wallpapermanager
     * only when detected not being a Live Wallpaper
     */
	public void setWallpaper(boolean fromIntentReceiver){
		if(mWallpaperManager.getWallpaperInfo()!=null || !wallpaperHack){
				mWallpaperDrawable=null;
				mWallpaperLoaded=false;
			lwpSupport=true;
		}else{
			if(fromIntentReceiver || mWallpaperDrawable==null){
				final Drawable drawable = mWallpaperManager.getDrawable();
				mWallpaperDrawable=(BitmapDrawable) drawable;
				mWallpaperLoaded=true;
			}
			lwpSupport=false;
		}
		mLauncher.setWindowBackground(lwpSupport);
		invalidate();
		requestLayout();
	}
	public void setWallpaperHack(boolean hack){
		wallpaperHack=hack;
		if(wallpaperHack && mWallpaperManager.getWallpaperInfo()==null){
			lwpSupport=false;
		}else{
			lwpSupport=true;
		}
		mLauncher.setWindowBackground(lwpSupport);
	}
	/**
	 * ADW: Set the desktop scrolling speed (default scrolling duration)
	 * @param speed
	 */
	public void setSpeed(int speed){
		mScrollingSpeed=speed;
	}
	/**
	 * ADW: Set the desktop scrolling bounce amount (0 to disable)
	 * @param amount
	 */
	public void setBounceAmount(int amount){
		mScrollingBounce=amount;
		mScroller.setInterpolator(new ElasticInterpolator(mScrollingBounce/10));
	}
	public void openSense(boolean open){
		mScroller.abortAnimation();
		enableChildrenCache();
        //TODO:ADW We nedd to find the "longer row" and get the best children width
        int maxItemsPerRow=0;
        int distro_set=getChildCount()-1;
        int numRows=distro[distro_set].length;
        for(int rows=0;rows<distro[distro_set].length;rows++){
        		if(distro[distro_set][rows]>maxItemsPerRow){
        			maxItemsPerRow=distro[distro_set][rows];
        		}
        }
        int maxPreviewHeight=(getMeasuredHeight()/numRows);
        float w = getMeasuredWidth() / maxItemsPerRow;
        int maxPreviewWidth=(int) w;
        //Decide who wins:
        float scaleW=((float)maxPreviewWidth/(float)getWidth());
        float scaleH=((float)maxPreviewHeight/(float)getHeight());
        previewScale=(scaleW>scaleH)?scaleH:scaleW;
        if(previewScale>=1)previewScale=.8f;

		if(open){
			mSensemode=true;
			isAnimating=true;
			mStatus=SENSE_OPENING;
			startTime=0;
		}else{
			mSensemode=true;
			isAnimating=true;
			mStatus=SENSE_CLOSING;
			startTime=0;
		}
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		int saveCount = canvas.save();
		if(mSensemode){
			if(isAnimating||mStatus==SENSE_OPEN){
				long currentTime=SystemClock.uptimeMillis()-startTime;
				Rect r1=new Rect(0, 0, child.getWidth(), child.getHeight());
				RectF r2=getScaledChild(child);
				float x=0; float y=0; float width=0;float height=0; float alpha=255;
				if(mStatus==SENSE_OPENING){
					alpha=easeOut(currentTime,0,100,mAnimationDuration);
					x=easeOut(currentTime, child.getLeft(), r2.left, mAnimationDuration);
					y=easeOut(currentTime, child.getTop(), r2.top, mAnimationDuration);
					width=easeOut(currentTime, child.getRight(), r2.right, mAnimationDuration);
					height=easeOut(currentTime, child.getBottom(), r2.bottom, mAnimationDuration);
				}else if (mStatus==SENSE_CLOSING){
					alpha=easeOut(currentTime,100,0,mAnimationDuration);
					x=easeOut(currentTime, r2.left,child.getLeft(), mAnimationDuration);
					y=easeOut(currentTime, r2.top, child.getTop(), mAnimationDuration);
					width=easeOut(currentTime, r2.right, child.getRight(), mAnimationDuration);
					height=easeOut(currentTime, r2.bottom, child.getBottom(), mAnimationDuration);
				}else if(mStatus==SENSE_OPEN){
					x=r2.left;
					y=r2.top;
					width=r2.right;
					height=r2.bottom;
					alpha=100;
				}
				float scale=((width-x)/r1.width());
				canvas.save();
				canvas.translate(x, y);
				canvas.scale(scale,scale);
				mPaint.setAlpha((int) alpha);
				canvas.drawRoundRect(new RectF(r1.left+5,r1.top+5,r1.right-5, r1.bottom-5), 15f, 15f, mPaint);
				mPaint.setAlpha(255);
				child.draw(canvas);
				canvas.restore();
			}else{
				child.draw(canvas);
			}
		}else{
			if(child!=null)super.drawChild(canvas, child, drawingTime);
		}
		canvas.restoreToCount(saveCount);
		return true;
	}

    /**
     * ADW: easing functions for animation
     */
	static float easeOut (float time, float begin, float end, float duration) {
		float change=end- begin;
		float value= change*((time=time/duration-1)*time*time + 1) + begin;
		if(change>0 && value>end) value=end;
		if(change<0 && value<end) value=end;
		return value;
	}
	static float easeIn (float time, float begin, float end, float duration) {
		float change=end- begin;
		float value=change*(time/=duration)*time*time + begin;
		if(change>0 && value>end) value=end;
		if(change<0 && value<end) value=end;
		return value;
	}
	static float easeInOut (float time, float begin, float end, float duration) {
		float change=end- begin;
		if ((time/=duration/2.0f) < 1) return change/2.0f*time*time*time + begin;
		return change/2.0f*((time-=2.0f)*time*time + 2.0f) + begin;
	}
	private RectF getScaledChild(View child){
        final int count = getChildCount();
        final int width = getWidth();//r - l;
        final int height = getHeight();//b-t;
        int xpos = getScrollX();
        int ypos = 0;

        int distro_set=count-1;
        int childPos=0;

        int childWidth=(int) (width*previewScale);
        int childHeight=(int) (height*previewScale);

        final int topMargin=(height/2)-((childHeight*distro[distro_set].length)/2);
        for(int rows=0;rows<distro[distro_set].length;rows++){
        	final int leftMargin=(width/2)-((childWidth*distro[distro_set][rows])/2);
        	for(int columns=0;columns<distro[distro_set][rows];columns++){
                if(childPos>getChildCount()-1) break;
        		final View c = getChildAt(childPos);
                if (child== c) {
                    return new RectF(leftMargin+xpos, topMargin+ypos, leftMargin+xpos + childWidth, topMargin+ypos + childHeight);
                }else{
                	xpos += childWidth;
                }
                childPos++;
        	}
            xpos = getScrollX();
            ypos += childHeight;
        }
        return new RectF();
	}
	private void findClickedPreview(float x, float y){
		for(int i=0;i<getChildCount();i++){
			RectF tmp=getScaledChild(getChildAt(i));
			if (tmp.contains(x+getScrollX(), y+getScrollY())){
				if(mCurrentScreen!=i){
			        mLauncher.dismissPreviews();
			        mScroller.setInterpolator(new ElasticInterpolator(0));
			        mRevertInterpolatorOnScrollFinish=true;
			        snapToScreen(i);
			        postInvalidate();
				}else{
					mLauncher.dismissPreviews();
				}
			}
		}
	}
	/**
	 * Wysie: Multitouch methods/events
	 */
	public Object getDraggableObjectAtPoint(PointInfo pt) {
		return this;
	}

	public void getPositionAndScale(Object obj,
			PositionAndScale objPosAndScaleOut) {
		objPosAndScaleOut.set(0.0f, 0.0f, true, 1.0f, false, 0.0f, 0.0f, false, 0.0f);
	}

	public void selectObject(Object obj, PointInfo pt) {
		if(mStatus!=SENSE_OPEN){
			mAllowLongPress=false;
		}else{
			mAllowLongPress=true;
		}
	}

	public boolean setPositionAndScale(Object obj,
			PositionAndScale update, PointInfo touchPoint) {
        float newRelativeScale = update.getScale();
        int targetZoom = (int) Math.round(Math.log(newRelativeScale) * ZOOM_LOG_BASE_INV);
        // Only works for pinch in
        if (targetZoom < 0 && mStatus==SENSE_CLOSED) { // Change to > 0 for pinch out, != 0 for both pinch in and out.
        	mLauncher.showPreviews(mLauncher.getDrawerHandle(), 0, getChildCount());
        	invalidate();
            return true;
        }
        return false;
	}

	public Activity getLauncherActivity() {
		// TODO Auto-generated method stub
		return mLauncher;
	}
	public int currentDesktopRows(){
		return mDesktopRows;
	}
	public int currentDesktopColumns(){
		return mDesktopColumns;
	}
    public boolean isWidgetAtLocationScrollable(int x, int y) {
		// will return true if widget at this position is scrollable.
    	// Get current screen from the whole desktop
    	CellLayout currentScreen = (CellLayout) getChildAt(mCurrentScreen);
    	int[] cell_xy = new int[2];
    	// Get the cell where the user started the touch event
    	currentScreen.pointToCellExact(x, y, cell_xy);
        int count = currentScreen.getChildCount();

        // Iterate to find which widget is located at that cell
        // Find widget backwards from a cell does not work with (View)currentScreen.getChildAt(cell_xy[0]*currentScreen.getCountX etc etc); As the widget is positioned at the very first cell of the widgetspace
        for (int i = 0; i < count; i++) {
            View child = currentScreen.getChildAt(i);
            if ( child !=null)
            {
            	// Get Layount graphical info about this widget
	            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
	            // Calculate Cell Margins
	            int left_cellmargin = lp.cellX;
	            int rigth_cellmargin = lp.cellX+lp.cellHSpan;
	            int top_cellmargin = lp.cellY;
	            int botton_cellmargin = lp.cellY + lp.cellVSpan;
	            // See if the cell where we touched is inside the Layout of the widget beeing analized
	            if (cell_xy[0] >= left_cellmargin && cell_xy[0] < rigth_cellmargin && cell_xy[1] >= top_cellmargin && cell_xy[1] < botton_cellmargin)  {
	            	try {
		            	// Get Widget ID
		            	int id = ((AppWidgetHostView)child).getAppWidgetId();
		            	// Ask to WidgetSpace if the Widget identified itself when created as 'Scrollable'
		            	return isWidgetScrollable(id);
	            	} catch (Exception e)
	            	{}
	            }
           }
        }
        return false;
	}
    public void unbindWidgetScrollableViews() {
    	unbindWidgetScrollable();
	}
    public void unbindWidgetScrollableViewsForWidget(int widgetId) {
    	Log.d("WORKSPACE", "trying to completely unallocate widget ID="+widgetId);
    	unbindWidgetScrollableId(widgetId);
	}

	public void setDefaultScreen(int defaultScreen) {
		mDefaultScreen=defaultScreen;
	}
	public void setWallpaperScroll(boolean scroll){
		mWallpaperScroll=scroll;
		postInvalidate();
	}
	/**
	 * ADW: hide live wallpaper to speedup the app drawer
	 * I think the live wallpaper needs to support the "hide" command
	 * and not every LWP supports it.
	 * http://developer.android.com/intl/de/reference/android/app/WallpaperManager.html#sendWallpaperCommand%28android.os.IBinder,%20java.lang.String,%20int,%20int,%20int,%20android.os.Bundle%29
	 * @param hide
	 */
    public void hideWallpaper(boolean hide) {
    	if(getWindowToken()!=null && mLauncher.getWindow()!=null){
	    	if (hide){
	    		mWallpaperManager.sendWallpaperCommand(getWindowToken(),
	    				"hide", 0, 0, 0, null);
	    	}else{
	    		mWallpaperManager.sendWallpaperCommand(getWindowToken(),
	    				"show", 0, 0, 0, null);
	    	}
    	}
    }
    /**
     * ADW: Remove the specified screen and all the contents
     * Almos update remaining screens content inside model
     * @param screen
     */
    protected void removeScreen(int screen){
        final CellLayout layout = (CellLayout) getChildAt(screen);
        int childCount = layout.getChildCount();
        final LauncherModel model = Launcher.getModel();
        for (int j = 0; j < childCount; j++) {
            final View view = layout.getChildAt(j);
            Object tag = view.getTag();
            //DELETE ALL ITEMS FROM SCREEN
            final ItemInfo item = (ItemInfo) tag;
            if (item!=null && item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                if (item instanceof LauncherAppWidgetInfo) {
                    model.removeDesktopAppWidget((LauncherAppWidgetInfo) item);
                } else {
                    model.removeDesktopItem(item);
                }
            }
            if (item!=null && item instanceof UserFolderInfo) {
                final UserFolderInfo userFolderInfo = (UserFolderInfo)item;
                LauncherModel.deleteUserFolderContentsFromDatabase(mLauncher, userFolderInfo);
                model.removeUserFolder(userFolderInfo);
            } else if (item!=null && item instanceof LauncherAppWidgetInfo) {
                final LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) item;
                final LauncherAppWidgetHost appWidgetHost = mLauncher.getAppWidgetHost();
                if (appWidgetHost != null) {
                    appWidgetHost.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
                }
            }
            LauncherModel.deleteItemFromDatabase(mLauncher, item);
        }
        moveItemPositions(screen, -1);
        removeView(getChildAt(screen));
        if(getChildCount()<=mCurrentScreen){
        	mCurrentScreen=0;
        	setCurrentScreen(mCurrentScreen);
        }
        if(getChildCount()<=mDefaultScreen){
        	AlmostNexusSettingsHelper.setDefaultScreen(mLauncher, 0);
        	mDefaultScreen=0;
        }
    	if(mLauncher.getDesktopIndicator()!=null)mLauncher.getDesktopIndicator().setItems(getChildCount());
    	indicatorLevels(mCurrentScreen);
        AlmostNexusSettingsHelper.setDesktopScreens(mLauncher, getChildCount());
    }
    protected CellLayout addScreen(int position){
        LayoutInflater layoutInflter=LayoutInflater.from(mLauncher);
    	CellLayout screen=(CellLayout)layoutInflter.inflate(R.layout.workspace_screen, this, false);
    	addView(screen,position);
    	screen.setOnLongClickListener(mLongClickListener);
    	if(mLauncher.getDesktopIndicator()!=null)mLauncher.getDesktopIndicator().setItems(getChildCount());
    	indicatorLevels(mCurrentScreen);
    	AlmostNexusSettingsHelper.setDesktopScreens(mLauncher, getChildCount());
    	moveItemPositions(position, +1);
    	return screen;
    }
    protected void swapScreens(int screen_a, int screen_b){
    	//Swap database positions for both screens
        CellLayout layout = (CellLayout) getChildAt(screen_a);
        layout.setScreen(screen_b);
        int childCount = layout.getChildCount();
        for (int j = 0; j < childCount; j++) {
            final View view = layout.getChildAt(j);
            Object tag = view.getTag();
            final ItemInfo item = (ItemInfo) tag;
            if(item!=null && item.container==LauncherSettings.Favorites.CONTAINER_DESKTOP){
                LauncherModel.moveItemInDatabase(mLauncher, item, item.container, screen_b, item.cellX, item.cellY);
            }
        }
        layout = (CellLayout) getChildAt(screen_b);
        layout.setScreen(screen_a);
        childCount = layout.getChildCount();
        for (int j = 0; j < childCount; j++) {
            final View view = layout.getChildAt(j);
            Object tag = view.getTag();
            final ItemInfo item = (ItemInfo) tag;
            if(item!=null && item.container==LauncherSettings.Favorites.CONTAINER_DESKTOP){
                LauncherModel.moveItemInDatabase(mLauncher, item, item.container, screen_a, item.cellX, item.cellY);
            }
        }
    	//swap the views
    	CellLayout a=(CellLayout) getChildAt(screen_a);
    	LayoutParams lp=a.getLayoutParams();
    	detachViewFromParent(a);
    	attachViewToParent(a, screen_b, lp);
    	requestLayout();
    }
    private void moveItemPositions(int screen, int diff){
        //MOVE THE REMAINING ITEMS FROM OTHER SCREENS
        for (int i=screen+1;i<getChildCount();i++){
            final CellLayout layout = (CellLayout) getChildAt(i);
            layout.setScreen(layout.getScreen()+diff);
            int childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                final View view = layout.getChildAt(j);
                Object tag = view.getTag();
                final ItemInfo item = (ItemInfo) tag;
                if(item!=null && item.container==LauncherSettings.Favorites.CONTAINER_DESKTOP){
	                LauncherModel.moveItemInDatabase(mLauncher, item, item.container, item.screen+diff, item.cellX, item.cellY);
                }
            }
        }
    }
    void updateCountersForPackage(String packageName,int counter, int color) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i);
            int childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                final View view = layout.getChildAt(j);
                Object tag = view.getTag();
                if (tag instanceof ApplicationInfo) {
                    ApplicationInfo info = (ApplicationInfo) tag;
                    // We need to check for ACTION_MAIN otherwise getComponent() might
                    // return null for some shortcuts (for instance, for shortcuts to
                    // web pages.)
                    final Intent intent = info.intent;
                    final ComponentName name = intent.getComponent();
                    if ((info.itemType==LauncherSettings.Favorites.ITEM_TYPE_APPLICATION||
                            info.itemType==LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) &&
                        Intent.ACTION_MAIN.equals(intent.getAction()) && name != null &&
                        packageName.equals(name.getPackageName())) {
                        ((BubbleTextView) view).setCounter(counter, color);
                        view.invalidate();
                        Launcher.getModel().updateCounterDesktopItem(info, counter, color);
                    }
                }
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // TODO Auto-generated method stub
        super.onSizeChanged(w, h, oldw, oldh);
        if(mLauncher!=null)mWallpaperY=h - mLauncher.getWindow().getDecorView().getHeight();
    }

}
