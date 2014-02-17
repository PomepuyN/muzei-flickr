package com.npi.muzeiflickr.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.doomonafireball.betterpickers.hmspicker.HmsPickerBuilder;
import com.doomonafireball.betterpickers.hmspicker.HmsPickerDialogFragment;
import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.db.Photo;
import com.npi.muzeiflickr.network.FlickrService;
import com.npi.muzeiflickr.api.FlickrSource;
import com.npi.muzeiflickr.data.PreferenceKeys;
import com.npi.muzeiflickr.utils.Utils;

import retrofit.Callback;
import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
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
public class SettingsActivity extends FragmentActivity implements HmsPickerDialogFragment.HmsPickerDialogHandler {
    public static final String PREFS_NAME = "main_prefs";
    private static final String TAG = SettingsActivity.class.getSimpleName();
    private TextView mRefreshRate;

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
        Button okButton = (Button) findViewById(R.id.ok_button);
        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        final EditText searchText = (EditText) findViewById(R.id.text_search);
        final EditText userText = (EditText) findViewById(R.id.text_user);
        final Spinner modeChooser = (Spinner) findViewById(R.id.mode_chooser);

        final LinearLayout searchContainer = (LinearLayout) findViewById(R.id.search_container);
        final LinearLayout userContainer = (LinearLayout) findViewById(R.id.user_container);
        final LinearLayout locationContainer = (LinearLayout) findViewById(R.id.location_container);
        Switch wifiOnly = (Switch) findViewById(R.id.wifi_only);
        mRefreshRate = (TextView) findViewById(R.id.refresh_rate);
        ImageView aboutShortcut = (ImageView) findViewById(R.id.about);

        //Wifi status and setting
        wifiOnly.setChecked(settings.getBoolean(PreferenceKeys.WIFI_ONLY, false));

        wifiOnly.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                editor.putBoolean(PreferenceKeys.WIFI_ONLY, isChecked);
                editor.commit();
            }
        });

        //Mode spinner management
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.modes, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeChooser.setAdapter(adapter);

        modeChooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Visibility of elements when mode is changed
                switch (position) {
                    case 0:
                        searchContainer.setVisibility(View.VISIBLE);
                        userContainer.setVisibility(View.GONE);
                        locationContainer.setVisibility(View.GONE);
                        break;
                    case 1:
                        searchContainer.setVisibility(View.GONE);
                        userContainer.setVisibility(View.VISIBLE);
                        locationContainer.setVisibility(View.GONE);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        //Other settings population
        String search = settings.getString(PreferenceKeys.SEARCH_TERM, "landscape");
        String user = settings.getString(PreferenceKeys.USER_NAME, "");
        int refreshRate = settings.getInt(PreferenceKeys.REFRESH_TIME, FlickrSource.DEFAULT_REFRESH_TIME);
        int mode = settings.getInt(PreferenceKeys.MODE, 0);

        mRefreshRate.setText(Utils.convertDurationtoString(refreshRate));

        modeChooser.setSelection(mode);

        if (!search.equals("landscape")) {
            searchText.setText(search);
        }
        userText.setText(user);

        mRefreshRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HmsPickerBuilder hpb = new HmsPickerBuilder()
                        .setFragmentManager(getSupportFragmentManager())
                        .setStyleResId(R.style.BetterPickersDialogFragment);
                hpb.show();
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int currentMode = settings.getInt(PreferenceKeys.MODE, 0);

                //Depending on the chosen mode actions will be different
                switch (modeChooser.getSelectedItemPosition()) {
                    //Search
                    case 0:
                        if (BuildConfig.DEBUG) Log.d(TAG, "Mode search selected");
                        String currentSearch = settings.getString(PreferenceKeys.SEARCH_TERM, "");

                        editor.putInt(PreferenceKeys.MODE, 0);

                        editor.putString(PreferenceKeys.SEARCH_TERM, searchText.getText().toString());

                        //We only invalidate the cache and launch an update if something has changed
                        if (currentMode != modeChooser.getSelectedItemPosition() || !searchText.getText().toString().equals(currentSearch)) {
                            invalidateQueryValues(editor);
                        }

                        break;
                    //User
                    case 1:
                        if (BuildConfig.DEBUG) Log.d(TAG, "Mode user selected");
                        editor.putInt(PreferenceKeys.MODE, 1);

                        String userTextS = userText.getText().toString();
                        //Let's see if this user exist
                        if (TextUtils.isEmpty(userTextS) || !userTextS.equals(settings.getString(PreferenceKeys.USER_NAME, ""))) {
                            getUserId(userTextS);
                        }

                        //If the mode has changed, invalidate the cache and launch an update
                        if (currentMode != modeChooser.getSelectedItemPosition()) {
                            invalidateQueryValues(editor);
                        }


                        break;
                }

                // Commit the edits!
                editor.commit();
                finish();

            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        aboutShortcut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AboutActivity.launchActivity(SettingsActivity.this);
            }
        });

    }

    /**
     * Invalidates the cache and launch an update
     *
     * @param editor
     */
    private void invalidateQueryValues(SharedPreferences.Editor editor) {

        Photo.deleteAll(Photo.class);
        editor.putInt(PreferenceKeys.CURRENT_PAGE, 0);
        editor.commit();
        startService(new Intent(SettingsActivity.this, FlickrSource.class).setAction(FlickrSource.ACTION_CLEAR_SERVICE));

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
    private void getUserId(final String user) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                final SharedPreferences.Editor editor = settings.edit();

                RestAdapter restAdapter = new RestAdapter.Builder()
//                        .setLogLevel(RestAdapter.LogLevel.FULL)
                        .setServer("http://api.flickr.com/services/rest")
                        .setErrorHandler(new ErrorHandler() {
                            @Override
                            public Throwable handleError(RetrofitError retrofitError) {
                                revertIfInvalidUser(settings, editor, user);
                                return retrofitError;
                            }
                        })
                        .build();

                FlickrService service = restAdapter.create(FlickrService.class);
                service.getUserByName(user, new Callback<FlickrService.UserByNameResponse>() {
                    @Override
                    public void success(FlickrService.UserByNameResponse userByNameResponse, Response response) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Looking for user");


                        //The user has not been found
                        if (userByNameResponse == null || userByNameResponse.user == null || userByNameResponse.user.nsid == null) {

                            revertIfInvalidUser(settings, editor, user);

                            return;
                        }


                        //The user has been found, we store it
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "User found: " + userByNameResponse.user.nsid + " for " + user);
                        }
                        String userId = userByNameResponse.user.nsid;

                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PreferenceKeys.USER_ID, userId);
                        editor.putString(PreferenceKeys.USER_NAME, user);
                        invalidateQueryValues(editor);
                        editor.commit();

                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        revertIfInvalidUser(settings, editor, user);
                    }
                });

            }
        }).run();

    }

    /**
     * Reverts to the correct mode and settings if the user has not been found
     * @param settings SharedPreferences to be used
     * @param editor SharedPreferences.Editor to be used
     * @param user user name not found
     */
    private void revertIfInvalidUser(SharedPreferences settings, SharedPreferences.Editor editor, String user) {
        //The last user had been set
        if (TextUtils.isEmpty(settings.getString(PreferenceKeys.USER_NAME, ""))) {

            //Reverting to search mode

            Toast.makeText(this, getString(R.string.user_not_found_revert, user), Toast.LENGTH_LONG).show();
            editor.putInt(PreferenceKeys.MODE, 0);
            editor.commit();
            invalidateQueryValues(editor);
        } else {
            //Reverting to previous user
            Toast.makeText(this, getString(R.string.user_not_found_no_revert, user), Toast.LENGTH_LONG).show();
        }
    }
}
