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

import static android.util.Log.d;
import static android.util.Log.e;
import static android.util.Log.w;

import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.android.launcher.IconShader.CompiledIconShader;
import com.android.launcher.catalogue.AppCatalogueFilters;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Process;

/**
 * Maintains in-memory state of the Launcher. It is expected that there should be only one
 * LauncherModel object held in a static. Also provide APIs for updating the database state
 * for the Launcher.
 */
public class LauncherModel {
    static final boolean DEBUG_LOADERS = true;
    static final String LOG_TAG = "HomeLoaders";

    private static final int UI_NOTIFICATION_RATE = 4;
    private static final int DEFAULT_APPLICATIONS_NUMBER = 42;
    private static final int INITIAL_ICON_CACHE_CAPACITY = 50;

    private static final Collator sCollator = Collator.getInstance();

    private boolean mApplicationsLoaded;
    private boolean mDesktopItemsLoaded;

    private ArrayList<ItemInfo> mDesktopItems;
    private ArrayList<LauncherAppWidgetInfo> mDesktopAppWidgets;
    private HashMap<Long, FolderInfo> mFolders;

    public static ArrayList<ApplicationInfo> mApplications;

    public static ApplicationsAdapter mApplicationsAdapter;
    private ApplicationsLoader mApplicationsLoader;
    private DesktopItemsLoader mDesktopItemsLoader;
    private Thread mApplicationsLoaderThread;
    private Thread mDesktopLoaderThread;
    private int mDesktopColumns;
    private int mDesktopRows;
    private final HashMap<ComponentName, ApplicationInfo> mAppInfoCache =
            new HashMap<ComponentName, ApplicationInfo>(INITIAL_ICON_CACHE_CAPACITY);

    private static String compiledIconShaderName;
    private static CompiledIconShader compiledIconShader;

    synchronized void abortLoaders() {
        if (DEBUG_LOADERS) d(LOG_TAG, "aborting loaders");

        if (mApplicationsLoader != null && mApplicationsLoader.isRunning()) {
            if (DEBUG_LOADERS) d(LOG_TAG, "  --> aborting applications loader");
            mApplicationsLoader.stop();
        }

        if (mDesktopItemsLoader != null && mDesktopItemsLoader.isRunning()) {
            if (DEBUG_LOADERS) d(LOG_TAG, "  --> aborting workspace loader");
            mDesktopItemsLoader.stop();
            mDesktopItemsLoaded = false;
        }
    }

    /**
     * Drop our cache of components to their lables & icons.  We do
     * this from Launcher when applications are added/removed.  It's a
     * bit overkill, but it's a rare operation anyway.
     */
    synchronized void dropApplicationCache() {
        mAppInfoCache.clear();
    }

    /**
     * Loads the list of installed applications in mApplications.
     *
     * @return true if the applications loader must be started
     *         (see startApplicationsLoader()), false otherwise.
     */
    synchronized boolean loadApplications(boolean isLaunching, Launcher launcher,
            boolean localeChanged) {

        if (DEBUG_LOADERS) d(LOG_TAG, "load applications");

        if (isLaunching && mApplicationsLoaded && !localeChanged) {
            mApplicationsAdapter = new ApplicationsAdapter(launcher, mApplications,
            		AppCatalogueFilters.getInstance().getDrawerFilter());
            if (DEBUG_LOADERS) d(LOG_TAG, "  --> applications loaded, return");
            return false;
        }

        stopAndWaitForApplicationsLoader();

        if (localeChanged) {
            dropApplicationCache();
        }

        if (mApplicationsAdapter == null || isLaunching || localeChanged) {
            mApplications = new ArrayList<ApplicationInfo>(DEFAULT_APPLICATIONS_NUMBER);
            mApplicationsAdapter = new ApplicationsAdapter(launcher, mApplications,
            		AppCatalogueFilters.getInstance().getDrawerFilter());
        }

        mApplicationsLoaded = false;

        if (!isLaunching) {
            startApplicationsLoaderLocked(launcher, false);
            return false;
        }

        return true;
    }

    private synchronized void stopAndWaitForApplicationsLoader() {
        if (mApplicationsLoader != null && mApplicationsLoader.isRunning()) {
            if (DEBUG_LOADERS) {
                d(LOG_TAG, "  --> wait for applications loader (" + mApplicationsLoader.mId + ")");
            }

            mApplicationsLoader.stop();
            // Wait for the currently running thread to finish, this can take a little
            // time but it should be well below the timeout limit
            try {
                mApplicationsLoaderThread.join();
            } catch (InterruptedException e) {
                e(LOG_TAG, "mApplicationsLoaderThread didn't exit in time");
            }
        }
    }

    private synchronized void startApplicationsLoader(Launcher launcher, boolean isLaunching) {
        if (DEBUG_LOADERS) d(LOG_TAG, "  --> starting applications loader unlocked");
        startApplicationsLoaderLocked(launcher, isLaunching);
    }

    private void startApplicationsLoaderLocked(Launcher launcher, boolean isLaunching) {
        if (DEBUG_LOADERS) d(LOG_TAG, "  --> starting applications loader");

        stopAndWaitForApplicationsLoader();

        mApplicationsLoader = new ApplicationsLoader(launcher, isLaunching);
        mApplicationsLoaderThread = new Thread(mApplicationsLoader, "Applications Loader");
        mApplicationsLoaderThread.start();
    }

    synchronized void addPackage(Launcher launcher, String packageName) {
        if (mApplicationsLoader != null && mApplicationsLoader.isRunning()) {
            startApplicationsLoaderLocked(launcher, false);
            return;
        }

        if (packageName != null && packageName.length() > 0 && mApplicationsAdapter!=null) {
            final PackageManager packageManager = launcher.getPackageManager();
            final List<ResolveInfo> matches = findActivitiesForPackage(packageManager, packageName);

            if (matches.size() > 0) {
                final ApplicationsAdapter adapter = mApplicationsAdapter;
                final HashMap<ComponentName, ApplicationInfo> cache = mAppInfoCache;

                for (ResolveInfo info : matches) {
                    adapter.setNotifyOnChange(false);
                    adapter.add(makeAndCacheApplicationInfo(packageManager, cache, info, launcher));
                }
				adapter.updateDataSet();

                //adapter.sort(new ApplicationInfoComparator());
                //adapter.notifyDataSetChanged();
            }
        }
    }

    synchronized void removePackage(Launcher launcher, String packageName) {
    	// for add/remove Package, we need use applications adapter's "full" list.

        if (mApplicationsLoader != null && mApplicationsLoader.isRunning()) {
            dropApplicationCache(); // TODO: this could be optimized
            startApplicationsLoaderLocked(launcher, false);
            return;
        }

        if (packageName != null && packageName.length() > 0 && mApplicationsAdapter!=null) {
            final ApplicationsAdapter adapter = mApplicationsAdapter;

            final List<ApplicationInfo> toRemove = new ArrayList<ApplicationInfo>();
            final ArrayList<ApplicationInfo> allItems = adapter.allItems;
            final int count = allItems.size();

            for (int i = 0; i < count; i++) {
                final ApplicationInfo applicationInfo = allItems.get(i);
                final Intent intent = applicationInfo.intent;
                final ComponentName component = intent.getComponent();
                if (packageName.equals(component.getPackageName())) {
                    toRemove.add(applicationInfo);
                }
            }

            final HashMap<ComponentName, ApplicationInfo> cache = mAppInfoCache;
            for (ApplicationInfo info : toRemove) {
                adapter.setNotifyOnChange(false);
                adapter.remove(info);
                cache.remove(info.intent.getComponent());
            }

            if (toRemove.size() > 0) {
				adapter.updateDataSet();
                //adapter.sort(new ApplicationInfoComparator());
                //adapter.notifyDataSetChanged();
            }
        }
    }

    synchronized void updatePackage(Launcher launcher, String packageName) {
        if (mApplicationsLoader != null && mApplicationsLoader.isRunning()) {
            startApplicationsLoaderLocked(launcher, false);
            return;
        }

        if (packageName != null && packageName.length() > 0 && mApplicationsAdapter!=null) {
            final PackageManager packageManager = launcher.getPackageManager();
            final ApplicationsAdapter adapter = mApplicationsAdapter;

            final List<ResolveInfo> matches = findActivitiesForPackage(packageManager, packageName);
            final int count = matches.size();

            boolean changed = false;

            for (int i = 0; i < count; i++) {
                final ResolveInfo info = matches.get(i);
                final ApplicationInfo applicationInfo = findIntent(adapter,
                        info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
                if (applicationInfo != null) {
                    updateAndCacheApplicationInfo(packageManager, info, applicationInfo, launcher);
                    changed = true;
                }
            }

            if (syncLocked(launcher, packageName)) changed = true;

            if (changed) {
				adapter.updateDataSet();
                //adapter.sort(new ApplicationInfoComparator());
                //adapter.notifyDataSetChanged();
            }
        }
    }

    private void updateAndCacheApplicationInfo(PackageManager packageManager, ResolveInfo info,
            ApplicationInfo applicationInfo, Context context) {

        updateApplicationInfoTitleAndIcon(packageManager, info, applicationInfo, context);

        ComponentName componentName = new ComponentName(
                info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
        mAppInfoCache.put(componentName, applicationInfo);
    }

    synchronized void syncPackage(Launcher launcher, String packageName) {
        if (mApplicationsLoader != null && mApplicationsLoader.isRunning()) {
            startApplicationsLoaderLocked(launcher, false);
            return;
        }

        if (packageName != null && packageName.length() > 0 && mApplicationsAdapter!=null) {
            if (syncLocked(launcher, packageName)) {
                final ApplicationsAdapter adapter = mApplicationsAdapter;
				adapter.updateDataSet();
                //adapter.sort(new ApplicationInfoComparator());
                //adapter.notifyDataSetChanged();
            }
        }
    }

    private boolean syncLocked(Launcher launcher, String packageName) {
        final PackageManager packageManager = launcher.getPackageManager();
        final List<ResolveInfo> matches = findActivitiesForPackage(packageManager, packageName);

        if (matches.size() > 0 && mApplicationsAdapter!=null) {
            final ApplicationsAdapter adapter = mApplicationsAdapter;

            // Find disabled activities and remove them from the adapter
            boolean removed = removeDisabledActivities(packageName, matches, adapter);
            // Find enable activities and add them to the adapter
            // Also updates existing activities with new labels/icons
            boolean added = addEnabledAndUpdateActivities(matches, adapter, launcher);

            return added || removed;
        }

        return false;
    }

    private static List<ResolveInfo> findActivitiesForPackage(PackageManager packageManager,
            String packageName) {

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        final List<ResolveInfo> matches = new ArrayList<ResolveInfo>();

        if (apps != null) {
            // Find all activities that match the packageName
            int count = apps.size();
            for (int i = 0; i < count; i++) {
                final ResolveInfo info = apps.get(i);
                final ActivityInfo activityInfo = info.activityInfo;
                if (packageName.equals(activityInfo.packageName)) {
                    matches.add(info);
                }
            }
        }

        return matches;
    }

    private boolean addEnabledAndUpdateActivities(List<ResolveInfo> matches,
            ApplicationsAdapter adapter, Launcher launcher) {

        final List<ApplicationInfo> toAdd = new ArrayList<ApplicationInfo>();
        final int count = matches.size();

        boolean changed = false;

        for (int i = 0; i < count; i++) {
            final ResolveInfo info = matches.get(i);
            final ApplicationInfo applicationInfo = findIntent(adapter,
                    info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
            if (applicationInfo == null) {
                toAdd.add(makeAndCacheApplicationInfo(launcher.getPackageManager(),
                        mAppInfoCache, info, launcher));
                changed = true;
            } else {
                updateAndCacheApplicationInfo(
                        launcher.getPackageManager(), info, applicationInfo, launcher);
                changed = true;
            }
        }

        for (ApplicationInfo info : toAdd) {
            adapter.setNotifyOnChange(false);
            adapter.add(info);
        }

        return changed;
    }

    private boolean removeDisabledActivities(String packageName, List<ResolveInfo> matches,
            ApplicationsAdapter adapter) {

        final List<ApplicationInfo> toRemove = new ArrayList<ApplicationInfo>();
        final int count = adapter.getCount();

        boolean changed = false;

        for (int i = 0; i < count; i++) {
            final ApplicationInfo applicationInfo = adapter.getItem(i);
            final Intent intent = applicationInfo.intent;
            final ComponentName component = intent.getComponent();
            if (packageName.equals(component.getPackageName())) {
                if (!findIntent(matches, component)) {
                    toRemove.add(applicationInfo);
                    changed = true;
                }
            }
        }

        final HashMap<ComponentName, ApplicationInfo> cache = mAppInfoCache;
        for (ApplicationInfo info : toRemove) {
            adapter.setNotifyOnChange(false);
            adapter.remove(info);
            cache.remove(info.intent.getComponent());
        }

        return changed;
    }

    private static ApplicationInfo findIntent(ApplicationsAdapter adapter, String packageName,
            String name) {

        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            final ApplicationInfo applicationInfo = adapter.getItem(i);
            final Intent intent = applicationInfo.intent;
            final ComponentName component = intent.getComponent();
            if (packageName.equals(component.getPackageName()) &&
                    name.equals(component.getClassName())) {
                return applicationInfo;
            }
        }

        return null;
    }

    private static boolean findIntent(List<ResolveInfo> apps, ComponentName component) {
        final String className = component.getClassName();
        for (ResolveInfo info : apps) {
            final ActivityInfo activityInfo = info.activityInfo;
            if (activityInfo.name.equals(className)) {
                return true;
            }
        }
        return false;
    }

    Drawable getApplicationInfoIcon(PackageManager manager, ApplicationInfo info) {
        final ResolveInfo resolveInfo = manager.resolveActivity(info.intent, 0);
        if (resolveInfo == null) {
            return null;
        }

        ComponentName componentName = new ComponentName(
                resolveInfo.activityInfo.applicationInfo.packageName,
                resolveInfo.activityInfo.name);
        ApplicationInfo application = mAppInfoCache.get(componentName);

        if (application == null) {
            return resolveInfo.activityInfo.loadIcon(manager);
        }

        return application.icon;
    }

    private static ApplicationInfo makeAndCacheApplicationInfo(PackageManager manager,
            HashMap<ComponentName, ApplicationInfo> appInfoCache, ResolveInfo info,
            Context context) {

        ComponentName componentName = new ComponentName(
                info.activityInfo.applicationInfo.packageName,
                info.activityInfo.name);
        ApplicationInfo application = appInfoCache.get(componentName);

        if (application == null) {
            application = new ApplicationInfo();
            application.container = ItemInfo.NO_ID;

            updateApplicationInfoTitleAndIcon(manager, info, application, context);

            application.setActivity(componentName,
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

            appInfoCache.put(componentName, application);
        }

        return application;
    }

    private static void updateApplicationInfoTitleAndIcon(PackageManager manager, ResolveInfo info,
            ApplicationInfo application, Context context) {

        application.title = info.loadLabel(manager);
        if (application.title == null) {
            application.title = info.activityInfo.name;
        }

        application.icon = getIcon(manager, context, info.activityInfo);

        application.filtered = false;
    }

    private static final AtomicInteger sAppsLoaderCount = new AtomicInteger(1);
    private static final AtomicInteger sWorkspaceLoaderCount = new AtomicInteger(1);

    private class ApplicationsLoader implements Runnable {
        private final WeakReference<Launcher> mLauncher;

        private volatile boolean mStopped;
        private volatile boolean mRunning;
        private final boolean mIsLaunching;
        private final int mId;

        ApplicationsLoader(Launcher launcher, boolean isLaunching) {
            mIsLaunching = isLaunching;
            mLauncher = new WeakReference<Launcher>(launcher);
            mRunning = true;
            mId = sAppsLoaderCount.getAndIncrement();
        }

        void stop() {
            mStopped = true;
        }

        boolean isRunning() {
            return mRunning;
        }

        public void run() {
            if (DEBUG_LOADERS) d(LOG_TAG, "  ----> running applications loader (" + mId + ")");

            // Elevate priority when Home launches for the first time to avoid
            // starving at boot time. Staring at a blank home is not cool.
            android.os.Process.setThreadPriority(mIsLaunching ? Process.THREAD_PRIORITY_DEFAULT :
                    Process.THREAD_PRIORITY_BACKGROUND);

            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            final Launcher launcher = mLauncher.get();
            final PackageManager manager = launcher.getPackageManager();
            final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);

            if (apps != null && !mStopped) {
                final int count = apps.size();
                // Can be set to null on the UI thread by the unbind() method
                // Do not access without checking for null first
                final ApplicationsAdapter applicationList = mApplicationsAdapter;

                ChangeNotifier action = new ChangeNotifier(applicationList, true);
                final HashMap<ComponentName, ApplicationInfo> appInfoCache = mAppInfoCache;

                for (int i = 0; i < count && !mStopped; i++) {
                    ResolveInfo info = apps.get(i);
                    ApplicationInfo application =
                        makeAndCacheApplicationInfo(manager, appInfoCache, info, launcher);

                    if (action.add(application) && !mStopped) {
                        launcher.runOnUiThread(action);
                        action = new ChangeNotifier(applicationList, false);
                    }
                }

                launcher.runOnUiThread(action);
            }

            /* If we've made it this far and mStopped isn't set, we've successfully loaded
             * applications.  Otherwise, applications aren't loaded. */
            mApplicationsLoaded = !mStopped;

            if (mStopped) {
                if (DEBUG_LOADERS) d(LOG_TAG, "  ----> applications loader stopped (" + mId + ")");
            }
            mRunning = false;
        }
    }

    private static class ChangeNotifier implements Runnable {
        private final ApplicationsAdapter mApplicationList;
        private final ArrayList<ApplicationInfo> mBuffer;

        private boolean mFirst = true;

        ChangeNotifier(ApplicationsAdapter applicationList, boolean first) {
            mApplicationList = applicationList;
            mFirst = first;
            mBuffer = new ArrayList<ApplicationInfo>(UI_NOTIFICATION_RATE);
        }

        public void run() {
            final ApplicationsAdapter applicationList = mApplicationList;
            // Can be set to null on the UI thread by the unbind() method
            if (applicationList == null) return;

            if (mFirst) {
                applicationList.setNotifyOnChange(false);
                applicationList.clear();
                if (DEBUG_LOADERS) d(LOG_TAG, "  ----> cleared application list");
                mFirst = false;
            }

            final ArrayList<ApplicationInfo> buffer = mBuffer;
            final int count = buffer.size();

            for (int i = 0; i < count; i++) {
                applicationList.setNotifyOnChange(false);
                applicationList.add(buffer.get(i));
            }

            buffer.clear();

			applicationList.updateDataSet();
            //applicationList.sort(new ApplicationInfoComparator());
            //applicationList.notifyDataSetChanged();
        }

        boolean add(ApplicationInfo application) {
            final ArrayList<ApplicationInfo> buffer = mBuffer;
            buffer.add(application);
            return buffer.size() >= UI_NOTIFICATION_RATE;
        }
    }

    static class ApplicationInfoComparator implements Comparator<ApplicationInfo> {
        public final int compare(ApplicationInfo a, ApplicationInfo b) {
            return sCollator.compare(a.title.toString(), b.title.toString());
        }
    }

    boolean isDesktopLoaded() {
        return mDesktopItems != null && mDesktopAppWidgets != null && mDesktopItemsLoaded;
    }

    /**
     * Loads all of the items on the desktop, in folders, or in the dock.
     * These can be apps, shortcuts or widgets
     */
    void loadUserItems(boolean isLaunching, Launcher launcher, boolean localeChanged,
            boolean loadApplications) {
        if (DEBUG_LOADERS) d(LOG_TAG, "loading user items in " + Thread.currentThread().toString());
        //ADW: load columns/rows settings
        mDesktopRows=AlmostNexusSettingsHelper.getDesktopRows(launcher);
        mDesktopColumns=AlmostNexusSettingsHelper.getDesktopColumns(launcher);
        if (isLaunching && isDesktopLoaded()) {
            if (DEBUG_LOADERS) d(LOG_TAG, "  --> items loaded, return");
            if (loadApplications) startApplicationsLoader(launcher, true);
            //if (SystemProperties.get("debug.launcher.ignore-cache", "") == "") {
                // We have already loaded our data from the DB
                if (DEBUG_LOADERS) d(LOG_TAG, "  --> loading from cache: " + mDesktopItems.size() + ", " + mDesktopAppWidgets.size());
                launcher.onDesktopItemsLoaded(mDesktopItems, mDesktopAppWidgets);
                return;
            //}
            //else
            //{
                //d(LOG_TAG, "  ----> debug: forcing reload of workspace");
            //}
        }

        if (mDesktopItemsLoader != null && mDesktopItemsLoader.isRunning()) {
            if (DEBUG_LOADERS) d(LOG_TAG, "  --> stopping workspace loader");
            mDesktopItemsLoader.stop();
            // Wait for the currently running thread to finish, this can take a little
            // time but it should be well below the timeout limit
            try {
                mDesktopLoaderThread.join();
            } catch (InterruptedException e) {
                e(LOG_TAG, "mDesktopLoaderThread didn't exit in time");
            }

            // If the thread we are interrupting was tasked to load the list of
            // applications make sure we keep that information in the new loader
            // spawned below
            // note: we don't apply this to localeChanged because the thread can
            // only be stopped *after* the localeChanged handling has occured
            loadApplications = mDesktopItemsLoader.mLoadApplications;
        }

        if (DEBUG_LOADERS) d(LOG_TAG, "  --> starting workspace loader");
        mDesktopItemsLoaded = false;
        mDesktopItemsLoader = new DesktopItemsLoader(launcher, localeChanged, loadApplications,
                isLaunching);
        mDesktopLoaderThread = new Thread(mDesktopItemsLoader, "Desktop Items Loader");
        mDesktopLoaderThread.start();
    }

    private static String getLabel(PackageManager manager, ActivityInfo activityInfo) {
        String label = activityInfo.loadLabel(manager).toString();
        if (label == null) {
            label = manager.getApplicationLabel(activityInfo.applicationInfo).toString();
            if (label == null) {
                label = activityInfo.name;
            }
        }
        return label;
    }

    private class DesktopItemsLoader implements Runnable {
        private volatile boolean mStopped;
        private volatile boolean mFinished;

        private final WeakReference<Launcher> mLauncher;
        private final boolean mLocaleChanged;
        private final boolean mLoadApplications;
        private final boolean mIsLaunching;
        private final int mId;

        DesktopItemsLoader(Launcher launcher, boolean localeChanged, boolean loadApplications,
                boolean isLaunching) {
            mLoadApplications = loadApplications;
            mIsLaunching = isLaunching;
            mLauncher = new WeakReference<Launcher>(launcher);
            mLocaleChanged = localeChanged;
            mId = sWorkspaceLoaderCount.getAndIncrement();
            mFinished = false;
        }

        void stop() {
            d(LOG_TAG, "  ----> workspace loader " + mId + " stopped from " + Thread.currentThread().toString());
            mStopped = true;
        }

        boolean isRunning() {
            return !mFinished;
        }

        public void run() {
            assert(!mFinished); // can only run once
            load_workspace();
            mFinished = true;
        }

        private void load_workspace() {
            if (DEBUG_LOADERS) d(LOG_TAG, "  ----> running workspace loader (" + mId + ")");

            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

            final Launcher launcher = mLauncher.get();
            final ContentResolver contentResolver = launcher.getContentResolver();
            final PackageManager manager = launcher.getPackageManager();

            if (mLocaleChanged) {
                updateShortcutLabels(contentResolver, manager);
            }

            final ArrayList<ItemInfo> desktopItems = new ArrayList<ItemInfo>();
            final ArrayList<LauncherAppWidgetInfo> desktopAppWidgets = new ArrayList<LauncherAppWidgetInfo>();
            final HashMap<Long, FolderInfo> folders = new HashMap<Long, FolderInfo>();

            final Cursor c = contentResolver.query(
                    LauncherSettings.Favorites.CONTENT_URI, null, null, null, null);

            try {
                final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
                final int intentIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
                final int titleIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
                final int iconTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_TYPE);
                final int iconIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
                final int iconPackageIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_PACKAGE);
                final int iconResourceIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_RESOURCE);
                final int containerIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
                final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
                final int appWidgetIdIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.APPWIDGET_ID);
                final int screenIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
                final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
                final int spanXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
                final int spanYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);
                final int uriIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.URI);
                final int displayModeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.DISPLAY_MODE);

                ApplicationInfo info;
                String intentDescription;
                //Widget widgetInfo;
                LauncherAppWidgetInfo appWidgetInfo;
                int container;
                long id;
                Intent intent;

                while (!mStopped && c.moveToNext()) {
                    try {
                        int itemType = c.getInt(itemTypeIndex);
                        switch (itemType) {
                        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                        case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                            intentDescription = c.getString(intentIndex);
                            try {
                                intent = Intent.parseUri(intentDescription, 0);
                            } catch (java.net.URISyntaxException e) {
                                continue;
                            }
                            if (itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                                info = getApplicationInfo(manager, intent, launcher);
                            } else {
                                info = getApplicationInfoShortcut(c, launcher, iconTypeIndex,
                                        iconPackageIndex, iconResourceIndex, iconIndex);
                            }

                            if (info == null) {
                                info = new ApplicationInfo();
                                info.icon = manager.getDefaultActivityIcon();
                            }

                            if (info != null) {
                                info.title = c.getString(titleIndex);
                                info.intent = intent;

                                info.id = c.getLong(idIndex);
                                container = c.getInt(containerIndex);
                                info.container = container;
                                info.screen = c.getInt(screenIndex);
                                info.cellX = c.getInt(cellXIndex);
                                info.cellY = c.getInt(cellYIndex);

                                switch (container) {
                                case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                case LauncherSettings.Favorites.CONTAINER_DOCKBAR:
                                case LauncherSettings.Favorites.CONTAINER_LAB:
                                case LauncherSettings.Favorites.CONTAINER_RAB:
                                case LauncherSettings.Favorites.CONTAINER_LAB2:
                                case LauncherSettings.Favorites.CONTAINER_RAB2:
                                    desktopItems.add(info);
                                    break;
                                default:
                                    // Item is in a user folder
                                    UserFolderInfo folderInfo =
                                            findOrMakeUserFolder(folders, container);
                                    folderInfo.add(info);
                                    break;
                                }
                            }
                            break;
                        case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:

                            id = c.getLong(idIndex);
                            UserFolderInfo folderInfo = findOrMakeUserFolder(folders, id);

                            folderInfo.title = c.getString(titleIndex);

                            folderInfo.id = id;
                            container = c.getInt(containerIndex);
                            folderInfo.container = container;
                            folderInfo.screen = c.getInt(screenIndex);
                            folderInfo.cellX = c.getInt(cellXIndex);
                            folderInfo.cellY = c.getInt(cellYIndex);

                            switch (container) {
                                case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                case LauncherSettings.Favorites.CONTAINER_DOCKBAR:
                                case LauncherSettings.Favorites.CONTAINER_LAB:
                                case LauncherSettings.Favorites.CONTAINER_RAB:
                                case LauncherSettings.Favorites.CONTAINER_LAB2:
                                case LauncherSettings.Favorites.CONTAINER_RAB2:
                                    desktopItems.add(folderInfo);
                                    break;
                            }
                            break;
                        case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:

                            id = c.getLong(idIndex);
                            LiveFolderInfo liveFolderInfo = findOrMakeLiveFolder(folders, id);

                            intentDescription = c.getString(intentIndex);
                            intent = null;
                            if (intentDescription != null) {
                                try {
                                    intent = Intent.parseUri(intentDescription, 0);
                                } catch (java.net.URISyntaxException e) {
                                    // Ignore, a live folder might not have a base intent
                                }
                            }

                            liveFolderInfo.title = c.getString(titleIndex);
                            liveFolderInfo.id = id;
                            container = c.getInt(containerIndex);
                            liveFolderInfo.container = container;
                            liveFolderInfo.screen = c.getInt(screenIndex);
                            liveFolderInfo.cellX = c.getInt(cellXIndex);
                            liveFolderInfo.cellY = c.getInt(cellYIndex);
                            liveFolderInfo.uri = Uri.parse(c.getString(uriIndex));
                            liveFolderInfo.baseIntent = intent;
                            liveFolderInfo.displayMode = c.getInt(displayModeIndex);

                            loadLiveFolderIcon(launcher, c, iconTypeIndex, iconPackageIndex,
                                    iconResourceIndex, liveFolderInfo);

                            switch (container) {
                                case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                case LauncherSettings.Favorites.CONTAINER_DOCKBAR:
                                case LauncherSettings.Favorites.CONTAINER_LAB:
                                case LauncherSettings.Favorites.CONTAINER_RAB:
                                case LauncherSettings.Favorites.CONTAINER_LAB2:
                                case LauncherSettings.Favorites.CONTAINER_RAB2:
                                    desktopItems.add(liveFolderInfo);
                                    break;
                            }
                            break;
                        /*case LauncherSettings.Favorites.ITEM_TYPE_WIDGET_SEARCH:
                            widgetInfo = Widget.makeSearch();

                            container = c.getInt(containerIndex);
                            if (container != LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                                e(Launcher.LOG_TAG, "Widget found where container "
                                        + "!= CONTAINER_DESKTOP  ignoring!");
                                continue;
                            }

                            widgetInfo.id = c.getLong(idIndex);
                            widgetInfo.screen = c.getInt(screenIndex);
                            widgetInfo.container = container;
                            widgetInfo.cellX = c.getInt(cellXIndex);
                            widgetInfo.cellY = c.getInt(cellYIndex);
                            widgetInfo.spanX = c.getInt(spanXIndex);
                            widgetInfo.spanY = c.getInt(spanYIndex);

                            desktopItems.add(widgetInfo);
                            break;*/
                        case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                            // Read all Launcher-specific widget details
                            int appWidgetId = c.getInt(appWidgetIdIndex);
                            appWidgetInfo = new LauncherAppWidgetInfo(appWidgetId);
                            appWidgetInfo.id = c.getLong(idIndex);
                            appWidgetInfo.screen = c.getInt(screenIndex);
                            appWidgetInfo.cellX = c.getInt(cellXIndex);
                            appWidgetInfo.cellY = c.getInt(cellYIndex);
                            appWidgetInfo.spanX = c.getInt(spanXIndex);
                            appWidgetInfo.spanY = c.getInt(spanYIndex);

                            container = c.getInt(containerIndex);
                            if (container != LauncherSettings.Favorites.CONTAINER_DESKTOP) {
                                e(Launcher.LOG_TAG, "Widget found where container "
                                        + "!= CONTAINER_DESKTOP -- ignoring!");
                                continue;
                            }
                            appWidgetInfo.container = c.getInt(containerIndex);

                            desktopAppWidgets.add(appWidgetInfo);
                            break;
                        }
                    } catch (Exception e) {
                        w(Launcher.LOG_TAG, "Desktop items loading interrupted:", e);
                    }
                }
            } finally {
                c.close();
            }
            if (DEBUG_LOADERS) {
                d(LOG_TAG, "  ----> workspace loader " + mId + " finished loading data");
                d(LOG_TAG, "  ----> worskpace items=" + desktopItems.size());
                d(LOG_TAG, "  ----> worskpace widgets=" + desktopAppWidgets.size());
            }

            synchronized(LauncherModel.this) {
                if (!mStopped) {
                    if (DEBUG_LOADERS)  {
                        d(LOG_TAG, "  --> done loading workspace; not stopped");
                    }

                    // Create a copy of the lists in case the workspace loader is restarted
                    // and the list are cleared before the UI can go through them
                    final ArrayList<ItemInfo> uiDesktopItems =
                            new ArrayList<ItemInfo>(desktopItems);
                    final ArrayList<LauncherAppWidgetInfo> uiDesktopWidgets =
                            new ArrayList<LauncherAppWidgetInfo>(desktopAppWidgets);

                    if (!mStopped) {
                        d(LOG_TAG, "  ----> items cloned, ready to refresh UI");
                        launcher.runOnUiThread(new Runnable() {
                            public void run() {
                                if (DEBUG_LOADERS) d(LOG_TAG, "  ----> onDesktopItemsLoaded()");
                                launcher.onDesktopItemsLoaded(uiDesktopItems, uiDesktopWidgets);
                            }
                        });
                    }

                    if (mLoadApplications) {
                        if (DEBUG_LOADERS) {
                            d(LOG_TAG, "  ----> loading applications from workspace loader");
                        }
                        startApplicationsLoader(launcher, mIsLaunching);
                    }

                    mDesktopItems = desktopItems;
                    mDesktopAppWidgets = desktopAppWidgets;
                    mFolders = folders;
                    mDesktopItemsLoaded = true;
                } else {
                    if (DEBUG_LOADERS) d(LOG_TAG, "  ----> worskpace loader was stopped");
                }
            }
        }

        private void updateShortcutLabels(ContentResolver resolver, PackageManager manager) {
            final Cursor c = resolver.query(LauncherSettings.Favorites.CONTENT_URI,
                    new String[] { LauncherSettings.Favorites._ID, LauncherSettings.Favorites.TITLE,
                            LauncherSettings.Favorites.INTENT, LauncherSettings.Favorites.ITEM_TYPE },
                    null, null, null);

            final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
            final int intentIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
            final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
            final int titleIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);

            // boolean changed = false;

            try {
                while (!mStopped && c.moveToNext()) {
                    try {
                        if (c.getInt(itemTypeIndex) !=
                                LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
                            continue;
                        }

                        final String intentUri = c.getString(intentIndex);
                        if (intentUri != null) {
                            final Intent shortcut = Intent.parseUri(intentUri, 0);
                            if (Intent.ACTION_MAIN.equals(shortcut.getAction())) {
                                final ComponentName name = shortcut.getComponent();
                                if (name != null) {
                                    final ActivityInfo activityInfo = manager.getActivityInfo(name, 0);
                                    final String title = c.getString(titleIndex);
                                    String label = getLabel(manager, activityInfo);

                                    if (title == null || !title.equals(label)) {
                                        final ContentValues values = new ContentValues();
                                        values.put(LauncherSettings.Favorites.TITLE, label);

                                        resolver.update(
                                                LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION,
                                                values, "_id=?",
                                                new String[] { String.valueOf(c.getLong(idIndex)) });

                                        // changed = true;
                                    }
                                }
                            }
                        }
                    } catch (URISyntaxException e) {
                        // Ignore
                    } catch (PackageManager.NameNotFoundException e) {
                        // Ignore
                    }
                }
            } finally {
                c.close();
            }

            // if (changed) resolver.notifyChange(Settings.Favorites.CONTENT_URI, null);
        }
    }

    private static void loadLiveFolderIcon(Launcher launcher, Cursor c, int iconTypeIndex,
            int iconPackageIndex, int iconResourceIndex, LiveFolderInfo liveFolderInfo) {

        int iconType = c.getInt(iconTypeIndex);
        switch (iconType) {
            case LauncherSettings.Favorites.ICON_TYPE_RESOURCE:
                String packageName = c.getString(iconPackageIndex);
                String resourceName = c.getString(iconResourceIndex);
                PackageManager packageManager = launcher.getPackageManager();
                try {
                    Resources resources = packageManager.getResourcesForApplication(packageName);
                    final int id = resources.getIdentifier(resourceName, null, null);
                    liveFolderInfo.icon = resources.getDrawable(id);
                } catch (Exception e) {
                    liveFolderInfo.icon =
                            launcher.getResources().getDrawable(R.drawable.ic_launcher_folder);
                }
                liveFolderInfo.iconResource = new Intent.ShortcutIconResource();
                liveFolderInfo.iconResource.packageName = packageName;
                liveFolderInfo.iconResource.resourceName = resourceName;
                break;
            default:
                liveFolderInfo.icon =
                        launcher.getResources().getDrawable(R.drawable.ic_launcher_folder);
        }
    }

    /**
     * Finds the user folder defined by the specified id.
     *
     * @param id The id of the folder to look for.
     *
     * @return A UserFolderInfo if the folder exists or null otherwise.
     */
    FolderInfo findFolderById(long id) {
        return mFolders.get(id);
    }

    void addFolder(FolderInfo info) {
        mFolders.put(info.id, info);
    }

    /**
     * Return an existing UserFolderInfo object if we have encountered this ID previously, or make a
     * new one.
     */
    private UserFolderInfo findOrMakeUserFolder(HashMap<Long, FolderInfo> folders, long id) {
        // See if a placeholder was created for us already
        FolderInfo folderInfo = folders.get(id);
        if (folderInfo == null || !(folderInfo instanceof UserFolderInfo)) {
            // No placeholder -- create a new instance
            folderInfo = new UserFolderInfo();
            folders.put(id, folderInfo);
        }
        return (UserFolderInfo) folderInfo;
    }

    /**
     * Return an existing UserFolderInfo object if we have encountered this ID previously, or make a
     * new one.
     */
    private LiveFolderInfo findOrMakeLiveFolder(HashMap<Long, FolderInfo> folders, long id) {
        // See if a placeholder was created for us already
        FolderInfo folderInfo = folders.get(id);
        if (folderInfo == null || !(folderInfo instanceof LiveFolderInfo)) {
            // No placeholder -- create a new instance
            folderInfo = new LiveFolderInfo();
            folders.put(id, folderInfo);
        }
        return (LiveFolderInfo) folderInfo;
    }

    /**
     * Remove the callback for the cached drawables or we leak the previous
     * Home screen on orientation change.
     */
    void unbind() {
        // Interrupt the applications loader before setting the adapter to null
        stopAndWaitForApplicationsLoader();
        mApplicationsAdapter = null;
        unbindAppDrawables(mApplications);
        unbindDrawables(mDesktopItems);
        unbindAppWidgetHostViews(mDesktopAppWidgets);
        unbindCachedIconDrawables();
    }

    /**
     * Remove the callback for the cached drawables or we leak the previous
     * Home screen on orientation change.
     */
    private void unbindDrawables(ArrayList<ItemInfo> desktopItems) {
        if (desktopItems != null) {
            final int count = desktopItems.size();
            for (int i = 0; i < count; i++) {
                ItemInfo item = desktopItems.get(i);
                switch (item.itemType) {
                case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    ((ApplicationInfo)item).icon.setCallback(null);
                    break;
                }
            }
        }
    }

    /**
     * Remove the callback for the cached drawables or we leak the previous
     * Home screen on orientation change.
     */
    private void unbindAppDrawables(ArrayList<ApplicationInfo> applications) {
        if (applications != null) {
            final int count = applications.size();
            for (int i = 0; i < count; i++) {
                applications.get(i).icon.setCallback(null);
            }
        }
    }

    /**
     * Remove any {@link LauncherAppWidgetHostView} references in our widgets.
     */
    private void unbindAppWidgetHostViews(ArrayList<LauncherAppWidgetInfo> appWidgets) {
        if (appWidgets != null) {
            final int count = appWidgets.size();
            for (int i = 0; i < count; i++) {
                LauncherAppWidgetInfo launcherInfo = appWidgets.get(i);
                launcherInfo.hostView = null;
            }
        }
    }

    /**
     * Remove the callback for the cached drawables or we leak the previous
     * Home screen on orientation change.
     */
    private void unbindCachedIconDrawables() {
        for (ApplicationInfo appInfo : mAppInfoCache.values()) {
            appInfo.icon.setCallback(null);
        }
    }

    /**
     * Fills in the occupied structure with all of the shortcuts, apps, folders and widgets in
     * the model.
     */
    void findAllOccupiedCells(boolean[][] occupied, int countX, int countY, int screen) {
        final ArrayList<ItemInfo> desktopItems = mDesktopItems;
        if (desktopItems != null) {
            final int count = desktopItems.size();
            for (int i = 0; i < count; i++) {
                //ADW: Don't load items outer current columns/rows limits
                if((desktopItems.get(i).cellX+(desktopItems.get(i).spanX-1))<mDesktopColumns &&
                		(desktopItems.get(i).cellY+(desktopItems.get(i).spanY-1))<mDesktopRows)
                addOccupiedCells(occupied, screen, desktopItems.get(i));
            }
        }

        final ArrayList<LauncherAppWidgetInfo> desktopAppWidgets = mDesktopAppWidgets;
        if (desktopAppWidgets != null) {
            final int count = desktopAppWidgets.size();
            for (int i = 0; i < count; i++) {
                addOccupiedCells(occupied, screen, desktopAppWidgets.get(i));
            }
        }
    }

    /**
     * Add the footprint of the specified item to the occupied array
     */
    private void addOccupiedCells(boolean[][] occupied, int screen,
            ItemInfo item) {
        if (item.screen == screen) {
            for (int xx = item.cellX; xx < item.cellX + item.spanX; xx++) {
                for (int yy = item.cellY; yy < item.cellY + item.spanY; yy++) {
                    if(xx<mDesktopColumns && yy<mDesktopRows)
                    	occupied[xx][yy] = true;
                }
            }
        }
    }

    /**
     * @return The current list of applications
     */
    ApplicationsAdapter getApplicationsAdapter() {
        return mApplicationsAdapter;
    }

    /**
     * Add an item to the desktop
     * @param info
     */
    void addDesktopItem(ItemInfo info) {
        // TODO: write to DB; also check that folder has been added to folders list
        mDesktopItems.add(info);
    }

    /**
     * Remove an item from the desktop
     * @param info
     */
    void removeDesktopItem(ItemInfo info) {
        // TODO: write to DB; figure out if we should remove folder from folders list
        mDesktopItems.remove(info);
    }

    /**
     * Add a widget to the desktop
     */
    void addDesktopAppWidget(LauncherAppWidgetInfo info) {
        mDesktopAppWidgets.add(info);
    }

    /**
     * Remove a widget from the desktop
     */
    void removeDesktopAppWidget(LauncherAppWidgetInfo info) {
        mDesktopAppWidgets.remove(info);
    }

    /**
     * Make an ApplicationInfo object for an application
     */
    private static ApplicationInfo getApplicationInfo(PackageManager manager, Intent intent,
                                                      Context context) {
        //ADW: Changed the check to avoid bypassing SDcard apps in froyo
    	ComponentName componentName = intent.getComponent();
        if (componentName == null) {
            return null;
        }

        final ResolveInfo resolveInfo = manager.resolveActivity(intent, 0);

        final ApplicationInfo info = new ApplicationInfo();
        if(resolveInfo!=null){
	        final ActivityInfo activityInfo = resolveInfo.activityInfo;

	        info.icon = getIcon(manager, context, activityInfo);

	        if (info.title == null || info.title.length() == 0) {
	            info.title = activityInfo.loadLabel(manager);
	        }
	        if (info.title == null) {
	            info.title = "";
	        }
        }else{
        	//ADW: add default icon for apps on SD
        	info.icon=manager.getDefaultActivityIcon();
        }
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
        return info;
    }

    /**
     * Make an ApplicationInfo object for a shortcut
     */
    private ApplicationInfo getApplicationInfoShortcut(Cursor c, Context context,
            int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, int iconIndex) {

        final ApplicationInfo info = new ApplicationInfo();
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;

        int iconType = c.getInt(iconTypeIndex);
        switch (iconType) {
            case LauncherSettings.Favorites.ICON_TYPE_RESOURCE:
                String packageName = c.getString(iconPackageIndex);
                String resourceName = c.getString(iconResourceIndex);
                PackageManager packageManager = context.getPackageManager();
                try {
                    Resources resources = packageManager.getResourcesForApplication(packageName);
                    final int id = resources.getIdentifier(resourceName, null, null);
                    info.icon = Utilities.createIconThumbnail(resources.getDrawable(id), context);
                } catch (Exception e) {
                    info.icon = packageManager.getDefaultActivityIcon();
                }
                info.iconResource = new Intent.ShortcutIconResource();
                info.iconResource.packageName = packageName;
                info.iconResource.resourceName = resourceName;
                info.customIcon = false;
                break;
            case LauncherSettings.Favorites.ICON_TYPE_BITMAP:
                byte[] data = c.getBlob(iconIndex);
                try {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    info.icon = new FastBitmapDrawable(
                            Utilities.createBitmapThumbnail(bitmap, context));
                } catch (Exception e) {
                    packageManager = context.getPackageManager();
                    info.icon = packageManager.getDefaultActivityIcon();
                }
                info.filtered = true;
                info.customIcon = true;
                break;
            default:
                info.icon = context.getPackageManager().getDefaultActivityIcon();
                info.customIcon = false;
                break;
        }
        return info;
    }

    /**
     * Remove an item from the in-memory representation of a user folder. Does not change the DB.
     */
    void removeUserFolderItem(UserFolderInfo folder, ItemInfo info) {
        //noinspection SuspiciousMethodCalls
        folder.contents.remove(info);
    }

    /**
     * Removes a UserFolder from the in-memory list of folders. Does not change the DB.
     * @param userFolderInfo
     */
    void removeUserFolder(UserFolderInfo userFolderInfo) {
        mFolders.remove(userFolderInfo.id);
    }

    /**
     * Adds an item to the DB if it was not created previously, or move it to a new
     * <container, screen, cellX, cellY>
     */
    static void addOrMoveItemInDatabase(Context context, ItemInfo item, long container,
            int screen, int cellX, int cellY) {
        if (item.container == ItemInfo.NO_ID) {
            // From all apps
            addItemToDatabase(context, item, container, screen, cellX, cellY, false);
        } else {
            // From somewhere else
            moveItemInDatabase(context, item, container, screen, cellX, cellY);
        }
    }

    /**
     * Move an item in the DB to a new <container, screen, cellX, cellY>
     */
    static void moveItemInDatabase(Context context, ItemInfo item, long container, int screen,
            int cellX, int cellY) {
        item.container = container;
        item.screen = screen;
        item.cellX = cellX;
        item.cellY = cellY;

        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();

        values.put(LauncherSettings.Favorites.CONTAINER, item.container);
        values.put(LauncherSettings.Favorites.CELLX, item.cellX);
        values.put(LauncherSettings.Favorites.CELLY, item.cellY);
        values.put(LauncherSettings.Favorites.SCREEN, item.screen);

        cr.update(LauncherSettings.Favorites.getContentUri(item.id, false), values, null, null);
    }

    /**
     * Returns true if the shortcuts already exists in the database.
     * we identify a shortcut by its title and intent.
     */
    static boolean shortcutExists(Context context, String title, Intent intent) {
        final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI,
            new String[] { "title", "intent" }, "title=? and intent=?",
            new String[] { title, intent.toUri(0) }, null);
        boolean result = false;
        try {
            result = c.moveToFirst();
        } finally {
            c.close();
        }
        return result;
    }

    FolderInfo getFolderById(Context context, long id) {
        final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, null,
                "_id=? and (itemType=? or itemType=?)",
                new String[] { String.valueOf(id),
                        String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER),
                        String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER) }, null);

        try {
            if (c.moveToFirst()) {
                final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
                final int titleIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
                final int containerIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
                final int screenIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
                final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);

                FolderInfo folderInfo = null;
                switch (c.getInt(itemTypeIndex)) {
                    case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
                        folderInfo = findOrMakeUserFolder(mFolders, id);
                        break;
                    case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
                        folderInfo = findOrMakeLiveFolder(mFolders, id);
                        break;
                }

                folderInfo.title = c.getString(titleIndex);
                folderInfo.id = id;
                folderInfo.container = c.getInt(containerIndex);
                folderInfo.screen = c.getInt(screenIndex);
                folderInfo.cellX = c.getInt(cellXIndex);
                folderInfo.cellY = c.getInt(cellYIndex);

                return folderInfo;
            }
        } finally {
            c.close();
        }

        return null;
    }

    /**
     * Add an item to the database in a specified container. Sets the container, screen, cellX and
     * cellY fields of the item. Also assigns an ID to the item.
     */
    static void addItemToDatabase(Context context, ItemInfo item, long container,
            int screen, int cellX, int cellY, boolean notify) {
        item.container = container;
        item.screen = screen;
        item.cellX = cellX;
        item.cellY = cellY;

        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();

        item.onAddToDatabase(values);

        Uri result = cr.insert(notify ? LauncherSettings.Favorites.CONTENT_URI :
                LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, values);

        if (result != null) {
            item.id = Integer.parseInt(result.getPathSegments().get(1));
        }
    }

    /**
     * Update an item to the database in a specified container.
     */
    static void updateItemInDatabase(Context context, ItemInfo item) {
        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();

        item.onAddToDatabase(values);

        cr.update(LauncherSettings.Favorites.getContentUri(item.id, false), values, null, null);
    }

    /**
     * Removes the specified item from the database
     * @param context
     * @param item
     */
    static void deleteItemFromDatabase(Context context, ItemInfo item) {
        final ContentResolver cr = context.getContentResolver();

        cr.delete(LauncherSettings.Favorites.getContentUri(item.id, false), null, null);
    }


    /**
     * Remove the contents of the specified folder from the database
     */
    static void deleteUserFolderContentsFromDatabase(Context context, UserFolderInfo info) {
        final ContentResolver cr = context.getContentResolver();

        cr.delete(LauncherSettings.Favorites.getContentUri(info.id, false), null, null);
        cr.delete(LauncherSettings.Favorites.CONTENT_URI,
                LauncherSettings.Favorites.CONTAINER + "=" + info.id, null);
    }

    /**
     * Get an the icon for an activity
     * Accounts for theme and icon shading
     */
    static Drawable getIcon(PackageManager manager, Context context, ActivityInfo activityInfo) {
        String themePackage=AlmostNexusSettingsHelper.getThemePackageName(context, Launcher.THEME_DEFAULT);
        Drawable icon = null;
        if(themePackage.equals(Launcher.THEME_DEFAULT)){
            icon = Utilities.createIconThumbnail(activityInfo.loadIcon(manager), context);
        }else{
            // get from theme
            Resources themeResources = null;
            if(AlmostNexusSettingsHelper.getThemeIcons(context)){
                activityInfo.name=activityInfo.name.toLowerCase().replace(".", "_");
                try {
                    themeResources = manager.getResourcesForApplication(themePackage);
                } catch (NameNotFoundException e) {
                    //e.printStackTrace();
                }
                if(themeResources!=null){
                    int resource_id = themeResources.getIdentifier(activityInfo.name, "drawable", themePackage);
                    if(resource_id!=0){
                        icon=themeResources.getDrawable(resource_id);
                    }

                    // use IconShader
                    if(icon==null){
                        if (compiledIconShaderName==null ||
                            compiledIconShaderName.compareTo(themePackage)!=0){
                            compiledIconShader = null;
                            resource_id = themeResources.getIdentifier("shader", "xml", themePackage);
                            if(resource_id!=0){
                                XmlResourceParser xpp = themeResources.getXml(resource_id);
                                compiledIconShader = IconShader.parseXml(xpp);
                            }
                        }

                        if(compiledIconShader!=null){
                            icon = Utilities.createIconThumbnail(activityInfo.loadIcon(manager), context);
                            try {
                                icon = IconShader.processIcon(icon, compiledIconShader);
                            } catch (Exception e) {}
                        }
                    }
                }
            }

            if(icon==null){
                icon = Utilities.createIconThumbnail(activityInfo.loadIcon(manager), context);
            }else{
                icon = Utilities.createIconThumbnail(icon, context);
            }
        }
        return icon;
    }
    /**
     * Resize an item in the DB to a new <container, screen, cellX, cellY>
     */
    static void resizeItemInDatabase(Context context, ItemInfo item, long container, int screen,
            int cellX, int cellY, int spanX, int spanY) {
        item.container = container;
        item.screen = screen;
        item.cellX = cellX;
        item.cellY = cellY;
        item.spanX = spanX;
        item.spanY = spanY;

        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();

        values.put(LauncherSettings.Favorites.CONTAINER, item.container);
        values.put(LauncherSettings.Favorites.CELLX, item.cellX);
        values.put(LauncherSettings.Favorites.CELLY, item.cellY);
        values.put(LauncherSettings.Favorites.SPANX, item.spanX);
        values.put(LauncherSettings.Favorites.SPANY, item.spanY);
        values.put(LauncherSettings.Favorites.SCREEN, item.screen);

        cr.update(LauncherSettings.Favorites.getContentUri(item.id, false), values, null, null);
    }
    boolean ocuppiedArea(int screen,int id,Rect rect){
        final ArrayList<ItemInfo>desktopItems=mDesktopItems;
        int count =desktopItems.size();
        Rect r=new Rect();
        for (int i=0;i<count;i++){
            if(desktopItems.get(i).screen==screen){
                ItemInfo it=desktopItems.get(i);
                r.set(it.cellX,it.cellY,it.cellX+it.spanX,it.cellY+it.spanY);
                if(rect.intersect(r)){
                    return true;
                }
            }
        }
        final ArrayList<LauncherAppWidgetInfo>desktopWidgets=mDesktopAppWidgets;
        count=desktopWidgets.size();
        for (int i=0;i<count;i++){
            if(desktopWidgets.get(i).screen==screen){
                LauncherAppWidgetInfo it=desktopWidgets.get(i);
                if(id!=it.appWidgetId){
                    r.set(it.cellX,it.cellY,it.cellX+it.spanX,it.cellY+it.spanY);
                    if(rect.intersect(r)){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    synchronized void updateCounterForPackage(Launcher launcher, String packageName, int counter) {
        if (mApplicationsLoader != null && mApplicationsLoader.isRunning()) {
            startApplicationsLoaderLocked(launcher, false);
            return;
        }
        boolean changed=false;
        if (packageName != null && packageName.length() > 0 && mApplicationsAdapter!=null) {
            final int count=mApplications.size();
            for (int i = 0; i < count; i++) {
                final ApplicationInfo info = mApplications.get(i);
                final Intent intent = info.intent;
                final ComponentName name = intent.getComponent();
                if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION &&
                        packageName.equals(name.getPackageName()) &&
                        info.counter!=counter) {
                    info.counter=counter;
                    changed=true;
                }
            }
        }
        if(changed)mApplicationsAdapter.notifyDataSetChanged();
    }
    void updateCounterDesktopItem(ItemInfo info, int counter) {
        // TODO: write to DB; figure out if we should remove folder from folders list
        if(mDesktopItems.get(mDesktopItems.indexOf(info)) instanceof ApplicationInfo){
            ((ApplicationInfo)mDesktopItems.get(mDesktopItems.indexOf(info))).counter=counter;
        }
    }
}
