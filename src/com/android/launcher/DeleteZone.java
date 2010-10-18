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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

public class DeleteZone extends ImageView implements DropTarget, DragController.DragListener {
    private static final int ORIENTATION_HORIZONTAL = 1;
    private static final int TRANSITION_DURATION = 250;
    private static final int ANIMATION_DURATION = 200;
	private static final String LOG_TAG = "DeleteZone";

    private final int[] mLocation = new int[2];
    
    private Launcher mLauncher;
    private boolean mTrashMode;

    private AnimationSet mInAnimation;
    private AnimationSet mOutAnimation;
    private Animation mHandleInAnimation;
    private Animation mHandleOutAnimation;

    private int mOrientation;
    private DragLayer mDragLayer;

    private final RectF mRegion = new RectF();
    private TransitionDrawable mTransition;
    private View mHandle;
    private boolean shouldUninstall=false;
    private Handler mHandler = new Handler();
	private boolean mUninstallTarget=false;
	String UninstallPkg = null;
	//ADW: We need to move the DeleteZone if Dockbar is open 
	private int mCustomPadding=60;
	private boolean mTrickyLocation=false;

    public DeleteZone(Context context) {
        super(context);
    }

    public DeleteZone(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeleteZone(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.DeleteZone, defStyle, 0);
        mOrientation = a.getInt(R.styleable.DeleteZone_direction, ORIENTATION_HORIZONTAL);
        a.recycle();
    }

    /*@Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTransition = (TransitionDrawable) getBackground();
    }*/

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
        return true;
    }
    
    public Rect estimateDropLocation(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo, Rect recycle) {
        return null;
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, Object dragInfo) {
        final ItemInfo item = (ItemInfo) dragInfo;

        if (item.container == -1) return;

        final LauncherModel model = Launcher.getModel();
        if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
            if (item instanceof LauncherAppWidgetInfo) {
                model.removeDesktopAppWidget((LauncherAppWidgetInfo) item);
            } else {
                model.removeDesktopItem(item);
            }
        } else {
            if (source instanceof UserFolder) {
                final UserFolder userFolder = (UserFolder) source;
                final UserFolderInfo userFolderInfo = (UserFolderInfo) userFolder.getInfo();
                model.removeUserFolderItem(userFolderInfo, item);
            }
        }
        if (item instanceof UserFolderInfo) {
            final UserFolderInfo userFolderInfo = (UserFolderInfo)item;
            LauncherModel.deleteUserFolderContentsFromDatabase(mLauncher, userFolderInfo);
            model.removeUserFolder(userFolderInfo);
        } else if (item instanceof LauncherAppWidgetInfo) {
            final LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) item;
            final LauncherAppWidgetHost appWidgetHost = mLauncher.getAppWidgetHost();
            mLauncher.getWorkspace().unbindWidgetScrollableId(launcherAppWidgetInfo.appWidgetId);
            if (appWidgetHost != null) {
                appWidgetHost.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
            }
        }
        LauncherModel.deleteItemFromDatabase(mLauncher, item);
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
    	//ADW: show uninstall message
    	final ItemInfo item = (ItemInfo) dragInfo;
    	if (item instanceof ApplicationInfo){
	    	mTransition.reverseTransition(TRANSITION_DURATION);
	    	mUninstallTarget = true;
	        mHandler.removeCallbacks(mShowUninstaller);
	        mHandler.postDelayed(mShowUninstaller, 1000);
    	}
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset,
            Object dragInfo) {
        mTransition.reverseTransition(TRANSITION_DURATION);
        //ADW: not show uninstall message
        //ADW We need to call this delayed cause onDragExit is always called just before onDragEnd :(
    	mHandler.removeCallbacks(mShowUninstaller);
        if(shouldUninstall){
	        mUninstallTarget = false;
	        mHandler.postDelayed(mShowUninstaller, 100);
        }
    }

    public void onDragStart(View v, DragSource source, Object info, int dragAction) {
        final ItemInfo item = (ItemInfo) info;
        //ADW: we need to know how much the view will we moved
        mCustomPadding=mLauncher.getTrashPadding();
        if (item != null) {
            mTrashMode = true;
            createAnimations();
            final int[] location = mLocation;
            getLocationOnScreen(location);
            if(mLauncher.isDockBarOpen()){
            	MarginLayoutParams tmp=(MarginLayoutParams) getLayoutParams();
            	if(mOrientation==ORIENTATION_HORIZONTAL){
            		tmp.bottomMargin=mCustomPadding;
            		tmp.rightMargin=0;
            	}else{
            		tmp.bottomMargin=0;
            		tmp.rightMargin=mCustomPadding;
            	}
            	setLayoutParams(tmp);
                //TODO: ADW we need to hack the real location the first time we move the trash can
                if(!mTrickyLocation){
                    if(mOrientation==ORIENTATION_HORIZONTAL){
                    	mRegion.set(location[0], location[1]-mCustomPadding, location[0] + mRight - mLeft,
                            location[1] + mBottom - mTop-mCustomPadding);
                    }else{
                    	mRegion.set(location[0]-mCustomPadding, location[1], location[0] + mRight - mLeft -mCustomPadding,
                                location[1] + mBottom - mTop);
                    }
                    mTrickyLocation=true;
                }else{
                    mRegion.set(location[0], location[1], location[0] + mRight - mLeft,
                            location[1] + mBottom - mTop);
                }
            }else{
            	MarginLayoutParams tmp=(MarginLayoutParams) getLayoutParams();
        		tmp.bottomMargin=0;
        		tmp.rightMargin=0;
            	setLayoutParams(tmp);
                //TODO: ADW we need to hack the real location the first time we move the trash can
                if(mTrickyLocation){
                	if(mOrientation==ORIENTATION_HORIZONTAL){
                		mRegion.set(location[0], location[1]+mCustomPadding, location[0] + mRight - mLeft,
                            location[1] + mBottom - mTop+mCustomPadding);
                	}else{
                		mRegion.set(location[0]+mCustomPadding, location[1], location[0] + mRight - mLeft+mCustomPadding,
                                location[1] + mBottom - mTop);
                	}
                    mTrickyLocation=false;
                }else{
                    mRegion.set(location[0], location[1], location[0] + mRight - mLeft,
                            location[1] + mBottom - mTop);
                }
            }
            mDragLayer.setDeleteRegion(mRegion);
            mTransition.resetTransition();
            startAnimation(mInAnimation);
            if(!mLauncher.isDockBarOpen()){
            	mHandle.startAnimation(mHandleOutAnimation);
            }
            setVisibility(VISIBLE);
            
            //ADW Store app data for uninstall if its an Application
            //ADW Thanks to irrenhaus@xda & Rogro82@xda :)
			if(item instanceof ApplicationInfo){
				try{
					final ApplicationInfo appInfo=(ApplicationInfo) item;
		            if(appInfo.iconResource != null)
						UninstallPkg = appInfo.iconResource.packageName;
					else
					{
						PackageManager mgr = DeleteZone.this.getContext().getPackageManager();
						ResolveInfo res = mgr.resolveActivity(appInfo.intent, 0);
						UninstallPkg = res.activityInfo.packageName;
					}
				}catch (Exception e) {
					Log.w(LOG_TAG, "Could not load shortcut icon: " + item);
					UninstallPkg=null;
				}
			}            
        }
    }

    public void onDragEnd() {
        if (mTrashMode) {
            mTrashMode = false;
            mDragLayer.setDeleteRegion(null);
            startAnimation(mOutAnimation);
            if(!mLauncher.isDockBarOpen()){
            	mHandle.startAnimation(mHandleInAnimation);
            }
            if(mLauncher.isDockBarOpen()){
            	MarginLayoutParams tmp=(MarginLayoutParams) getLayoutParams();
            	if(mOrientation==ORIENTATION_HORIZONTAL){
            		tmp.bottomMargin=0;
            	}else{
            		tmp.rightMargin=0;
            	}
            	setLayoutParams(tmp);
            }
            setVisibility(GONE);
        }
        if(shouldUninstall && UninstallPkg!=null){
			Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:"+UninstallPkg));
			DeleteZone.this.getContext().startActivity(uninstallIntent);
        }
        
    }

    private void createAnimations() {
        if (mInAnimation == null) {
            mInAnimation = new FastAnimationSet();
            final AnimationSet animationSet = mInAnimation;
            animationSet.setInterpolator(new AccelerateInterpolator());
            animationSet.addAnimation(new AlphaAnimation(0.0f, 1.0f));
            if (mOrientation == ORIENTATION_HORIZONTAL) {
                animationSet.addAnimation(new TranslateAnimation(Animation.ABSOLUTE, 0.0f,
                        Animation.ABSOLUTE, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f));
            } else {
                animationSet.addAnimation(new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                        1.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, 0.0f,
                        Animation.ABSOLUTE, 0.0f));
            }
            animationSet.setDuration(ANIMATION_DURATION);
        }
        if (mHandleInAnimation == null) {
            if (mOrientation == ORIENTATION_HORIZONTAL) {
                mHandleInAnimation = new TranslateAnimation(Animation.ABSOLUTE, 0.0f,
                        Animation.ABSOLUTE, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
                        Animation.RELATIVE_TO_SELF, 0.0f);
            } else {
                mHandleInAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                        1.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.ABSOLUTE, 0.0f,
                        Animation.ABSOLUTE, 0.0f);
            }
            mHandleInAnimation.setDuration(ANIMATION_DURATION);
        }
        if (mOutAnimation == null) {
            mOutAnimation = new FastAnimationSet();
            final AnimationSet animationSet = mOutAnimation;
            animationSet.setInterpolator(new AccelerateInterpolator());
            animationSet.addAnimation(new AlphaAnimation(1.0f, 0.0f));
            if (mOrientation == ORIENTATION_HORIZONTAL) {
                animationSet.addAnimation(new FastTranslateAnimation(Animation.ABSOLUTE, 0.0f,
                        Animation.ABSOLUTE, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 1.0f));
            } else {
                animationSet.addAnimation(new FastTranslateAnimation(Animation.RELATIVE_TO_SELF,
                        0.0f, Animation.RELATIVE_TO_SELF, 1.0f, Animation.ABSOLUTE, 0.0f,
                        Animation.ABSOLUTE, 0.0f));
            }
            animationSet.setDuration(ANIMATION_DURATION);
        }
        if (mHandleOutAnimation == null) {
            if (mOrientation == ORIENTATION_HORIZONTAL) {
                mHandleOutAnimation = new FastTranslateAnimation(Animation.ABSOLUTE, 0.0f,
                        Animation.ABSOLUTE, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                        Animation.RELATIVE_TO_SELF, 1.0f);
            } else {
                mHandleOutAnimation = new FastTranslateAnimation(Animation.RELATIVE_TO_SELF,
                        0.0f, Animation.RELATIVE_TO_SELF, 1.0f, Animation.ABSOLUTE, 0.0f,
                        Animation.ABSOLUTE, 0.0f);
            }
            mHandleOutAnimation.setFillAfter(true);
            mHandleOutAnimation.setDuration(ANIMATION_DURATION);
        }
    }

    void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }

    void setDragController(DragLayer dragLayer) {
        mDragLayer = dragLayer;
    }

    void setHandle(View view) {
        mHandle = view;
    }

    private static class FastTranslateAnimation extends TranslateAnimation {
        public FastTranslateAnimation(int fromXType, float fromXValue, int toXType, float toXValue,
                int fromYType, float fromYValue, int toYType, float toYValue) {
            super(fromXType, fromXValue, toXType, toXValue,
                    fromYType, fromYValue, toYType, toYValue);
        }

        @Override
        public boolean willChangeTransformationMatrix() {
            return true;
        }

        @Override
        public boolean willChangeBounds() {
            return false;
        }
    }

    private static class FastAnimationSet extends AnimationSet {
        FastAnimationSet() {
            super(false);
        }

        @Override
        public boolean willChangeTransformationMatrix() {
            return true;
        }

        @Override
        public boolean willChangeBounds() {
            return false;
        }
    }
    //ADW Runnable to show the uninstall message (or reset the uninstall status)
    private Runnable mShowUninstaller = new Runnable() {
		public void run() {
    	       shouldUninstall=mUninstallTarget;
    	       if(shouldUninstall){
    	    	   Toast.makeText(mContext, R.string.drop_to_uninstall, 500).show();
    	       }
		}
    };

	@Override
	public void setBackgroundDrawable(Drawable d) {
		// TODO Auto-generated method stub
		super.setBackgroundDrawable(d);
        mTransition = (TransitionDrawable) d;
	}
}
