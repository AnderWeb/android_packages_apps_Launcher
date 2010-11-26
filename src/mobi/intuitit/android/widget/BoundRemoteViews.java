package mobi.intuitit.android.widget;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;

public class BoundRemoteViews extends SimpleRemoteViews {

	class CursorCache {

		final ArrayList<HashMap<Action, Object>> mCache;
		final HashMap<Action, Object> mDefaults;

		public Object getValueOrDefault(int index, Action target) {
			HashMap<Action, Object> row = mCache.get(index);
			Object result = null;
			if (row.containsKey(target))
				 result = row.get(target);
			if (result == null)
				result = mDefaults.get(target);
			return result;
		}

		public CursorCache(Cursor cursor, Context context) {
			final int cacheSize = cursor != null ? cursor.getCount() : 0;

			mCache = new ArrayList<HashMap<Action,Object>>(cacheSize);
			mDefaults = new HashMap<Action, Object>();

			final ArrayList<Action> actions = BoundRemoteViews.this.mActions;

			for (int i = 0; i < actions.size(); i++) {
				Action act = actions.get(i);
				if (act instanceof BindingAction)
					mDefaults.put(act, ((BindingAction)act).getDefault(context));
				else if (act instanceof SetBoundOnClickIntent)
					mDefaults.put(act, null);
			}
			if (cursor != null) {
				cursor.moveToFirst();

				while(!cursor.isAfterLast()) {

					HashMap<Action, Object> row = new HashMap<Action, Object>();


					for (int i = 0; i < actions.size(); i++) {
						Action act = actions.get(i);
						if (act instanceof BindingAction)
							row.put(act, ((BindingAction)act).readValue(cursor, context));
						else if (act instanceof SetBoundOnClickIntent)
							row.put(act, ((SetBoundOnClickIntent)act).readValue(cursor));
					}

					mCache.add(row);
					cursor.moveToNext();
				}
			}
		}

		private void dropCacheRow(HashMap<Action,Object> row) {
			for(Action key : row.keySet()) {
				Object value = row.get(key);
				if (value instanceof Bitmap) {
					((Bitmap)value).recycle();
				}
			}
			row.clear();
		}

		public void clear() {
			for(HashMap<Action, Object> row : mCache) {
				dropCacheRow(row);
			}
			mCache.clear();
			dropCacheRow(mDefaults);
		}
	}


	protected class BindingAction extends SimpleRemoteViews.ReflectionAction
	{
		public static final int tag = 99;

		private int mCursorIndex;
		private int mDefaultResource;

		public BindingAction(int viewId, String methodName, int type, int cursorIndex, int defaultResource) {
			super(viewId, methodName, type);
			mCursorIndex = cursorIndex;
			mDefaultResource = defaultResource;
	    }

		public BindingAction(Parcel in) {
			super(in);
		}

		@Override
	    protected int getTag() {
	    	return tag;
	    }

		@Override
		protected void readValue(Parcel in) {
			mCursorIndex = in.readInt();
			mDefaultResource = in.readInt();
		}

		@Override
		protected void writeValue(Parcel out, int flags) {
			out.writeInt(mCursorIndex);
			out.writeInt(mDefaultResource);
		}

		@Override
		protected Object getValue(Context context) {
			return mCursor.getValueOrDefault(mCursorPos, this);
		}

		public Object readValue(Cursor cursor, Context context) {
			try
			{
				switch(this.type) {
					case STRING:
					case CHAR_SEQUENCE:
						return cursor.getString(mCursorIndex);
					case BYTE:
						return (byte)cursor.getInt(mCursorIndex);
					case SHORT:
						return (short)cursor.getInt(mCursorIndex);
					case INT:
						return cursor.getInt(mCursorIndex);
					case LONG:
						return cursor.getLong(mCursorIndex);
					case FLOAT:
						return cursor.getFloat(mCursorIndex);
					case DOUBLE:
						return cursor.getDouble(mCursorIndex);
					case CHAR:
						return cursor.getString(mCursorIndex).charAt(0);
					case URI:
						return Uri.parse(cursor.getString(mCursorIndex));
					case BITMAP:
						byte[] blob = cursor.getBlob(mCursorIndex);
		                return BitmapFactory.decodeByteArray(blob, 0, blob.length);
				}
			}
			catch(Exception e) {
				return null;
			}
			return null;
		}

		public Object getDefault(Context context) {
			try
			{
				switch(this.type) {
					case STRING:
					case CHAR_SEQUENCE:
						return context.getString(mDefaultResource);
					case BITMAP:
						return BitmapFactory.decodeResource(context.getResources(), mDefaultResource);
				}
			}
			catch(Exception e) {
				return null;
			}
			return null;
		}

		@Override
		public void apply(View root) {
			super.apply(root);
		}
	}

	protected class SetBoundOnClickIntent extends Action {
		private static final int TAG = 100;

		private final String mExtraName;
		private final int mExtraCursorIndex;
		private final int mViewId;
		private final PendingIntent mIntent;

        public SetBoundOnClickIntent(int id, PendingIntent intent,
        		String extraName, int extraCursorIndex) {
        	mViewId = id;
        	mIntent = intent;
        	mExtraName = extraName;
        	mExtraCursorIndex = extraCursorIndex;
        }

        public SetBoundOnClickIntent(Parcel parcel) {
        	mViewId = parcel.readInt();
        	mExtraName = parcel.readString();
        	mExtraCursorIndex = parcel.readInt();
        	mIntent = PendingIntent.CREATOR.createFromParcel(parcel);
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(TAG);
            dest.writeInt(mViewId);
            dest.writeString(mExtraName);
            dest.writeInt(mExtraCursorIndex);
            mIntent.writeToParcel(dest, 0 /* no flags */);
        }

        @Override
        public void apply(View root) {
            final View target = root.findViewById(mViewId);
            if (target != null && mIntent != null) {
                target.setOnClickListener(new BoundOnClickListener(mCursorPos));
            }
        }

        private class BoundOnClickListener implements OnClickListener {

        	private final int myCursorPos;

        	public BoundOnClickListener(int cursorPos) {
        		myCursorPos = cursorPos;
        	}

        	public void onClick(View v) {
                // Find target view location in screen coordinates and
                // fill into PendingIntent before sending.
          	    final int[] location = new int[2];
                v.getLocationOnScreen(location);
                Rect srcRect = new Rect();
                srcRect.left = location[0];
                srcRect.top = location[1];
                srcRect.right = srcRect.left + v.getWidth();
                srcRect.bottom = srcRect.top + v.getHeight();
                Intent intent = new Intent();
                intent.setSourceBounds(srcRect);
                prepareIntent(intent);
                try {
                	mIntent.send(v.getContext(), 0, intent, null, null);
                } catch (PendingIntent.CanceledException e) {
                    android.util.Log.e("SetOnClickPendingIntent", "Cannot send pending intent: ", e);
                }
            }

            protected void prepareIntent(Intent intent) {
            	String value = (String)mCursor.getValueOrDefault(myCursorPos, SetBoundOnClickIntent.this);
            	intent.putExtra(mExtraName, value);
            }
        }

        public String readValue(Cursor cursor) {
        	return cursor.getString(mExtraCursorIndex);
        }
	}

	private CursorCache mCursor;
	private int mCursorPos;

	public BoundRemoteViews(Parcel parcel) {
		super(parcel);
	}

	public BoundRemoteViews(int layoutId) {
		super(layoutId);
	}

	public void setBindingCursor(Cursor cursor, Context context) {
		mCursor = new CursorCache(cursor,context);
	}

	public void dropCache() {
		if (mCursor != null) {
			mCursor.clear();
		}
		mCursor = null;
	}

	public int getCursorCacheSize() {
		if (mCursor != null)
			return mCursor.mCache.size();
		else
			return 0;
	}

	public void moveCursor(int newPosition) {
		mCursorPos = newPosition;
	}

	@Override
	protected Action loadActionFromParcel(int tag, Parcel parcel) {
		if (tag == BoundRemoteViews.BindingAction.tag)
			return new BindingAction(parcel);
		else if (tag == SetBoundOnClickIntent.TAG)
			return new SetBoundOnClickIntent(parcel);
		else
			return super.loadActionFromParcel(tag, parcel);
	}

	public void reapplyBinding(View v) {
	    try
	    {
	        if (mActions != null) {
	            final int count = mActions.size();
	            for (int i = 0; i < count; i++) {
	                Action a = mActions.get(i);
	                if (a instanceof BindingAction || a instanceof SetBoundOnClickIntent)
	             	   a.apply(v);
	            }
	        }
	    } catch (OutOfMemoryError e) {
            System.gc();
	    }
	}

    public void setBoundString(int viewId, String methodName, int cursorIndex, int defaultResource) {
    	addAction(new BindingAction(viewId, methodName, ReflectionAction.STRING,
    								cursorIndex, defaultResource));
    }

    public void setBoundCharSequence(int viewId, String methodName, int cursorIndex, int defaultResource) {
    	addAction(new BindingAction(viewId, methodName, ReflectionAction.CHAR_SEQUENCE,
    								cursorIndex, defaultResource));
    }

    public void setBoundByte(int viewId, String methodName, int cursorIndex) {
    	addAction(new BindingAction(viewId, methodName, ReflectionAction.BYTE,
    								cursorIndex,0));
    }

    public void setBoundShort(int viewId, String methodName, int cursorIndex) {
    	addAction(new BindingAction(viewId, methodName, ReflectionAction.SHORT,
    								cursorIndex,0));
    }

    public void setBoundInt(int viewId, String methodName, int cursorIndex) {
    	addAction(new BindingAction(viewId, methodName, ReflectionAction.INT,
    								cursorIndex,0));
    }

    public void setBoundLong(int viewId, String methodName, int cursorIndex) {
    	addAction(new BindingAction(viewId, methodName, ReflectionAction.LONG,
    								cursorIndex,0));
    }

    public void setBoundFloat(int viewId, String methodName, int cursorIndex) {
    	addAction(new BindingAction(viewId, methodName, ReflectionAction.FLOAT,
    								cursorIndex,0));
    }

    public void setBoundDouble(int viewId, String methodName, int cursorIndex) {
    	addAction(new BindingAction(viewId, methodName, ReflectionAction.DOUBLE,
    								cursorIndex,0));
    }

    public void setBoundChar(int viewId, String methodName, int cursorIndex) {
    	addAction(new BindingAction(viewId, methodName, ReflectionAction.CHAR,
    								cursorIndex,0));
    }

    public void setBoundUri(int viewId, String methodName, int cursorIndex) {
    	addAction(new BindingAction(viewId, methodName, ReflectionAction.URI,
    								cursorIndex,0));
    }

    public void setBoundBitmap(int viewId, String methodName, int cursorIndex, int defaultResource) {
    	addAction(new BindingAction(viewId, methodName, ReflectionAction.BITMAP,
    								cursorIndex, defaultResource));
    }

    public void SetBoundOnClickIntent(int viewId, PendingIntent intent,
    		String extraName, int extraCursorIndex) {
        addAction(new SetBoundOnClickIntent(viewId, intent, extraName, extraCursorIndex));
    }

    /**
     * Parcelable.Creator that instantiates RemoteViews objects
     */
    public static final Parcelable.Creator<BoundRemoteViews> CREATOR = new Parcelable.Creator<BoundRemoteViews>() {
        public BoundRemoteViews createFromParcel(Parcel parcel) {
            return new BoundRemoteViews(parcel);
        }

        public BoundRemoteViews[] newArray(int size) {
            return new BoundRemoteViews[size];
        }
    };
}
