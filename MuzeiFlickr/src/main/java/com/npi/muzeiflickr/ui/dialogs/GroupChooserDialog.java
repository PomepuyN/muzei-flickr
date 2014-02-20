package com.npi.muzeiflickr.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.network.FlickrApiData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nicolas on 20/02/14.
 */
public class GroupChooserDialog extends DialogFragment {

    private View mLayoutView;
    private ListView mList;
    private ArrayList<FlickrApiData.Group> mGroups;
    private GroupChooserAdapter mAdapter;
    private AlertDialog mDialog;

    public GroupChooserDialog() {
        super();
    }

    public static final GroupChooserDialog newInstance(ArrayList<FlickrApiData.Group> groups) {
        GroupChooserDialog f = new GroupChooserDialog();

        Bundle args = new Bundle();
        args.putSerializable("groups", groups);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setStyle(STYLE_NO_TITLE, 0);
        mGroups = (ArrayList<FlickrApiData.Group>) getArguments().getSerializable("groups");

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        mLayoutView = View.inflate(getActivity(), R.layout.group_chooser_dialog, null);

        mList = (ListView) mLayoutView.findViewById(android.R.id.list);

        mAdapter = new GroupChooserAdapter(getActivity(), 0,mGroups);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChooseGroupDialogListener activity = (ChooseGroupDialogListener) getActivity();
                activity.onFinishChoosingDialog(mGroups.get(position));
                dismiss();
            }
        });

        mDialog = new AlertDialog.Builder(getActivity()).setView(mLayoutView)
                .setTitle(getString(R.string.choose_group))
                .create();


        return mDialog;
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

    public interface ChooseGroupDialogListener {
        void onFinishChoosingDialog(FlickrApiData.Group group);
    }

}
