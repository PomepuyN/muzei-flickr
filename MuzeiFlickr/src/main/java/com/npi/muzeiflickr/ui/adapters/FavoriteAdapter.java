package com.npi.muzeiflickr.ui.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nhaarman.listviewanimations.itemmanipulation.ExpandableListItemAdapter;
import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.db.Favorite;
import com.npi.muzeiflickr.db.Photo;
import com.npi.muzeiflickr.db.SourceTypeEnum;
import com.npi.muzeiflickr.network.FlickrApiData;
import com.npi.muzeiflickr.network.FlickrService;
import com.npi.muzeiflickr.network.FlickrServiceInterface;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by nicolas on 17/02/14.
 */
public class FavoriteAdapter extends ExpandableListItemAdapter<Favorite> {

    private static final String TAG = FavoriteAdapter.class.getSimpleName();
    private final List<Favorite> mFavorites;
    private final List<String> mFlickrFavorites = new ArrayList<String>();
    private final Context mContext;
    private final FavoriteAdapterListener mFavoriteAdapterListener;
    private final HashMap<String, Photo> mPhotos;

    public FavoriteAdapter(Context context, List<Favorite> items, FavoriteAdapterListener listener) {
        super(context, R.layout.favorite_list_item, R.id.title_view, R.id.expandable_content, items);
        mFavorites = items;
        mPhotos = new HashMap<String, Photo>();
        mContext = context;
        mFavoriteAdapterListener = listener;

        FlickrService.getInstance(context).getFavorites(new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
            @Override
            public void onFailure() {

            }

            @Override
            public void onSuccess(FlickrApiData.PhotosResponse response) {
                for (FlickrApiData.Photo photo : response.photos.photo) {
                    mFlickrFavorites.add(photo.id);
                    if (BuildConfig.DEBUG) Log.d(TAG, "Found fav: " + photo.id);
                    if (mPhotos.get(photo.id) != null) {
                        mPhotos.get(photo.id).isFavorite = true;
                    }
                }


                notifyDataSetChanged();
            }
        });

    }

    @Override
    public int getCount() {
        return mFavorites.size();
    }

    @Override
    public Favorite getItem(int position) {
        return mFavorites.get(position);
    }


    @Override
    public View getTitleView(final int position, View convertView, ViewGroup parent) {
        final LocalHolder holder;
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.favorite_list_item_title, null);
            holder = new LocalHolder();
            holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.author = (TextView) convertView.findViewById(R.id.author);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);
            holder.synched = (ImageView) convertView.findViewById(R.id.synched);
            convertView.setTag(holder);
        } else {
            holder = (LocalHolder) convertView.getTag();
        }

        final Favorite item = getItem(position);

        Photo photo = mPhotos.get(item.photoId);
        if (photo != null) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Item Found");
            holder.title.setText(photo.title);
            holder.author.setText(photo.owner);

            if (photo.isFavorite) {
                holder.synched.setVisibility(View.VISIBLE);
            } else {
                holder.synched.setVisibility(View.GONE);

            }
            holder.thumbnail.setBackground(null);

            Picasso.with(mContext).load(photo.thumbnail).into(holder.thumbnail);

        } else {

            holder.title.setText("");
            holder.author.setText("");
            holder.thumbnail.setImageDrawable(null);
            holder.thumbnail.setBackground(mContext.getResources().getDrawable(R.drawable.widget_background));
            holder.synched.setVisibility(View.GONE);


            if (BuildConfig.DEBUG) Log.d(TAG, "Item Loading");
            //Load content
            FlickrService.getInstance(mContext).getPhotoInfo(item.photoId, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotoResponse>() {
                @Override
                public void onFailure() {

                }

                @Override
                public void onSuccess(final FlickrApiData.PhotoResponse photo) {
                    FlickrService.getInstance(mContext).getSize(photo.photo.id, new FlickrServiceInterface.IRequestListener<FlickrApiData.SizeResponse>() {
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
                            FlickrApiData.Size thumbnailSize = null;
                            FlickrApiData.Size largestSize = null;
                            for (FlickrApiData.Size size : responseSize.sizes.size) {
                                if (size.height == 150) {
                                    thumbnailSize = size;
                                }
                                if (size.height >= 640 && largestSize == null) {
                                    largestSize = size;
                                }
                            }

                            if (largestSize != null) {

                                FlickrApiData.UserResponse responseUser = null;
                                //Request user info (for the title)
                                final FlickrApiData.Size finalLargestSize = largestSize;

                                final FlickrApiData.Size finalThumbnailSize = thumbnailSize;
                                FlickrService.getInstance(mContext).getUser(photo.photo.owner.nsid, new FlickrServiceInterface.IRequestListener<FlickrApiData.UserResponse>() {
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
                                        photoEntity.thumbnail = finalThumbnailSize.source;
                                        photoEntity.title = photo.photo.title._content;
                                        photoEntity.photoId = photo.photo.id;
                                        photoEntity.owner = photo.photo.owner.username;
                                        photoEntity.sourceType = SourceTypeEnum.FAVORITES.ordinal();
                                        photoEntity.sourceId = item.getId();

                                        for (String photoId:mFlickrFavorites) {
                                            if (photoId.equals(photoEntity.photoId)) {
                                                photoEntity.isFavorite = true;
                                            }
                                        }

                                        holder.title.setText(photoEntity.title);
                                        holder.author.setText(photoEntity.owner);
                                        mPhotos.put(item.photoId, photoEntity);

                                        if (photoEntity.isFavorite) {
                                            holder.synched.setVisibility(View.VISIBLE);
                                        } else {
                                            holder.synched.setVisibility(View.GONE);

                                        }
                                        holder.thumbnail.setBackground(mContext.getResources().getDrawable(R.drawable.widget_background));



                                        Picasso.with(mContext).load(photoEntity.thumbnail).into(holder.thumbnail);
                                    }
                                });

                            }
                        }
                    });
                }
            });
        }


        return convertView;
    }

    @Override
    public View getContentView(final int position, View convertView, ViewGroup parent) {

        final LocalContentHolder holder;
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.favorite_list_item_content, null);
            holder = new LocalContentHolder();
            holder.download = (ImageButton) convertView.findViewById(R.id.action_download);
            holder.picture = (ImageButton) convertView.findViewById(R.id.action_picture);
            holder.remove = (ImageButton) convertView.findViewById(R.id.action_remove);
            holder.upload = (ImageButton) convertView.findViewById(R.id.action_upload);
            convertView.setTag(holder);
        } else {
            holder = (LocalContentHolder) convertView.getTag();
        }

        final Favorite favorite = getItem(position);

        if (mPhotos.get(favorite.photoId) != null && mPhotos.get(favorite.photoId).isFavorite) {
            holder.upload.setVisibility(View.GONE);
        } else {
            holder.upload.setVisibility(View.VISIBLE);
            holder.upload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Photo item = mPhotos.get(favorite.photoId);
                    if (item != null) {
                        mFavoriteAdapterListener.onUploadPhoto(item);
                    }
                }
            });
        }

        holder.download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Photo item = mPhotos.get(favorite.photoId);
                if (item != null) {
                    mFavoriteAdapterListener.onDownloadPhoto(item);
                }
            }
        });
        holder.picture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Photo item = mPhotos.get(favorite.photoId);
                if (item != null) {
                    mFavoriteAdapterListener.onShowPhoto(item);
                } else {
                    Log.w(TAG, "Unable to find image");
                }
            }
        });
        holder.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Favorite item = getItem(position);
                mPhotos.remove(item.photoId);
                item.delete();
                mFavoriteAdapterListener.onRemovePhoto(position);
            }
        });


        return convertView;

    }


    static class LocalHolder {
        public ImageView thumbnail;
        public TextView title;
        public TextView author;
        public ImageView synched;
    }

    static class LocalContentHolder {
        public ImageButton picture;
        public ImageButton download;
        public ImageButton upload;
        public ImageButton remove;
    }

    public interface FavoriteAdapterListener {
        void onDownloadPhoto(Photo photo);

        void onShowPhoto(Photo photo);

        void onUploadPhoto(Photo photo);

        void onRemovePhoto(int position);
    }
}
