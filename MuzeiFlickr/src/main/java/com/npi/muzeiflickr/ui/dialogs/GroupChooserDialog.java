package com.npi.muzeiflickr.ui.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.network.FlickrApiData;
import com.npi.muzeiflickr.network.FlickrService;
import com.npi.muzeiflickr.network.FlickrServiceInterface;

import java.util.List;

import eu.inmite.android.lib.dialogs.BaseDialogFragment;

/**
 * Created by nicolas on 20/02/14.
 */
public class GroupChooserDialog extends BaseDialogFragment {

    private static final String ARG_GROUP_SEARCH = "groups";
    private static final String TAG = GroupChooserDialog.class.getSimpleName();
    private View mLayoutView;
    private ListView mList;
    private GroupChooserAdapter mAdapter;
    private FlickrApiData.Group mChosenGroup;

    public GroupChooserDialog() {
        super();
    }

    public static void show(FragmentActivity activity, String search) {
        GroupChooserDialog dialog = new GroupChooserDialog();
        Bundle args = new Bundle();
        args.putString(ARG_GROUP_SEARCH, search);
        dialog.setArguments(args);
        dialog.show(activity.getSupportFragmentManager(), TAG);
    }



    @Override
    public Builder build(Builder builder) {
        mLayoutView = View.inflate(getActivity(), R.layout.group_chooser_dialog, null);

        mList = (ListView) mLayoutView.findViewById(android.R.id.list);
        final ProgressBar mProgress = (ProgressBar) mLayoutView.findViewById(R.id.progress);
        final TextView mError = (TextView) mLayoutView.findViewById(R.id.error);






        FlickrService.getInstance(getActivity()).getGroups(getArguments().getString(ARG_GROUP_SEARCH), new FlickrServiceInterface.IRequestListener<FlickrApiData.GroupsResponse>() {
            @Override
            public void onFailure() {
            }

            @Override
            public void onSuccess(final FlickrApiData.GroupsResponse photosResponse) {
                if (photosResponse == null || photosResponse.groups == null || photosResponse.groups.group == null) {
                    mError.setVisibility(View.VISIBLE);
                    mProgress.setVisibility(View.GONE);

                } else if (photosResponse.groups.group.size() > 0) {

                    mProgress.setVisibility(View.GONE);
                    mList.setVisibility(View.VISIBLE);

                    mAdapter = new GroupChooserAdapter(getActivity(), 0,photosResponse.groups.group);
                    mList.setAdapter(mAdapter);
                    mAdapter.notifyDataSetChanged();
                    mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            mChosenGroup = photosResponse.groups.group.get(position);
                            dismiss();
                        }
                    });
                } else {
                    mError.setVisibility(View.VISIBLE);
                    mProgress.setVisibility(View.GONE);
                }
            }
        });




        builder.setView(mLayoutView).setTitle(getString(R.string.choose_group));
        return builder;
    }



    private class GroupChooserAdapter extends ArrayAdapter<FlickrApiData.Group> {

        private final List<FlickrApiData.Group> mItems;
        private Context mContext;

        public GroupChooserAdapter(Context context, int textViewResourceId, List<FlickrApiData.Group> objects) {
            super(context, textViewResourceId, objects);
            mContext = context;
            mItems = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = View.inflate(mContext, R.layout.group_chooser_item, null);
            TextView groupName = (TextView) convertView.findViewById(R.id.text);



            groupName.setText(mItems.get(position).name);
            return convertView;
        }

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        ChooseGroupDialogListener activity = (ChooseGroupDialogListener) getActivity();
        activity.onFinishChoosingDialog(mChosenGroup);
    }

    public interface ChooseGroupDialogListener {
        void onFinishChoosingDialog(FlickrApiData.Group group);
    }

}
