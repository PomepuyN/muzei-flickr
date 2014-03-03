package com.npi.muzeiflickr.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.FlickrMuzeiApplication;
import com.npi.muzeiflickr.data.PreferenceKeys;
import com.npi.muzeiflickr.network.retrofitsigner.RetrofitHttpOAuthConsumer;
import com.npi.muzeiflickr.network.retrofitsigner.SigningOkClient;
import com.npi.muzeiflickr.ui.activities.SettingsActivity;
import com.npi.muzeiflickr.utils.Config;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by nicolas on 19/02/14.
 */
public class FlickrService {


    private static final String TAG = FlickrService.class.getSimpleName();
    private static FlickrService INSTANCE;
    private final Context mContext;
    private RestAdapter mRestAdapter;

    public FlickrService(Context context) {
        mContext = context;
    }

    public static FlickrService getInstance(Context context) {
        if (INSTANCE == null) INSTANCE = new FlickrService(context);
        return INSTANCE;
    }

    private FlickrServiceInterface getService() {
        if (mRestAdapter == null) {

            RestAdapter.Builder builder = new RestAdapter.Builder()
                    .setLogLevel(RestAdapter.LogLevel.FULL)
                    .setServer("http://api.flickr.com/services/rest");

            final SharedPreferences settings = mContext.getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
            String token = settings.getString(PreferenceKeys.LOGIN_TOKEN, "");
            String secret = settings.getString(PreferenceKeys.LOGIN_SECRET, "");
            if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(secret)) {
                RetrofitHttpOAuthConsumer oAuthConsumer = new RetrofitHttpOAuthConsumer(Config.CONSUMER_KEY, Config.CONSUMER_SECRET);
                oAuthConsumer.setTokenWithSecret(token, secret);
                builder.setClient(new SigningOkClient(oAuthConsumer));
                if (BuildConfig.DEBUG) Log.d(TAG, "Using signed request");
            }


            mRestAdapter = builder.build();
        }

        return mRestAdapter.create(FlickrServiceInterface.class);
    }


    public void getPopularPhotos(String text, int page, final FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse> listener) {


        getService().getPopularPhotos(text, page, new Callback<FlickrApiData.PhotosResponse>() {
            @Override
            public void success(FlickrApiData.PhotosResponse photosResponse, Response response) {
                listener.onSuccess(photosResponse);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }

    public void getPopularPhotosByUser(String text, int page, final FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse> listener) {
        getService().getPopularPhotosByUser(text, page, new Callback<FlickrApiData.PhotosResponse>() {
            @Override
            public void success(FlickrApiData.PhotosResponse photosResponse, Response response) {
                listener.onSuccess(photosResponse);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }

    public void getUser(String id, final FlickrServiceInterface.IRequestListener<FlickrApiData.UserResponse> listener) {
        getService().getUser(id, new Callback<FlickrApiData.UserResponse>() {
            @Override
            public void success(FlickrApiData.UserResponse userResponse, Response response) {
                listener.onSuccess(userResponse);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }

    public void getSize(String photo, final FlickrServiceInterface.IRequestListener<FlickrApiData.SizeResponse> listener) {
        getService().getSize(photo, new Callback<FlickrApiData.SizeResponse>() {
            @Override
            public void success(FlickrApiData.SizeResponse sizeResponse, Response response) {
                listener.onSuccess(sizeResponse);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }


    public void getUserByName(String user, final FlickrServiceInterface.IRequestListener<FlickrApiData.UserByNameResponse> listener) {
        getService().getUserByName(user, new Callback<FlickrApiData.UserByNameResponse>() {
            @Override
            public void success(FlickrApiData.UserByNameResponse sizeResponse, Response response) {
                listener.onSuccess(sizeResponse);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }

    public void getLogin( final FlickrServiceInterface.IRequestListener<FlickrApiData.UserLoginResponse> listener) {
        getService().getLogin(new Callback<FlickrApiData.UserLoginResponse>() {
            @Override
            public void success(FlickrApiData.UserLoginResponse userLoginResponse, Response response) {
                listener.onSuccess(userLoginResponse);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }


    public void getPopularPhotosByTag(String text, int page, final FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse> listener) {
        getService().getPopularPhotosByTag(text, page, new Callback<FlickrApiData.PhotosResponse>() {
            @Override
            public void success(FlickrApiData.PhotosResponse photosResponse, Response response) {
                listener.onSuccess(photosResponse);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }

    public void getGroups(String text, final FlickrServiceInterface.IRequestListener<FlickrApiData.GroupsResponse> listener) {
        getService().getGroups(text, new Callback<FlickrApiData.GroupsResponse>() {
            @Override
            public void success(FlickrApiData.GroupsResponse photosResponse, Response response) {
                listener.onSuccess(photosResponse);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }

    public void getGroupPhotos(String groupId, int page, final FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse> listener) {
        getService().getGroupPhotos(groupId, page, new Callback<FlickrApiData.PhotosResponse>() {
            @Override
            public void success(FlickrApiData.PhotosResponse photosResponse, Response response) {
                listener.onSuccess(photosResponse);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }

    public void getContacts(final FlickrServiceInterface.IRequestListener<FlickrApiData.ContactResponse> listener) {
        getService().getContacts(new Callback<FlickrApiData.ContactResponse>() {
            @Override
            public void success(FlickrApiData.ContactResponse contactResponse, Response response) {
                listener.onSuccess(contactResponse);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }

    public void getGroupsByUser(String nsid, final FlickrServiceInterface.IRequestListener<FlickrApiData.GroupsResponse> listener) {
        getService().getGroupsByUser(nsid, new Callback<FlickrApiData.GroupsResponse>() {
            @Override
            public void success(FlickrApiData.GroupsResponse groupsResponse, Response response) {
                listener.onSuccess(groupsResponse);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }

    public void getPhotoInfo(String photoId, final FlickrServiceInterface.IRequestListener<FlickrApiData.PhotoResponse> listener) {
        getService().getPhotoInfo(photoId, new Callback<FlickrApiData.PhotoResponse>() {
            @Override
            public void success(FlickrApiData.PhotoResponse photo, Response response) {
                listener.onSuccess(photo);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }

    public void getFavorites(final FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse> listener) {
        getService().getFavorites( new Callback<FlickrApiData.PhotosResponse>() {
            @Override
            public void success(FlickrApiData.PhotosResponse photo, Response response) {
                listener.onSuccess(photo);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }

    public void addFavorite(String photoId, final FlickrServiceInterface.IRequestListener<FlickrApiData.AddFavoriteResponse> listener) {
        getService().addFavorite(photoId, new Callback<FlickrApiData.AddFavoriteResponse>() {
            @Override
            public void success(FlickrApiData.AddFavoriteResponse addFavoriteResponse, Response response) {
                listener.onSuccess(addFavoriteResponse);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }

    public void getGroupUrl(String groupId, final FlickrServiceInterface.IRequestListener<FlickrApiData.GroupUrlResponse> listener) {
        getService().getGroupUrl(groupId, new Callback<FlickrApiData.GroupUrlResponse>() {
            @Override
            public void success(FlickrApiData.GroupUrlResponse addFavoriteResponse, Response response) {
                listener.onSuccess(addFavoriteResponse);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }

    public void getInterrestingness(final FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse> listener) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String today = sdf.format(new Date());
        if (BuildConfig.DEBUG) Log.d(TAG, "interestingness Today: "+today);
        int page = FlickrMuzeiApplication.getSettings().getInt(PreferenceKeys.INTERESTINGNESS_PAGE, 0);
        if (!FlickrMuzeiApplication.getSettings().getString(PreferenceKeys.INTERESTINGNESS_DATE, "").equals(today)) {
            page = 0;
            FlickrMuzeiApplication.getEditor().putInt(PreferenceKeys.INTERESTINGNESS_PAGE, 0);
            FlickrMuzeiApplication.getEditor().putString(PreferenceKeys.INTERESTINGNESS_DATE, today);
            FlickrMuzeiApplication.getEditor().commit();
            if (BuildConfig.DEBUG) Log.d(TAG, "interestingness new day: reset ");
        }
        if (BuildConfig.DEBUG) Log.d(TAG, "interestingness new page: "+page);

        getService().getInterrestingness(String.valueOf(page), new Callback<FlickrApiData.PhotosResponse>() {
            @Override
            public void success(FlickrApiData.PhotosResponse photos, Response response) {
                listener.onSuccess(photos);
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                listener.onFailure();
            }
        });
    }

    public void invalidateAdapter() {
        mRestAdapter = null;
    }
}
