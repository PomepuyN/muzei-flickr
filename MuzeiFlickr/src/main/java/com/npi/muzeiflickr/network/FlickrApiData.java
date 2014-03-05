package com.npi.muzeiflickr.network;

import com.npi.muzeiflickr.data.ImportableData;

import java.io.Serializable;
import java.util.List;

/**
 * Created by nicolas on 19/02/14.
 */
public class FlickrApiData {

    public static class SizeResponse {
        public Sizes sizes;
    }

    public static class UserLoginResponse {
        public UserLoginResult user;
    }

    public static class UserByNameResponse {
        public UserResult user;
    }

    public static class UserResponse {
        public Person person;
    }

    public static class PhotosResponse implements ParsablePhotosResponse {
        public Response photos;

        @Override
        public Response getResponse() {
            return photos;
        }
    }


    public static class GroupsResponse {
        public Groups groups;
    }

    public static class Sizes {
        public List<Size> size;
    }

    public static class Size {
        public int height;
        public String source;
    }

    public static class Person {
        public ContentString realname;
        public ContentString username;
        public ContentString profileurl;
    }

    public static class ContentString {
        public String _content;
    }

    public static class Response {
        public List<Photo> photo;
        public int pages;
        public int total;
        public String owner;

    }

    public static class Photo {
        public String id;
        public String title;
        public String owner;
    }


    public static class UserResult {
        public String nsid;
    }

    public static class UserLoginResult {
        public ContentString username;
        public String id;
    }

    public static class Groups {
        public List<Group> group;
    }

    public static class Group implements Serializable, ImportableData {
        public String nsid;
        public String name;

        @Override
        public String getName() {
            return name;
        }
    }

    public static class ContactResponse {
        public Contacts contacts;
    }

    public static class Contacts {
        public List<Contact> contact;
    }

    public static class Contact implements ImportableData {
        public String nsid;
        public String username;

        @Override
        public String getName() {
            return username;
        }
    }

    public static class PhotoResponse {
        public PhotoInfo photo;
    }

    public static class PhotoInfo {
        public String id;
        public Title title;
        public Owner owner;
    }

    public static class Title {
        public String _content;
    }

    public static class Owner {
        public String nsid;
        public String username;
    }

    public static class AddFavoriteResponse {
        public String stat;
    }

    public static class GroupUrlResponse {
        public GroupUrl group;
    }

    public static class GroupUrl {
        public String url;
    }


    public static class PhotosSetsResponse  {
        public PhotoSets photosets;

    }

    public static class PhotoSets  {
        public List<PhotoSet> photoset;
    }
    public static class PhotoSet implements ImportableData {
        public String id;
        public String photos;
        public ContentString title;

        @Override
        public String getName() {
            return title._content;
        }
    }

    public static class SetPhotosResponse implements ParsablePhotosResponse{
        public Response photoset;

        @Override
        public Response getResponse() {
            return photoset;
        }
    }

    public interface ParsablePhotosResponse {
        Response getResponse();
    }
}
