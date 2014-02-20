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

package com.npi.muzeiflickr.api;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.google.android.apps.muzei.api.UserCommand;
import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.data.PreferenceKeys;
import com.npi.muzeiflickr.db.FGroup;
import com.npi.muzeiflickr.db.Photo;
import com.npi.muzeiflickr.db.RequestData;
import com.npi.muzeiflickr.db.Search;
import com.npi.muzeiflickr.db.Tag;
import com.npi.muzeiflickr.db.User;
import com.npi.muzeiflickr.network.FlickrApiData;
import com.npi.muzeiflickr.network.FlickrService;
import com.npi.muzeiflickr.network.FlickrServiceInterface;
import com.npi.muzeiflickr.ui.activities.SettingsActivity;
import com.npi.muzeiflickr.ui.widgets.FlickrWidget;
import com.npi.muzeiflickr.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlickrSource extends RemoteMuzeiArtSource {
    private static final String TAG = "FlickrSource";
    private static final String SOURCE_NAME = "FlickrSource";

    public static final String ACTION_CLEAR_SERVICE = "com.npi.muzeiflickr.ACTION_CLEAR_SERVICE";
    public static final String ACTION_REFRESH_FROM_WIDGET = "com.npi.muzeiflickr.NEXT_FROM_WIDGET";
    public static final int DEFAULT_REFRESH_TIME = 7200000;
    private static final int COMMAND_ID_SHARE = 1;
    private static final int COMMAND_ID_PAUSE = 2;
    private static final int COMMAND_ID_RESTART = 3;
    private List<Photo> storedPhotos;

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

        //Avoid refresh if it's paused
        if (settings.getBoolean(PreferenceKeys.PAUSED, false)) {
            manageUserCommands(settings);
            return;
        }

        // Check if we cancel the update due to WIFI connection
        if (settings.getBoolean(PreferenceKeys.WIFI_ONLY, false) && !Utils.isWifiConnected(this)) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Refresh avoided: no wifi");
            scheduleUpdate(System.currentTimeMillis() + settings.getInt(PreferenceKeys.REFRESH_TIME, DEFAULT_REFRESH_TIME));
            return;
        }


        //The memory photo cache is empty let's populate it
        if (storedPhotos == null) {
            storedPhotos = Photo.listAll(Photo.class);
        }

        //No photo in the cache, let load some from flickr
        if (storedPhotos == null || storedPhotos.size() == 0) {
            if (BuildConfig.DEBUG) Log.d(TAG, "No photo: retrying");
            requestPhotos();

            throw new RetryException();
        }

        Collections.shuffle(storedPhotos);


        //Get the photo
        Photo photo = storedPhotos.get(0);


        String name = photo.userName.substring(0, 1).toUpperCase() + photo.userName.substring(1);

        //Increment the photo counter
        try {
            if (photo.getSource() != null) photo.getSource().incrementCurrent();
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage(), e);
        }


        //Publick the photo to Muzei
        publishArtwork(new Artwork.Builder()
                .title(photo.title)
                .byline(name)
                .imageUri(Uri.parse(photo.source))
                .token(photo.photoId)
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
        storedPhotos.get(0).delete();
        storedPhotos.remove(0);

        scheduleUpdate(System.currentTimeMillis() + settings.getInt(PreferenceKeys.REFRESH_TIME, DEFAULT_REFRESH_TIME));
        //No photo left, let's load some more
        if (storedPhotos.size() == 0) {
            requestPhotos();
        }
        manageUserCommands(settings);
    }

    private void manageUserCommands(SharedPreferences settings) {
        List<UserCommand> commands = new ArrayList<UserCommand>();
        commands.add(new UserCommand(COMMAND_ID_SHARE, getString(R.string.share)));
        commands.add(new UserCommand(BUILTIN_COMMAND_ID_NEXT_ARTWORK, ""));
        if (settings.getBoolean(PreferenceKeys.PAUSED, false)) {
            commands.add(new UserCommand(COMMAND_ID_RESTART, getString(R.string.restart)));
        } else {
            commands.add(new UserCommand(COMMAND_ID_PAUSE, getString(R.string.pause)));
        }
        setUserCommands(commands);
    }

    @Override
    protected void onCustomCommand(int id) {
        super.onCustomCommand(id);
        final SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        switch (id) {
            case COMMAND_ID_PAUSE:
                editor.putBoolean(PreferenceKeys.PAUSED, true);
                editor.commit();
                break;
            case COMMAND_ID_RESTART:
                editor.putBoolean(PreferenceKeys.PAUSED, false);
                editor.commit();
                scheduleUpdate(System.currentTimeMillis() + settings.getInt(PreferenceKeys.REFRESH_TIME, DEFAULT_REFRESH_TIME));
                break;
            case COMMAND_ID_SHARE:
                Artwork currentArtwork = getCurrentArtwork();
                if (currentArtwork == null) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(FlickrSource.this,
                                    getString(R.string.no_artwork),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                String detailUrl = (currentArtwork.getViewIntent().getDataString());
                String artist = currentArtwork.getByline()
                        .replaceFirst("\\.\\s*($|\\n).*", "").trim();

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "My Android wallpaper is '"
                        + currentArtwork.getTitle().trim()
                        + "' by " + artist
                        + ". \nShared with Flickr for Muzei\n\n"
                        + detailUrl);
                shareIntent = Intent.createChooser(shareIntent, "Share Flickr photo");
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(shareIntent);
                break;

        }
        manageUserCommands(settings);

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

        List<User> users = User.listAll(User.class);
        List<Search> searches = Search.listAll(Search.class);
        List<Tag> tags = Tag.listAll(Tag.class);
        List<FGroup> groups = FGroup.listAll(FGroup.class);

        for (final User user : users) {


            if (BuildConfig.DEBUG) Log.d(TAG, "User" + user.getTitle() + " - page " + user.page);
            FlickrService.getInstance().getPopularPhotosByUser(user.userId, user.page, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
                @Override
                public void onFailure() {

                }

                @Override
                public void onSuccess(FlickrApiData.PhotosResponse response) {
                    managePhotoResponse(user, response);
                }
            });


        }
        for (final Search search : searches) {

            FlickrService.getInstance().getPopularPhotos(search.term, search.page, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
                @Override
                public void onFailure() {

                }

                @Override
                public void onSuccess(FlickrApiData.PhotosResponse response) {
                    managePhotoResponse(search, response);
                }
            });


        }

        for (final Tag tag : tags) {

            FlickrService.getInstance().getPopularPhotosByTag(tag.term, tag.page, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
                @Override
                public void onFailure() {

                }

                @Override
                public void onSuccess(FlickrApiData.PhotosResponse response) {
                    managePhotoResponse(tag, response);
                }
            });


        }

        for (final FGroup group : groups) {

            FlickrService.getInstance().getGroupPhotos(group.groupId, group.page, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
                @Override
                public void onFailure() {

                }

                @Override
                public void onSuccess(FlickrApiData.PhotosResponse response) {
                    managePhotoResponse(group, response);
                }
            });


        }


    }

    private void managePhotoResponse(final RequestData requestData, FlickrApiData.PhotosResponse photosResponse) {

        if (photosResponse == null || photosResponse.photos.photo == null || photosResponse.photos == null) {
            Log.w(TAG, "Unable to get the photo list");
            return;
        }

        int currentPage = requestData.getCurrentPage();
        if (photosResponse.photos.pages < currentPage) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Last page: " + currentPage + "/" + photosResponse.photos.pages);
            requestData.setPage(1);
        } else {
            if (BuildConfig.DEBUG) Log.d(TAG, "Set page to " + String.valueOf(currentPage + 1));
            requestData.setPage(currentPage + 1);
        }


        //Store photos
        for (final FlickrApiData.Photo photo : photosResponse.photos.photo) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Getting infos for photo: " + photo.id);


            FlickrService.getInstance().getSize(photo.id, new FlickrServiceInterface.IRequestListener<FlickrApiData.SizeResponse>() {
                @Override
                public void onFailure() {

                }

                @Override
                public void onSuccess(FlickrApiData.SizeResponse responseSize) {
                    if (responseSize == null || responseSize.sizes == null) {
                        Log.w(TAG, "Unable to get the infos for photo");
                        return;
                    }

                    //Get the largest size limited to screen height to avoid too much loading
                    int currentSizeHeight = 0;
                    FlickrApiData.Size largestSize = null;
                    for (FlickrApiData.Size size : responseSize.sizes.size) {
                        if (size.height > currentSizeHeight && size.height < Utils.getScreenHeight(FlickrSource.this)) {
                            currentSizeHeight = size.height;
                            largestSize = size;
                        }
                    }

                    if (largestSize != null) {

                        FlickrApiData.UserResponse responseUser = null;
                        //Request user info (for the title)
                        final FlickrApiData.Size finalLargestSize = largestSize;

                        FlickrService.getInstance().getUser(photo.owner, new FlickrServiceInterface.IRequestListener<FlickrApiData.UserResponse>() {
                            @Override
                            public void onFailure() {

                            }

                            @Override
                            public void onSuccess(FlickrApiData.UserResponse responseUser) {
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
                                Photo photoEntity = new Photo(FlickrSource.this);
                                photoEntity.userName = name;
                                photoEntity.url = "http://www.flickr.com/photos/" + photo.owner + "/" + photo.id;
                                photoEntity.source = finalLargestSize.source;
                                photoEntity.title = photo.title;
                                photoEntity.photoId = photo.id;
                                photoEntity.sourceType = requestData.getSourceType();
                                photoEntity.sourceId = requestData.getSourceId();

                                if (storedPhotos == null) {
                                    storedPhotos = Photo.listAll(Photo.class);
                                }
                                if (storedPhotos == null) {
                                    storedPhotos = new ArrayList<Photo>();
                                }
                                storedPhotos.add(photoEntity);
                                photoEntity.save();
                            }
                        });

                    }
                }
            });
        }

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
        manageUserCommands(settings);

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

