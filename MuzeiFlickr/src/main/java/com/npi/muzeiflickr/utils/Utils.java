package com.npi.muzeiflickr.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;

import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
 * Contains utils to be used elswhere
 */
public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    /**
     * Converts a duration into a human readable String
     *
     * @param duration the duration
     * @return a human readable String
     */
    public static String convertDurationtoString(long duration) {
        int hour = (int) (duration / 3600000);
        int min = (int) ((duration - (hour * 3600000)) / 60000);
        int sec = (int) ((duration - (hour * 3600000) - (min * 60000)) / 1000);
        StringBuilder builder = new StringBuilder();
        builder.append(hour).append("h");
        builder.append(firstDigit(min));
        if (sec != 0)
            builder.append("m").append(firstDigit(sec)).append("s");

        return builder.toString();

    }

    /**
     * Adds a 0 to number below 10
     *
     * @param min the number
     * @return the generated String
     */
    private static String firstDigit(int min) {
        if (min < 10) {
            return "0" + String.valueOf(min);
        }
        return String.valueOf(min);
    }

    /**
     * Gets the user's screen height
     *
     * @param context Context needed for the calculation
     * @return the screen height
     */
    public static int getScreenHeight(Context context) {

        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    /**
     * Determines if the WIFI is connected
     *
     * @param context the needed Context
     * @return true if connected
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    public static int convertDPItoPixels(Context context, int dpi) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpi * scale + 0.5f);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static void downloadImage(Context context, String urlS, String title, String author) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;

        String[] separated = urlS.split("\\.");
        String imageName = title.replace("/", "");
        imageName += "." + separated[separated.length - 1];
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context).setSmallIcon(android.R.drawable.stat_sys_download).setProgress(0, 0, true)
                .setContentTitle(context.getString(R.string.downloading_image))
                .setContentText(context.getString(R.string.being_downloaded, imageName))
                .setTicker(context.getString(R.string.downloading_image));
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + imageName;
        try {


            URL url = new URL(urlS);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                mBuilder.setContentText(context.getString(R.string.download_error)).setProgress(0, 0, false);
                mNotificationManager.notify(0, mBuilder.build());
                return;
            }


            // download the file
            input = connection.getInputStream();
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Path:" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + imageName);
            output = new FileOutputStream(path);

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {

                output.write(data, 0, count);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            mBuilder.setContentText(context.getString(R.string.download_error)).setProgress(0, 0, false);
            mNotificationManager.notify(0, mBuilder.build());
            return;
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
                mBuilder.setContentText(context.getString(R.string.download_error)).setProgress(0, 0, false);
                mNotificationManager.notify(0, mBuilder.build());
                return;
            }

            if (connection != null)
                connection.disconnect();
        }

        mBuilder.setProgress(0, 0, false).setContentTitle(context.getString(R.string.image_downloaded)).setSmallIcon(android.R.drawable.stat_sys_download_done);
        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
        bigPictureStyle.setBigContentTitle(context.getString(R.string.image_downloaded));
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        bigPictureStyle.bigPicture(bitmap);

        mBuilder.setStyle(bigPictureStyle);

        MimeTypeMap myMime = MimeTypeMap.getSingleton();

        Intent newIntent = new Intent(android.content.Intent.ACTION_VIEW);

        newIntent.setDataAndType(Uri.fromFile(new File(path)), "image/*");
        PendingIntent pi = PendingIntent.getActivity(context, 0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/*");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, "My Android wallpaper is '"
                + title.trim()
                + "' by " + author
                + ". \nShared with Flickr for Muzei\n\n");
        share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path)));
        PendingIntent pishare = PendingIntent.getActivity(context, 0, share, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.addAction(R.drawable.ic_picture, "Open", pi);
        mBuilder.addAction(R.drawable.ic_share, "Share", pishare);
        mBuilder.setContentIntent(pi);
        mBuilder.setAutoCancel(true);
        mNotificationManager.notify(0, mBuilder.build());
    }

    /**
     * Hides the keyboard.
     *
     * @param view
     */
    public static void hideKeyboard(View view) {
        Context context = view.getContext();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Shows the keyboard.
     *
     * @param view
     */
    public static void showKeyboard(View view) {
        Context context = view.getContext();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }
}
