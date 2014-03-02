package com.npi.muzeiflickr.ui.dialogs;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.db.FGroup;
import com.npi.muzeiflickr.db.RequestData;
import com.npi.muzeiflickr.db.User;
import com.npi.muzeiflickr.network.FlickrApiData;
import com.npi.muzeiflickr.network.FlickrService;
import com.npi.muzeiflickr.network.FlickrServiceInterface;

import eu.inmite.android.lib.dialogs.BaseDialogFragment;

/**
 * Created by nicolas on 20/02/14.
 */
public class SourceInfoDialog extends BaseDialogFragment {

    private static final String ARG_REQUEST_DATA = "groups";
    private static final String TAG = SourceInfoDialog.class.getSimpleName();
    private View mLayoutView;

    public SourceInfoDialog() {
        super();
    }

    public static void show(FragmentActivity activity, RequestData item) {
        SourceInfoDialog dialog = new SourceInfoDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_REQUEST_DATA, item);
        dialog.setArguments(args);
        dialog.show(activity.getSupportFragmentManager(), TAG);
    }



    @Override
    public Builder build(Builder builder) {
        mLayoutView = View.inflate(getActivity(), R.layout.source_settings_dialog, null);

        TextView viewInFlickr = (TextView) mLayoutView.findViewById(R.id.view_flickr);
        TextView remove = (TextView) mLayoutView.findViewById(R.id.remove);
        final RequestData item = (RequestData) getArguments().getSerializable(ARG_REQUEST_DATA);


        if (!(item instanceof User) && !(item instanceof FGroup) ) {
            viewInFlickr.setVisibility(View.GONE);
        }

        viewInFlickr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (item instanceof User) {
                    FlickrService.getInstance(getActivity()).getUser(((User) item).userId, new FlickrServiceInterface.IRequestListener<FlickrApiData.UserResponse>() {
                        @Override
                        public void onFailure() {

                        }

                        @Override
                        public void onSuccess(FlickrApiData.UserResponse response) {
                            if (BuildConfig.DEBUG) Log.d(TAG, "URL: "+response.person.profileurl._content);
                            Intent intent = new Intent(new Intent(Intent.ACTION_VIEW, Uri.parse(response.person.profileurl._content)));
                            startActivity(intent);
                        }
                    });
                } else if (item instanceof FGroup) {
                    FlickrService.getInstance(getActivity()).getGroupUrl(((FGroup) item).groupId, new FlickrServiceInterface.IRequestListener<FlickrApiData.GroupUrlResponse>() {
                        @Override
                        public void onFailure() {

                        }

                        @Override
                        public void onSuccess(FlickrApiData.GroupUrlResponse response) {
                            if (BuildConfig.DEBUG) Log.d(TAG, "URL: "+response.group.url);
                            Intent intent = new Intent(new Intent(Intent.ACTION_VIEW, Uri.parse(response.group.url)));
                            startActivity(intent);
                        }
                    });
                }
            }
        });

        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                item.delete();
                dismiss();
            }
        });



        builder.setView(mLayoutView).setTitle(item.getTitle());
        return builder;
    }



    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        SourceDialogListener activity = (SourceDialogListener) getActivity();
        activity.onFinishSourceDialog();
    }

    public interface SourceDialogListener {
        void onFinishSourceDialog();
    }

}
