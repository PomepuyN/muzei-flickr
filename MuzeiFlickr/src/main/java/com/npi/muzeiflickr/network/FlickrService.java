package com.npi.muzeiflickr.network;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by nicolas on 19/02/14.
 */
public class FlickrService {


    private static FlickrService INSTANCE;
    private RestAdapter mRestAdapter;

    public static FlickrService getInstance() {
        if (INSTANCE == null) INSTANCE = new FlickrService();
        return INSTANCE;
    }

    private FlickrServiceInterface getService() {
        if (mRestAdapter == null) {

            mRestAdapter = new RestAdapter.Builder()
//                .setLogLevel(RestAdapter.LogLevel.FULL)
                    .setServer("http://api.flickr.com/services/rest")
                    .build();
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
}
