package com.npi.muzeiflickr.ui.dialogs;

import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.npi.muzeiflickr.BuildConfig;
import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.data.ImportableData;
import com.npi.muzeiflickr.db.FSet;
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
    private View mLayoutView;
    private ListView mList;
    private FlickrApiData.PhotoSet mChosenSet;
    private EditText mUserEditText;
    private ProgressBar mEmpty;
    private CheckBox mSelectAll;
    private RelativeLayout mSelectAllContainer;
    private TextView mImportResult;
    private ItemImportAdapter mAdapter;

    public SetChooserDialog() {
        super();
    }

    public static void show(FragmentActivity activity) {
        SetChooserDialog dialog = new SetChooserDialog();
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
                        }
                    }
                });

            }
        });
        return builder;
    }

    private void findSet(String userId) {
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

                    mList.setAdapter(mAdapter);
                    mList.setVisibility(View.VISIBLE);
                    mUserEditText.setVisibility(View.GONE);
                    Utils.hideKeyboard(mUserEditText);

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


            FSet set = new FSet(setResponse.id, mUserEditText.getText().toString() + " / " + setResponse.getName(), 0, Integer.valueOf(setResponse.photos));
            set.save();

            dismiss();
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
