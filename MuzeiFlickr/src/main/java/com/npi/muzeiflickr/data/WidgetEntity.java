package com.npi.muzeiflickr.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.npi.muzeiflickr.ui.activities.SettingsActivity;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
 * Describes a Photo to be stored in the cache system.
 */
public class WidgetEntity {

    public int id;

    public int size;

    // Retrieve widgets list from SharedPreferences
    public static List<WidgetEntity> retrieveWidgets(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<WidgetEntity>>() {
        }.getType();

        List<WidgetEntity> widgets = gson.fromJson(settings.getString(PreferenceKeys.WIDGETS, ""), type);
        if (widgets == null) widgets = new ArrayList<WidgetEntity>();
        return widgets;

    }

    // Save widgets list in SharedPreferences
    public static void saveWidgets(Context context, List<WidgetEntity> photos) {
        SharedPreferences settings = context.getSharedPreferences(SettingsActivity.PREFS_NAME, 0);
        Gson gson = new Gson();
        String storableWidgetsJson = gson.toJson(photos);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PreferenceKeys.WIDGETS, storableWidgetsJson);


        editor.commit();
    }


    public static int getWidgetSize(Context context, int appWidgetId) {
        List<WidgetEntity> widgetEntities = retrieveWidgets(context);
        for (WidgetEntity widgetEntity:widgetEntities) {
            if (widgetEntity.id == appWidgetId) {
                return widgetEntity.size;
            }
        }

        //Arbitrary size above min size for layout change
        return 200;
    }
}
