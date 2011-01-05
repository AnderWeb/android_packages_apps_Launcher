/**
* @author AnderWeb <anderweb@gmail.com>
*
**/

package com.android.launcher;


import java.io.InputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;

public class CustomShirtcutActivity extends Activity implements OnClickListener {
	private static final String ACTION_ADW_PICK_ICON="org.adw.launcher.icons.ACTION_PICK_ICON";
	public static final String ACTION_LAUNCHERACTION = "com.android.launcher.action.launcheraction";
	public static final String EXTRA_APPLICATIONINFO = "EXTRA_APPLICATIONINFO";

	private static final int PICK_CUSTOM_ICON=1;
	private static final int PICK_STANDARD_MENU=2;
	private static final int PICK_STANDARD_SHORTCUT=3;
	private static final int PICK_STANDARD_APPLICATION=4;
	private static final int PICK_CUSTOM_PICTURE=5;
	private static final int PICK_FROM_ICON_PACK=6;

	private static final int DIALOG_ICON_TYPE=1;
	private Button btPickActivity;
	private ImageButton btPickIcon;
	private Button btOk;
	private EditText edLabel;
	//private ActivityInfo mInfo;
	private Bitmap mBitmap;
	PackageManager mPackageManager;
	private Intent mIntent;
	private ShortcutIconResource mIconResource;
	private int mIconSize;

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable("mBitmap", mBitmap);
		outState.putParcelable("mIntent", mIntent);
		outState.putParcelable("mIconResource", mIconResource);
		outState.putInt("mIconSize", mIconSize);
		outState.putBoolean("btOk_enabled", btOk.isEnabled());
		outState.putBoolean("btPickIcon_enabled", btPickIcon.isEnabled());
		outState.putCharSequence("btPickActivity_text", btPickActivity.getText());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null && savedInstanceState.size() >= 7) {
			mBitmap = savedInstanceState.getParcelable("mBitmap");
			mIntent = savedInstanceState.getParcelable("mIntent");
			mIconResource = savedInstanceState.getParcelable("mIconResource");
			mIconSize = savedInstanceState.getInt("mIconResource");

			if (mBitmap != null)
				btPickIcon.setImageBitmap(mBitmap);
			else if (mIconResource != null) {
				Resources resources;
				try {
					resources = mPackageManager.getResourcesForApplication(mIconResource.packageName);
	                final int id = resources.getIdentifier(mIconResource.resourceName, null, null);
	                btPickIcon.setImageDrawable(resources.getDrawable(id));
				} catch (NameNotFoundException e) {
				}
			}
			btPickActivity.setText(savedInstanceState.getCharSequence("btPickActivity_text"));
			btPickIcon.setEnabled(savedInstanceState.getBoolean("btPickIcon_enabled"));
			btOk.setEnabled(savedInstanceState.getBoolean("btOk_enabled"));
		}
		super.onRestoreInstanceState(savedInstanceState);
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_LAUNCHERACTION)) {
			LauncherActions.getInstance().launch(intent);
			finish();
		}

		setContentView(R.layout.custom_shirtcuts);
		btPickActivity=(Button) findViewById(R.id.pick_activity);
		btPickActivity.setOnClickListener(this);
		btPickIcon=(ImageButton) findViewById(R.id.pick_icon);
		btPickIcon.setOnClickListener(this);
		btPickIcon.setEnabled(false);
		btOk=(Button) findViewById(R.id.shirtcut_ok);
		btOk.setEnabled(false);
		btOk.setOnClickListener(this);
		edLabel=(EditText) findViewById(R.id.shirtcut_label);
		mPackageManager=getPackageManager();
		mIconSize=(int) getResources().getDimension(android.R.dimen.app_icon_size);
		loadFromAppInfo(getAppInfo());
	}

	private ApplicationInfo getAppInfo() {
		final Intent intent = getIntent();
		if (intent != null && intent.getAction() != null &&
				intent.getAction().equals(Intent.ACTION_EDIT)
				&& intent.hasExtra(EXTRA_APPLICATIONINFO)) {
			long id = intent.getLongExtra(EXTRA_APPLICATIONINFO, 0);
			return LauncherModel.loadApplicationInfoById(this, id);
		}
		return null;
	}

	private void loadFromAppInfo(ApplicationInfo info) {
		if (info == null)
			return;
		edLabel.setText(info.title);
		mIntent = info.intent;
		btPickIcon.setImageDrawable(info.icon);
		btPickIcon.setEnabled(true);
		btOk.setEnabled(true);
		ComponentName component = mIntent.getComponent();
		if (component != null) {
			if (component.getClassName().equals(CustomShirtcutActivity.class.getName()) &&
				mIntent.getAction().equals(ACTION_LAUNCHERACTION)){
			}
			else
			{
		        ActivityInfo activityInfo = null;
		        try {
		            activityInfo = mPackageManager.getActivityInfo(component, 0);
		        } catch (NameNotFoundException e) {
		        }
		        String title=null;
		        if (activityInfo != null) {
		            title = activityInfo.loadLabel(mPackageManager).toString();
		            if (title == null) {
		                title = activityInfo.name;
		            }
					btPickActivity.setText(title);
                    mIconResource = new ShortcutIconResource();
                    mIconResource.packageName = activityInfo.packageName;
                    try {
                        Resources resources = mPackageManager.getResourcesForApplication(mIconResource.packageName);
                        mIconResource.resourceName = resources.getResourceName(activityInfo.getIconResource());
                    } catch (NameNotFoundException e) {
                        mIconResource=null;
                    } catch (Resources.NotFoundException e) {
                        mIconResource=null;
                    }
		        }
			}
		}
		else
			btPickActivity.setText(info.title);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode==RESULT_OK){
			switch (requestCode) {
			case PICK_CUSTOM_PICTURE:
				mBitmap = (Bitmap) data.getParcelableExtra("data");
				if(mBitmap!=null){
					if(mBitmap.getWidth()>mIconSize)
						mBitmap=Utilities.createBitmapThumbnail(mBitmap, this);
					btPickIcon.setImageBitmap(mBitmap);
				}
				break;
			case PICK_CUSTOM_ICON:
				Uri photoUri = data.getData();
				try {
					InputStream is = getContentResolver().openInputStream(
							photoUri);
					BitmapFactory.Options opts = new BitmapFactory.Options();
					Bitmap bitmap;
					opts.inJustDecodeBounds = true;
					bitmap = BitmapFactory.decodeStream(is, null, opts);

					BitmapFactory.Options ops2 = new BitmapFactory.Options();
					int width = mIconSize;
					float w = opts.outWidth;
					//int scale = Math.round(w / width);
					int scale = (int) (w / width);
					ops2.inSampleSize = scale;
					is = getContentResolver().openInputStream(photoUri);
					mBitmap = BitmapFactory.decodeStream(is, null, ops2);
					if(mBitmap!=null){
						if(mBitmap.getWidth()>mIconSize)mBitmap=Utilities.createBitmapThumbnail(mBitmap, this);
						btPickIcon.setImageBitmap(mBitmap);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case PICK_FROM_ICON_PACK:
				mBitmap = (Bitmap) data.getParcelableExtra("icon");
				if(mBitmap!=null){
					if(mBitmap.getWidth()>mIconSize)
						mBitmap=Utilities.createBitmapThumbnail(mBitmap, this);
					btPickIcon.setImageBitmap(mBitmap);
				}
				break;
			case PICK_STANDARD_MENU:
		        String applicationName = getResources().getString(R.string.group_applications);
		        String activitiesName=getResources().getString(R.string.pref_label_activities);
		        String launcheractionsName = getResources().getString(R.string.launcher_actions);
		        String shortcutName = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

		        if (applicationName != null && applicationName.equals(shortcutName)) {
		            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
		            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
		            startActivityForResult(pickIntent, PICK_STANDARD_APPLICATION);
		        } else if (activitiesName != null && activitiesName.equals(shortcutName)) {
					Intent picker=new Intent();
		        	picker.setClass(this, ActivityPickerActivity.class);
					startActivityForResult(picker,PICK_STANDARD_SHORTCUT);
		        } else if (launcheractionsName != null && launcheractionsName.equals(shortcutName)) {
		        	AlertDialog.Builder builder = new AlertDialog.Builder(this);
		        	builder.setTitle(getString(R.string.launcher_actions));
		        	final ListAdapter adapter = LauncherActions.getInstance().getSelectActionAdapter();
		        	builder.setAdapter(adapter, new Dialog.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									LauncherActions.Action action = (LauncherActions.Action)adapter.getItem(which);
									Intent result = new Intent();
									result.putExtra(Intent.EXTRA_SHORTCUT_NAME, action.getName());
									result.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
											LauncherActions.getInstance().getIntentForAction(action));
							        ShortcutIconResource iconResource = new ShortcutIconResource();
							        iconResource.packageName = CustomShirtcutActivity.this.getPackageName();
						            iconResource.resourceName = getResources().getResourceName(action.getIconResourceId());
						            result.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
									onActivityResult(PICK_STANDARD_SHORTCUT, RESULT_OK, result);
								}
							});
		        	builder.create().show();
		        } else {
		            startActivityForResult(data, PICK_STANDARD_SHORTCUT);
		        }
		        break;
			case PICK_STANDARD_APPLICATION:
				if(mBitmap!=null){
					mBitmap.recycle();
					mBitmap=null;
				}
		        ComponentName component = data.getComponent();
		        ActivityInfo activityInfo = null;
		        try {
		            activityInfo = mPackageManager.getActivityInfo(component, 0 /* no flags */);
		        } catch (NameNotFoundException e) {
		        }
		        String title=null;
		        if (activityInfo != null) {
		            title = activityInfo.loadLabel(mPackageManager).toString();
		            if (title == null) {
		                title = activityInfo.name;
		            }
			        mIconResource = new ShortcutIconResource();
			        mIconResource.packageName = activityInfo.packageName;
			        try {
						Resources resources = mPackageManager.getResourcesForApplication(mIconResource.packageName);
						mIconResource.resourceName = resources.getResourceName(activityInfo.getIconResource());
					} catch (NameNotFoundException e) {
						mIconResource=null;
					} catch (Resources.NotFoundException e) {
						mIconResource=null;
					}

			        mIntent=data;
					btPickActivity.setText(title);
					btPickIcon.setImageDrawable(activityInfo.loadIcon(mPackageManager));
					btPickIcon.setEnabled(true);
					btOk.setEnabled(true);
					edLabel.setText(title);
		        }
				break;
			case PICK_STANDARD_SHORTCUT:
				if(mBitmap!=null){
					mBitmap.recycle();
					mBitmap=null;
				}
		        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
		        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
		        Bitmap bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

		        Drawable icon = null;
		        mIconResource=null;
		        if (bitmap != null) {
		            icon = new FastBitmapDrawable(Utilities.createBitmapThumbnail(bitmap, this));
		            mBitmap=bitmap;
		        } else {
		            Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
		            if (extra != null && extra instanceof ShortcutIconResource) {
		                try {
		                    mIconResource = (ShortcutIconResource) extra;
		                    Resources resources = mPackageManager.getResourcesForApplication(
		                            mIconResource.packageName);
		                    final int id = resources.getIdentifier(mIconResource.resourceName, null, null);
		                    icon = resources.getDrawable(id);
		                } catch (Exception e) {
		                }
		            }
		        }
		        if (icon == null) {
		            icon = getPackageManager().getDefaultActivityIcon();
		        }
		        mIntent=intent;
				btPickActivity.setText(name);
				btPickIcon.setImageDrawable(icon);
				btPickIcon.setEnabled(true);
				btOk.setEnabled(true);
				edLabel.setText(name);
				break;
			default:
				break;
			}
		}
	}
	@Override
	public void onClick(View v) {
		//Intent picker=new Intent();
		if(v.equals(btPickActivity)){
	        Bundle bundle = new Bundle();
	        ArrayList<String> shortcutNames = new ArrayList<String>();
	        shortcutNames.add(getString(R.string.group_applications));
	        shortcutNames.add(getString(R.string.pref_label_activities));
	        shortcutNames.add(getString(R.string.launcher_actions));
	        bundle.putStringArrayList(Intent.EXTRA_SHORTCUT_NAME, shortcutNames);

	        ArrayList<ShortcutIconResource> shortcutIcons = new ArrayList<ShortcutIconResource>();
	        shortcutIcons.add(ShortcutIconResource.fromContext(CustomShirtcutActivity.this,
	                        R.drawable.ic_launcher_application));
	        shortcutIcons.add(ShortcutIconResource.fromContext(CustomShirtcutActivity.this,
                    R.drawable.ic_launcher_home));
	        shortcutIcons.add(ShortcutIconResource.fromContext(CustomShirtcutActivity.this,
	        		R.drawable.ic_launcher_home));
	        bundle.putParcelableArrayList(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcons);

	        Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
	        pickIntent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_CREATE_SHORTCUT));
	        pickIntent.putExtras(bundle);
	        startActivityForResult(pickIntent, PICK_STANDARD_MENU);
		}else if(v.equals(btPickIcon)){
			showDialog(DIALOG_ICON_TYPE);
		}else if(v.equals(btOk)){
	        Intent mReturnData = new Intent();
	        mReturnData.putExtra(Intent.EXTRA_SHORTCUT_INTENT, mIntent);
	        mReturnData.putExtra(Intent.EXTRA_SHORTCUT_NAME, edLabel.getText().toString());
	        Intent intent = getIntent();
			if (intent != null && intent.getAction() != null &&
					intent.getAction().equals(Intent.ACTION_EDIT)
					&& intent.hasExtra(EXTRA_APPLICATIONINFO)) {
				long id = intent.getLongExtra(EXTRA_APPLICATIONINFO, 0);
				mReturnData.putExtra(EXTRA_APPLICATIONINFO, id);
			}
	        if(mBitmap==null){
				if(mIconResource!=null)mReturnData.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, mIconResource);
	        }else{
	        	mReturnData.putExtra(Intent.EXTRA_SHORTCUT_ICON, mBitmap);
	        }
			setResult(RESULT_OK,mReturnData);
			finish();
		}
	}
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ICON_TYPE:
			return new IconTypeDialog().createDialog();
		default:
			break;
		}
		return super.onCreateDialog(id);
	}
    protected class IconTypeDialog implements DialogInterface.OnClickListener,
	    DialogInterface.OnCancelListener, DialogInterface.OnDismissListener,
	    DialogInterface.OnShowListener {

		private ArrayAdapter<String> mAdapter;

		Dialog createDialog() {
		    mAdapter = new ArrayAdapter<String>(CustomShirtcutActivity.this, R.layout.add_list_item);
		    mAdapter.add(getString(R.string.shirtcuts_select_picture));
		    mAdapter.add(getString(R.string.shirtcuts_crop_picture));
		    mAdapter.add(getString(R.string.shirtcuts_icon_packs));

		    final AlertDialog.Builder builder = new AlertDialog.Builder(CustomShirtcutActivity.this);
		    builder.setTitle(getString(R.string.shirtcuts_select_icon_type));
		    builder.setAdapter(mAdapter, this);

		    builder.setInverseBackgroundForced(false);

		    AlertDialog dialog = builder.create();
		    dialog.setOnCancelListener(this);
		    dialog.setOnDismissListener(this);
		    dialog.setOnShowListener(this);
		    return dialog;
		}
		public void onCancel(DialogInterface dialog) {
		    cleanup();
		}
		public void onDismiss(DialogInterface dialog) {
		}
		private void cleanup() {
		}
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case 0:
				//Select icon
				Intent pickerIntent=new Intent(Intent.ACTION_PICK);
				pickerIntent.setType("image/*");
				startActivityForResult(Intent.createChooser(pickerIntent, "Select icon"), PICK_CUSTOM_ICON);
				break;
			case 1:
				//Crop picture
				int width;
				int height;
			    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			    intent.setType("image/*");
				width = height=mIconSize;
		        intent.putExtra("crop", "true");
				intent.putExtra("outputX", width);
				intent.putExtra("outputY", height);
				intent.putExtra("aspectX", width);
				intent.putExtra("aspectY", height);
		        intent.putExtra("noFaceDetection", true);
		        intent.putExtra("return-data", true);
				startActivityForResult(intent, PICK_CUSTOM_PICTURE);
				break;
			case 2:
				//Icon packs
				Intent packIntent=new Intent(ACTION_ADW_PICK_ICON);
				startActivityForResult(Intent.createChooser(packIntent, getString(R.string.shirtcuts_select_icon_pack)), PICK_FROM_ICON_PACK);
				break;

			default:
				break;
			}
			cleanup();
		}
		public void onShow(DialogInterface dialog) {
		}
	}


}
