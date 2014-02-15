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

package com.npi.muzeiflickr;

import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

interface FlickrService {
    @GET("/?&method=flickr.photos.search&per_page=5&safe_search=1&sort=interestingness-desc&format=json&license=1,2,3,4,5,6,7&privacy_filter=1&api_key="+Config.CONSUMER_KEY+"&nojsoncallback=1")
    PhotosResponse getPopularPhotos(@Query("page") int page);

    @GET("/?&method=flickr.people.getPhotos&per_page=5&format=json&privacy_filter=1&api_key="+Config.CONSUMER_KEY+"&nojsoncallback=1")
    PhotosResponse getPopularPhotosByUser(@Query("page") int page);

    @GET("/?&method=flickr.people.getInfo&format=json&api_key="+Config.CONSUMER_KEY+"&nojsoncallback=1")
    UserResponse getUser(@Query("user_id") String owner);

    @GET("/?&method=flickr.photos.getSizes&format=json&api_key="+Config.CONSUMER_KEY+"&nojsoncallback=1")
    SizeResponse getSize(@Query("photo_id") String photo);

    @GET("/?&method=flickr.people.findByUsername&format=json&api_key="+Config.CONSUMER_KEY+"&nojsoncallback=1")
    void getUserByName(@Query("username") String user, Callback<UserByNameResponse> callback);

    static class SizeResponse {
        Sizes sizes;
    }

    static class UserByNameResponse {
        UserResult user;
    }

    static class UserResponse {
        Person person;
    }

    static class PhotosResponse {
        Response photos;
    }

    static class Sizes {
        List<Size> size;
    }

    static class Size {
        int height;
        String source;
    }

    static class Person {
        Username realname;
        Username username;
    }

    static class Username {
        String _content;
    }
    static class Response {
        List<Photo> photo;

    }
    static class Photo {
        String id;
        String title;
        public String owner;
    }


    static class UserResult {
        String nsid;
    }

}
