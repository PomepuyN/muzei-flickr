package com.npi.muzeiflickr.db;

import android.content.Context;
import android.util.Log;

import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.FlickrMuzeiApplication;
import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.data.PreferenceKeys;
import com.npi.muzeiflickr.data.SourceDescriptor;

/**
 * Created by nicolas on 23/02/14.
 */
public class InterestingnessSource implements RequestData {

    private static final String TAG = InterestingnessSource.class.getSimpleName();
    private final Context mContext;

    public InterestingnessSource(Context context) {
        mContext = context;
    }

    @Override
    public String getTitle() {
        return mContext.getString(R.string.interestingness);
    }

    @Override
    public int getIconRessource() {
        return R.drawable.thumb_up;
    }

    @Override
    public String getPhotoTotal() {
        return "0";
    }

    @Override
    public String getCurrentPhotoIndex() {
        return "0";
    }

    @Override
    public int getCurrentPage() {
        return FlickrMuzeiApplication.getSettings().getInt(PreferenceKeys.INTERESTINGNESS_PAGE, 0);
    }

    @Override
    public void setPage(int i) {
        if (BuildConfig.DEBUG) Log.d(TAG, "interestingness New page: "+i);
        FlickrMuzeiApplication.getEditor().putInt(PreferenceKeys.INTERESTINGNESS_PAGE, i);
        FlickrMuzeiApplication.getEditor().commit();
    }

    @Override
    public int getSourceType() {
        return SourceDescriptor.INTERESTINGNESS.getId();
    }

    @Override
    public long getSourceId() {
        return 0;
    }

    @Override
    public void incrementCurrent() {

    }

    @Override
    public void delete() {
        FlickrMuzeiApplication.getEditor().putBoolean(PreferenceKeys.USE_INTERESTINGNESS, false);
        FlickrMuzeiApplication.getEditor().commit();
    }
}
