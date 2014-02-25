package com.npi.muzeiflickr.ui.dialogs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.data.ImportableData;
import com.npi.muzeiflickr.db.User;
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
public class UserImportDialog extends BaseDialogFragment {

    private static final String TAG = UserImportDialog.class.getSimpleName();
    private View mLayoutView;
    private ListView mList;
    private ItemImportAdapter mAdapter;
    private AlertDialog mDialog;
    private List<ImportableData> mContacts;
    private ProgressBar mEmpty;
    private CheckBox mSelectAll;
    private RelativeLayout mSelectAllContainer;
    private TextView mImportResult;

    public static void show(FragmentActivity activity) {
        UserImportDialog dialog = new UserImportDialog();
        dialog.show(activity.getSupportFragmentManager(), TAG);
    }

    @Override
    protected BaseDialogFragment.Builder build(BaseDialogFragment.Builder builder) {
        mLayoutView = View.inflate(getActivity(), R.layout.import_dialog, null);

        mList = (ListView) mLayoutView.findViewById(android.R.id.list);
        mEmpty = (ProgressBar) mLayoutView.findViewById(android.R.id.empty);
        mSelectAll = (CheckBox) mLayoutView.findViewById(R.id.checkbox_select_all);
        mSelectAllContainer = (RelativeLayout) mLayoutView.findViewById(R.id.all_container);
        mImportResult = (TextView) mLayoutView.findViewById(R.id.import_result);

        mContacts = new ArrayList<ImportableData>();

        mAdapter = new ItemImportAdapter(getActivity(), 0, mContacts);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            }
        });

        builder.setTitle(getString(R.string.import_contacts)).setView(mLayoutView)
                .setPositiveButton(getString(android.R.string.ok), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismiss();
                    }
                });

                FlickrService.getInstance(getActivity()).getContacts(new FlickrServiceInterface.IRequestListener<FlickrApiData.ContactResponse>() {
                    @Override
                    public void onFailure() {

                    }

                    @Override
                    public void onSuccess(FlickrApiData.ContactResponse response) {

                        //Removing already added contacts
                        List<FlickrApiData.Contact> contactsToShow = new ArrayList<FlickrApiData.Contact>();
                        List<User> users = User.listAll(User.class);
                        for (FlickrApiData.Contact contact : response.contacts.contact) {
                            boolean found = false;
                            for (User user : users) {
                                if (user.userId.equals(contact.nsid)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                contactsToShow.add(contact);
                            }
                        }

                        if (contactsToShow.size() > 0) {
                            mAdapter.addAll(contactsToShow);
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

                        }
                    }
                });



        mSelectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAdapter.setAllChecked(isChecked);
            }
        });

        return builder;
    }


    private void positiveButtonAction() {
        mEmpty.setVisibility(View.VISIBLE);
        mList.setVisibility(View.GONE);
        mSelectAllContainer.setVisibility(View.GONE);

        //Hide cancel button

        getPositiveButton().setEnabled(false);

        final List<ImportableData> importedContactSuccess = new ArrayList<ImportableData>();
        final List<ImportableData> importedContactFailed = new ArrayList<ImportableData>();

        final List<ImportableData> itemsToImport = mAdapter.getCheckedItems();

        if (itemsToImport.size() == 0) {
            dismiss();
            return;
        }

        setCancelable(false);
        for (final ImportableData item : itemsToImport) {
            FlickrService.getInstance(getActivity()).getPopularPhotosByUser(((FlickrApiData.Contact) item).nsid, 0, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosResponse>() {
                @Override
                public void onFailure() {
                    importedContactFailed.add(item);
                    endImportIfNeeded();
                }

                @Override
                public void onSuccess(FlickrApiData.PhotosResponse photosResponse) {
                    if (photosResponse.photos.photo.size() > 0) {
                        User userDB = new User(getActivity(), ((FlickrApiData.Contact) item).nsid, item.getName(), 1, 0, photosResponse.photos.total);
                        userDB.save();
                        importedContactSuccess.add(item);
                    } else {
                        importedContactFailed.add(item);
                    }
                    endImportIfNeeded();
                }

                private void endImportIfNeeded() {
                    if (importedContactFailed.size() + importedContactSuccess.size() == itemsToImport.size()) {
                        mEmpty.setVisibility(View.GONE);
                        mImportResult.setText(getString(R.string.import_result, importedContactSuccess.size(), importedContactFailed.size()));
                        mImportResult.setVisibility(View.VISIBLE);

                        getPositiveButton().setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                ((ImportDialogListener) getActivity()).onImportFinished();
                                dismiss();
                            }
                        });
                        getPositiveButton().setEnabled(true);
                    }
                }
            });
        }
    }

    public interface ImportDialogListener {
        void onImportFinished();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        ((UserImportDialog.ImportDialogListener) getActivity()).onImportFinished();
        super.onDismiss(dialog);
    }


}
