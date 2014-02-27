package com.npi.muzeiflickr.db;

import android.content.Context;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

/**
 * Created by nicolas on 17/02/14.
 */
public class Photo extends SugarRecord<Photo> {

    public String photoId;

    public String title;
    public String userName;
    public String owner;
    public String url;
    @Ignore
    public String thumbnail;
    @Ignore
    public boolean isFavorite;
    public String source;
    public int sourceType;
    public long sourceId;

    public Photo() {
        super();
    }

    public RequestData getSource(Context context) {
        if (sourceType == SourceTypeEnum.SEARCH.ordinal()) {
            return Search.findById(Search.class, sourceId);
        }
        if (sourceType == SourceTypeEnum.USER.ordinal()) {
            return User.findById(User.class, sourceId);
        }
        if (sourceType == SourceTypeEnum.TAG.ordinal()) {
            return Tag.findById(Tag.class, sourceId);
        }
        if (sourceType == SourceTypeEnum.GROUP.ordinal()) {
            return FGroup.findById(FGroup.class, sourceId);
        }
        if (sourceType == SourceTypeEnum.FAVORITES.ordinal()) {
            return new FavoriteSource(context);
        }

        throw new IllegalStateException("Photo has no valid sourceType");
    }
}
