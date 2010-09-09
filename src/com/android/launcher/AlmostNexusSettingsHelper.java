package com.android.launcher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.widget.ScrollView;
import android.widget.TextView;

public final class AlmostNexusSettingsHelper {
	public static final int ORIENTATION_SENSOR=1;
	public static final int ORIENTATION_PORTRAIT=2;
	public static final int ORIENTATION_LANDSCAPE=3;
	
	public static final int CACHE_LOW=1;
	public static final int CACHE_AUTO=2;
	public static final int CACHE_DISABLED=3;
	
	private static final String ALMOSTNEXUS_PREFERENCES = "launcher.preferences.almostnexus";
	private static final String[] restart_keys={"drawerNew","uiHideLabels","highlights_color",
		"highlights_color_focus","uiNewSelectors","desktopRows","desktopColumns","autosizeIcons","uiDesktopIndicatorType",
		"screenCache","uiDesktopIndicator","themePackageName","themeIcons"};

	public static boolean needsRestart(String key){
		for(int i=0;i<restart_keys.length;i++){
			if(restart_keys[i].equals(key))
				return true;
		}
		return false;
	}
	public static int getDesktopScreens(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int screens = sp.getInt("desktopScreens", context.getResources().getInteger(R.integer.config_desktopScreens))+1;
		return screens;
	}
	public static int getDefaultScreen(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int def_screen = sp.getInt("defaultScreen", context.getResources().getInteger(R.integer.config_defaultScreen));
		return def_screen;
	}
	public static int getPageHorizontalMargin(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = sp.getInt("pageHorizontalMargin", context.getResources().getInteger(R.integer.config_pageHorizontalMargin));
		return newD;
	}
	public static int getColumnsPortrait(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int screens = sp.getInt("drawerColumnsPortrait", context.getResources().getInteger(R.integer.config_drawerColumnsPortrait))+1;
		return screens;
	}
	public static int getRowsPortrait(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int screens = sp.getInt("drawerRowsPortrait", context.getResources().getInteger(R.integer.config_drawerRowsPortrait))+1;
		return screens;
	}
	public static int getColumnsLandscape(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int screens = sp.getInt("drawerColumnsLandscape", context.getResources().getInteger(R.integer.config_drawerColumnsLandscape))+1;
		return screens;
	}
	public static int getRowsLandscape(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int screens = sp.getInt("drawerRowsLandscape", context.getResources().getInteger(R.integer.config_drawerRowsLandscape))+1;
		return screens;
	}
	public static boolean getDrawerAnimated(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean animated = sp.getBoolean("drawerAnimated", context.getResources().getBoolean(R.bool.config_drawerAnimated));
		return animated;
	}
	public static boolean getDrawerNew(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("drawerNew", context.getResources().getBoolean(R.bool.config_drawerNew));
		return newD;
	}
	public static boolean getHideStatusbar(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("hideStatusbar", context.getResources().getBoolean(R.bool.config_hideStatusbar));
		return newD;
	}
	public static boolean getNewPreviews(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("previewsNew", context.getResources().getBoolean(R.bool.config_previewsNew));
		return newD;
	}
	public static boolean getFullScreenPreviews(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("previewsFullScreen", context.getResources().getBoolean(R.bool.config_previewsFullScreen));
		return newD;
	}
	public static int getHomeBinding(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = Integer.valueOf(sp.getString("homeBinding", context.getResources().getString(R.string.config_homeBinding)));
		return newD;
	}
	public static boolean getUIDots(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiDots", context.getResources().getBoolean(R.bool.config_uiDots));
		return newD;
	}
	public static boolean getUIDockbar(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiDockbar", context.getResources().getBoolean(R.bool.config_uiDockbar));
		return newD;
	}
	public static boolean getUICloseDockbar(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiCloseDockbar", context.getResources().getBoolean(R.bool.config_uiCloseDockbar));
		return newD;
	}
	public static boolean getUICloseFolder(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiCloseFolder", context.getResources().getBoolean(R.bool.config_uiCloseFolder));
		return newD;
	}
	public static boolean getUILAB(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiLAB", context.getResources().getBoolean(R.bool.config_uiLAB));
		return newD;
	}
	public static boolean getUIRAB(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiRAB", context.getResources().getBoolean(R.bool.config_uiRAB));
		return newD;
	}
	public static boolean getUITint(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiTint", context.getResources().getBoolean(R.bool.config_uiTint));
		return newD;
	}
	public static int getDesktopSpeed(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = sp.getInt("desktopSpeed", context.getResources().getInteger(R.integer.config_desktopSpeed));
		return newD;
	}
	public static int getDesktopBounce(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = sp.getInt("desktopBounce", context.getResources().getInteger(R.integer.config_desktopBounce));
		return newD;
	}
	public static boolean getUIAppsBg(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiAppsBg", context.getResources().getBoolean(R.bool.config_uiAppsBg));
		return newD;
	}
	public static boolean getUIABBg(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiABBg", context.getResources().getBoolean(R.bool.config_uiABBg));
		return newD;
	}
	public static int getZoomSpeed(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = sp.getInt("zoomSpeed", context.getResources().getInteger(R.integer.config_zoomSpeed))+300;
		return newD;
	}
	public static float getuiScaleAB(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = sp.getInt("uiScaleAB", context.getResources().getInteger(R.integer.config_uiScaleAB))+1;
		float scale=(float)newD/10f;
		return scale;
	}
	public static boolean getUIHideLabels(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiHideLabels", context.getResources().getBoolean(R.bool.config_uiHideLabels));
		return newD;
	}
	public static boolean getWallpaperHack(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("wallpaperHack", context.getResources().getBoolean(R.bool.config_wallpaperHack));
		return newD;
	}
	public static int getHighlightsColor(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = sp.getInt("highlights_color", context.getResources().getInteger(R.integer.config_highlights_color));
		return newD;
	}
	public static int getHighlightsColorFocus(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = sp.getInt("highlights_color_focus", context.getResources().getInteger(R.integer.config_highlights_color_focus));
		return newD;
	}
	public static boolean getUINewSelectors(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiNewSelectors", context.getResources().getBoolean(R.bool.config_new_selectors));
		return newD;
	}
	public static int getDrawerColor(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = sp.getInt("drawer_color", context.getResources().getInteger(R.integer.config_drawer_color));
		return newD;
	}
	public static int getDesktopColumns(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int screens = sp.getInt("desktopColumns", context.getResources().getInteger(R.integer.config_desktopColumns))+3;
		return screens;
	}
	public static int getDesktopRows(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int screens = sp.getInt("desktopRows", context.getResources().getInteger(R.integer.config_desktopRows))+3;
		return screens;
	}
	public static boolean getUIAB2(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiAB2", context.getResources().getBoolean(R.bool.config_uiAB2));
		return newD;
	}
	public static boolean getAutosizeIcons(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("autosizeIcons", context.getResources().getBoolean(R.bool.config_autosizeIcons));
		return newD;
	}
	public static boolean getUIScrollableWidgets(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiScrollableWidgets", context.getResources().getBoolean(R.bool.config_uiScrollableWidgets));
		return newD;
	}
	public static void setUIScrollableWidgets(Context context, boolean value) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
	    SharedPreferences.Editor editor = sp.edit();
		editor.putBoolean("uiScrollableWidgets", value);
	    editor.commit();
	}
	public static boolean getDrawerLabels(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("drawerLabels", context.getResources().getBoolean(R.bool.config_drawerLabels));
		return newD;
	}
	public static boolean getFadeDrawerLabels(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("fadeDrawerLabels", context.getResources().getBoolean(R.bool.config_fadeDrawerLabels));
		return newD;
	}
	public static int getScreenCache(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = Integer.valueOf(sp.getString("screenCache", context.getResources().getString(R.string.config_screenCache)));
		return newD;
	}
	public static boolean getDesktopIndicator(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiDesktopIndicator", context.getResources().getBoolean(R.bool.config_desktop_indicator));
		return newD;
	}
	public static boolean getDesktopIndicatorAutohide(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("uiDesktopIndicatorAutohide", context.getResources().getBoolean(R.bool.config_desktop_indicator_autohide));
		return newD;
	}
	public static int getDesktopIndicatorType(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = Integer.valueOf(sp.getString("uiDesktopIndicatorType", context.getResources().getString(R.string.config_desktop_indicator_type)));
		return newD;
	}
	public static boolean getSystemPersistent(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("systemPersistent", context.getResources().getBoolean(R.bool.config_system_persistent));
		return newD;
	}
	public static String getSwipeDownAppToLaunchPackageName(Context context)
	{
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		return sp.getString("swipeDownAppToLaunchPackageName", "");
	}
	public static String getSwipeUpAppToLaunchPackageName(Context context)
	{
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		return sp.getString("swipeUpAppToLaunchPackageName", "");
	}
	public static String getHomeBindingAppToLaunchPackageName(Context context)
	{
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		return sp.getString("homeBindingAppToLaunchPackageName", "");
	}
	public static String getSwipeDownAppToLaunchName(Context context)
	{
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		return sp.getString("swipeDownAppToLaunchName", "");
	}
	public static String getSwipeUpAppToLaunchName(Context context)
	{
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		return sp.getString("swipeUpAppToLaunchName", "");
	}
	public static String getHomeBindingAppToLaunchName(Context context)
	{
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		return sp.getString("homeBindingAppToLaunchName", "");
	}
	public static void setSwipeDownAppToLaunch(Context context, ApplicationInfo info)
	{
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
	    SharedPreferences.Editor editor = sp.edit();
	    editor.putString("swipeDownAppToLaunchPackageName", info.intent.getComponent().getPackageName());
	    editor.putString("swipeDownAppToLaunchName", info.intent.getComponent().getClassName());
	    editor.commit();
	}
	public static void setSwipeUpAppToLaunch(Context context, ApplicationInfo info)
	{
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
	    SharedPreferences.Editor editor = sp.edit();
	    editor.putString("swipeUpAppToLaunchPackageName", info.intent.getComponent().getPackageName());
	    editor.putString("swipeUpAppToLaunchName", info.intent.getComponent().getClassName());
	    editor.commit();
	}
	public static void setHomeBindingAppToLaunch(Context context, ApplicationInfo info)
	{
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
	    SharedPreferences.Editor editor = sp.edit();
	    editor.putString("homeBindingAppToLaunchPackageName", info.intent.getComponent().getPackageName());
	    editor.putString("homeBindingAppToLaunchName", info.intent.getComponent().getClassName());
	    editor.commit();
	}
	public static int getSwipeDownActions(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = Integer.valueOf(sp.getString("swipedownActions", context.getResources().getString(R.string.config_swipedown_actions)));
		return newD;
	}
	public static int getSwipeUpActions(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = Integer.valueOf(sp.getString("swipeupActions", context.getResources().getString(R.string.config_swipeup_actions)));
		return newD;
	}
	public static String getThemePackageName(Context context, String default_theme)
	{
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		return sp.getString("themePackageName", default_theme);
	}
	public static void setThemePackageName(Context context, String packageName)
	{
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
	    SharedPreferences.Editor editor = sp.edit();
		editor.putString("themePackageName", packageName);
	    editor.commit();
	}
	public static boolean getThemeIcons(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("themeIcons", true);
		return newD;
	}
	public static int getDesktopOrientation(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = Integer.valueOf(sp.getString("homeOrientation", context.getResources().getString(R.string.config_orientation_default)));
		return newD;
	}
	public static boolean getWallpaperScrolling(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		boolean newD = sp.getBoolean("wallpaper_scrolling", true);
		return newD;
	}
	public static void setDesktopScreens(Context context,int screens) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
	    SharedPreferences.Editor editor = sp.edit();
		editor.putInt("desktopScreens", screens-1);
	    editor.commit();
	}
	public static void setDefaultScreen(Context context,int screens) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
	    SharedPreferences.Editor editor = sp.edit();
		editor.putInt("defaultScreen", screens);
	    editor.commit();
	}
	
	public static int getCurrentAppCatalog(Context context) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
		int newD = sp.getInt("currentAppCatalog", -1);
		return newD;
	}
	public static void setCurrentAppCatalog(Context context, int group) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
	    SharedPreferences.Editor editor = sp.edit();
		editor.putInt("currentAppCatalog", group);
	    editor.commit();
	}
	
	public static void setChangelogVersion(Context context,String version) {
		SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
	    SharedPreferences.Editor editor = sp.edit();
		editor.putString("changelogReadVersion", version);
	    editor.commit();
	}
	public static boolean shouldShowChangelog(Context context) {
		Boolean config=context.getResources().getBoolean(R.bool.config_nagScreen);
		if(config){
			SharedPreferences sp = context.getSharedPreferences(ALMOSTNEXUS_PREFERENCES, Context.MODE_PRIVATE);
			String readV = sp.getString("changelogReadVersion", "0");
			String actualV=context.getString(R.string.adw_version);
			boolean ret=!readV.equals(actualV);
			if(ret){
				//Once verified and showed, disable it ultill the next update
				setChangelogVersion(context, actualV);
			}
			return ret;
		}else{
			return false;
		}
	}
	/**
	 * Creates the "changes" dialog to be shown when updating ADW.
	 * @author adw
	 *
	 */
	public static class ChangelogDialogBuilder {
		public static AlertDialog create( Context context ) throws NameNotFoundException {

			String aboutTitle = String.format("%s Changelog", context.getString(R.string.adw_version));
			Spanned aboutText = Html.fromHtml(context.getString(R.string.adw_changelog, TextView.BufferType.SPANNABLE));
			
			// Set up the holder scrollview
			ScrollView mainView=new ScrollView(context);
			// Set up the TextView
			final TextView message = new TextView(context);
			mainView.addView(message);
			// We'll use a spannablestring to be able to make links clickable
			//final SpannableString s = new SpannableString(aboutText);

			// Set some padding
			message.setPadding(5, 5, 5, 5);
			// Set up the final string
			message.setText(aboutText);			

			return new AlertDialog.Builder(context).setTitle(aboutTitle).setCancelable(true).setIcon(R.drawable.ic_launcher_home).setPositiveButton(
				 context.getString(android.R.string.ok), null).setView(mainView).create();
		}
	}	
}
