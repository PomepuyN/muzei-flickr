package com.npi.muzeiflickr.data;

import com.npi.muzeiflickr.FlickrMuzeiApplication;
import com.npi.muzeiflickr.R;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nicolas on 04/03/14.
 */
public enum SourceAdapterItem {

    SEARCH(0, R.string.search),
    USER(1, R.string.user),
    TAG(2, R.string.tag),
    GROUP(3, R.string.group),
//    SET(4, R.string.set),
    FAVORITES(5, R.string.favorites),
    INTERESTINGNESS(6, R.string.interestingness);


    private final int titleResource;
    private final int id;


    SourceAdapterItem(int id, int titleResource) {
        this.id = id;
        this.titleResource = titleResource;
    }

    public int getTitleResource() {
        return titleResource;
    }

    public int getId() {
        return id;
    }

    public static List<SourceAdapterItem> getFilteredEntries() {

        List<SourceAdapterItem> result = new LinkedList<SourceAdapterItem>(Arrays.asList(values()));

        if (FlickrMuzeiApplication.getSettings().getBoolean(PreferenceKeys.USE_FAVORITES, false)) {
            result.remove(FAVORITES);
        }

        if (FlickrMuzeiApplication.getSettings().getBoolean(PreferenceKeys.USE_INTERESTINGNESS, false)) {
            result.remove(INTERESTINGNESS);
        }

        return result;

    }

}
