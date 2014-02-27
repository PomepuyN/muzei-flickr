package com.npi.muzeiflickr.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.nhaarman.listviewanimations.itemmanipulation.AnimateDismissAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.db.Favorite;
import com.npi.muzeiflickr.db.Photo;
import com.npi.muzeiflickr.network.FlickrApiData;
import com.npi.muzeiflickr.network.FlickrService;
import com.npi.muzeiflickr.network.FlickrServiceInterface;
import com.npi.muzeiflickr.ui.adapters.FavoriteAdapter;
import com.npi.muzeiflickr.utils.Utils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by nicolas on 26/02/14.
 */
public class FavoritesActivity extends Activity {
    private static final String TAG = FavoritesActivity.class.getSimpleName();
    private ListView mFavoriteList;
    private FavoriteAdapter mFavoriteAdapter;
    private ArrayList<Photo> mFavoritesPhotos = new ArrayList<Photo>();
    private PhotoView mPreview;
    PhotoViewAttacher mAttacher;
    private ProgressBar mProgressBar;
    private AnimateDismissAdapter mSwingRightInAnimationAdapter;


    public static void launchActivity(Context context) {
        Intent intent = new Intent(context, FavoritesActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.favorites_activity);

        mFavoriteList = (ListView) findViewById(android.R.id.list);
        mPreview = (PhotoView) findViewById(R.id.preview);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);



        mAttacher = new PhotoViewAttacher(mPreview);
        mAttacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener(){
            @Override
            public void onPhotoTap(View view, float x, float y) {
                showList();
            }
        });

        View view = new View(this);
        final TypedArray styledAttributes = getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        int actionBarSize = (int) styledAttributes.getDimension(0, 0);
        view.setMinimumHeight(actionBarSize);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setIcon(null);
        getActionBar().setDisplayShowHomeEnabled(false);

        mFavoriteList.addHeaderView(view);
        mFavoriteList.setOnScrollListener(new AbsListView.OnScrollListener() {
            public int mLastFirstVisibleItem = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (view.getId() == mFavoriteList.getId()) {
                    final int currentFirstVisibleItem = mFavoriteList.getFirstVisiblePosition();

                    if (currentFirstVisibleItem > mLastFirstVisibleItem) {
                        getActionBar().hide();
                    } else if (currentFirstVisibleItem < mLastFirstVisibleItem) {
                        getActionBar().show();
                    }

                    mLastFirstVisibleItem = currentFirstVisibleItem;
                }

            }


        });


        final List<Favorite> favorites = Favorite.listAll(Favorite.class);

        mFavoriteAdapter = new FavoriteAdapter(this, favorites, new FavoriteAdapter.FavoriteAdapterListener() {
            @Override
            public void onDownloadPhoto(final Photo photo) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.downloadImage(FavoritesActivity.this, photo.source, photo.title, photo.userName);
                    }
                }).start();


            }

            @Override
            public void onShowPhoto(Photo photo) {
                Picasso.with(FavoritesActivity.this).load(photo.source).into(mPreview, new Callback() {
                    @Override
                    public void onSuccess() {
                        mProgressBar.setVisibility(View.GONE);

                    }

                    @Override
                    public void onError() {
                        mProgressBar.setVisibility(View.GONE);
                    }
                });
                hideList();
            }

            @Override
            public void onUploadPhoto(Photo photo) {

                uploadFavorites(photo);

            }

            @Override
            public void onRemovePhoto(int position) {

                mSwingRightInAnimationAdapter.animateDismiss(position);

            }

        });
        mFavoriteAdapter.setLimit(1);
        mSwingRightInAnimationAdapter = new AnimateDismissAdapter(mFavoriteAdapter, new OnDismissCallback() {
            @Override
            public void onDismiss(AbsListView listView, int[] reverseSortedPositions) {
                for (int position : reverseSortedPositions) {
                    mFavoriteAdapter.collapse(position);
                    mFavoriteAdapter.remove(position);
                }
            }
        });
        mFavoriteList.setAdapter(mSwingRightInAnimationAdapter);
        mSwingRightInAnimationAdapter.setAbsListView(mFavoriteList);


        //Getting all the photos


    }

    private void uploadFavorites(Photo photo) {
        FlickrService.getInstance(this).addFavorite(photo.photoId, new FlickrServiceInterface.IRequestListener<FlickrApiData.AddFavoriteResponse>() {
            @Override
            public void onFailure() {

            }

            @Override
            public void onSuccess(FlickrApiData.AddFavoriteResponse response) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Result: " + response.stat);
                mFavoriteAdapter.notifyDataSetChanged();
            }
        });
    }

    private void hideList() {
        mPreview.setVisibility(View.VISIBLE);
        mFavoriteList.setVisibility(View.GONE);
        getActionBar().hide();
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void showList() {
        mPreview.setVisibility(View.GONE);
        mFavoriteList.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
        getActionBar().show();
    }

    @Override
    public void onBackPressed() {
        if (mPreview.getVisibility() == View.VISIBLE) {
            showList();
        } else {
        super.onBackPressed();
        }
    }
}
