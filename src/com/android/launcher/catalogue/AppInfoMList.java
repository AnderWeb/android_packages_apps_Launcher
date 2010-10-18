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

package com.android.launcher.catalogue;

import java.util.ArrayList;
import java.util.List;

import com.android.launcher.ApplicationInfo;
import com.android.launcher.LauncherModel;
import com.android.launcher.R;
import com.android.launcher.catalogue.AppCatalogueFilters.Catalogue;

import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class AppInfoMList extends ListActivity implements
		View.OnCreateContextMenuListener, View.OnClickListener,
		DialogInterface.OnCancelListener {
	private static final String TAG = "AppInfoMList";
	private static final boolean DBG = true;
	public static final String EXTRA_CATALOGUE_INDEX = "EXTRA_CATALOGUE_INDEX";

	// Custom Adapter used for managing items in the list
	private ApplicationListAdapter mAppInfoAdapter;
	// list of task info
	private ListView mAppInfoList;

	private Button mOkButton;
	private Catalogue mCatalogue;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		int GroupIndex = intent.getIntExtra(EXTRA_CATALOGUE_INDEX,
				AppCatalogueFilters.getInstance().getDrawerFilter().getCurrentFilterIndex());
		mCatalogue = AppCatalogueFilters.getInstance().getCatalogue(GroupIndex);
		if (mCatalogue == null) {
			setResult(RESULT_CANCELED);
			finish();
			return;
		}

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.app_group_conf_list);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title1);

		List<AppListInfo> appInfos = new ArrayList<AppListInfo>();

		/* list info */
		mAppInfoAdapter = new ApplicationListAdapter(this, appInfos);
		mAppInfoList = getListView();
		setListAdapter(mAppInfoAdapter);
		mAppInfoList.setOnCreateContextMenuListener(this);
		mAppInfoList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

		/* button info */
		mOkButton = ((Button) findViewById(R.id.Button_Ok_App));
		mOkButton.setOnClickListener(this);
		CheckBox cb = (CheckBox) findViewById(R.id.checkAll);
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				updateAppList(isChecked);
			}
		});

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		updateAppList();
	}

	// Finish the activity if the user presses the back button to cancel the
	// activity
	public void onCancel(DialogInterface dialog) {
		finish();
	}

	/*
	 * Method implementing functionality of buttons clicked
	 *
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v) {
		if (v == mOkButton) {

			SharedPreferences curAppGrp = mCatalogue.getPreferences();
			if (curAppGrp == null)
				return;// should not go here.

			SharedPreferences.Editor editor = curAppGrp.edit();
			//editor.clear();
			ApplicationListAdapter adapter = (ApplicationListAdapter) mAppInfoList.getAdapter();
			for (int i = 0; i < adapter.getCount(); i++) {
				AppListInfo tempAppListInfo = (AppListInfo) adapter.getItem(i);
				boolean checked = tempAppListInfo.checked;
				//ADW: Change to only store hidden apps
				if (checked)
					editor.putBoolean(tempAppListInfo.className, true);
				else
					editor.remove(tempAppListInfo.className);

				if (DBG && checked)
					Log.v("-----", tempAppListInfo.className);
			}

			editor.commit();
			setResult(RESULT_OK);
			finish();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		ApplicationListAdapter adapter = (ApplicationListAdapter) mAppInfoList.getAdapter();
		AppListInfo tempAppInfo = (AppListInfo) adapter.getItem(position);

		/* change check of list item */
		tempAppInfo.checked = !tempAppInfo.checked;
		mAppInfoAdapter.updateList();
		return;
	}

	/* update app into */
	private void updateAppList() {

		ArrayList<ApplicationInfo> appInfos = LauncherModel.mApplicationsAdapter.allItems;
		/* app info */
		final List<AppListInfo> savedAppInfos = new ArrayList<AppListInfo>();

		TextView t = (TextView) findViewById(R.id.left_title_text);

		mCatalogue.setTitleView(t);

		SharedPreferences curAppGrp = mCatalogue.getPreferences();

		for (int i = 0; i < appInfos.size(); i++) {
			AppListInfo tempAppListInfo = new AppListInfo();
			/* get App info */
			ApplicationInfo tempAppInfo = appInfos.get(i);

			tempAppListInfo.className = tempAppInfo.intent.getComponent()
					.flattenToString();
			tempAppListInfo.icon = tempAppInfo.icon;

			tempAppListInfo.title = tempAppInfo.title.toString();
			if (curAppGrp != null)
				tempAppListInfo.checked = curAppGrp.getBoolean(
						tempAppListInfo.className, false);
			else
				tempAppListInfo.checked = false;

			savedAppInfos.add(tempAppListInfo);
			if (DBG) Log.d(TAG, tempAppListInfo.className + " "
					+ tempAppListInfo.checked);
		}

		mAppInfoAdapter.updateList(savedAppInfos);
	}

	/* update app into */
	private void updateAppList(boolean bool) {
		ApplicationListAdapter adapter = (ApplicationListAdapter) mAppInfoList.getAdapter();
		for (int i = 0; i < adapter.getCount(); i++) {
			AppListInfo tempAppListInfo = (AppListInfo) adapter.getItem(i);
			tempAppListInfo.checked=bool;
		}
		mAppInfoAdapter.updateList();
	}
}
