package com.npi.muzeiflickr.ui.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.FlickrMuzeiApplication;
import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.data.ImportableData;
import com.npi.muzeiflickr.data.PreferenceKeys;
import com.npi.muzeiflickr.db.FSet;
import com.npi.muzeiflickr.db.User;
import com.npi.muzeiflickr.network.FlickrApiData;
import com.npi.muzeiflickr.network.FlickrService;
import com.npi.muzeiflickr.network.FlickrServiceInterface;
import com.npi.muzeiflickr.ui.adapters.ItemImportAdapter;
import com.npi.muzeiflickr.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import eu.inmite.android.lib.dialogs.BaseDialogFragment;

/**
 * Created by nicolas on 20/02/14.
 */
public class SetChooserDialog extends BaseDialogFragment {

    private static final String TAG = SetChooserDialog.class.getSimpleName();
    private static final String ARG_SET_USER = "set_user";
    private View mLayoutView;
    private ListView mList;
    private FlickrApiData.PhotoSet mChosenSet;
    private EditText mUserEditText;
    private ProgressBar mEmpty;
    private CheckBox mSelectAll;
    private RelativeLayout mSelectAllContainer;
    private TextView mImportResult;
    private ItemImportAdapter mAdapter;
    private TextView mHeader;
    private String mUsername;

    public SetChooserDialog() {
        super();
    }

    public static void show(FragmentActivity activity, User user) {

        SetChooserDialog dialog = new SetChooserDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SET_USER, user);

        dialog.setArguments(args);
        dialog.show(activity.getSupportFragmentManager(), TAG);
    }


    @Override
    public Builder build(Builder builder) {
        mLayoutView = View.inflate(getActivity(), R.layout.set_chooser_dialog, null);

        mList = (ListView) mLayoutView.findViewById(android.R.id.list);
        mUserEditText = (EditText) mLayoutView.findViewById(R.id.user);
        final TextView mError = (TextView) mLayoutView.findViewById(R.id.error);
        mEmpty = (ProgressBar) mLayoutView.findViewById(android.R.id.empty);
        mSelectAll = (CheckBox) mLayoutView.findViewById(R.id.checkbox_select_all);
        mSelectAllContainer = (RelativeLayout) mLayoutView.findViewById(R.id.all_container);
        mImportResult = (TextView) mLayoutView.findViewById(R.id.import_result);

        Utils.showKeyboard(mUserEditText);

        User askedUser = (User) getArguments().getSerializable(ARG_SET_USER);
        if (askedUser != null) {
            findSet(askedUser.userId);
            mUsername = askedUser.userName;
        } else {


            if (!TextUtils.isEmpty(FlickrMuzeiApplication.getSettings().getString(PreferenceKeys.LOGIN_USERNAME, ""))) {
                FlickrService.getInstance(getActivity()).getContacts(new FlickrServiceInterface.IRequestListener<FlickrApiData.ContactResponse>() {
                    @Override
                    public void onFailure() {

                    }

                    @Override
                    public void onSuccess(final FlickrApiData.ContactResponse response) {
                        List<FlickrApiData.Contact> contactsToShow = response.contacts.contact;

                        if (contactsToShow.size() > 0) {

                            final ContactAdapter adapter = new ContactAdapter(getActivity(), android.R.layout.simple_list_item_1, contactsToShow);
                            mList.setAdapter(adapter);
                            mList.setVisibility(View.VISIBLE);
                            mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    findSet(adapter.getItem(position - 1).nsid);
                                    mUsername = adapter.getItem(position - 1).username;
                                }
                            });

                            mHeader = new TextView(getActivity());
                            mHeader.setGravity(Gravity.CENTER);


                            mHeader.setText("or");
                            mHeader.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.convertDPItoPixels(getActivity(), 48)));
                            mList.addHeaderView(mHeader);

                        }
                    }
                });
            }
        }


        builder.setView(mLayoutView).setTitle(getString(R.string.choose_set)).setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        }).setPositiveButton(R.string.next, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlickrService.getInstance(getActivity()).getUserByName(mUserEditText.getText().toString(), new FlickrServiceInterface.IRequestListener<FlickrApiData.UserByNameResponse>() {
                    @Override
                    public void onFailure() {
                        Toast.makeText(getActivity(), getString(R.string.user_not_found), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onSuccess(FlickrApiData.UserByNameResponse response) {
                        if (response.user == null) {
                            Toast.makeText(getActivity(), getString(R.string.user_not_found), Toast.LENGTH_LONG).show();
                        } else {
                            findSet(response.user.nsid);
                            mUsername = mUserEditText.getText().toString();
                        }
                    }
                });

            }
        });
        return builder;
    }

    private void findSet(String userId) {
        mList.setVisibility(View.GONE);
        mEmpty.setVisibility(View.VISIBLE);
        mUserEditText.setVisibility(View.GONE);
        FlickrService.getInstance(getActivity()).findSetByUser(userId, new FlickrServiceInterface.IRequestListener<FlickrApiData.PhotosSetsResponse>() {
            @Override
            public void onFailure() {

            }

            @Override
            public void onSuccess(FlickrApiData.PhotosSetsResponse response) {
                if (response == null || response.photosets == null || response.photosets.photoset == null || response.photosets.photoset.size() == 0) {
                    Toast.makeText(getActivity(), getString(R.string.user_not_set), Toast.LENGTH_LONG).show();
                } else {
                    for (FlickrApiData.PhotoSet set : response.photosets.photoset) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Set title: " + set.title._content);

                    }
                    List<ImportableData> sets = new ArrayList<ImportableData>();
                    sets.addAll(response.photosets.photoset);

                    mAdapter = new ItemImportAdapter(getActivity(), 0, sets);

                    mList.removeHeaderView(mHeader);
                    mList.setAdapter(mAdapter);
                    mList.setVisibility(View.VISIBLE);
                    mEmpty.setVisibility(View.GONE);

                    Utils.hideKeyboard(mUserEditText);

                    getPositiveButton().setText(getString(android.R.string.ok));
                    getPositiveButton().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            positiveButtonAction();
                        }
                    });

                }
            }
        });
    }

    private void positiveButtonAction() {
        mEmpty.setVisibility(View.VISIBLE);
        mList.setVisibility(View.GONE);
        mSelectAllContainer.setVisibility(View.GONE);

        //Hide cancel button

        getPositiveButton().setEnabled(false);


        final List<ImportableData> itemsToImport = mAdapter.getCheckedItems();

        if (itemsToImport.size() == 0) {
            dismiss();
            return;
        }

        setCancelable(false);
        for (final ImportableData item : itemsToImport) {

            FlickrApiData.PhotoSet setResponse = (FlickrApiData.PhotoSet) item;


            FSet set = new FSet(setResponse.id, mUsername + " / " + setResponse.getName(), 0, Integer.valueOf(setResponse.photos));
            set.save();

            dismiss();
        }
    }


    private class ContactAdapter extends ArrayAdapter<FlickrApiData.Contact> {

        private final List<FlickrApiData.Contact> mItems;
        private Context mContext;

        public ContactAdapter(Context context, int textViewResourceId, List<FlickrApiData.Contact> objects) {
            super(context, textViewResourceId, objects);
            mContext = context;
            mItems = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = View.inflate(mContext, R.layout.group_chooser_item, null);
            TextView groupName = (TextView) convertView.findViewById(R.id.text);


            groupName.setText(mItems.get(position).getName());
            return convertView;
        }

    }


    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        ChooseSetDialogListener activity = (ChooseSetDialogListener) getActivity();
        if (activity != null)
            activity.onFinishChoosingDialog(mChosenSet);
    }

    public interface ChooseSetDialogListener {
        void onFinishChoosingDialog(FlickrApiData.PhotoSet set);
    }

}
