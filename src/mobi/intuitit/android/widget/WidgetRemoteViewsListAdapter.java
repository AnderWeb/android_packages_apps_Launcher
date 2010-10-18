package mobi.intuitit.android.widget;

import mobi.intuitit.android.content.LauncherIntent;
import android.content.AsyncQueryHandler;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;

/**
 *
 * @author Florian Sundermann
 *
 */
public class WidgetRemoteViewsListAdapter extends ScrollableBaseAdapter {

    private BoundRemoteViews mRemoteViews = null;
    private final Context mContext;
    private Intent mIntent;
    private final MyQueryHandler mAsyncQuery;
    ComponentName mAppWidgetProvider;

    /**
     *
     * @param context
     *            remote context
     * @param c
     *            cursor for reading data
     * @param intent
     *            broadcast intent initiated the replacement, don't save it
     * @param appWidgetId
     * @param listViewId
     */
    public WidgetRemoteViewsListAdapter(Context context, Intent intent, ComponentName provider,
            int appWidgetId, int listViewId) throws IllegalArgumentException {
        super();

        mContext = context;
        mAppWidgetProvider = provider;
        mIntent = intent;

        mRemoteViews = (BoundRemoteViews)intent.getParcelableExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS);
        mAsyncQuery=new MyQueryHandler(mContext.getContentResolver());
        mHandler.post(mQueryDataRunnable);
    }

    public void updateFromIntent(Intent intent) {
    	if (intent.hasExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS)) {
    		if (mRemoteViews != null) {
    			mRemoteViews.dropCache();
    		}
    		mIntent = intent;
    		mRemoteViews = (BoundRemoteViews)intent.getParcelableExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_REMOTEVIEWS);
            mHandler.post(mQueryDataRunnable);
    	}
    }

    final Handler mHandler = new Handler();
	// Create runnable for posting
	final Runnable mQueryDataRunnable = new Runnable() {
		public void run() {
	        android.util.Log.d("LAUNCHER","API v2 START QUERY");
	        mAsyncQuery.startQuery(1, "cookie",
	                Uri.parse(mIntent.getStringExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI)) ,
	                mIntent.getStringArrayExtra(LauncherIntent.Extra.Scroll.EXTRA_PROJECTION),
	                mIntent.getStringExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION),
	                mIntent.getStringArrayExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION_ARGUMENTS),
	                mIntent.getStringExtra(LauncherIntent.Extra.Scroll.EXTRA_SORT_ORDER));

		}
	};

    @Override
    public synchronized void notifyToRegenerate() {
    	mHandler.post(mQueryDataRunnable);
    }

    @Override
    public int getCount() {
    	return mRemoteViews.getCursorCacheSize();
    }

    @Override
    public Object getItem(int position) {
    	mRemoteViews.moveCursor(position);
    	return mRemoteViews;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
    	mRemoteViews.moveCursor(position);
    	if (convertView == null)
    		convertView = mRemoteViews.apply(mContext, null);
    	else
    		mRemoteViews.reapplyBinding(convertView);
    	return convertView;
    }

	@Override
	public void dropCache(Context context) {
		dropCache();
	}

	public void dropCache() {
		mRemoteViews.dropCache();
	}
    /**
     * AsyncQueryHandler helper class to do async queries
     * instead of blocking the UI thread
     * (yeah, don't know why but the runnable was not avoiding
     * the UI lock
     * @author adw
     *
     */
    private class MyQueryHandler extends AsyncQueryHandler
    {
        public MyQueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
		protected void onQueryComplete(int token, Object cookie,
                Cursor cursor) {
            super.onQueryComplete(token, cookie, cursor);
            android.util.Log.d("LAUNCHER","API v2 QUERY COMPLETE");
            mRemoteViews.setBindingCursor(cursor, mContext);
            cursor.close();
            notifyDataSetInvalidated();
        }
    }
}
