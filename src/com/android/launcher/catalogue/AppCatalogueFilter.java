package com.android.launcher.catalogue;

import android.content.SharedPreferences;

public class AppCatalogueFilter {

	private AppCatalogueFilters.Catalogue mCatalogue;

	public AppCatalogueFilter() {
		this(AppGroupAdapter.APP_GROUP_ALL);
	}

	public AppCatalogueFilter(int index) {
		setCurrentGroupIndex(index);
	}

	public boolean checkAppInGroup(String className) {
		boolean result = true;
		if (mCatalogue != null) {
			final SharedPreferences prefs = mCatalogue.getPreferences();
			if (prefs != null)
				result = prefs.getBoolean(className, false);
		}
		return result;
	}

	public boolean isUserGroup() {
		return mCatalogue != null;
	}

	public int getCurrentFilterIndex() {
		if (mCatalogue != null)
			return mCatalogue.getIndex();
		else
			return AppGroupAdapter.APP_GROUP_ALL;
	}

	public synchronized void setCurrentGroupIndex(int index) {
		if (index != getCurrentFilterIndex()) {
			mCatalogue = AppCatalogueFilters.getInstance().getCatalogue(index);
		}
	}


}
