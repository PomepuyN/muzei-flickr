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
import com.npi.muzeiflickr.FlickrMuzeiApplication;
import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.data.PreferenceKeys;
import com.npi.muzeiflickr.data.SourceDescriptor;
import com.npi.muzeiflickr.db.FGroup;
import com.npi.muzeiflickr.db.FSet;
import com.npi.muzeiflickr.db.Favorite;
import com.npi.muzeiflickr.db.InterestingnessSource;
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
import java.util.Random;

public class FlickrSource extends RemoteMuzeiArtSource {
    private static final String TAG = "FlickrSource";
    private static final String SOURCE_NAME = "FlickrSource";

    public static final String ACTION_CLEAR_SERVICE = "com.npi.muzeiflickr.ACTION_CLEAR_SERVICE";
    public static final String ACTION_REFRESH_FROM_WIDGET = "com.npi.muzeiflickr.NEXT_FROM_WIDGET";
    public static final String ACTION_RELOAD_SOME_PHOTOS = "com.npi.muzeiflickr.ACTION_RELOAD_SOME_PHOTOS";
    public static final String ACTION_PAUSE_FROM_WIDGET = "com.npi.muzeiflickr.ACTION_PAUSE_FROM_WIDGET";
    public static final String ACTION_FAVORITE_FROM_WIDGET = "com.npi.muzeiflickr.ACTION_FAVORITE_FROM_WIDGET";
    public static final String ACTION_DOWNLOAD_FROM_WIDGET = "com.npi.muzeiflickr.ACTION_DOWNLOAD_FROM_WIDGET";
    public static final int DEFAULT_REFRESH_TIME = 7200000;
    private static final int COMMAND_ID_SHARE = 1;
    private static final int COMMAND_ID_PAUSE = 2;
    private static final int COMMAND_ID_RESTART = 3;
    private static final int COMMAND_ID_ADD_ARTIST = 4;
    private static final int COMMAND_ID_ADD_FAVORITE = 5;
    private static final int COMMAND_ID_DOWNLOAD = 6;
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

        //If no connection wait for network
        if (!Utils.isNetworkAvailable(this)) {
            setWantsNetworkAvailable(true);
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
            if (photo.getSource(this) != null) photo.getSource(this).incrementCurrent();
        } catch (IllegalStateException e) {
            if (BuildConfig.DEBUG) Log.e(TAG, e.getMessage(), e);
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


        FlickrMuzeiApplication.getEditor().putString(PreferenceKeys.CURRENT_TITLE, photo.title);
        FlickrMuzeiApplication.getEditor().putString(PreferenceKeys.CURRENT_AUTHOR, name);
        FlickrMuzeiApplication.getEditor().putString(PreferenceKeys.CURRENT_URL, photo.url);
        FlickrMuzeiApplication.getEditor().putString(PreferenceKeys.CURRENT_PHOTO_ID, photo.photoId);
        FlickrMuzeiApplication.getEditor().commit();

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
        commands.add(new UserCommand(COMMAND_ID_DOWNLOAD, getString(R.string.download)));
        commands.add(new UserCommand(BUILTIN_COMMAND_ID_NEXT_ARTWORK, ""));
        commands.add(new UserCommand(COMMAND_ID_ADD_FAVORITE, getString(R.string.add_favorites)));
        commands.add(new UserCommand(COMMAND_ID_ADD_ARTIST, getString(R.string.add_artist)));
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
        switch (id) {
            case COMMAND_ID_PAUSE:
                FlickrMuzeiApplication.getEditor().putBoolean(PreferenceKeys.PAUSED, true);
                FlickrMuzeiApplication.getEditor().commit();
                break;
            case COMMAND_ID_RESTART:
                FlickrMuzeiApplication.getEditor().putBoolean(PreferenceKeys.PAUSED, false);
                FlickrMuzeiApplication.getEditor().commit();
                scheduleUpdate(System.currentTimeMillis() + settings.getInt(PreferenceKeys.REFRESH_TIME, DEFAULT_REFRESH_TIME));
                break;
            case COMMAND_ID_ADD_ARTIST:
                getUserId(settings.getString(PreferenceKeys.CURRENT_AUTHOR, ""));
                break;
            case COMMAND_ID_DOWNLOAD:

                Utils.downloadImage(this, getCurrentArtwork().getImageUri().toString(), getCurrentArtwork().getTitle(), getCurrentArtwork().getByline()
                        .replaceFirst("\\.\\s*($|\\n).*", "").trim());

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
            case COMMAND_ID_ADD_FAVORITE:
                favoriteCurrent();

                break;

        }
        manageUserCommands(settings);

    }

    private void favoriteCurrent() {
        Favorite fav = new Favorite();
        String photoId = FlickrMuzeiApplication.getSettings().getString(PreferenceKeys.CURRENT_PHOTO_ID, "");
        if (Favorite.find(Favorite.class, "photo_id = ?", photoId).size() > 0) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(FlickrSource.this, getString(R.string.already_in_favorites), Toast.LENGTH_LONG).show();
                }
            });
            return;
        }
        if (!TextUtils.isEmpty(photoId)) {
            fav.photoId = photoId;
            fav.save();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(FlickrSource.this, getString(R.string.added_to_favorites), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(FlickrSource.this, getString(R.string.unable_add_favorite), Toast.LENGTH_LONG).show();
                }
            });

        }
        return;
    }


    @Override
    protected void onNetworkAvailable() {
        super.onNetworkAvailable();
        scheduleUpdate(System.currentTimeMillis() + 1000);
        setWantsNetworkAvailable(false);

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
        List<FSet> sets = FSet.listAll(FSet.class);

        if (FlickrMuzeiApplication.getSettings().getBoolean(PreferenceKeys.USE_FAVORITES, false)) {
            List<Favorite> favs = Favorite.listAll(Favorite.class);

            List<Favorite> favsToLoad = new ArrayList<Favorite>();
            Random random = new Random();

            for (int i = 0; i < 1; i++) {
                if (favs.size() == 0) {
                    break;
                }
                int position = random.nextInt(favs.size());
                favsToLoad.add(favs.get(position));
                favs.remove(position);
            }
            for (final Favorite favorite : favsToLoad) {
                FlickrService.getInstance(this).getPhotoInfo(favorite.photoId, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotoResponse>() {
                    @Override
                    public void onFailure() {

                    }

                    @Override
                    public void onSuccess(final FlickrApiData.PhotoResponse photo) {
                        FlickrService.getInstance(FlickrSource.this).getSize(photo.photo.id, new FlickrServiceInterface.IRequestListener<FlickrApiData.SizeResponse>() {
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

                                    FlickrService.getInstance(FlickrSource.this).getUser(photo.photo.owner.nsid, new FlickrServiceInterface.IRequestListener<FlickrApiData.UserResponse>() {
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
                                            Photo photoEntity = new Photo();
                                            photoEntity.userName = name;
                                            photoEntity.url = "http://www.flickr.com/photos/" + photo.photo.owner + "/" + photo.photo.id;
                                            photoEntity.source = finalLargestSize.source;
                                            photoEntity.title = photo.photo.title._content;
                                            photoEntity.photoId = photo.photo.id;
                                            photoEntity.owner = photo.photo.owner.username;
                                            photoEntity.sourceType = SourceDescriptor.FAVORITES.getId();
                                            photoEntity.sourceId = favorite.getId();

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
                });
            }

        }
        if (FlickrMuzeiApplication.getSettings().getBoolean(PreferenceKeys.USE_INTERESTINGNESS, false)) {
            FlickrService.getInstance(this).getInterrestingness(new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
                @Override
                public void onFailure() {

                }

                @Override
                public void onSuccess(final FlickrApiData.PhotosResponse response) {
                    managePhotoResponse(new InterestingnessSource(FlickrSource.this), response);
                }
            });

        }


        for (final User user : users) {

            int page = user.page;
            if (settings.getBoolean(PreferenceKeys.RANDOMIZE, false)) {

                Random random = new Random();
                if (BuildConfig.DEBUG) Log.d(TAG, "Randomize! Current page: " + page);
                page = random.nextInt((user.total / 5) + 1) + 1;
                if (BuildConfig.DEBUG) Log.d(TAG, "Randomize! Randomized page: " + page);
            }

            if (BuildConfig.DEBUG) Log.d(TAG, "User" + user.getTitle() + " - page " + page);
            FlickrService.getInstance(this).getPopularPhotosByUser(user.userId, page, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
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

            FlickrService.getInstance(this).getPopularPhotos(search.term, search.page, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
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

            FlickrService.getInstance(this).getPopularPhotosByTag(tag.term, tag.page, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
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

            FlickrService.getInstance(this).getGroupPhotos(group.groupId, group.page, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
                @Override
                public void onFailure() {

                }

                @Override
                public void onSuccess(FlickrApiData.PhotosResponse response) {
                    managePhotoResponse(group, response);
                }
            });


        }
        for (final FSet set : sets) {

            FlickrService.getInstance(this).getSetPhotos(set.setId, set.page, new FlickrServiceInterface.IRequestListener<FlickrApiData.SetPhotosResponse>() {
                @Override
                public void onFailure() {

                }

                @Override
                public void onSuccess(FlickrApiData.SetPhotosResponse response) {
                    managePhotoResponse(set, response);
                }
            });


        }


    }

    private void managePhotoResponse(final RequestData requestData, final FlickrApiData.ParsablePhotosResponse photosResponse) {

        if (photosResponse == null || photosResponse.getResponse() == null || photosResponse.getResponse().photo == null) {
            Log.w(TAG, "Unable to get the photo list");
            return;
        }

        final SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
        if (!settings.getBoolean(PreferenceKeys.RANDOMIZE, false)) {
            int currentPage = requestData.getCurrentPage();
            if (photosResponse.getResponse().pages < currentPage) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Last page: " + currentPage + "/" + photosResponse.getResponse().pages);
                requestData.setPage(1);
            } else {
                if (BuildConfig.DEBUG) Log.d(TAG, "Set page to " + String.valueOf(currentPage + 1));
                requestData.setPage(currentPage + 1);
            }
        }


        //Store photos
        for (final FlickrApiData.Photo photo : photosResponse.getResponse().photo) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Getting infos for photo: " + photo.id);


            FlickrService.getInstance(this).getSize(photo.id, new FlickrServiceInterface.IRequestListener<FlickrApiData.SizeResponse>() {
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

                        String owner;
                        if (photo.owner != null) {
                            owner = photo.owner;
                        } else {
                            owner = photosResponse.getResponse().owner;

                        }

                        FlickrService.getInstance(FlickrSource.this).getUser(owner, new FlickrServiceInterface.IRequestListener<FlickrApiData.UserResponse>() {
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
                                Photo photoEntity = new Photo();
                                photoEntity.userName = name;
                                photoEntity.url = "http://www.flickr.com/photos/" + photo.owner + "/" + photo.id;
                                photoEntity.source = finalLargestSize.source;
                                photoEntity.title = photo.title;
                                photoEntity.photoId = photo.id;
                                photoEntity.owner = photo.owner;
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

    /**
     * Determine if a user exists
     *
     * @param user the user to search
     */
    private void getUserId(final String user) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                FlickrService.getInstance(FlickrSource.this).getUserByName(user, new FlickrServiceInterface.IRequestListener<FlickrApiData.UserByNameResponse>() {
                    @Override
                    public void onFailure() {
                        Toast.makeText(FlickrSource.this, getString(R.string.unable_add_user), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(FlickrApiData.UserByNameResponse userByNameResponse) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Looking for user");


                        //The user has not been found
                        if (userByNameResponse == null || userByNameResponse.user == null || userByNameResponse.user.nsid == null) {
                            if (BuildConfig.DEBUG) Log.d(TAG, "User not found");
                            Toast.makeText(FlickrSource.this, getString(R.string.unable_add_user), Toast.LENGTH_LONG).show();
                            return;
                        }


                        //The user has been found, we store it
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "User found: " + userByNameResponse.user.nsid + " for " + user);
                        }
                        final String userId = userByNameResponse.user.nsid;


                        //User has been found, let's see if he has photos

                        FlickrService.getInstance(FlickrSource.this).getPopularPhotosByUser(userId, 0, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
                            @Override
                            public void onFailure() {
                            }

                            @Override
                            public void onSuccess(FlickrApiData.PhotosResponse photosResponse) {
                                if (photosResponse.photos.photo.size() > 0) {
                                    User userDB = new User(FlickrSource.this, userId, user, 1, 0, photosResponse.photos.total);
                                    userDB.save();
                                    Toast.makeText(FlickrSource.this, getString(R.string.user_added), Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(FlickrSource.this, getString(R.string.unable_add_user), Toast.LENGTH_LONG).show();

                                }
                            }
                        });
                    }
                });


            }
        }).run();

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
        if (ACTION_PAUSE_FROM_WIDGET.equals(action)) {
            if (FlickrMuzeiApplication.getSettings().getBoolean(PreferenceKeys.PAUSED, false)) {
                FlickrMuzeiApplication.getEditor().putBoolean(PreferenceKeys.PAUSED, false);
                FlickrMuzeiApplication.getEditor().commit();

            } else {
                FlickrMuzeiApplication.getEditor().putBoolean(PreferenceKeys.PAUSED, true);
                FlickrMuzeiApplication.getEditor().commit();
            }
            updateWidgets();
            return;

        }
        if (ACTION_DOWNLOAD_FROM_WIDGET.equals(action)) {
            Utils.downloadImage(this, getCurrentArtwork().getImageUri().toString(), getCurrentArtwork().getTitle(), getCurrentArtwork().getByline()
                    .replaceFirst("\\.\\s*($|\\n).*", "").trim());
            return;

        }
        if (ACTION_FAVORITE_FROM_WIDGET.equals(action)) {
            favoriteCurrent();
            return;

        }
        if (ACTION_RELOAD_SOME_PHOTOS.equals(action)) {
            requestPhotos();
            return;

        }

        super.onHandleIntent(intent);
    }


    @Override
    protected void onDisabled() {
        if (BuildConfig.DEBUG) Log.d(TAG, "onDisabled");
        final SharedPreferences settings = getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
        FlickrMuzeiApplication.getEditor().putBoolean(PreferenceKeys.IS_SUSCRIBER_ENABLED, false);
        FlickrMuzeiApplication.getEditor().commit();

        updateWidgets();
        manageUserCommands(settings);

        super.onDisabled();
    }

    @Override
    protected void onEnabled() {
        if (BuildConfig.DEBUG) Log.d(TAG, "onEnabled");
        FlickrMuzeiApplication.getEditor().putBoolean(PreferenceKeys.IS_SUSCRIBER_ENABLED, true);

        FlickrMuzeiApplication.getEditor().commit();

        updateWidgets();

        super.onEnabled();
    }

}

