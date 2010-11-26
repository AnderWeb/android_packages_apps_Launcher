package com.android.launcher;

import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * @author AnderWeb
 *
 */
public class CounterReceiver extends BroadcastReceiver {
    private HashMap<String, FilterData> mReceiverFilter;
    private OnCounterChangedListener mListener;
    public CounterReceiver(Context context) {
        mReceiverFilter=new HashMap<String, FilterData>();
        parseFilters(context);
    }
    public void setCounterListener(OnCounterChangedListener l){
        mListener=l;
    }
    public IntentFilter getFilter(){
        IntentFilter tmp=new IntentFilter();
        for(String key : mReceiverFilter.keySet()){
            tmp.addAction(key);
        }
        return tmp;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        FilterData tmp=mReceiverFilter.get(intent.getAction());
        if(tmp!=null){
            int counter=0;
            int color=0xFFFF0000;
            String packagename=tmp.pname;
            for(String extra:tmp.extras){
                if("PNAME".equals(extra)){
                    packagename=intent.getStringExtra(extra);
                }else if("COLOR".equals(extra)){
                    color=intent.getIntExtra(extra, color);
                }else{
                    counter+=intent.getIntExtra(extra, 0);
                }
            }
            if(mListener!=null){
                mListener.onTrigger(packagename, counter, color);
            }
        }
    }

    private void parseFilters(Context context) {
        XmlPullParser parser = context.getResources().getXml(R.xml.counter_filter);
        try {
            // auto-detect the encoding from the stream
            int eventType = parser.getEventType();
            FilterData currentItem = null;
            boolean done = false;
            while (eventType != XmlPullParser.END_DOCUMENT && !done){
                String name = null;
                switch (eventType){
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase("counter")){
                            currentItem = new FilterData();
                            //get attributes:
                            currentItem.action=parser.getAttributeValue(null, "action");
                            currentItem.pname=parser.getAttributeValue(null, "package");
                            currentItem.extras=new ArrayList<String>();
                        } else if (currentItem != null){
                            if (name.equalsIgnoreCase("extra")){
                                currentItem.extras.add(parser.nextText());
                            }    
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        name = parser.getName();
                        if (name.equalsIgnoreCase("counter") && currentItem != null){
                            mReceiverFilter.put(currentItem.action,currentItem);
                        } else if (name.equalsIgnoreCase("counters")){
                            done = true;
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    private class FilterData{
        protected String action;
        protected String pname;
        protected ArrayList<String> extras;
    }
    /**
     * Interface definition for a callback to be invoked when a update
     * is required
     */
    public interface OnCounterChangedListener {
        void onTrigger(String pname, int counter, int color);
    }

}
