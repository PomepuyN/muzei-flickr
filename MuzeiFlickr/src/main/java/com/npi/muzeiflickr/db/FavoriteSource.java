package com.npi.muzeiflickr.db;

import android.content.Context;

import com.npi.muzeiflickr.R;

/**
 * Created by nicolas on 23/02/14.
 */
public class FavoriteSource implements RequestData {

    private final Context mContext;

    public FavoriteSource(Context context) {
        mContext = context;
    }

    @Override
    public String getTitle() {
        return mContext.getString(R.string.favorites);
    }

    @Override
    public int getIconRessource() {
        return R.drawable.icon_favorite;
    }

    @Override
    public String getPhotoTotal() {
        return String.valueOf(Favorite.count(Favorite.class,"",new String[0]));
    }

    @Override
    public String getCurrentPhotoIndex() {
        return "0";
    }

    @Override
    public int getCurrentPage() {
        return 0;
    }

    @Override
    public void setPage(int i) {

    }

    @Override
    public int getSourceType() {
        return SourceTypeEnum.FAVORITES.ordinal();
    }

    @Override
    public long getSourceId() {
        return 0;
    }

    @Override
    public void incrementCurrent() {

    }
}
