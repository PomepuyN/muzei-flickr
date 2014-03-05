package com.npi.muzeiflickr.db;

import android.content.Context;

import com.npi.muzeiflickr.data.SourceDescriptor;
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
        if (sourceType == SourceDescriptor.SEARCH.getId()) {
            return Search.findById(Search.class, sourceId);
        }
        if (sourceType == SourceDescriptor.USER.getId()) {
            return User.findById(User.class, sourceId);
        }
        if (sourceType == SourceDescriptor.TAG.getId()) {
            return Tag.findById(Tag.class, sourceId);
        }
        if (sourceType == SourceDescriptor.SET.getId()) {
            return FSet.findById(FSet.class, sourceId);
        }
        if (sourceType == SourceDescriptor.GROUP.getId()) {
            return FGroup.findById(FGroup.class, sourceId);
        }
        if (sourceType == SourceDescriptor.FAVORITES.getId()) {
            return new FavoriteSource(context);
        }

        throw new IllegalStateException("Photo has no valid sourceType");
    }
}
