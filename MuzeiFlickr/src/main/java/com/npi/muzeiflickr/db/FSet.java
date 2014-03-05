package com.npi.muzeiflickr.db;

import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.data.SourceDescriptor;
import com.orm.SugarRecord;

/**
 * Created by nicolas on 17/02/14.
 */
public class FSet extends SugarRecord<FSet> implements RequestData {

    int total;
    int current;
    String name;
    public String setId;
    public int page;

    public FSet() {
        super();
    }

    public FSet(String setId, String name, int current, int total) {
        super();
        this.name = name;
        this.setId = setId;
        this.current = current;
        this.total = total;
    }


    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public int getIconRessource() {
        return R.drawable.icon_set;
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

    @Override
    public int getSourceType() {
        return SourceDescriptor.SET.getId();
    }

    @Override
    public long getSourceId() {
        return id;
    }

    @Override
    public void incrementCurrent() {
        current++;
        save();
    }
}
