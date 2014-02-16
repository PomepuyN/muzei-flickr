package com.npi.muzeiflickr.ui.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.api.FlickrSource;
import com.npi.muzeiflickr.data.PreferenceKeys;
import com.npi.muzeiflickr.ui.activities.SettingsActivity;

/**
 * Created by nicolas on 15/02/14.
 */
public class FlickrWidget extends AppWidgetProvider {


    public static final String WIDGET_DATA_KEY = "WidgetData";
    private static final String TAG = FlickrWidget.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if (BuildConfig.DEBUG) Log.d(TAG, "Refresh widgets");

        if (BuildConfig.DEBUG) Log.d(TAG, "Refresh widgets has data");
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, FlickrWidget.class));

        this.onUpdate(context, AppWidgetManager.getInstance(context), ids);
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; i++) {
            RemoteViews views = buildUpdate(context, appWidgetIds[i], true);
            appWidgetManager.updateAppWidget(appWidgetIds[i], views);

        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public RemoteViews buildUpdate(Context context, int appWidgetId, boolean useBigLayout) {
        RemoteViews rview = new RemoteViews(context.getPackageName(), R.layout.flickr_widget);
        final SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_NAME, 0);

        if (!settings.getBoolean(PreferenceKeys.IS_SUSCRIBER_ENABLED, false)) {
            rview.setViewVisibility(R.id.widget_left_container, View.GONE);
            rview.setViewVisibility(R.id.widget_play_button, View.GONE);
            rview.setViewVisibility(R.id.widget_disabled, View.VISIBLE);

            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage("net.nurik.roman.muzei");
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            rview.setOnClickPendingIntent(R.id.widget_disabled, pendingIntent);

        } else {
            rview.setViewVisibility(R.id.widget_left_container, View.VISIBLE);
            rview.setViewVisibility(R.id.widget_play_button, View.VISIBLE);
            rview.setViewVisibility(R.id.widget_disabled, View.GONE);
            rview.setTextViewText(R.id.widget_title, settings.getString(PreferenceKeys.CURRENT_TITLE,""));
            rview.setTextViewText(R.id.widget_author, settings.getString(PreferenceKeys.CURRENT_AUTHOR,""));

            PendingIntent pendingIntentUrl = PendingIntent.getActivity(context, 0, new Intent(Intent.ACTION_VIEW,
                    Uri.parse(settings.getString(PreferenceKeys.CURRENT_URL, ""))), PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent pendingIntent = PendingIntent.getService(context, 0, new Intent(FlickrSource.ACTION_REFRESH_FROM_WIDGET), PendingIntent.FLAG_UPDATE_CURRENT);


//        editor.putString(PreferenceKeys.CURRENT_TITLE, photo.title);
//        editor.putString(PreferenceKeys.CURRENT_AUTHOR, name);
//        editor.putString(PreferenceKeys.CURRENT_URL, photo.url);



            rview.setOnClickPendingIntent(R.id.widget_play_button, pendingIntent);
            rview.setOnClickPendingIntent(R.id.widget_left_container, pendingIntentUrl);
        }



        return rview;
    }
}
