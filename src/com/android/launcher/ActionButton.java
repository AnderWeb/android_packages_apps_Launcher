package com.android.launcher;

import com.android.launcher.DragController.DragListener;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

public class ActionButton extends CounterImageView implements DropTarget, DragListener {
	private Launcher mLauncher;
	private int mIdent=LauncherSettings.Favorites.CONTAINER_LAB;
	private ItemInfo mCurrentInfo;
	private Drawable bgResource;
	private Drawable bgEmpty;
	private Drawable mIconNormal;
	private Drawable mIconSpecial;
	private boolean specialMode=false;
	private boolean hiddenBg=false;
	private int specialAction=0;
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
		//bgResource=a.getDrawable(R.styleable.ActionButton_background);
		bgEmpty=context.getResources().getDrawable(R.drawable.lab_rab_empty_bg);
		a.recycle();
	}

	public boolean acceptDrop(DragSource source, int x, int y, int xOffset,
			int yOffset, Object dragInfo) {
		// TODO Auto-generated method stub
		return !specialMode;
	}

	public Rect estimateDropLocation(DragSource source, int x, int y,
			int xOffset, int yOffset, Object dragInfo, Rect recycle) {
		// TODO Auto-generated method stub
		return null;
	}

	public void onDragEnter(DragSource source, int x, int y, int xOffset,
			int yOffset, Object dragInfo) {
		// TODO Auto-generated method stub
		setPressed(true);
	}

	public void onDragExit(DragSource source, int x, int y, int xOffset,
			int yOffset, Object dragInfo) {
		// TODO Auto-generated method stub
		setPressed(false);

	}

	public void onDragOver(DragSource source, int x, int y, int xOffset,
			int yOffset, Object dragInfo) {
		// TODO Auto-generated method stub

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
        switch (info.itemType) {
        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
            if (info.container == NO_ID) {
                // Came from all apps -- make a copy
                info = new ApplicationInfo((ApplicationInfo) info);
            }
            setCounter(((ApplicationInfo)info).counter);
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
	}

	public void onDragEnd() {
		// TODO Auto-generated method stub

	}

	public void onDragStart(View v, DragSource source, Object info,
			int dragAction) {
		// TODO Auto-generated method stub

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
	public void reloadIcon(){
		if(mCurrentInfo==null)return;
		if(mCurrentInfo.itemType==LauncherSettings.Favorites.ITEM_TYPE_APPLICATION){
	        ApplicationInfo info=(ApplicationInfo) mCurrentInfo;
			final Drawable icon = Launcher.getModel().getApplicationInfoIcon(
	                mLauncher.getPackageManager(), info);
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
	public void setSpecialIcon(Drawable d){
        if(mIconSpecial!=null){
            mIconSpecial.setCallback(null);
            mIconSpecial=null;
        }
        mIconSpecial=d;
        if(specialMode){
            setImageDrawable(mIconSpecial);
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
}
