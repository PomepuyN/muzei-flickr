package com.npi.muzeiflickr.db;

import com.orm.SugarRecord;

/**
 * Created by nicolas on 23/02/14.
 */
public class Favorite extends SugarRecord<Favorite> {

    public Favorite(){
        super();
    }

    public String photoId;
}
