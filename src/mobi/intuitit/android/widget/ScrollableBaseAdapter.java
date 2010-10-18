package mobi.intuitit.android.widget;

import android.content.Context;
import android.widget.BaseAdapter;

/*
 * Base Adapter class that brings a common interface for WidgetRemoteViewsListAdapter and
 * WidgetListAdapter.
 */
public abstract class ScrollableBaseAdapter extends BaseAdapter {

	/*
	 * Tell the adapter to regenerate the data cache
	 */
	public abstract void notifyToRegenerate();

	/*
	 * Tell the adapter to drop the cache
	 */
	public abstract void dropCache(Context context);

}
