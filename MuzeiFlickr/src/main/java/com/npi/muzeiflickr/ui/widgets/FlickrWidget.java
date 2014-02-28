package com.npi.muzeiflickr.ui.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.api.FlickrSource;
import com.npi.muzeiflickr.data.PreferenceKeys;
import com.npi.muzeiflickr.data.WidgetEntity;
import com.npi.muzeiflickr.ui.activities.SettingsActivity;

import java.util.List;

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
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        //Getting the widgets

        List<WidgetEntity> widgets = WidgetEntity.retrieveWidgets(context);

        boolean found = false;
        for (WidgetEntity widget : widgets) {
            if (widget.id == appWidgetId) {
                widget.size = newOptions.getInt("appWidgetMinWidth");
                found = true;
                break;
            }
        }

        if (!found) {
            WidgetEntity widget = new WidgetEntity();
            widget.id = appWidgetId;
            widget.size = newOptions.getInt("appWidgetMinWidth");
            widgets.add(widget);
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "New size: "+newOptions.getInt("appWidgetMinWidth"));


        WidgetEntity.saveWidgets(context, widgets);

        this.onUpdate(context, AppWidgetManager.getInstance(context), new int[]{appWidgetId});
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

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
        int size = WidgetEntity.getWidgetSize(context, appWidgetId);


        RemoteViews rview;
        if (size > 125) {
            rview = new RemoteViews(context.getPackageName(), R.layout.flickr_widget);
        } else {
            rview = new RemoteViews(context.getPackageName(), R.layout.flickr_widget_small);
        }

        final SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_NAME, 0);

        if (!settings.getBoolean(PreferenceKeys.IS_SUSCRIBER_ENABLED, false)) {
            rview.setViewVisibility(R.id.widget_left_container, View.GONE);
            rview.setViewVisibility(R.id.widget_play_button, View.GONE);
            rview.setViewVisibility(R.id.widget_disabled, View.VISIBLE);

            try {
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage("net.nurik.roman.muzei");
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                rview.setOnClickPendingIntent(R.id.widget_disabled, pendingIntent);

            } catch (NullPointerException exception) {
                Log.w(TAG, exception.getMessage(), exception);
            }

        } else {



            rview.setViewVisibility(R.id.widget_play_button, View.VISIBLE);
            rview.setViewVisibility(R.id.widget_disabled, View.GONE);
            rview.setTextViewText(R.id.widget_title, settings.getString(PreferenceKeys.CURRENT_TITLE, ""));
            rview.setTextViewText(R.id.widget_author, settings.getString(PreferenceKeys.CURRENT_AUTHOR, ""));

            PendingIntent pendingIntentUrl = PendingIntent.getActivity(context, 0, new Intent(Intent.ACTION_VIEW,
                    Uri.parse(settings.getString(PreferenceKeys.CURRENT_URL, ""))), PendingIntent.FLAG_UPDATE_CURRENT);

            PendingIntent pendingIntent = PendingIntent.getService(context, 0, new Intent(FlickrSource.ACTION_REFRESH_FROM_WIDGET), PendingIntent.FLAG_UPDATE_CURRENT);

            rview.setOnClickPendingIntent(R.id.widget_play_button, pendingIntent);
            rview.setOnClickPendingIntent(R.id.widget_left_container, pendingIntentUrl);
        }


        return rview;
    }
}
