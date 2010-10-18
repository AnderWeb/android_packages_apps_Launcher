/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

// This class is used by CropImage to display a highlighted cropping rectangle
// overlayed with the image. There are two coordinate spaces in use. One is
// image, another is screen. computeLayout() uses mMatrix to map from image
// space to screen space.
class ResizeViewHandler extends View {

    public ResizeViewHandler(Context context) {
        super(context);
    }

    @SuppressWarnings("unused")
    private static final String TAG = "HighlightView";

    public static final int GROW_NONE        = (1 << 0);
    public static final int GROW_LEFT_EDGE   = (1 << 1);
    public static final int GROW_RIGHT_EDGE  = (1 << 2);
    public static final int GROW_TOP_EDGE    = (1 << 3);
    public static final int GROW_BOTTOM_EDGE = (1 << 4);
    public static final int MOVE             = (1 << 5);

    private void init() {
        android.content.res.Resources resources = getResources();
        mResizeDrawableWidth =
                resources.getDrawable(R.drawable.camera_crop_width);
        mResizeDrawableHeight =
                resources.getDrawable(R.drawable.camera_crop_height);
        mResizeDrawableDiagonal =
                resources.getDrawable(R.drawable.indicator_autocrop);
    }

    boolean mIsFocused;
    boolean mHidden;

    @Override
	public boolean hasFocus() {
        return mIsFocused;
    }

    public void setFocus(boolean f) {
        mIsFocused = f;
    }

    public void setHidden(boolean hidden) {
        mHidden = hidden;
    }

    @Override
	public void draw(Canvas canvas) {
        if (mHidden) {
            return;
        }
        canvas.save();
        Path path = new Path();
        Rect viewDrawingRect = new Rect();
        getDrawingRect(viewDrawingRect);
        if (mCircle) {
            float width  = mDrawRect.width();
            float height = mDrawRect.height();
            path.addCircle(mDrawRect.left + (width  / 2),
                           mDrawRect.top + (height / 2),
                           width / 2,
                           Path.Direction.CW);
            mOutlinePaint.setColor(0xFFEF04D6);
        } else {
            path.addRect(new RectF(mDrawRect), Path.Direction.CW);
            mOutlinePaint.setColor(0xFFFF8A00);
        }
        canvas.clipPath(path, Region.Op.DIFFERENCE);
        canvas.drawRect(viewDrawingRect,mNoFocusPaint);
        if(mCollision){
            canvas.clipPath(path, Region.Op.REVERSE_DIFFERENCE);
            canvas.drawRect(viewDrawingRect,mFocusPaint);
        }

        canvas.restore();
        canvas.drawPath(path, mOutlinePaint);

        if (mCircle) {
            int width  = mResizeDrawableDiagonal.getIntrinsicWidth();
            int height = mResizeDrawableDiagonal.getIntrinsicHeight();

            int d  = (int) Math.round(Math.cos(/*45deg*/Math.PI / 4D)
                    * (mDrawRect.width() / 2D));
            int x  = mDrawRect.left
                    + (mDrawRect.width() / 2) + d - width / 2;
            int y  = mDrawRect.top
                    + (mDrawRect.height() / 2) - d - height / 2;
            mResizeDrawableDiagonal.setBounds(x, y,
                    x + mResizeDrawableDiagonal.getIntrinsicWidth(),
                    y + mResizeDrawableDiagonal.getIntrinsicHeight());
            mResizeDrawableDiagonal.draw(canvas);
        } else {
            int left    = mDrawRect.left   + 1;
            int right   = mDrawRect.right  + 1;
            int top     = mDrawRect.top    + 4;
            int bottom  = mDrawRect.bottom + 3;

            int widthWidth   =
                    mResizeDrawableWidth.getIntrinsicWidth() / 2;
            int widthHeight  =
                    mResizeDrawableWidth.getIntrinsicHeight() / 2;
            int heightHeight =
                    mResizeDrawableHeight.getIntrinsicHeight() / 2;
            int heightWidth  =
                    mResizeDrawableHeight.getIntrinsicWidth() / 2;

            int xMiddle = mDrawRect.left
                    + ((mDrawRect.right  - mDrawRect.left) / 2);
            int yMiddle = mDrawRect.top
                    + ((mDrawRect.bottom - mDrawRect.top) / 2);

            mResizeDrawableWidth.setBounds(left - widthWidth,
                                           yMiddle - widthHeight,
                                           left + widthWidth,
                                           yMiddle + widthHeight);
            mResizeDrawableWidth.draw(canvas);

            mResizeDrawableWidth.setBounds(right - widthWidth,
                                           yMiddle - widthHeight,
                                           right + widthWidth,
                                           yMiddle + widthHeight);
            mResizeDrawableWidth.draw(canvas);

            mResizeDrawableHeight.setBounds(xMiddle - heightWidth,
                                            top - heightHeight,
                                            xMiddle + heightWidth,
                                            top + heightHeight);
            mResizeDrawableHeight.draw(canvas);

            mResizeDrawableHeight.setBounds(xMiddle - heightWidth,
                                            bottom - heightHeight,
                                            xMiddle + heightWidth,
                                            bottom + heightHeight);
            mResizeDrawableHeight.draw(canvas);
        }
    }

    public void setMode(ModifyMode mode) {
        if (mode != mMode) {
            mMode = mode;
            invalidate();
        }
    }

    // Determines which edges are hit by touching at (x, y).
    public int getHit(float x, float y) {
        Rect r = computeLayout();
        final float hysteresis = 20F;
        int retval = GROW_NONE;

        if (mCircle) {
            float distX = x - r.centerX();
            float distY = y - r.centerY();
            int distanceFromCenter =
                    (int) Math.sqrt(distX * distX + distY * distY);
            int radius  = mDrawRect.width() / 2;
            int delta = distanceFromCenter - radius;
            if (Math.abs(delta) <= hysteresis) {
                if (Math.abs(distY) > Math.abs(distX)) {
                    if (distY < 0) {
                        retval = GROW_TOP_EDGE;
                    } else {
                        retval = GROW_BOTTOM_EDGE;
                    }
                } else {
                    if (distX < 0) {
                        retval = GROW_LEFT_EDGE;
                    } else {
                        retval = GROW_RIGHT_EDGE;
                    }
                }
            } else if (distanceFromCenter < radius) {
                retval = MOVE;
            } else {
                retval = GROW_NONE;
            }
        } else {
            // verticalCheck makes sure the position is between the top and
            // the bottom edge (with some tolerance). Similar for horizCheck.
            boolean verticalCheck = (y >= r.top - hysteresis)
                    && (y < r.bottom + hysteresis);
            boolean horizCheck = (x >= r.left - hysteresis)
                    && (x < r.right + hysteresis);

            // Check whether the position is near some edge(s).
            if ((Math.abs(r.left - x)     < hysteresis)  &&  verticalCheck) {
                retval |= GROW_LEFT_EDGE;
            }
            if ((Math.abs(r.right - x)    < hysteresis)  &&  verticalCheck) {
                retval |= GROW_RIGHT_EDGE;
            }
            if ((Math.abs(r.top - y)      < hysteresis)  &&  horizCheck) {
                retval |= GROW_TOP_EDGE;
            }
            if ((Math.abs(r.bottom - y)   < hysteresis)  &&  horizCheck) {
                retval |= GROW_BOTTOM_EDGE;
            }

            // Not near any edge but inside the rectangle: move.
            if (retval == GROW_NONE && r.contains((int) x, (int) y)) {
                retval = MOVE;
            }
        }
        return retval;
    }

    // Handles motion (dx, dy) in screen space.
    // The "edge" parameter specifies which edges the user is dragging.
    void handleMotion(int edge, float dx, float dy) {
        Rect r = computeLayout();
        if (edge == GROW_NONE) {
            return;
        } else if (edge == MOVE) {
            // Convert to image space before sending to moveBy().
            //moveBy(dx * (mCropRect.width() / r.width()),
                   //dy * (mCropRect.height() / r.height()));
        } else {
            if (((GROW_LEFT_EDGE | GROW_RIGHT_EDGE) & edge) == 0) {
                dx = 0;
            }

            if (((GROW_TOP_EDGE | GROW_BOTTOM_EDGE) & edge) == 0) {
                dy = 0;
            }

            // Convert to image space before sending to growBy().
            float xDelta = dx * (mCropRect.width() / r.width());
            float yDelta = dy * (mCropRect.height() / r.height());
            /*growBy((((edge & GROW_LEFT_EDGE) != 0) ? -1 : 1) * xDelta,
                    (((edge & GROW_TOP_EDGE) != 0) ? -1 : 1) * yDelta);*/
            if((edge & GROW_LEFT_EDGE) == GROW_LEFT_EDGE)
                growBy(-1*xDelta, 0, true);
            else if((edge & GROW_RIGHT_EDGE) == GROW_RIGHT_EDGE)
                growBy(1*xDelta, 0, false);
            else if((edge & GROW_TOP_EDGE) == GROW_TOP_EDGE)
                growBy(0, -1*yDelta, true);
            else if((edge & GROW_BOTTOM_EDGE) == GROW_BOTTOM_EDGE)
                growBy(0,1*yDelta, false);
        }
    }

    // Grows the cropping rectange by (dx, dy) in image space.
    void moveBy(float dx, float dy) {
        Rect invalRect = new Rect(mDrawRect);

        mCropRect.offset(dx, dy);

        // Put the cropping rectangle inside image rectangle.
        mCropRect.offset(
                Math.max(0, mImageRect.left - mCropRect.left),
                Math.max(0, mImageRect.top  - mCropRect.top));

        mCropRect.offset(
                Math.min(0, mImageRect.right  - mCropRect.right),
                Math.min(0, mImageRect.bottom - mCropRect.bottom));

        mDrawRect = computeLayout();
        invalRect.union(mDrawRect);
        invalRect.inset(-10, -10);
        invalidate(invalRect);
    }

    // Grows the cropping rectange by (dx, dy) in image space.
    void growBy(float dx, float dy,boolean from) {
        if (mMaintainAspectRatio) {
            if (dx != 0) {
                dy = dx / mInitialAspectRatio;
            } else if (dy != 0) {
                dx = dy * mInitialAspectRatio;
            }
        }

        // Don't let the cropping rectangle grow too fast.
        // Grow at most half of the difference between the image rectangle and
        // the cropping rectangle.
        RectF r = new RectF(mCropRect);
        if (dx > 0F && r.width() + 2 * dx > mImageRect.width()) {
            float adjustment = (mImageRect.width() - r.width()) / 2F;
            dx = adjustment;
            if (mMaintainAspectRatio) {
                dy = dx / mInitialAspectRatio;
            }
        }
        if (dy > 0F && r.height() + 2 * dy > mImageRect.height()) {
            float adjustment = (mImageRect.height() - r.height()) / 2F;
            dy = adjustment;
            if (mMaintainAspectRatio) {
                dx = dy * mInitialAspectRatio;
            }
        }

        //r.inset(-dx, -dy);
        if(from){
            r.set(r.left-dx, r.top-dy, r.right, r.bottom);
        }else{
            r.set(r.left, r.top,r.right+dx,r.bottom+dy);
        }

        if(r.width()<mMinWidth){
            r.left=mCropRect.left;
            r.right=mCropRect.right;
        }
        if(r.height()<mMinHeight){
            r.top=mCropRect.top;
            r.bottom=mCropRect.bottom;
        }
        // Put the cropping rectangle inside the image rectangle.
        if (r.left < mImageRect.left) {
            r.left=mImageRect.left;
        } else if (r.right > mImageRect.right) {
            r.right=mImageRect.right;
        }
        if (r.top < mImageRect.top) {
            r.top=mImageRect.top;
        } else if (r.bottom > mImageRect.bottom) {
            r.bottom=mImageRect.bottom;
        }

        mCropRect.set(r);
        mDrawRect = computeLayout();
        invalidate();
        dispatchTriggerEvent(mCropRect);
    }

    // Returns the cropping rectangle in image space.
    public Rect getCropRect() {
        return new Rect((int) mCropRect.left, (int) mCropRect.top,
                        (int) mCropRect.right, (int) mCropRect.bottom);
    }

    // Maps the cropping rectangle from image space to screen space.
    private Rect computeLayout() {
        RectF r = new RectF(mCropRect.left, mCropRect.top,
                            mCropRect.right, mCropRect.bottom);
        mMatrix.mapRect(r);
        return new Rect(Math.round(r.left), Math.round(r.top),
                        Math.round(r.right), Math.round(r.bottom));
    }

    @Override
	public void invalidate() {
        mDrawRect = computeLayout();
        super.invalidate();
    }

    public void setup(Matrix m, Rect imageRect, RectF cropRect, boolean circle,
                      boolean maintainAspectRatio, float minWidth, float minHeight) {
        if (circle) {
            maintainAspectRatio = true;
        }
        mMatrix = new Matrix(m);

        mCropRect = cropRect;
        mImageRect = new RectF(imageRect);
        mMaintainAspectRatio = maintainAspectRatio;
        mCircle = circle;

        mInitialAspectRatio = mCropRect.width() / mCropRect.height();
        mDrawRect = computeLayout();

        mFocusPaint.setARGB(125, 255, 50, 50);
        mNoFocusPaint.setARGB(125, 50, 50, 50);
        mOutlinePaint.setStrokeWidth(3F);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setAntiAlias(true);

        mMode = ModifyMode.None;
        mMinWidth=minWidth;
        mMinHeight=minHeight;
        init();
    }

    enum ModifyMode { None, Move, Grow }

    private ModifyMode mMode = ModifyMode.None;

    Rect mDrawRect;  // in screen space
    private RectF mImageRect;  // in image space
    RectF mCropRect;  // in image space
    Matrix mMatrix;

    private boolean mMaintainAspectRatio = false;
    private float mInitialAspectRatio;
    private boolean mCircle = false;

    private Drawable mResizeDrawableWidth;
    private Drawable mResizeDrawableHeight;
    private Drawable mResizeDrawableDiagonal;

    private final Paint mFocusPaint = new Paint();
    private final Paint mNoFocusPaint = new Paint();
    private final Paint mOutlinePaint = new Paint();
    private float mLastX;
    private float mLastY;
    private int mMotionEdge;
    private float mMinWidth=25F;
    private float mMinHeight=25F;
    private OnSizeChangedListener mOnTriggerListener=null;
    private OnSizeChangedListener mOnValidateSizingListener=null;
    private boolean mCollision=false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int edge = getHit(event.getX(), event.getY());
                mMotionEdge = edge;
                if (edge != GROW_NONE) {
                    mLastX = event.getX();
                    mLastY = event.getY();
                    setMode(
                            (edge == MOVE)
                            ? ModifyMode.Move
                            : ModifyMode.Grow);
                    break;
                }
                break;
            case MotionEvent.ACTION_UP:
                setMode(ModifyMode.None);
                dispatchValidateSizingRect();
                break;
            case MotionEvent.ACTION_MOVE:
                handleMotion(mMotionEdge,
                        event.getX() - mLastX,
                        event.getY() - mLastY);
                mLastX = event.getX();
                mLastY = event.getY();
                break;
        }
        return true;
    }

    public void setColliding(boolean colliding){
        mCollision=colliding;
    }
    public void setOnValidateSizingRect(OnSizeChangedListener listener) {
    	mOnValidateSizingListener = listener;
    }

    private void dispatchValidateSizingRect() {
    	if (mOnValidateSizingListener != null) {
    		mOnValidateSizingListener.onTrigger(mCropRect);
    		invalidate();
    	}
    }

    public void setOnSizeChangedListener(OnSizeChangedListener listener) {
        mOnTriggerListener = listener;
    }

    /**
     * Dispatches a trigger event to listener. Ignored if a listener is not set.
     * @param whichHandle the handle that triggered the event.
     */
    private void dispatchTriggerEvent(RectF r) {
        if (mOnTriggerListener != null) {
            mOnTriggerListener.onTrigger(r);
        }
    }

    /**
     * Interface definition for a callback to be invoked when a resize triggered
     * by moving the handlers beyond a threshold.
     */
    public interface OnSizeChangedListener {
        /**
         * Called when the user moves a handle beyond the threshold.
         *
         * @param r: the resulting Rect
         */
        void onTrigger(RectF r);
    }

}