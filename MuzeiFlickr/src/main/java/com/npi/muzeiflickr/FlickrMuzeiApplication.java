package com.npi.muzeiflickr;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import com.npi.muzeiflickr.data.PreferenceKeys;
import com.npi.muzeiflickr.db.Search;
import com.npi.muzeiflickr.db.User;
import com.npi.muzeiflickr.network.FlickrApiData;
import com.npi.muzeiflickr.network.FlickrService;
import com.npi.muzeiflickr.network.FlickrServiceInterface;
import com.npi.muzeiflickr.ui.activities.SettingsActivity;
import com.orm.SugarApp;

/**
 * Created by nicolas on 21/02/14.
 */
public class FlickrMuzeiApplication extends SugarApp {

    private static final String TAG = FlickrMuzeiApplication.class.getSimpleName();

    @Override
    public void onCreate() {

        final SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
        final SharedPreferences.Editor editor = settings.edit();

        int oldVersion = settings.getInt(PreferenceKeys.CURRENT_VERSION, 0);

        int versionNum = 0;
        try {
            versionNum = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "NameNotFoundException: " + e.getMessage(), e);
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "Migration data: " + versionNum + " / " + oldVersion);


        super.onCreate();
        //New install => adding landscape
        if (oldVersion < 0) {
            initData();
        }
        if (versionNum > oldVersion) {
            // DO YOUR MIGRATION STUFF
            if (oldVersion < 20000) {
                Log.w(TAG, "Migration needed from 1.x.x version");
                migrateFrom1(settings);
            }
        }
        editor.putInt(PreferenceKeys.CURRENT_VERSION, versionNum);
        editor.commit();
    }

    private void initData() {
        FlickrService.getInstance().getPopularPhotos("landscape", 0, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
            @Override
            public void onFailure() {
                //Creating dummy data as a fallback
                Search searchDB = new Search(FlickrMuzeiApplication.this, "landscape", 1, 0, 0);
                searchDB.save();
            }

            @Override
            public void onSuccess(FlickrApiData.PhotosResponse photosResponse) {
                if (photosResponse.photos.photo.size() > 0) {
                    Search searchDB = new Search(FlickrMuzeiApplication.this, "landscape", 1, 0, photosResponse.photos.total);
                    searchDB.save();
                }
            }
        });
    }

    private void migrateFrom1(final SharedPreferences settings) {
        final int mode = settings.getInt(PreferenceKeys.MODE, 0);
        if (mode == 0) {
            final String search = settings.getString(PreferenceKeys.SEARCH_TERM, "landscape");
            FlickrService.getInstance().getPopularPhotos(search, 0, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
                @Override
                public void onFailure() {
                    //Creating dummy data as a fallback
                    Search searchDB = new Search(FlickrMuzeiApplication.this, search, 1, 0, 0);
                    searchDB.save();
                }

                @Override
                public void onSuccess(FlickrApiData.PhotosResponse photosResponse) {
                    if (photosResponse.photos.photo.size() > 0) {
                        Search searchDB = new Search(FlickrMuzeiApplication.this, search, 1, 0, photosResponse.photos.total);
                        searchDB.save();
                    }
                }
            });

        } else {

            final String user = settings.getString(PreferenceKeys.USER_ID, "");
            if (TextUtils.isEmpty(user)) return;
            FlickrService.getInstance().getPopularPhotosByUser(user, 0, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
                @Override
                public void onFailure() {
                    //Creating dummy data as a fallback
                    User userDB = new User(FlickrMuzeiApplication.this, user, settings.getString(PreferenceKeys.USER_NAME, ""), 1, 0, 0);
                    userDB.save();
                }

                @Override
                public void onSuccess(FlickrApiData.PhotosResponse photosResponse) {
                    if (photosResponse.photos.photo.size() > 0) {
                        User userDB = new User(FlickrMuzeiApplication.this, user, settings.getString(PreferenceKeys.USER_NAME, ""), 1, 0, photosResponse.photos.total);
                        userDB.save();
                    } else {
                        //Creating dummy data as a fallback
                        User userDB = new User(FlickrMuzeiApplication.this, user, settings.getString(PreferenceKeys.USER_NAME, ""), 1, 0, 0);
                        userDB.save();

                    }
                }
            });
        }


    }
}
