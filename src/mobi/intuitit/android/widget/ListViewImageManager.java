package mobi.intuitit.android.widget;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

public class ListViewImageManager {

	private static final String TAG = "ListViewImageManager";

	private static final boolean LOGD = false;

	private static ListViewImageManager instance;

	public static ListViewImageManager getInstance() {
		if (instance == null)
			instance = new ListViewImageManager();
		return instance;
	}

	private final HashMap<String, SoftReference<Drawable>> mCacheForImageByUri = new HashMap<String, SoftReference<Drawable>>();
	private final HashMap<Integer, ArrayList<String>> mWidgetCacheUsageByUri = new HashMap<Integer, ArrayList<String>>();

	private final HashMap<Integer, SoftReference<Drawable>> mCacheForImageById = new HashMap<Integer, SoftReference<Drawable>>();
	private final HashMap<Integer, ArrayList<Integer>> mWidgetCacheUsageById = new HashMap<Integer, ArrayList<Integer>>();

	public Drawable getImageFromUri(Context mContext, int widgetId, String imgUri) {
		Drawable d = null;
		if (mCacheForImageByUri.containsKey(imgUri) && mCacheForImageByUri.get(imgUri) != null) {
			SoftReference<Drawable> ref = mCacheForImageByUri.get(imgUri);
			if (ref != null) {
				d = ref.get();
			}
		}

		if (LOGD)
			if (d != null)
				Log.d(TAG, "image URI restored (width = " + d.getMinimumWidth() + " / weight = " + d.getMinimumHeight()
						+ ")");

		if (d == null) {
			Uri mUri = Uri.parse(imgUri);
			String scheme = mUri.getScheme();
			if (ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme)) {
				// try {
				// // Load drawable through Resources, to get the source density
				// information
				// ContentResolver.OpenResourceIdResult r =
				// mContext.getContentResolver().getResourceId(mUri);
				// d = r.r.getDrawable(r.id);
				// } catch (Exception e) {
				// Log.w("ImageView", "Unable to open content: " + mUri, e);
				// }
				Log.w("ImageView", "Unable to open content: " + mUri);
			} else if (ContentResolver.SCHEME_CONTENT.equals(scheme) || ContentResolver.SCHEME_FILE.equals(scheme)) {
				try {
					d = Drawable.createFromStream(mContext.getContentResolver().openInputStream(mUri), null);
				} catch (Exception e) {
					Log.w("ImageView", "Unable to open content: " + mUri, e);
				}
			} else {
				d = Drawable.createFromPath(mUri.toString());
			}

			if (LOGD)
				Log.d(TAG, "image URI decoded (width = " + d.getMinimumWidth() + " / weight = " + d.getMinimumHeight()
						+ ")");
			mCacheForImageByUri.put(imgUri, new SoftReference<Drawable>(d));

			// store image key usage
			ArrayList<String> list = mWidgetCacheUsageByUri.get(widgetId);
			if (list == null)
				list = new ArrayList<String>();
			list.add(imgUri);
			mWidgetCacheUsageByUri.put(widgetId, list);

		}
		return d;
	}

	public Drawable getImageFromId(Context ctx, int widgetId, int imgId) {
		Drawable drawable = null;
		if (mCacheForImageById.containsKey(imgId) && mCacheForImageById.get(imgId) != null) {
			SoftReference<Drawable> ref = mCacheForImageById.get(imgId);
			if (ref != null) {
				drawable = ref.get();
			} else {
				if (LOGD)
					Log.d(TAG, "image ID missing !!!!!!!!!!");
			}
		}

		if (LOGD)
			if (drawable != null)
				Log.d(TAG, "image ID restored");

		if (drawable == null) {
			if (LOGD)
				Log.d(TAG, "image ID decoded");

			InputStream rawResource = ctx.getResources().openRawResource( imgId);
			try
			{
				drawable = Drawable.createFromStream(rawResource, ctx.getResources().getResourceName(imgId));
			}
			finally
			{
				try {
					rawResource.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			mCacheForImageById.put(imgId, new SoftReference<Drawable>(drawable));

			// store image key usage
			ArrayList<Integer> list = mWidgetCacheUsageById.get(widgetId);
			if (list == null)
				list = new ArrayList<Integer>();
			list.add(imgId);
			mWidgetCacheUsageById.put(widgetId, list);

		}
		return drawable;
	}

	public void unbindDrawables() {

		for (Entry<Integer, SoftReference<Drawable>> drawableEntry : mCacheForImageById.entrySet()) {
			if ((drawableEntry != null) && (drawableEntry.getValue() != null)
					&& (drawableEntry.getValue().get() != null))
				drawableEntry.getValue().get().setCallback(null);
		}

		for (Entry<String, SoftReference<Drawable>> drawableEntry : mCacheForImageByUri.entrySet()) {
			if ((drawableEntry != null) && (drawableEntry.getValue() != null)
					&& (drawableEntry.getValue().get() != null))
				drawableEntry.getValue().get().setCallback(null);
		}
	}

	public void clearCache() {
		mCacheForImageById.clear();
		mCacheForImageByUri.clear();
	}

	public String clearCacheForWidget(Context ctx, int widgetId) {
		Drawable drawable = null;
		ArrayList<String> listByUri = mWidgetCacheUsageByUri.get(widgetId);
		if (listByUri != null) {
			for (String imgUri : listByUri) {
				if (mCacheForImageByUri.containsKey(imgUri) && mCacheForImageByUri.get(imgUri) != null) {
					SoftReference<Drawable> ref = mCacheForImageByUri.get(imgUri);
					if (ref != null) {
						drawable = ref.get();
						if (drawable != null)
							drawable.setCallback(null);
					}
					mCacheForImageByUri.remove(imgUri);
					Log.d(TAG, "image URI removed from cache : " + imgUri);
				}
			}
		}
		mWidgetCacheUsageByUri.remove(widgetId);

		ArrayList<Integer> listById = mWidgetCacheUsageById.get(widgetId);
		if (listById != null) {
			for (Integer imgId : listById) {
				if (mCacheForImageById.containsKey(imgId) && mCacheForImageById.get(imgId) != null) {
					SoftReference<Drawable> ref = mCacheForImageById.get(imgId);
					if (ref != null) {
						drawable = ref.get();
						if (drawable != null)
							drawable.setCallback(null);
					}
					mCacheForImageById.remove(imgId);
					Log.d(TAG, "image ID removed from cache : " + imgId);
				}
			}
		}
		mWidgetCacheUsageById.remove(widgetId);

		return null;
	}

}