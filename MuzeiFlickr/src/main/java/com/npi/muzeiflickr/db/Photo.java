package com.npi.muzeiflickr.db;

import android.content.Context;

import com.orm.SugarRecord;

/**
 * Created by nicolas on 17/02/14.
 */
public class Photo extends SugarRecord<Photo> {

    public String photoId;

    public String title;
    public String userName;
    public String url;
    public String source;

    public Photo(Context context) {
        super(context);
    }
}
