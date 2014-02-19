package com.npi.muzeiflickr.db;

import android.content.Context;

import com.npi.muzeiflickr.R;
import com.orm.SugarRecord;

/**
 * Created by nicolas on 17/02/14.
 */
public class User extends SugarRecord<User> implements RequestData {

    int total;
    int current;
    String userName;
    public String userId;
    public int page;

    public User(Context context) {
        super(context);
    }

    public User(Context context, String userId, String user,int page, int current, int total) {
        super(context);
        this.userName = user;
        this.userId = userId;
        this.current = current;
        this.total = total;
    }


    @Override
    public String getTitle() {
        return userName;
    }

    @Override
    public int getIconRessource() {
        return R.drawable.icon_user;
    }

    @Override
    public String getPhotoTotal() {
        return String.valueOf(total);
    }

    @Override
    public String getCurrentPhotoIndex() {
        return String.valueOf(current);
    }

    @Override
    public int getCurrentPage() {
        return page;
    }

    @Override
    public void setPage(int i) {
        page = i;
        save();
    }
}
