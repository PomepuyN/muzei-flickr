package com.npi.muzeiflickr.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.data.ImportableData;
import com.npi.muzeiflickr.data.PreferenceKeys;
import com.npi.muzeiflickr.db.FGroup;
import com.npi.muzeiflickr.network.FlickrApiData;
import com.npi.muzeiflickr.network.FlickrService;
import com.npi.muzeiflickr.network.FlickrServiceInterface;
import com.npi.muzeiflickr.ui.activities.SettingsActivity;
import com.npi.muzeiflickr.ui.adapters.ItemImportAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nicolas on 23/02/14.
 */
public class GroupImportDialog extends DialogFragment {

    private static final String TAG = GroupImportDialog.class.getSimpleName();
    private View mLayoutView;
    private ListView mList;
    private ItemImportAdapter mAdapter;
    private AlertDialog mDialog;
    private List<ImportableData> mGroups;
    private ProgressBar mEmpty;
    private CheckBox mSelectAll;
    private RelativeLayout mSelectAllContainer;
    private TextView mImportResult;
    private Button mNegativeButton;
    private Button mPositiveButton;

    public static final GroupImportDialog newInstance() {
        return new GroupImportDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        mLayoutView = View.inflate(getActivity(), R.layout.import_dialog, null);

        mList = (ListView) mLayoutView.findViewById(android.R.id.list);
        mEmpty = (ProgressBar) mLayoutView.findViewById(android.R.id.empty);
        mSelectAll = (CheckBox) mLayoutView.findViewById(R.id.checkbox_select_all);
        mSelectAllContainer = (RelativeLayout) mLayoutView.findViewById(R.id.all_container);
        mImportResult = (TextView) mLayoutView.findViewById(R.id.import_result);

        mGroups = new ArrayList<ImportableData>();

        mAdapter = new ItemImportAdapter(getActivity(), 0, mGroups);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            }
        });

        mDialog = new AlertDialog.Builder(getActivity()).setView(mLayoutView)
                .setTitle(getString(R.string.import_groups))
                .setPositiveButton(getString(android.R.string.ok), null)
                .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .create();

        final SharedPreferences settings = getActivity().getSharedPreferences(SettingsActivity.PREFS_NAME, 0);


        mDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                mNegativeButton = mDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                mPositiveButton = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);

                if (mPositiveButton != null) {
                    mPositiveButton.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            dismiss();
                        }
                    });
                }
                FlickrService.getInstance(getActivity()).getGroupsByUser(settings.getString(PreferenceKeys.LOGIN_NSID, ""), new FlickrServiceInterface.IRequestListener<FlickrApiData.GroupsResponse>() {
                    @Override
                    public void onFailure() {

                    }

                    @Override
                    public void onSuccess(FlickrApiData.GroupsResponse response) {

                        //Removing already added groups
                        List<FlickrApiData.Group> groupsToShow = new ArrayList<FlickrApiData.Group>();
                        List<FGroup> groups = FGroup.listAll(FGroup.class);
                        for (FlickrApiData.Group group : response.groups.group) {
                            boolean found = false;
                            for (FGroup fgroup : groups) {
                                if (fgroup.groupId.equals(group.nsid)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                groupsToShow.add(group);
                            }
                        }

                        if (groupsToShow.size() > 0) {
                            mAdapter.addAll(groupsToShow);
                            mAdapter.notifyDataSetChanged();
                            mEmpty.setVisibility(View.GONE);
                            mList.setVisibility(View.VISIBLE);
                            mSelectAllContainer.setVisibility(View.VISIBLE);
                            mPositiveButton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    positiveButtonAction();
                                }
                            });
                        } else {
                            mEmpty.setVisibility(View.GONE);
                            mImportResult.setVisibility(View.VISIBLE);
                            mImportResult.setText(getActivity().getString(R.string.nothing_to_import));
                            mImportResult.setCompoundDrawablesWithIntrinsicBounds(getActivity().getResources().getDrawable(R.drawable.thumb_up), null, null, null);
                            mNegativeButton.setVisibility(View.GONE);

                        }
                    }
                });
            }
        });


        mSelectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAdapter.setAllChecked(isChecked);
            }
        });


        return mDialog;
    }

    private void positiveButtonAction() {
        mEmpty.setVisibility(View.VISIBLE);
        mList.setVisibility(View.GONE);
        mSelectAllContainer.setVisibility(View.GONE);

        //Hide cancel button
        mNegativeButton.setVisibility(View.GONE);

        mPositiveButton.setEnabled(false);

        final List<ImportableData> importedGroupSuccess = new ArrayList<ImportableData>();
        final List<ImportableData> importedGroupFailed = new ArrayList<ImportableData>();

        final List<ImportableData> itemsToImport = mAdapter.getCheckedItems();
        setCancelable(false);
        for (final ImportableData item : itemsToImport) {
            FlickrService.getInstance(getActivity()).getGroupPhotos(((FlickrApiData.Group) item).nsid, 0, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
                @Override
                public void onFailure() {
                    importedGroupFailed.add(item);
                    endImportIfNeeded();
                }

                @Override
                public void onSuccess(FlickrApiData.PhotosResponse photosResponse) {
                    if (photosResponse.photos.photo.size() > 0) {
                        FGroup userDB = new FGroup(getActivity(), ((FlickrApiData.Group) item).nsid, item.getName(), 1, 0, photosResponse.photos.total);
                        userDB.save();
                        importedGroupSuccess.add(item);
                    } else {
                        importedGroupFailed.add(item);
                    }
                    endImportIfNeeded();
                }

                private void endImportIfNeeded() {
                    if (importedGroupFailed.size() + importedGroupSuccess.size() == itemsToImport.size()) {
                        mEmpty.setVisibility(View.GONE);
                        mImportResult.setText(getString(R.string.import_result, importedGroupSuccess.size(), importedGroupFailed.size()));
                        mImportResult.setVisibility(View.VISIBLE);

                        mPositiveButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                ((UserImportDialog.ImportDialogListener) getActivity()).onImportFinished();
                                dismiss();
                            }
                        });
                        mPositiveButton.setEnabled(true);
                    }
                }
            });
        }
    }



}
