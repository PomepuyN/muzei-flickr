package com.npi.muzeiflickr.db;

import android.content.Context;

import com.orm.SugarRecord;

/**
 * Created by nicolas on 17/02/14.
 */
public class User extends SugarRecord<User> {

    String userName;
    String userId;
    int page;

    public User(Context context) {
        super(context);
    }


}
