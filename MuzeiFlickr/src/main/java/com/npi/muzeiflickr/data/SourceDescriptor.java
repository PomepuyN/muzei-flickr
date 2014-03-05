package com.npi.muzeiflickr.data;

import com.npi.muzeiflickr.FlickrMuzeiApplication;
import com.npi.muzeiflickr.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by nicolas on 04/03/14.
 */
public enum SourceDescriptor {

    SEARCH(0, R.string.search),
    USER(1, R.string.user),
    TAG(2, R.string.tag),
    GROUP(3, R.string.group),
    FAVORITES(5, R.string.favorites),
    SET(4, R.string.set),
    INTERESTINGNESS(6, R.string.interestingness);


    private final int titleResource;
    private final int id;


    SourceDescriptor(int id, int titleResource) {
        this.id = id;
        this.titleResource = titleResource;
    }

    public int getTitleResource() {
        return titleResource;
    }

    public int getId() {
        return id;
    }

    public static List<SourceDescriptor> getFilteredEntries() {

        List<SourceDescriptor> result = new LinkedList<SourceDescriptor>(Arrays.asList(values()));

        Collections.sort(result, new Comparator<SourceDescriptor>() {
            @Override
            public int compare(SourceDescriptor lhs, SourceDescriptor rhs) {
                return lhs.id-rhs.id;
            }
        });

        if (FlickrMuzeiApplication.getSettings().getBoolean(PreferenceKeys.USE_FAVORITES, false)) {
            result.remove(FAVORITES);
        }

        if (FlickrMuzeiApplication.getSettings().getBoolean(PreferenceKeys.USE_INTERESTINGNESS, false)) {
            result.remove(INTERESTINGNESS);
        }

        return result;

    }

}
