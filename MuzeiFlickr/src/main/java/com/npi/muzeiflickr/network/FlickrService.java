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

    public static FlickrService getInstance() {
        if (INSTANCE == null) INSTANCE = new FlickrService();
        return INSTANCE;
    }


    public static void getPopularPhotos(String text, int page, final FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse> listener) {
        RestAdapter restAdapter = new RestAdapter.Builder()
//                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setServer("http://api.flickr.com/services/rest")
                .build();


        final FlickrServiceInterface service = restAdapter.create(FlickrServiceInterface.class);

        service.getPopularPhotos(text, page, new Callback<FlickrApiData.PhotosResponse>() {
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

    public static void getPopularPhotosByUser(String text, int page, final FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse> listener) {
        RestAdapter restAdapter = new RestAdapter.Builder()
//                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setServer("http://api.flickr.com/services/rest")
                .build();


        final FlickrServiceInterface service = restAdapter.create(FlickrServiceInterface.class);

        service.getPopularPhotosByUser(text, page, new Callback<FlickrApiData.PhotosResponse>() {
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

    public static void getUser(String id, final FlickrServiceInterface.IRequestListener<FlickrApiData.UserResponse> listener) {
        RestAdapter restAdapter = new RestAdapter.Builder()
//                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setServer("http://api.flickr.com/services/rest")
                .build();


        final FlickrServiceInterface service = restAdapter.create(FlickrServiceInterface.class);

        service.getUser(id, new Callback<FlickrApiData.UserResponse>() {
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

    public static void getSize(String photo, final FlickrServiceInterface.IRequestListener<FlickrApiData.SizeResponse> listener) {
        RestAdapter restAdapter = new RestAdapter.Builder()
//                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setServer("http://api.flickr.com/services/rest")
                .build();


        final FlickrServiceInterface service = restAdapter.create(FlickrServiceInterface.class);

        service.getSize(photo, new Callback<FlickrApiData.SizeResponse>() {
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



}
