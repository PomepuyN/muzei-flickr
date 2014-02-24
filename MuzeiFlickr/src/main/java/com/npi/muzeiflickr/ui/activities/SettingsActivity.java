package com.npi.muzeiflickr.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.mobeta.android.dslv.DragSortListView;
import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.FlickrMuzeiApplication;
import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.api.FlickrApi;
import com.npi.muzeiflickr.api.FlickrSource;
import com.npi.muzeiflickr.data.PreferenceKeys;
import com.npi.muzeiflickr.db.FGroup;
import com.npi.muzeiflickr.db.Favorite;
import com.npi.muzeiflickr.db.FavoriteSource;
import com.npi.muzeiflickr.db.Photo;
import com.npi.muzeiflickr.db.RequestData;
import com.npi.muzeiflickr.db.Search;
import com.npi.muzeiflickr.db.Tag;
import com.npi.muzeiflickr.db.User;
import com.npi.muzeiflickr.network.FlickrApiData;
import com.npi.muzeiflickr.network.FlickrService;
import com.npi.muzeiflickr.network.FlickrServiceInterface;
import com.npi.muzeiflickr.ui.adapters.RequestAdapter;
import com.npi.muzeiflickr.ui.adapters.SourceSpinnerAdapter;
import com.npi.muzeiflickr.ui.dialogs.GroupChooserDialog;
import com.npi.muzeiflickr.ui.dialogs.GroupImportDialog;
import com.npi.muzeiflickr.ui.dialogs.UserImportDialog;
import com.npi.muzeiflickr.ui.hhmmpicker.HHmsPickerBuilder;
import com.npi.muzeiflickr.ui.hhmmpicker.HHmsPickerDialogFragment;
import com.npi.muzeiflickr.utils.Config;
import com.npi.muzeiflickr.utils.Utils;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

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
 * Main settings activity
 */
public class SettingsActivity extends FragmentActivity implements HHmsPickerDialogFragment.HHmsPickerDialogHandler, GroupChooserDialog.ChooseGroupDialogListener, UserImportDialog.ImportDialogListener {
    public static final String PREFS_NAME = "main_prefs";
    private static final String TAG = SettingsActivity.class.getSimpleName();
    private TextView mRefreshRate;

    private DragSortListView mRequestList;
    private RequestAdapter mRequestAdapter;

    private RequestData mLastDeletedItem;
    private RelativeLayout mUndoContainer;
    private TextView mLastDeletedItemText;
    private DragSortListView.RemoveListener onRemove =
            new DragSortListView.RemoveListener() {
                @Override
                public void remove(int which) {


                    if (BuildConfig.DEBUG) Log.d(TAG, "Removing item");

                    RequestData item = mRequestAdapter.getItem(which);
                    managePhotoFromSourceDeletion();
                    mRequestAdapter.remove(item);
                    if (item instanceof User) {
                        ((User) item).delete();
                    } else if (item instanceof Search) {
                        ((Search) item).delete();
                    } else if (item instanceof Tag) {
                        ((Tag) item).delete();
                    } else if (item instanceof FGroup) {
                        ((FGroup) item).delete();
                    }else if (item instanceof FavoriteSource) {
                        FlickrMuzeiApplication.getEditor().putBoolean(PreferenceKeys.USE_FAVORITES, false);
                        FlickrMuzeiApplication.getEditor().commit();
                    }
                    mLastDeletedItem = item;
                    mRequestAdapter.notifyDataSetChanged();
                    mLastDeletedItemText.setText(item.getTitle());
                    mUndoContainer.setVisibility(View.VISIBLE);
                }
            };
    private UserInfoListener<FGroup> mCurrentGroupListener;
    private boolean mSourceAdded = false;
    private WebView oauthWebView;
    private Token mRequestToken;
    private TextView mLoginShortcut;

    private void managePhotoFromSourceDeletion() {
        if (mLastDeletedItem != null) {
            //Find all photos of the source
            Photo.deleteAll(Photo.class, "source_id = ? and source_type = ?", String.valueOf(mLastDeletedItem.getSourceId()), String.valueOf(mLastDeletedItem.getSourceType()));
        }
    }

    //Needed for Calligraphy
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new CalligraphyContextWrapper(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault("");

        setContentView(R.layout.settings_activity);

        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        final SharedPreferences.Editor editor = settings.edit();

        //Find views

        Switch wifiOnly = (Switch) findViewById(R.id.wifi_only);
        Switch randomize = (Switch) findViewById(R.id.randomize);
        mRefreshRate = (TextView) findViewById(R.id.refresh_rate);
        TextView aboutShortcut = (TextView) findViewById(R.id.about);
        mLoginShortcut = (TextView) findViewById(R.id.login);
        mRequestList = (DragSortListView) findViewById(R.id.content_list);
        mUndoContainer = (RelativeLayout) findViewById(R.id.undo_container);
        mLastDeletedItemText = (TextView) findViewById(R.id.last_deleted_item);
        TextView mLastDeletedUndo = (TextView) findViewById(R.id.last_deleted_undo);
        oauthWebView = (WebView) findViewById(R.id.oauth_webview);

        List<RequestData> items = getRequestDatas();


        mRequestAdapter = new RequestAdapter(this, items);

        final View footerView = getLayoutInflater().inflate(R.layout.list_footer, null);
        mRequestList.addFooterView(footerView);

        mRequestList.setAdapter(mRequestAdapter);

        mRequestList.setRemoveListener(onRemove);

        populateFooter(footerView);

        //Wifi status and setting
        randomize.setChecked(settings.getBoolean(PreferenceKeys.RANDOMIZE, false));

        wifiOnly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean(PreferenceKeys.RANDOMIZE, isChecked);
                editor.commit();

            }
        });

        //Wifi status and setting
        wifiOnly.setChecked(settings.getBoolean(PreferenceKeys.WIFI_ONLY, false));

        wifiOnly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean(PreferenceKeys.WIFI_ONLY, isChecked);
                editor.commit();
            }
        });


        //Other settings population
        int refreshRate = settings.getInt(PreferenceKeys.REFRESH_TIME, FlickrSource.DEFAULT_REFRESH_TIME);

        mRefreshRate.setText(Utils.convertDurationtoString(refreshRate));
        mRefreshRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HHmsPickerBuilder hpb = new HHmsPickerBuilder()
                        .setFragmentManager(getSupportFragmentManager())
                        .setStyleResId(R.style.MyCustomBetterPickerTheme);
                hpb.show();
            }
        });


        aboutShortcut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AboutActivity.launchActivity(SettingsActivity.this);
            }
        });

        manageLoginClickListener(settings, editor);
        String login = settings.getString(PreferenceKeys.LOGIN_USERNAME, "");
        if (!TextUtils.isEmpty(login)) {
            mLoginShortcut.setText(login);
        }

        mLastDeletedUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastDeletedItem instanceof User) {
                    User user = ((User) mLastDeletedItem);
                    user.setId(null);
                    user.save();
                } else if (mLastDeletedItem instanceof Search) {
                    Search search = ((Search) mLastDeletedItem);
                    search.setId(null);
                    search.save();
                }else if (mLastDeletedItem instanceof FGroup) {
                    FGroup group = ((FGroup) mLastDeletedItem);
                    group.setId(null);
                    group.save();
                }else if (mLastDeletedItem instanceof FavoriteSource) {
                    FlickrMuzeiApplication.getEditor().putBoolean(PreferenceKeys.USE_FAVORITES, true);
                    FlickrMuzeiApplication.getEditor().commit();
                }
                mRequestAdapter.add(mLastDeletedItem);
                mRequestAdapter.notifyDataSetChanged();
                mUndoContainer.setVisibility(View.GONE);
            }
        });

    }

    private void manageLoginClickListener(final SharedPreferences settings, final SharedPreferences.Editor editor) {
        mLoginShortcut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(settings.getString(PreferenceKeys.LOGIN_USERNAME, ""))) {
                    final Handler mHandler = new Handler(Looper.getMainLooper());
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            oAuth(mHandler);
                        }
                    }).start();
                } else {
                    PopupMenu popupmenu = new PopupMenu(SettingsActivity.this, mLoginShortcut);
                    popupmenu.inflate(R.menu.logged_menu);
                    popupmenu.show();
                    popupmenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu menu) {
                            mRequestList.animate().alpha(1F).start();
                        }
                    });
                    popupmenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.menu_contacts:
                                    UserImportDialog.newInstance().show(getFragmentManager(), "UserImportDialog");
                                    break;
                                case R.id.menu_groups:
                                    GroupImportDialog.newInstance().show(getFragmentManager(), "GroupImportDialog");

                                    break;
                                case R.id.menu_favorites:
                                    FlickrService.getInstance(SettingsActivity.this).getFavorites(new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
                                        @Override
                                        public void onFailure() {
                                        }

                                        @Override
                                        public void onSuccess(FlickrApiData.PhotosResponse photosResponse) {
                                            if (photosResponse == null || photosResponse.photos == null || photosResponse.photos.photo == null) {

                                            } else {
                                                List<Favorite> currentFavorites = Favorite.listAll(Favorite.class);
                                                int count = 0;
                                                for (FlickrApiData.Photo photo: photosResponse.photos.photo) {

                                                    boolean found = false;
                                                    for (Favorite favorite:currentFavorites) {
                                                        if (photo.id.equals(favorite.photoId)) {
                                                            found = true;
                                                            break;
                                                        }
                                                    }
                                                    if (!found) {
                                                        Favorite fav = new Favorite();
                                                        fav.photoId = photo.id;
                                                        fav.save();
                                                        count++;
                                                    }
                                                }
                                                Toast.makeText(SettingsActivity.this, count+" favorites have been added", Toast.LENGTH_LONG).show();

                                            }
                                        }
                                    });

                                    break;
                                case R.id.menu_logout:
                                    editor.putString(PreferenceKeys.LOGIN_USERNAME,"");
                                    editor.putString(PreferenceKeys.LOGIN_SECRET,"");
                                    editor.putString(PreferenceKeys.LOGIN_NSID,"");
                                    editor.putString(PreferenceKeys.LOGIN_TOKEN, "");
                                    editor.commit();
                                    mLoginShortcut.setText(getString(R.string.login));
                                    manageLoginClickListener(settings, editor);
                                    break;
                            }
                            mLastDeletedItem = null;
                            mUndoContainer.setVisibility(View.GONE);


                            return false;
                        }
                    });
                    mRequestList.animate().alpha(0.1F).start();
                }
            }
        });
    }


    private void oAuth(Handler mHandler) {
        // Replace these with your own api key and secret
        final String apiKey = Config.CONSUMER_KEY;
        final String apiSecret = Config.CONSUMER_SECRET;
        final OAuthService service = new ServiceBuilder().provider(FlickrApi.class).apiKey(apiKey).apiSecret(apiSecret).callback("flickrmuzei://oauth").build();
        Scanner in = new Scanner(System.in);

        // Obtain the Request Token
        mRequestToken = service.getRequestToken();
        final String authorizationUrl = service.getAuthorizationUrl(mRequestToken);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                oauthWebView.getSettings().setJavaScriptEnabled(true);
                oauthWebView.setWebViewClient(new WebViewClient());
                oauthWebView.setVisibility(View.VISIBLE);
                oauthWebView.loadUrl(authorizationUrl);
                oauthWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        if (url.toLowerCase().startsWith("http") || url.toLowerCase().startsWith("https") || url.toLowerCase().startsWith("file")) {
                            view.loadUrl(url);
                        } else {

                            Uri uri = Uri.parse(url);
                            Log.d(TAG, "callback: " + uri.toString());


                            String verifierS = uri.getQueryParameter("oauth_verifier");
                            final Verifier verifier = new Verifier(verifierS);

                            // Trade the Request Token and Verfier for the Access Token
                            oauthWebView.setVisibility(View.GONE);

                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Token accessToken = service.getAccessToken(mRequestToken, verifier);

                                    final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                                    final SharedPreferences.Editor editor = settings.edit();
                                    editor.putString(PreferenceKeys.LOGIN_TOKEN, accessToken.getToken());
                                    editor.putString(PreferenceKeys.LOGIN_SECRET, accessToken.getSecret());
                                    editor.commit();

                                    FlickrService.getInstance(SettingsActivity.this).invalidateAdapter();

                                    //Get the user information
                                    FlickrService.getInstance(SettingsActivity.this).getLogin(new FlickrServiceInterface.IRequestListener<FlickrApiData.UserLoginResponse>() {
                                        @Override
                                        public void onFailure() {
                                            Log.e(TAG, "Can't get username");

                                        }

                                        @Override
                                        public void onSuccess(FlickrApiData.UserLoginResponse userLoginResponse) {
                                            if (BuildConfig.DEBUG) Log.d(TAG, "Looking for user");


                                            //The user has not been found
                                            if (userLoginResponse == null || userLoginResponse.user == null || userLoginResponse.user.username == null) {
                                                Log.e(TAG, "Can't get username");
                                            } else {
                                                if (BuildConfig.DEBUG) Log.d(TAG, "User found: "+userLoginResponse.user.id);
                                                editor.putString(PreferenceKeys.LOGIN_USERNAME, userLoginResponse.user.username._content);
                                                editor.putString(PreferenceKeys.LOGIN_NSID, userLoginResponse.user.id);
                                                editor.commit();
                                                mLoginShortcut.setText(userLoginResponse.user.username._content);
                                            }


                                        }
                                    });

                                }
                            }).start();

                        }
                        return true;

                    }
                });
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        managePhotoFromSourceDeletion();

        //Launch an update if a source has been added
        if (mSourceAdded) {
            Intent intent = new Intent(FlickrSource.ACTION_RELOAD_SOME_PHOTOS);
            intent.setClass(this, FlickrSource.class);
            startService(intent);
        }
    }

    private void populateFooter(View footerView) {
        final View footerButton =  footerView.findViewById(R.id.list_footer_button);
        final Spinner footerModeChooser = (Spinner) footerView.findViewById(R.id.mode_chooser);
        final RelativeLayout addItemContainer = (RelativeLayout) footerView.findViewById(R.id.new_item_container);
        final ImageButton footerSearchButton = (ImageButton) footerView.findViewById(R.id.footer_search_button);
        final ProgressBar footerProgress = (ProgressBar) footerView.findViewById(R.id.footer_progress);
        final EditText footerTerm = (EditText) footerView.findViewById(R.id.footer_term);

        footerButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int[] pos = new int[2];
                footerButton.getLocationInWindow(pos);

                String contentDesc = footerButton.getContentDescription().toString();
                Toast t = Toast.makeText(SettingsActivity.this, contentDesc, Toast.LENGTH_SHORT);
                t.show();
                t.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, pos[1] + (footerButton.getHeight() / 2));

                return true;
            }
        });

        footerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addItemContainer.animate().alpha(1F);
                footerButton.animate().alpha(0F);
            }
        });


        //Mode spinner management
        ArrayAdapter<CharSequence> adapter = new SourceSpinnerAdapter(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.modes));


        footerModeChooser.setAdapter(adapter);
        footerModeChooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 4) {
                    footerTerm.setVisibility(View.GONE);
                    footerSearchButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_add));
                } else {
                    footerTerm.setVisibility(View.VISIBLE);
                    footerSearchButton.setImageDrawable(getResources().getDrawable(R.drawable.icon_search));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        footerSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchString = footerTerm.getText().toString();
                switch (footerModeChooser.getSelectedItemPosition()) {
                    case 0:

                        //It's a search

                        //Looking for a same existing search
                        List<Search> searchs = Search.listAll(Search.class);
                        for (Search search : searchs) {
                            if (search.getTitle().equals(searchString)) {
                                Toast.makeText(SettingsActivity.this, getString(R.string.search_exists), Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        footerSearchButton.setVisibility(View.GONE);
                        footerProgress.setVisibility(View.VISIBLE);

                        getSearch(searchString, new UserInfoListener<Search>() {
                            @Override
                            public void onSuccess(Search search) {
                                mRequestAdapter.add(search);
                                mRequestAdapter.notifyDataSetChanged();
                                footerSearchButton.setVisibility(View.VISIBLE);
                                footerProgress.setVisibility(View.GONE);
                                footerTerm.setText("");
                                footerModeChooser.setSelection(0);
                                addItemContainer.animate().alpha(0F);
                                footerButton.animate().alpha(1F);
                            }

                            @Override
                            public void onError(String reason) {
                                Toast.makeText(SettingsActivity.this, reason, Toast.LENGTH_LONG).show();
                                footerSearchButton.setVisibility(View.VISIBLE);
                                footerProgress.setVisibility(View.GONE);
                            }
                        });

                        break;
                    case 1:
                        //It's an user

                        //Looking for a same existing search
                        List<User> users = User.listAll(User.class);
                        for (User user : users) {
                            if (user.getTitle().equals(searchString)) {
                                Toast.makeText(SettingsActivity.this, getString(R.string.user_exists), Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        footerSearchButton.setVisibility(View.GONE);
                        footerProgress.setVisibility(View.VISIBLE);

                        getUserId(searchString, new UserInfoListener<User>() {
                            @Override
                            public void onSuccess(User user) {
                                mRequestAdapter.add(user);
                                mRequestAdapter.notifyDataSetChanged();
                                footerSearchButton.setVisibility(View.VISIBLE);
                                footerProgress.setVisibility(View.GONE);
                                footerTerm.setText("");
                                footerModeChooser.setSelection(0);
                                addItemContainer.animate().alpha(0F);
                                footerButton.animate().alpha(1F);
                            }

                            @Override
                            public void onError(String reason) {
                                Toast.makeText(SettingsActivity.this, reason, Toast.LENGTH_LONG).show();
                                footerSearchButton.setVisibility(View.VISIBLE);
                                footerProgress.setVisibility(View.GONE);
                            }
                        });
                        break;
                    case 2:
                        //It's a tag

                        //Looking for a same existing search
                        List<Tag> tags = Tag.listAll(Tag.class);
                        for (Tag tag : tags) {
                            if (tag.getTitle().equals(searchString)) {
                                Toast.makeText(SettingsActivity.this, getString(R.string.user_exists), Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        footerSearchButton.setVisibility(View.GONE);
                        footerProgress.setVisibility(View.VISIBLE);

                        getTag(searchString, new UserInfoListener<Tag>() {
                            @Override
                            public void onSuccess(Tag tag) {
                                mRequestAdapter.add(tag);
                                mRequestAdapter.notifyDataSetChanged();
                                footerSearchButton.setVisibility(View.VISIBLE);
                                footerProgress.setVisibility(View.GONE);
                                footerTerm.setText("");
                                footerModeChooser.setSelection(0);
                                addItemContainer.animate().alpha(0F);
                                footerButton.animate().alpha(1F);
                            }

                            @Override
                            public void onError(String reason) {
                                Toast.makeText(SettingsActivity.this, reason, Toast.LENGTH_LONG).show();
                                footerSearchButton.setVisibility(View.VISIBLE);
                                footerProgress.setVisibility(View.GONE);
                            }
                        });
                        break;

                    case 3:
                        //It's an user

                        //Looking for a same existing search
                        List<FGroup> groups = FGroup.listAll(FGroup.class);
                        for (FGroup group : groups) {
                            if (group.getTitle().equals(searchString)) {
                                Toast.makeText(SettingsActivity.this, getString(R.string.group_exists), Toast.LENGTH_LONG).show();
                                return;
                            }
                        }

                        footerSearchButton.setVisibility(View.GONE);
                        footerProgress.setVisibility(View.VISIBLE);

                        getGroupId(searchString, new UserInfoListener<FGroup>() {
                            @Override
                            public void onSuccess(FGroup group) {
                                mRequestAdapter.add(group);
                                mRequestAdapter.notifyDataSetChanged();
                                footerSearchButton.setVisibility(View.VISIBLE);
                                footerProgress.setVisibility(View.GONE);
                                footerTerm.setText("");
                                footerModeChooser.setSelection(0);
                                addItemContainer.animate().alpha(0F);
                                footerButton.animate().alpha(1F);
                            }

                            @Override
                            public void onError(String reason) {
                                Toast.makeText(SettingsActivity.this, reason, Toast.LENGTH_LONG).show();
                                footerSearchButton.setVisibility(View.VISIBLE);
                                footerProgress.setVisibility(View.GONE);
                            }
                        });
                        break;
                    case 4:
                        FlickrMuzeiApplication.getEditor().putBoolean(PreferenceKeys.USE_FAVORITES, true);
                        FlickrMuzeiApplication.getEditor().commit();
                        mRequestAdapter.add(new FavoriteSource(SettingsActivity.this));
                        mRequestAdapter.notifyDataSetChanged();
                        footerSearchButton.setVisibility(View.VISIBLE);
                        footerProgress.setVisibility(View.GONE);
                        footerTerm.setText("");
                        footerModeChooser.setSelection(0);
                        addItemContainer.animate().alpha(0F);
                        footerButton.animate().alpha(1F);
                        break;
                }
            }
        });
    }

    private void getGroupId(final String search, final UserInfoListener<FGroup> userInfoListener) {
        FlickrService.getInstance(this).getGroups(search, new FlickrServiceInterface.IRequestListener<FlickrApiData.GroupsResponse>() {
            @Override
            public void onFailure() {
                userInfoListener.onError(getString(R.string.network_error));
            }

            @Override
            public void onSuccess(FlickrApiData.GroupsResponse photosResponse) {
                if (photosResponse == null || photosResponse.groups == null || photosResponse.groups.group == null) {
                    userInfoListener.onError(getString(R.string.network_error));
                } else if (photosResponse.groups.group.size() > 0) {
                    GroupChooserDialog.newInstance(new ArrayList<FlickrApiData.Group>(photosResponse.groups.group)).show(getFragmentManager(), "GroupChooserDialog");
                    mCurrentGroupListener = userInfoListener;
                } else {
                    userInfoListener.onError(getString(R.string.no_group_found));

                }
            }
        });
    }

    @Override
    public void onFinishChoosingDialog(final FlickrApiData.Group group) {

        //Get the photos number of the group
        FlickrService.getInstance(this).getGroupPhotos(group.nsid, 0, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
            @Override
            public void onFailure() {
                mCurrentGroupListener.onError(getString(R.string.group_no_photo));
            }

            @Override
            public void onSuccess(FlickrApiData.PhotosResponse photosResponse) {
                if (photosResponse == null || photosResponse.photos == null || photosResponse.photos.photo == null) {
                    mCurrentGroupListener.onError(getString(R.string.network_error));
                } else if (photosResponse.photos.photo.size() > 0) {
                    FGroup groupDB = new FGroup(SettingsActivity.this, group.nsid, group.name, 1, 0, photosResponse.photos.total);
                    groupDB.save();
                    mSourceAdded = true;
                    mCurrentGroupListener.onSuccess(groupDB);
                } else {
                    mCurrentGroupListener.onError(getString(R.string.group_no_photo));

                }
            }
        });


    }


    private void getSearch(final String search, final UserInfoListener<Search> userInfoListener) {


        FlickrService.getInstance(this).getPopularPhotos(search, 0, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
            @Override
            public void onFailure() {
                userInfoListener.onError(getString(R.string.network_error));
            }

            @Override
            public void onSuccess(FlickrApiData.PhotosResponse photosResponse) {
                if (photosResponse == null || photosResponse.photos == null || photosResponse.photos.photo == null) {
                    userInfoListener.onError(getString(R.string.network_error));
                } else if (photosResponse.photos.photo.size() > 0) {
                    Search searchDB = new Search(SettingsActivity.this, search, 1, 0, photosResponse.photos.total);
                    searchDB.save();
                    mSourceAdded = true;
                    userInfoListener.onSuccess(searchDB);
                } else {
                    userInfoListener.onError(getString(R.string.user_no_photo));

                }
            }
        });


    }

    private void getTag(final String search, final UserInfoListener<Tag> userInfoListener) {


        FlickrService.getInstance(this).getPopularPhotosByTag(search, 0, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
            @Override
            public void onFailure() {
                userInfoListener.onError(getString(R.string.network_error));
            }

            @Override
            public void onSuccess(FlickrApiData.PhotosResponse photosResponse) {
                if (photosResponse == null || photosResponse.photos == null || photosResponse.photos.photo == null) {
                    userInfoListener.onError(getString(R.string.network_error));
                } else if (photosResponse.photos.photo.size() > 0) {
                    Tag tagDB = new Tag(SettingsActivity.this, search, 1, 0, photosResponse.photos.total);
                    tagDB.save();
                    mSourceAdded = true;
                    userInfoListener.onSuccess(tagDB);
                } else {
                    userInfoListener.onError(getString(R.string.tag_no_photo));

                }
            }
        });


    }


    /**
     * The duration picker has been closed
     *
     * @param reference
     * @param hours
     * @param minutes
     * @param seconds
     */
    @Override
    public void onDialogHmsSet(int reference, int hours, int minutes, int seconds) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        int duration = hours * 3600000 + minutes * 60000 + seconds * 1000;
        editor.putInt(PreferenceKeys.REFRESH_TIME, duration);
        editor.commit();
        mRefreshRate.setText(Utils.convertDurationtoString(duration));


    }

    /**
     * Determine if a user exists
     *
     * @param user the user to search
     */
    private void getUserId(final String user, final UserInfoListener userInfoListener) {

        FlickrService.getInstance(this).getUserByName(user, new FlickrServiceInterface.IRequestListener<FlickrApiData.UserByNameResponse>() {
            @Override
            public void onFailure() {
                userInfoListener.onError(getString(R.string.network_error));

            }

            @Override
            public void onSuccess(FlickrApiData.UserByNameResponse userByNameResponse) {
                if (BuildConfig.DEBUG) Log.d(TAG, "Looking for user");


                //The user has not been found
                if (userByNameResponse == null || userByNameResponse.user == null || userByNameResponse.user.nsid == null) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "User not found");
                    userInfoListener.onError(getString(R.string.user_not_found));

                    return;
                }


                //The user has been found, we store it
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "User found: " + userByNameResponse.user.nsid + " for " + user);
                }
                final String userId = userByNameResponse.user.nsid;


                //User has been found, let's see if he has photos

                FlickrService.getInstance(SettingsActivity.this).getPopularPhotosByUser(userId, 0, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
                    @Override
                    public void onFailure() {
                        userInfoListener.onError(getString(R.string.user_no_photo));
                    }

                    @Override
                    public void onSuccess(FlickrApiData.PhotosResponse photosResponse) {
                        if (photosResponse.photos.photo.size() > 0) {
                            User userDB = new User(SettingsActivity.this, userId, user, 1, 0, photosResponse.photos.total);
                            userDB.save();
                            mSourceAdded = true;
                            userInfoListener.onSuccess(userDB);
                        } else {
                            userInfoListener.onError(getString(R.string.user_no_photo));

                        }
                    }
                });
            }
        });


    }


    @Override
    public void onImportFinished() {
        mRequestAdapter.setItems(getRequestDatas());
    }

    private List<RequestData> getRequestDatas() {
        List<RequestData> items = new ArrayList<RequestData>();
        items.addAll(Search.listAll(Search.class));
        items.addAll(User.listAll(User.class));
        items.addAll(Tag.listAll(Tag.class));
        items.addAll(FGroup.listAll(FGroup.class));
        if (FlickrMuzeiApplication.getSettings().getBoolean(PreferenceKeys.USE_FAVORITES, false)) {
            items.add(new FavoriteSource(this));
        }
        return items;
    }


    public interface UserInfoListener<T> {
        void onSuccess(T user);

        void onError(String reason);
    }


}
