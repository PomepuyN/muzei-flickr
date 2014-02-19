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
 * Retrofit service describing requests
 */

package com.npi.muzeiflickr.network;

import com.npi.muzeiflickr.utils.Config;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

public interface FlickrService {
    @GET("/?&method=flickr.photos.search&per_page=5&safe_search=1&sort=interestingness-desc&format=json&license=1,2,3,4,5,6,7&privacy_filter=1&api_key="+ Config.CONSUMER_KEY+"&nojsoncallback=1")
    PhotosResponse getPopularPhotos(@Query("page") int page);

    @GET("/?&method=flickr.people.getPhotos&per_page=5&format=json&privacy_filter=1&api_key="+Config.CONSUMER_KEY+"&nojsoncallback=1")
    PhotosResponse getPopularPhotosByUser(@Query("page") int page);

    @GET("/?&method=flickr.people.getPhotos&per_page=5&format=json&privacy_filter=1&api_key="+Config.CONSUMER_KEY+"&nojsoncallback=1")
    void getPopularPhotosByUser(@Query("page") int page, Callback<PhotosResponse> callback);

    @GET("/?&method=flickr.people.getInfo&format=json&api_key="+Config.CONSUMER_KEY+"&nojsoncallback=1")
    void getUser(@Query("user_id") String owner, Callback<UserResponse> callback);

    @GET("/?&method=flickr.photos.search&per_page=5&safe_search=1&sort=interestingness-desc&format=json&license=1,2,3,4,5,6,7&privacy_filter=1&api_key="+ Config.CONSUMER_KEY+"&nojsoncallback=1")
    void getPopularPhotos(@Query("page") int page, Callback<PhotosResponse> callback);

    @GET("/?&method=flickr.photos.getSizes&format=json&api_key="+Config.CONSUMER_KEY+"&nojsoncallback=1")
    SizeResponse getSize(@Query("photo_id") String photo);

    @GET("/?&method=flickr.photos.getSizes&format=json&api_key="+Config.CONSUMER_KEY+"&nojsoncallback=1")
    void getSize(@Query("photo_id") String photo, Callback<SizeResponse> callback);

    @GET("/?&method=flickr.people.findByUsername&format=json&api_key="+Config.CONSUMER_KEY+"&nojsoncallback=1")
    void getUserByName(@Query("username") String user, Callback<UserByNameResponse> callback);

    public static class SizeResponse {
        public Sizes sizes;
    }

    public static class UserByNameResponse {
        public UserResult user;
    }

    public static class UserResponse {
        public Person person;
    }

    public static class PhotosResponse {
        public Response photos;
    }

    public static class Sizes {
        public List<Size> size;
    }

    public static class Size {
        public int height;
        public String source;
    }

    public static class Person {
        public Username realname;
        public Username username;
    }

    public static class Username {
        public String _content;
    }
    public static class Response {
        public List<Photo> photo;
        public int pages;
        public int total;

    }
    public static class Photo {
        public String id;
        public  String title;
        public String owner;
    }


    public static class UserResult {
        public String nsid;
    }

}
