package com.npi.muzeiflickr.ui.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    private ActionMode mActionMode;
    private TextView mEmptyList;


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
        mEmptyList = (TextView) findViewById(R.id.empty_list);

        mAttacher = new PhotoViewAttacher(mPreview);
        mAttacher.setOnPhotoTapListener(new PhotoViewAttacher.OnPhotoTapListener() {
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

        if (favorites.size() == 0) {
            showEmptyList();
        }

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
            public void onItemSelected(int position, boolean force) {
                if (force || mActionMode != null) {
                    mFavoriteList.setItemChecked(position + 1, !mFavoriteList.isItemChecked(position + 1));
                    if (mFavoriteList.getCheckedItemCount() == 0) {
                        mActionMode.finish();
                        return;
                    }
                } else {
                    return;
                }
                if (mActionMode != null) {
                    return;
                }

                // Start the CAB using the ActionMode.Callback defined above
                mActionMode = startActionMode(mActionModeCallback);
            }


        });
        OnDismissCallback callback = new OnDismissCallback() {
            @Override
            public void onDismiss(AbsListView listView, int[] reverseSortedPositions) {
                for (int position : reverseSortedPositions) {
                    mFavoriteAdapter.get(position).delete();
                    mFavoriteAdapter.remove(position);
                    if (mFavoriteAdapter.getCount() == 0) {
                        showEmptyList();
                    }
                }
            }
        };
        mSwingRightInAnimationAdapter = new AnimateDismissAdapter(mFavoriteAdapter, callback);


        mSwingRightInAnimationAdapter.setAbsListView(mFavoriteList);
        mFavoriteList.setAdapter(mSwingRightInAnimationAdapter);

        mFavoriteList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);


    }

    private void showEmptyList() {
        mEmptyList.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }

        return true;
    }

    private void uploadFavorites(final Photo photo) {
        FlickrService.getInstance(this).addFavorite(photo.photoId, new FlickrServiceInterface.IRequestListener<FlickrApiData.AddFavoriteResponse>() {
            @Override
            public void onFailure() {

            }

            @Override
            public void onSuccess(FlickrApiData.AddFavoriteResponse response) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Result: " + response.stat);
                mFavoriteAdapter.setFavorite(photo.photoId);
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

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.favorite_action_mode, menu);
            if (!mFavoriteAdapter.isLogged()) {
                menu.findItem(R.id.menu_send_flickr).setVisible(false);
            }
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_delete:

                    SparseBooleanArray checkedItemPositions = mFavoriteList.getCheckedItemPositions();
                    List<Integer> positions = new ArrayList<Integer>();
                    for (int i = 0; i < checkedItemPositions.size(); i++) {
                        int key = checkedItemPositions.keyAt(i);
                        boolean value = checkedItemPositions.get(key);
                        if (value) {
                            mFavoriteList.setItemChecked(key, false);
                            positions.add(key - 1);
                        }
                    }
                    mSwingRightInAnimationAdapter.animateDismiss(positions);

                    mode.finish();
                    return true;
                case R.id.menu_select_all:

                    boolean select = true;
                    if (mFavoriteList.getCheckedItemCount() == mFavoriteAdapter.size()) {
                        select = false;
                    }

                    for (int i = 0; i < mFavoriteAdapter.size(); i++) {
                        mFavoriteList.setItemChecked(i + 1, select);
                    }

                    if (!select) mode.finish();
                    return true;
                case R.id.menu_send_flickr:

                    checkedItemPositions = mFavoriteList.getCheckedItemPositions();
                    for (int i = 0; i < checkedItemPositions.size(); i++) {
                        int key = checkedItemPositions.keyAt(i);
                        boolean value = checkedItemPositions.get(key);
                        if (value) {
                            uploadFavorites(mFavoriteAdapter.getPhoto(key - 1));
                        }
                    }


                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }
    };

    @Override
    public void onBackPressed() {
        if (mPreview.getVisibility() == View.VISIBLE) {
            showList();
        } else {
            super.onBackPressed();
        }
    }
}
