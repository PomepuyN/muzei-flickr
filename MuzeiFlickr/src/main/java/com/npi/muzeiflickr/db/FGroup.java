package com.npi.muzeiflickr.db;

import android.content.Context;

import com.npi.muzeiflickr.R;
import com.orm.SugarRecord;

/**
 * Created by nicolas on 17/02/14.
 */
public class FGroup extends SugarRecord<FGroup> implements RequestData {

    int total;
    int current;
    String name;
    public String groupId;
    public int page;

    public FGroup(Context context) {
        super(context);
    }

    public FGroup(Context context, String groupId, String name, int page, int current, int total) {
        super(context);
        this.name = name;
        this.groupId = groupId;
        this.current = current;
        this.total = total;
    }


    @Override
    public String getTitle() {
        return name;
    }

    @Override
    public int getIconRessource() {
        return R.drawable.icon_group;
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
        return SourceTypeEnum.GROUP.ordinal();
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
