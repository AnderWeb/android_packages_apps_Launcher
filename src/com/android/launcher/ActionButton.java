package com.android.launcher;

import com.android.launcher.DragController.DragListener;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnLongClickListener;
import android.widget.Toast;

public class ActionButton extends CounterImageView implements DropTarget, DragListener,
									OnLongClickListener, DragSource {
	private Launcher mLauncher;
	private int mIdent=LauncherSettings.Favorites.CONTAINER_LAB;
	private ItemInfo mCurrentInfo;
	private Drawable bgResource;
	private Drawable bgEmpty;
	private Drawable mIconNormal;
	private int mIconSpecial_id;
	private Drawable mIconSpecial;
	private boolean specialMode=false;
	private boolean hiddenBg=false;
	private int specialAction=0;
    private GestureDetector mGestureDetector;
    private ABGestureListener mGestureListener;
    public boolean mInterceptClicks=false;
    private static final int ORIENTATION_HORIZONTAL = 1;
    private int mOrientation = ORIENTATION_HORIZONTAL;
    private SwipeListener mSwipeListener;
    private DragController mDragger;

	public ActionButton(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public ActionButton(Context context, AttributeSet attrs) {
		this(context, attrs,0);
		// TODO Auto-generated constructor stub
	}

	public ActionButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		setHapticFeedbackEnabled(true);
		TypedArray a=context.obtainStyledAttributes(attrs,R.styleable.ActionButton,defStyle,0);
		mIdent=a.getInt(R.styleable.ActionButton_ident, mIdent);
        mOrientation = a.getInt(R.styleable.ActionButton_direction, ORIENTATION_HORIZONTAL);
		//bgResource=a.getDrawable(R.styleable.ActionButton_background);
		bgEmpty=context.getResources().getDrawable(R.drawable.lab_rab_empty_bg);
		a.recycle();
        mGestureListener = new ABGestureListener();
        //mGestureDetector = new GestureDetector(mGestureListener);
        mGestureDetector = new GestureDetector(context, mGestureListener);
        this.setOnLongClickListener(this);
	}

	public boolean acceptDrop(DragSource source, int x, int y, int xOffset,
			int yOffset, Object dragInfo) {
		return !specialMode;
	}

	public Rect estimateDropLocation(DragSource source, int x, int y,
			int xOffset, int yOffset, Object dragInfo, Rect recycle) {
		return null;
	}

	public void onDragEnter(DragSource source, int x, int y, int xOffset,
			int yOffset, Object dragInfo) {
		setPressed(true);
	}

	public void onDragExit(DragSource source, int x, int y, int xOffset,
			int yOffset, Object dragInfo) {
		setPressed(false);
	}

	public void onDragOver(DragSource source, int x, int y, int xOffset,
			int yOffset, Object dragInfo) {
	}

	public void onDrop(DragSource source, int x, int y, int xOffset,
			int yOffset, Object dragInfo) {
		boolean accept=true;
    	ItemInfo info = (ItemInfo) dragInfo;
        switch (info.itemType) {
        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
        case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
        case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
        	//we do accept those
        	break;
        case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
        	Toast t=Toast.makeText(getContext(), R.string.toast_widgets_not_supported, Toast.LENGTH_SHORT);
        	t.show();
        	accept=false;
        	break;
        default:
        	Toast t2=Toast.makeText(getContext(), R.string.toast_unknown_item, Toast.LENGTH_SHORT);
        	t2.show();
        	accept=false;
        	break;
        }
        final LauncherModel model = Launcher.getModel();
        //TODO:ADW check this carefully
        //We need to remove current item from database before adding the new one
        if (info instanceof LauncherAppWidgetInfo) {
            model.removeDesktopAppWidget((LauncherAppWidgetInfo) info);
            final LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) info;
            final LauncherAppWidgetHost appWidgetHost = mLauncher.getAppWidgetHost();
            if (appWidgetHost != null) {
                appWidgetHost.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
            }
        }
        if(accept){
	        if(mCurrentInfo!=null){
	        	model.removeDesktopItem(mCurrentInfo);
	        	LauncherModel.deleteItemFromDatabase(mLauncher, mCurrentInfo);
	        }
	        model.addDesktopItem(info);
	        LauncherModel.addOrMoveItemInDatabase(mLauncher, info,
	                mIdent, -1, -1, -1);
	        UpdateLaunchInfo(info);
        }else{
        	LauncherModel.deleteItemFromDatabase(mLauncher, info);
        }
	}
	protected void UpdateLaunchInfo(ItemInfo info){
    	mCurrentInfo=info;
		//TODO:ADW extract icon and put it as the imageview src...
		Drawable myIcon=null;
		if (info != null) {
	        switch (info.itemType) {
	        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
	        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
	            if (info.container == NO_ID) {
	                // Came from all apps -- make a copy
	                info = new ApplicationInfo((ApplicationInfo) info);
	            }
	            setCounter(((ApplicationInfo)info).counter,((ApplicationInfo)info).counterColor);
	            myIcon = mLauncher.createSmallActionButtonIcon(info);
	            break;
	        case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
	        case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
	            myIcon = mLauncher.createSmallActionButtonIcon(info);
	        	break;
	        case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
	        	//Toast t=Toast.makeText(getContext(), "Widgets not supported... sorry :-)", Toast.LENGTH_SHORT);
	        	//t.show();
	        	return;
	        default:
	        	//Toast t2=Toast.makeText(getContext(), "Unknown item. We can't add unknown item types :-)", Toast.LENGTH_SHORT);
	        	//t2.show();
	        	return;
	            //throw new IllegalStateException("Unknown item type: " + info.itemType);
	        }
		}
        setIcon(myIcon);
    	updateIcon();
        invalidate();
	}

	public void onDragEnd() {
	}

	public void onDragStart(View v, DragSource source, Object info,
			int dragAction) {


	}
    void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }

	@Override
	public Object getTag() {
		// TODO Auto-generated method stub
		if(!specialMode){
		    return mCurrentInfo;
		}else{
		    return specialAction;
		}
	}
	public void updateIcon(){
    	if(mCurrentInfo!=null){
			ItemInfo info=mCurrentInfo;
			Drawable myIcon=null;
	        switch (info.itemType) {
	        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
	        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
	            if (info.container == NO_ID) {
	                // Came from all apps -- make a copy
	                info = new ApplicationInfo((ApplicationInfo) info);
	            }
	            myIcon = mLauncher.createSmallActionButtonIcon(info);
	            break;
	        case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
	        case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
	            myIcon = mLauncher.createSmallActionButtonIcon(info);
	        	break;
	        case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
	        	//Toast t=Toast.makeText(getContext(), "Widgets not supported... sorry :-)", Toast.LENGTH_SHORT);
	        	//t.show();
	        	return;
	        default:
	        	//Toast t2=Toast.makeText(getContext(), "Unknown item. We can't add unknown item types :-)", Toast.LENGTH_SHORT);
	        	//t2.show();
	        	return;
	            //throw new IllegalStateException("Unknown item type: " + info.itemType);
	        }
	        setIcon(myIcon);
	        invalidate();
    	}else{
    	    Drawable myIcon = mLauncher.createSmallActionButtonIcon(null);
            setIcon(myIcon);
            invalidate();
    	}
    	if(mIconSpecial_id!=0){
            setSpecialIcon(mIconSpecial_id);
    	}
	}
	/**
	 * ADW: show/hide background
	 * @param enable
	 */
	public void hideBg(boolean hide){
		if(hide!=hiddenBg){
			hiddenBg=hide;
			if(!hide)
				this.setBackgroundDrawable(bgResource);
			else{
				this.setBackgroundDrawable(bgEmpty);
			}
		}
	}

	@Override
	public void setBackgroundDrawable(Drawable d) {
		// TODO Auto-generated method stub
		super.setBackgroundDrawable(d);
		if(d!=bgEmpty){
			if(bgResource!=null)bgResource.setCallback(null);
			bgResource=d;
		}
	}
	/**
	 * ADW: Reload the proper icon
	 * This is mainly used when the apps from SDcard are available in froyo
	 */
	public void reloadIcon(String packageName){
		if(mCurrentInfo==null)return;
		if(mCurrentInfo instanceof ApplicationInfo){
            final ApplicationInfo info=(ApplicationInfo)mCurrentInfo;
		    final Intent intent = info.intent;
            final ComponentName name = intent.getComponent();
            if ((info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                    info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT)&&
                    Intent.ACTION_MAIN.equals(intent.getAction()) && name != null &&
                    packageName.equals(name.getPackageName())) {
    			final Drawable icon = Launcher.getModel().getApplicationInfoIcon(
    	                mLauncher.getPackageManager(), info, mLauncher);
    	        Drawable myIcon=null;
    			if (icon != null) {
    	            info.icon.setCallback(null);
    	            info.icon = Utilities.createIconThumbnail(icon, mLauncher);
    	            info.filtered = true;
    	            myIcon = mLauncher.createSmallActionButtonIcon(info);
    				setIcon(myIcon);
    		        invalidate();
    	        }
    		}
		}
	}
	private void setIcon(Drawable d){
	    if(mIconNormal!=null){
	        mIconNormal.setCallback(null);
	        mIconNormal=null;
	    }
	    mIconNormal=d;
	    if(!specialMode){
	        setImageDrawable(mIconNormal);
	    }
	}
	public void setSpecialIcon(int res_id){
	    Drawable d=null;
	    try{
	        d=getResources().getDrawable(res_id);
	        if(mIconSpecial!=null){
	            mIconSpecial.setCallback(null);
	            mIconSpecial=null;
	        }
	        mIconSpecial_id=res_id;
	        mIconSpecial=mLauncher.createSmallActionButtonDrawable(d);
	        if(specialMode){
	            setImageDrawable(mIconSpecial);
	        }
	    }catch (Exception e) {
        }
	}
	public void setSpecialMode(boolean special){
	    if(special!=specialMode){
	        specialMode=special;
	        if(specialMode)
	            setImageDrawable(mIconSpecial);
	        else
	            setImageDrawable(mIconNormal);
	    }
	}
	public void setSpecialAction(int action){
	    specialAction=action;
	}
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mGestureDetector.onTouchEvent(ev)) {
            return true;
        }
        if(!mInterceptClicks){
            return super.onTouchEvent(ev);
        }
        return false;
    }

	@Override
	public boolean onLongClick(View v) {
        if (mDragger == null || !v.isInTouchMode() || mCurrentInfo == null || specialMode || mLauncher.isDesktopBlocked()) {
            return false;
        }
        // Close Drawer if it is open...
        mLauncher.closeAllApplications();
        mLauncher.showActions(mCurrentInfo, v);
        mDragger.startDrag(v, this, mCurrentInfo, DragController.DRAG_ACTION_COPY);
        UpdateLaunchInfo(null);
        return true;
	}

	public void setDragger(DragController dragger) {
		mDragger=dragger;
	}

	@Override
	public void onDropCompleted(View target, boolean success) {
	}

    class ABGestureListener implements OnGestureListener {
        public boolean onDown(MotionEvent e) {
            mInterceptClicks=false;
            return false;
        }
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float velocity=0;
            if(mOrientation!= ORIENTATION_HORIZONTAL){
                if(velocityX<0 && Math.abs(velocityY)<Math.abs(velocityX))
                    velocity=Math.abs(velocityX);
            }else{
                if(velocityY<0 && Math.abs(velocityY)>Math.abs(velocityX))
                    velocity=Math.abs(velocityY);
            }
            if(velocity>0){
                dispatchSwipeEvent();
                mInterceptClicks=true;

                return true;
            }
            return false;
        }
        public void onLongPress(MotionEvent e) {
            //not used
        }
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            //not used
            return false;
        }
        public void onShowPress(MotionEvent e) {
            //not used
        }
        public boolean onSingleTapUp(MotionEvent e) {
            //not used
            return false;
        }
    }
    public void setSwipeListener(SwipeListener listener) {
        mSwipeListener = listener;
    }

    /**
     * Dispatches a trigger event to listener. Ignored if a listener is not set.
     * @param whichHandle the handle that triggered the event.
     */
    private void dispatchSwipeEvent() {
        if (mSwipeListener != null) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
            mSwipeListener.onSwipe();
        }
    }
    /**
     * Interface definition for a callback to be invoked when a tab is triggered
     * by moving it beyond a threshold.
     */
    public interface SwipeListener {
        void onSwipe();
    }

}
