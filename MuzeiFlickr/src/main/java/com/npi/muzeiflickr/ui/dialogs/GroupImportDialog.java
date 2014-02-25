package com.npi.muzeiflickr.ui.dialogs;

import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.FlickrMuzeiApplication;
import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.data.ImportableData;
import com.npi.muzeiflickr.data.PreferenceKeys;
import com.npi.muzeiflickr.db.FGroup;
import com.npi.muzeiflickr.network.FlickrApiData;
import com.npi.muzeiflickr.network.FlickrService;
import com.npi.muzeiflickr.network.FlickrServiceInterface;
import com.npi.muzeiflickr.ui.adapters.ItemImportAdapter;

import java.util.ArrayList;
import java.util.List;

import eu.inmite.android.lib.dialogs.BaseDialogFragment;

/**
 * Created by nicolas on 23/02/14.
 */
public class GroupImportDialog extends BaseDialogFragment {

    private static final String TAG = GroupImportDialog.class.getSimpleName();
    private View mLayoutView;
    private ListView mList;
    private ItemImportAdapter mAdapter;
    private List<ImportableData> mGroups;
    private ProgressBar mEmpty;
    private CheckBox mSelectAll;
    private RelativeLayout mSelectAllContainer;
    private TextView mImportResult;

    public static void show(FragmentActivity activity) {
        GroupImportDialog dialog = new GroupImportDialog();
        dialog.show(activity.getSupportFragmentManager(), TAG);
    }

    @Override
    protected Builder build(Builder builder) {
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


        mSelectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAdapter.setAllChecked(isChecked);
            }
        });

        builder.setTitle(getString(R.string.import_groups)).setView(mLayoutView)
                .setPositiveButton(getString(android.R.string.ok), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });


        FlickrService.getInstance(getActivity()).getGroupsByUser(FlickrMuzeiApplication.getSettings().getString(PreferenceKeys.LOGIN_NSID, ""), new FlickrServiceInterface.IRequestListener<FlickrApiData.GroupsResponse>() {
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
                    getPositiveButton().setOnClickListener(new View.OnClickListener() {
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
//                            getNegativeButton().setVisibility(View.GONE);

                }
            }
        });

        return builder;
    }


    private void positiveButtonAction() {
        mEmpty.setVisibility(View.VISIBLE);
        mList.setVisibility(View.GONE);
        mSelectAllContainer.setVisibility(View.GONE);

        //Hide cancel button
//        getNegativeButton().setVisibility(View.GONE);

        getPositiveButton().setEnabled(false);

        final List<ImportableData> importedGroupSuccess = new ArrayList<ImportableData>();
        final List<ImportableData> importedGroupFailed = new ArrayList<ImportableData>();


        final List<ImportableData> itemsToImport = mAdapter.getCheckedItems();

        if (itemsToImport.size() == 0) {
            dismiss();
            return;
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "Import size: " + itemsToImport.size());

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

                        getPositiveButton().setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                dismiss();
                            }
                        });
                        getPositiveButton().setEnabled(true);
                    }
                }
            });
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        ((UserImportDialog.ImportDialogListener) getActivity()).onImportFinished();
        super.onDismiss(dialog);
    }
}
