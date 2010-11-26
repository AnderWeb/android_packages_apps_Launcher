package com.android.launcher;

import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

public class FlingGesture {

	public interface FlingListener {
		public void OnFling(int Direction);
	}

    /**
     * The velocity at which a fling gesture will cause us to snap to the next screen
     */
    private static final int SNAP_VELOCITY = 500;

    public static final int FLING_NONE = 0;
    public static final int FLING_LEFT = 1;
    public static final int FLING_RIGHT = 2;
    public static final int FLING_UP = 3;
    public static final int FLING_DOWN = 4;

	private final int mMaximumVelocity;
	private VelocityTracker mVelocityTracker = null;
	private FlingListener mListener = null;

	public FlingGesture() {
		mMaximumVelocity = ViewConfiguration.getMaximumFlingVelocity();
	}

	public void setListener(FlingListener aListener) {
		mListener = aListener;
	}

	public void ForwardTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        if (ev.getAction() == MotionEvent.ACTION_UP) {
            mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
            final int velocityX = (int) mVelocityTracker.getXVelocity();
            final int velocityY = (int) mVelocityTracker.getYVelocity();

            if (mListener != null) {
            	int Direction = FLING_NONE;
	            if (velocityX > SNAP_VELOCITY) {
	            	Direction = FLING_LEFT;
	            } else if (velocityX < -SNAP_VELOCITY) {
	            	Direction = FLING_RIGHT;
	            } else if (velocityY > SNAP_VELOCITY) {
	            	Direction = FLING_DOWN;
	            } else if (velocityY < -SNAP_VELOCITY) {
	            	Direction = FLING_UP;
	            }
	            mListener.OnFling(Direction);
            }

            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }
        }



	}
}
