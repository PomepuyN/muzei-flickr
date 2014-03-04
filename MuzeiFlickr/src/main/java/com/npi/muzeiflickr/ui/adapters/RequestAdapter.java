package com.npi.muzeiflickr.ui.adapters;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nhaarman.listviewanimations.ArrayAdapter;
import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.db.RequestData;

import java.util.List;

/**
 * Created by nicolas on 17/02/14.
 */
public class RequestAdapter extends ArrayAdapter<RequestData> {

    private static final String TAG = RequestAdapter.class.getSimpleName();
    private final List<RequestData> mRequests;
    private final Context mContext;
    private final OnRequestAdapterChanged mListener;

    public RequestAdapter(Context context, List<RequestData> requests, OnRequestAdapterChanged listener) {
        super(requests);
        mRequests = requests;
        mContext = context;
        mListener = listener;
    }

    @Override
    public int getCount() {
        return mRequests.size();
    }

    @Override
    public RequestData getItem(int position) {
        return mRequests.get(position);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final LocalHolder holder;
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.request_list_item, null);
            holder = new LocalHolder();
            holder.title = (TextView) convertView.findViewById(R.id.title);
            // holder.email = (OTextView) convertView.findViewById(R.id.about_email);
            holder.photoCount = (TextView) convertView.findViewById(R.id.photo_count);
            convertView.setTag(holder);
        } else {
            holder = (LocalHolder) convertView.getTag();
        }

        RequestData item  = getItem(position);

        holder.title.setText(item.getTitle());
        holder.title.setCompoundDrawablesRelativeWithIntrinsicBounds(mContext.getResources().getDrawable(item.getIconRessource()), null, null,null);
        if (item.getPhotoTotal().equals("0")) {
            holder.photoCount.setVisibility(View.GONE);
        } else {
            holder.photoCount.setText(item.getCurrentPhotoIndex()+" / "+item.getPhotoTotal());
            holder.photoCount.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        mListener.OnNotifyDataSetChanged();
        if (BuildConfig.DEBUG) Log.d(TAG, "Adapter OnNotifyDataSetChanged");
        super.notifyDataSetChanged();
    }

    public void setItems(List<RequestData> items) {
        mRequests.clear();
        mRequests.addAll(items);
        notifyDataSetChanged();
    }

    static class LocalHolder {
        public TextView title;
        public TextView photoCount;
    }

    public interface OnRequestAdapterChanged {
        void OnNotifyDataSetChanged();
    }
}
