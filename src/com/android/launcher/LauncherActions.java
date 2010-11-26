package com.android.launcher;

import java.util.ArrayList;
import java.util.List;

import com.android.launcher.catalogue.AppCatalogueFilter;
import com.android.launcher.catalogue.AppCatalogueFilters;

import android.content.Intent;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

public class LauncherActions {

	public interface Action {

		public String getName();

		public void putIntentExtras(Intent intent);

		public boolean runIntent(Intent intent);

		public int getIconResourceId();
	}

	private static LauncherActions mInstance = null;
	private Launcher mLauncher;

	public static synchronized LauncherActions getInstance() {
		if (mInstance == null)
			mInstance = new LauncherActions();
		return mInstance;
	}

	private LauncherActions() {
	}

	public void init(Launcher launcher) {
		mLauncher = launcher;
	}

	private List<Action> getList() {
		List<Action> result = new ArrayList<Action>();
		String[] menuBindingsNames = mLauncher.getResources().getStringArray(R.array.menu_binding_entries);
		String[] menuBindingsValues = mLauncher.getResources().getStringArray(R.array.menu_binding_values);

		for (int i = 0; i < menuBindingsValues.length; i++) {
			int value = Integer.parseInt(menuBindingsValues[i]);
			String name = menuBindingsNames[i];
			if (value != Launcher.BIND_NONE && value != Launcher.BIND_APP_LAUNCHER &&
				value != Launcher.BIND_HOME_PREVIEWS && value != Launcher.BIND_HOME_NOTIFICATIONS) {
				DefaultLauncherAction lact = new DefaultLauncherAction(value, name);
				result.add(lact);
			}
		}
		AppCatalogueFilters filters = AppCatalogueFilters.getInstance();
		List<AppCatalogueFilters.Catalogue> catalogues = filters.getAllGroups();
		for(AppCatalogueFilters.Catalogue cat : catalogues) {
			ShowGroupAction act = new ShowGroupAction(cat);
			result.add(act);
		}
		return result;
	}



	public Intent getIntentForAction(Action action) {
		Intent result = new Intent(CustomShirtcutActivity.ACTION_LAUNCHERACTION);
		result.setClass(mLauncher, CustomShirtcutActivity.class);
		action.putIntentExtras(result);
		return result;
	}

	public void launch(Intent intent) {
		final List<Action> actions = getList();
		for(Action act : actions) {
			if (act.runIntent(intent))
				break;
		}
	}


	public ListAdapter getSelectActionAdapter() {
		final List<Action> mActions = getList();

		return new ListAdapter() {

			@Override
			public void unregisterDataSetObserver(DataSetObserver observer) {
			}

			@Override
			public void registerDataSetObserver(DataSetObserver observer) {
			}

			@Override
			public boolean isEmpty() {
				return mActions.isEmpty();
			}

			@Override
			public boolean hasStableIds() {
				return false;
			}

			@Override
			public int getViewTypeCount() {
				return 1;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (convertView == null)
				{
					LayoutInflater li = mLauncher.getLayoutInflater();
					convertView = li.inflate(R.layout.add_list_item, parent, false);

				}
				Action act = mActions.get(position);

				TextView textView = (TextView) convertView;
		        textView.setText(act.getName());
		        textView.setCompoundDrawablesWithIntrinsicBounds(
		        		mLauncher.getResources().getDrawable(act.getIconResourceId()), null, null, null);
				return convertView;
			}

			@Override
			public int getItemViewType(int position) {
				return 0;
			}

			@Override
			public long getItemId(int position) {
				return position;
			}

			@Override
			public Object getItem(int position) {
				return mActions.get(position);
			}

			@Override
			public int getCount() {
				return mActions.size();
			}

			@Override
			public boolean isEnabled(int position) {
				return true;
			}

			@Override
			public boolean areAllItemsEnabled() {
				return true;
			}
		};
	}

	private class DefaultLauncherAction implements Action {

		private static final String EXTRA_BINDINGVALUE = "DefaultLauncherAction.EXTRA_BINDINGVALUE";

		private final int mBindingValue;
		private final String mName;

		public DefaultLauncherAction(int bindingValue, String name) {
			mBindingValue = bindingValue;
			mName = name;
		}

		@Override
		public String getName() {
			return mName;
		}

		@Override
		public void putIntentExtras(Intent intent) {
			intent.putExtra(EXTRA_BINDINGVALUE, mBindingValue);
		}

		@Override
		public boolean runIntent(Intent intent) {
			if (intent.hasExtra(EXTRA_BINDINGVALUE))
			{
				int val = intent.getIntExtra(EXTRA_BINDINGVALUE, 0);
				if (val == mBindingValue) {
					mLauncher.fireHomeBinding(mBindingValue, 0);
					return true;
				}

			}

			return false;
		}

		@Override
		public int getIconResourceId() {
			switch(mBindingValue) {
				case Launcher.BIND_DEFAULT:
					return R.drawable.movetodefault_button;
				case Launcher.BIND_PREVIEWS:
					return R.drawable.showpreviews_button;
				case Launcher.BIND_APPS:
					return R.drawable.all_apps_button;
				case Launcher.BIND_STATUSBAR:
					return R.drawable.showhidestatusbar_button;
				case Launcher.BIND_NOTIFICATIONS:
					return R.drawable.openclosenotifications_button;
				case Launcher.BIND_DOCKBAR:
					return R.drawable.openclosedockbar_button;
				default:
					return R.drawable.ic_launcher_home;
			}
		}
	}

	private class ShowGroupAction implements Action {

		private final AppCatalogueFilters.Catalogue mCatalogue;

		private static final String EXTRA_CATALOG_INDEX = "EXTRA_CATALOG_INDEX";

 		public ShowGroupAction(AppCatalogueFilters.Catalogue catalogue) {
			mCatalogue = catalogue;
		}

		@Override
		public String getName() {
			return String.format(mLauncher.getString(R.string.show_catalog), mCatalogue.getTitle());
		}

		@Override
		public void putIntentExtras(Intent intent) {
			intent.putExtra(EXTRA_CATALOG_INDEX, mCatalogue.getIndex());
		}

		@Override
		public boolean runIntent(Intent intent) {
			if (intent.hasExtra(EXTRA_CATALOG_INDEX)) {
				int idx = intent.getIntExtra(EXTRA_CATALOG_INDEX, 0);
				if (idx == mCatalogue.getIndex()) {
					showDrawer();
					return true;
				}
			}
			return false;
		}

		private void showDrawer() {
			AppCatalogueFilter filter = new AppCatalogueFilter(mCatalogue.getIndex());
			mLauncher.showAllApps(true, filter);
		}

		@Override
		public int getIconResourceId() {
			return R.drawable.ic_launcher_home;
		}
	}

}
