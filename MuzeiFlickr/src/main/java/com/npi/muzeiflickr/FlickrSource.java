/*
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Created by nicolas on 14/02/14.
 * Muzei source
 */

package com.npi.muzeiflickr;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

public class FlickrSource extends RemoteMuzeiArtSource {
    private static final String TAG = "FlickrSource";
    private static final String SOURCE_NAME = "FlickrSource";

    public static final String ACTION_CLEAR_SERVICE = "com.npi.muzeiflickr.ACTION_CLEAR_SERVICE";
    public static final String ACTION_REFRESH_FROM_WIDGET = "com.npi.muzeiflickr.NEXT_FROM_WIDGET";
    private List<PhotoEntity> storedPhotos;

    public FlickrSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    /**
     * Muzei ask for an Artwork update
     *
     * @param reason reason for update
     * @throws RetryException
     */
    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        final SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);

        // Check if we cancel the update due to WIFI connection
        if (settings.getBoolean(PreferenceKeys.WIFI_ONLY, false) && !Utils.isWifiConnected(this)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Refresh avoided: no wifi");
            scheduleUpdate(System.currentTimeMillis() + settings.getInt(PreferenceKeys.REFRESH_TIME, 7200000));
            return;
        }


        //The memory photo cache is empty let's populate it
        if (storedPhotos == null) {
            storedPhotos = retrievePhotos(settings);
        }

        //No photo in the cache, let load some from flickr
        if (storedPhotos == null || storedPhotos.size() == 0) {
            if (BuildConfig.DEBUG) Log.d(TAG, "No photo: retrying");
            requestPhotos();

            throw new RetryException();
        }

        if (BuildConfig.DEBUG)
            Log.d(TAG, "Json stored: " + settings.getString(PreferenceKeys.PHOTOS, ""));

        //Get the photo
        PhotoEntity photo = storedPhotos.get(0);


        String name = photo.userName.substring(0, 1).toUpperCase() + photo.userName.substring(1);

        //Publick the photo to Muzei
        publishArtwork(new Artwork.Builder()
                .title(photo.title)
                .byline(name)
                .imageUri(Uri.parse(photo.source))
                .token(photo.id)
                .viewIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(photo.url)))
                .build());

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PreferenceKeys.CURRENT_TITLE, photo.title);
        editor.putString(PreferenceKeys.CURRENT_AUTHOR, name);
        editor.putString(PreferenceKeys.CURRENT_URL, photo.url);
        editor.commit();

        //Update widgets
        updateWidgets();

        //Update the cache
        storedPhotos.remove(0);
        savePhotos(settings, storedPhotos);
        scheduleUpdate(System.currentTimeMillis() + settings.getInt(PreferenceKeys.REFRESH_TIME, 7200000));
        //No photo left, let's load some more
        if (storedPhotos.size() == 0) {
            requestPhotos();
        }
    }

    private void updateWidgets() {
        Intent intent = new Intent(this, FlickrWidget.class);
        intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        int ids[] = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), FlickrWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    /**
     * Load photos from flickr
     */
    private void requestPhotos() {

        final SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);


        if (BuildConfig.DEBUG) Log.d(TAG, "Start service");

        RestAdapter restAdapter = new RestAdapter.Builder()
//                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setServer("http://api.flickr.com/services/rest")
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestInterceptor.RequestFacade request) {

                        //Change the request depending on the mode
                        int mode = settings.getInt(PreferenceKeys.MODE, 0);

                        switch (mode) {
                            case 0:

                                String search = settings.getString(PreferenceKeys.SEARCH_TERM, "landscape");
                                if (TextUtils.isEmpty(search)) search = "landscape";
                                if (BuildConfig.DEBUG) Log.d(TAG, "Request: " + search);
                                request.addQueryParam("text", search);

                                break;

                            case 1:

                                String user = settings.getString(PreferenceKeys.USER_ID, "");
                                request.addQueryParam("user_id", user);

                                break;

                        }


                    }
                })
                .setErrorHandler(new ErrorHandler() {
                    @Override
                    public Throwable handleError(RetrofitError retrofitError) {
                        //Issue with update. Let's wait for the next time
                        scheduleUpdate(System.currentTimeMillis() + settings.getInt(PreferenceKeys.REFRESH_TIME, 7200000));
                        return retrofitError;
                    }
                })
                .build();


        FlickrService service = restAdapter.create(FlickrService.class);
        //Request the correct page
        int page = settings.getInt(PreferenceKeys.CURRENT_PAGE, -1) + 1;
        if (BuildConfig.DEBUG) Log.d(TAG, "Requesting page: " + page);

        FlickrService.PhotosResponse response = null;
        int mode = settings.getInt(PreferenceKeys.MODE, 0);

        switch (mode) {
            case 0:
                response = service.getPopularPhotos(page);

                break;
            case 1:
                response = service.getPopularPhotosByUser(page);
                break;
        }

        if (response == null || response.photos.photo == null || response.photos == null) {
            Log.w(TAG, "Unable to get the photo list");
            return;
        }

        if (response.photos.photo.size() == 0) {
            Log.w(TAG, "No photos returned from API.");
            scheduleUpdate(System.currentTimeMillis() + settings.getInt(PreferenceKeys.REFRESH_TIME, 7200000));
            return;
        }

        //Store page number

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PreferenceKeys.CURRENT_PAGE, page);
        if (BuildConfig.DEBUG) Log.d(TAG, "Stored page: " + page);


        editor.commit();

        //Store photos
        for (FlickrService.Photo photo : response.photos.photo) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Getting infos for photo: " + photo.id);
            FlickrService.SizeResponse responseSize = service.getSize(photo.id);

            if (responseSize == null || responseSize.sizes == null) {
                Log.w(TAG, "Unable to get the infos for photo");
                return;
            }

            //Get the largest size limited to screen height to avoid too much loading
            int currentSizeHeight = 0;
            FlickrService.Size largestSize = null;
            for (FlickrService.Size size : responseSize.sizes.size) {
                if (size.height > currentSizeHeight && size.height < Utils.getScreenHeight(this)) {
                    currentSizeHeight = size.height;
                    largestSize = size;
                }
            }

            if (largestSize != null) {

                FlickrService.UserResponse responseUser = null;
                //Request user info (for the title)
                try {
                    responseUser = service.getUser(photo.owner);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    return;
                }

                if (responseUser == null || responseUser.person == null) {
                    Log.w(TAG, "Unable to get the infos for user");
                    return;
                }

                String name = "";
                if (responseUser.person.realname != null) {
                    name = responseUser.person.realname._content;
                }
                if (TextUtils.isEmpty(name) && responseUser.person.username != null) {
                    name = responseUser.person.username._content;
                }


                //Add the photo
                PhotoEntity photoEntity = new PhotoEntity(photo);
                photoEntity.userName = name;
                photoEntity.url = "http://www.flickr.com/photos/" + photo.owner + "/" + photo.id;
                photoEntity.source = largestSize.source;

                if (storedPhotos == null) {
                    storedPhotos = retrievePhotos(settings);
                }
                if (storedPhotos == null) {
                    storedPhotos = new ArrayList<PhotoEntity>();
                }
                storedPhotos.add(photoEntity);

                savePhotos(settings, storedPhotos);


            }
        }


    }

    // Retrieve photo list from SharedPreferences
    private List<PhotoEntity> retrievePhotos(SharedPreferences settings) {
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<PhotoEntity>>() {
        }.getType();

        return gson.fromJson(settings.getString(PreferenceKeys.PHOTOS, ""), type);

    }

    // Save photo list in SharedPreferences
    private void savePhotos(SharedPreferences settings, List<PhotoEntity> photos) {
        Gson gson = new Gson();
        String storablePhotosJson = gson.toJson(photos);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PreferenceKeys.PHOTOS, storablePhotosJson);


        editor.commit();
    }


    @Override
    protected void onHandleIntent(Intent intent) {


        if (intent == null) {
            super.onHandleIntent(intent);
            return;
        }
        if (BuildConfig.DEBUG) Log.d(TAG, "Handle intent: " + intent.getAction());

        String action = intent.getAction();
        if (ACTION_CLEAR_SERVICE.equals(action) || ACTION_REFRESH_FROM_WIDGET.equals(action)) {
            scheduleUpdate(System.currentTimeMillis() + 1000);
            return;

        }

        super.onHandleIntent(intent);
    }

    @Override
    protected void onDisabled() {
        if (BuildConfig.DEBUG) Log.d(TAG, "onDisabled");
        final SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PreferenceKeys.IS_SUSCRIBER_ENABLED, false);
        editor.commit();

        updateWidgets();

        super.onDisabled();
    }

    @Override
    protected void onEnabled() {
        if (BuildConfig.DEBUG) Log.d(TAG, "onEnabled");
        final SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(PreferenceKeys.IS_SUSCRIBER_ENABLED, true);

        editor.commit();

        updateWidgets();

        super.onEnabled();
    }

}

