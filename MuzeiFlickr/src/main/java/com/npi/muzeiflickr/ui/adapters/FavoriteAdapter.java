package com.npi.muzeiflickr.ui.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nhaarman.listviewanimations.ArrayAdapter;
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

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

/**
 * Created by nicolas on 17/02/14.
 */
public class FavoriteAdapter extends ArrayAdapter<Favorite> {

    private static final String TAG = FavoriteAdapter.class.getSimpleName();
    private final List<Favorite> mFavorites;
    private final List<String> mFlickrFavorites = new ArrayList<String>();
    private final Context mContext;
    private final FavoriteAdapterListener mFavoriteAdapterListener;
    private boolean mIsLogged;

    private final HashMap<String, Photo> mPhotos;
    private final View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {

            int[] pos = new int[2];
            v.getLocationInWindow(pos);

            String contentDesc = v.getContentDescription().toString();
            Toast t = Toast.makeText(mContext, contentDesc, Toast.LENGTH_SHORT);
            t.show();
            t.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, pos[1] + (v.getHeight() / 2));

            return true;
        }


    };

    public FavoriteAdapter(Context context, List<Favorite> items, FavoriteAdapterListener listener) {
        super(items);
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
                if (response == null || response.photos == null) {
                    mIsLogged = false;
                    return;
                }
                mIsLogged = true;
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        final LocalHolder holder;
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.favorite_list_item_title, null);
            holder = new LocalHolder();
            holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.author = (TextView) convertView.findViewById(R.id.author);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);
            holder.synched = (ImageView) convertView.findViewById(R.id.synched);
            holder.download = (ImageButton) convertView.findViewById(R.id.action_download);
            holder.picture = (ImageButton) convertView.findViewById(R.id.action_picture);
            holder.upload = (ImageButton) convertView.findViewById(R.id.action_upload);
            holder.progressBar = (SmoothProgressBar) convertView.findViewById(R.id.progress);
            holder.container = (RelativeLayout) convertView.findViewById(R.id.item_container);
            convertView.setTag(holder);
        } else {
            holder = (LocalHolder) convertView.getTag();
        }

        final Favorite favorite = getItem(position);

        holder.thumbnail.setTag(favorite.photoId);


        Photo photo = mPhotos.get(favorite.photoId);
        if (photo != null) {
            populateViews(photo, holder, favorite, position);

        } else {

            holder.title.setText("");
            holder.author.setText("");
            holder.thumbnail.setImageDrawable(null);
//            holder.thumbnail.setBackground(mContext.getResources().getDrawable(R.drawable.widget_background));
            holder.synched.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.VISIBLE);


            if (BuildConfig.DEBUG) Log.d(TAG, "Item Loading");
            //Load content
            FlickrService.getInstance(mContext).getPhotoInfo(favorite.photoId, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotoResponse>() {
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
                                        photoEntity.sourceId = favorite.getId();

                                        for (String photoId : mFlickrFavorites) {
                                            if (photoId.equals(photoEntity.photoId)) {
                                                photoEntity.isFavorite = true;
                                            }
                                        }
                                        mPhotos.put(favorite.photoId, photoEntity);

                                        if (!holder.thumbnail.getTag().equals(favorite.photoId)) {
                                            if (BuildConfig.DEBUG)
                                                Log.d(TAG, "POsition invalidated for " + photoEntity.title + " in position: " + position);
                                        } else {
                                            if (BuildConfig.DEBUG)
                                                Log.d(TAG, "Loading: " + photoEntity.title + " in position: " + position);
                                            populateViews(photoEntity, holder, favorite, position);
                                        }
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

    private void populateViews(Photo photoEntity, LocalHolder holder, final Favorite favorite, final int position) {
        holder.title.setText(photoEntity.title);
        holder.author.setText(photoEntity.owner);


        if (!mIsLogged) {
            holder.synched.setVisibility(View.GONE);
            holder.upload.setVisibility(View.GONE);
        } else if (photoEntity.isFavorite) {
            holder.synched.setVisibility(View.VISIBLE);
            holder.upload.setVisibility(View.GONE);
        } else {
            holder.synched.setVisibility(View.GONE);
            holder.upload.setVisibility(View.VISIBLE);

        }

        holder.upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Photo item = mPhotos.get(favorite.photoId);
                if (item != null) {
                    mFavoriteAdapterListener.onUploadPhoto(item);
                }
            }
        });

        holder.upload.setOnLongClickListener(mLongClickListener);

        holder.download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Photo item = mPhotos.get(favorite.photoId);
                if (item != null) {
                    mFavoriteAdapterListener.onDownloadPhoto(item);
                }
            }
        });
        holder.download.setOnLongClickListener(mLongClickListener);

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
        holder.picture.setOnLongClickListener(mLongClickListener);

        holder.picture.setVisibility(View.VISIBLE);
        holder.download.setVisibility(View.VISIBLE);
        holder.progressBar.setVisibility(View.GONE);

        holder.thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFavoriteAdapterListener.onItemSelected(position, true);
            }
        });
        holder.thumbnail.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

        holder.container.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mFavoriteAdapterListener.onItemSelected(position, true);
                return true;
            }
        });

        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFavoriteAdapterListener.onItemSelected(position, false);
            }
        });


        Picasso.with(mContext).load(photoEntity.thumbnail).into(holder.thumbnail);
    }


    @Override
    public int getCount() {
        return mFavorites.size();
    }

    @Override
    public Favorite getItem(int position) {
        return mFavorites.get(position);
    }

    public void setFavorite(String photoId) {
        mPhotos.get(photoId).isFavorite = true;
    }

    public Photo getPhoto(int position) {
        return mPhotos.get(get(position).photoId);
    }

    public boolean isLogged() {
        return mIsLogged;
    }


    static class LocalHolder {
        public ImageView thumbnail;
        public TextView title;
        public TextView author;
        public ImageView synched;
        public ImageButton download;
        public ImageButton picture;
        public ImageButton upload;
        public SmoothProgressBar progressBar;
        public RelativeLayout container;
    }


    public interface FavoriteAdapterListener {
        void onDownloadPhoto(Photo photo);

        void onShowPhoto(Photo photo);

        void onUploadPhoto(Photo photo);

        void onItemSelected(int position, boolean force);

    }
}
