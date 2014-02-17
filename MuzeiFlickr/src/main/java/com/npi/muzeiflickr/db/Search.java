package com.npi.muzeiflickr.db;

import android.content.Context;

import com.orm.SugarRecord;

/**
 * Created by nicolas on 17/02/14.
 */
public class Search extends SugarRecord<Search> {
    public Search(Context context) {
        super(context);
    }

    String term;
    int page;
}
