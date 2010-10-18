/**
 * @author AnderWeb <anderweb@gmail.com>
 *
 */
package com.android.launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ExpandableListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

public class ActivityPickerActivity extends ExpandableListActivity {
	private PackageManager mPackageManager;
    private final class LoadingTask extends AsyncTask<Void, Void, Void> {
    	List<PackageInfo> groups;
        @Override
        public void onPreExecute() {
            setProgressBarIndeterminateVisibility(true);
        }
        @Override
        public Void doInBackground(Void... params) {
            groups = mPackageManager.getInstalledPackages(0);
            Collections.sort(groups, new PackageInfoComparable());
            return null;
        }
        @Override
        public void onPostExecute(Void result) {
            setProgressBarIndeterminateVisibility(false);
            setListAdapter(new MyExpandableListAdapter(groups));
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_list);
        getExpandableListView().setTextFilterEnabled(true);
        mPackageManager = getPackageManager();
        // Start async loading the data
        new LoadingTask().execute();
    }
	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
        ActivityInfo info = (ActivityInfo) getExpandableListAdapter().getChild(groupPosition, childPosition);
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(info.applicationInfo.packageName,
                info.name));
        Intent mReturnData = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        mReturnData.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);

        // Set the name of the activity
        mReturnData.putExtra(Intent.EXTRA_SHORTCUT_NAME, info.loadLabel(mPackageManager));

        ShortcutIconResource iconResource = new ShortcutIconResource();
        iconResource.packageName = info.packageName;
        try {
			Resources resources = mPackageManager.getResourcesForApplication(iconResource.packageName);
			iconResource.resourceName = resources.getResourceName(info.getIconResource());
			mReturnData.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
		} catch (NameNotFoundException e) {
		} catch (Resources.NotFoundException e) {
		}
		setResult(RESULT_OK,mReturnData);
		finish();
        return true;
	}
	/**
	 * ExpandableListAdapter to handle packages and activities
	 * @author adw
	 *
	 */
    public class MyExpandableListAdapter extends BaseExpandableListAdapter {
    	private final List<PackageInfo> groups;
    	private final AbsListView.LayoutParams lpGroup;
    	private final AbsListView.LayoutParams lpChild;
    	private final int leftPadding;
        public MyExpandableListAdapter(List<PackageInfo> g) {
			super();
			groups=g;
            leftPadding=getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);

            lpGroup = new AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lpChild = new AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT, leftPadding);
		}
        public ActivityInfo getChild(int groupPosition, int childPosition) {
        	//return groups.get(groupPosition).activities[childPosition];
        	PackageInfo tmp;
			try {
				tmp = mPackageManager.getPackageInfo(groups.get(groupPosition).packageName, PackageManager.GET_ACTIVITIES);
	        	if(tmp.activities!=null)
	        		return tmp.activities[childPosition];
	        	else
	        		return null;
			} catch (NameNotFoundException e) {
				return null;
			}

        }
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }
        public int getChildrenCount(int groupPosition) {
        	//return groups.get(groupPosition).activities.length;
        	PackageInfo tmp;
			try {
				tmp = mPackageManager.getPackageInfo(groups.get(groupPosition).packageName, PackageManager.GET_ACTIVITIES);
	        	if(tmp.activities!=null)
	        		return tmp.activities.length;
	        	else
	        		return 0;
			} catch (NameNotFoundException e) {
				return 0;
			}
        }
        public TextView getGenericView() {
            TextView textView = new TextView(ActivityPickerActivity.this);
            textView.setLayoutParams(lpGroup);
            textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            textView.setPadding(leftPadding, 0, 0, 0);
            return textView;
        }
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent) {
            TextView textView = getGenericView();
            ActivityInfo activity=getChild(groupPosition, childPosition);
            if(activity!=null){
	            String name=activity.name.replace(activity.packageName, "");
	        	textView.setText(activity.loadLabel(mPackageManager)+"("+name+")");
	        	textView.setLayoutParams(lpChild);
            }
            return textView;
        }
        public PackageInfo getGroup(int groupPosition) {
        	return groups.get(groupPosition);
        }
        public int getGroupCount() {
        	return groups.size();
        }
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                ViewGroup parent) {
            TextView textView = getGenericView();
            PackageInfo info=getGroup(groupPosition);
            textView.setText(info.applicationInfo.loadLabel(mPackageManager));
            textView.setCompoundDrawablesWithIntrinsicBounds(Utilities.createIconThumbnail(info.applicationInfo.loadIcon(mPackageManager),ActivityPickerActivity.this), null, null, null);
            return textView;
        }
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
        public boolean hasStableIds() {
            return true;
        }
    }
    public class PackageInfoComparable implements Comparator<PackageInfo>{
    	@Override
    	public int compare(PackageInfo o1, PackageInfo o2) {
    		return o1.applicationInfo.loadLabel(mPackageManager).toString().compareToIgnoreCase(o2.applicationInfo.loadLabel(mPackageManager).toString());
    	}
    }
}
