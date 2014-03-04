package com.npi.muzeiflickr.ui.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.data.SourceAdapterItem;
import com.npi.muzeiflickr.utils.Utils;


/**
 * Created by nicolas on 21/02/14.
 */
public class SourceSpinnerAdapter extends ArrayAdapter<SourceAdapterItem> {

    private final Context mContext;

    public SourceSpinnerAdapter(Context context, int resource) {
        super(context, resource, SourceAdapterItem.getFilteredEntries());
        mContext = context;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        convertView = View.inflate(mContext, android.R.layout.simple_spinner_dropdown_item, null);
        return getCustomView(convertView, position, true);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {


        convertView = View.inflate(mContext, android.R.layout.simple_spinner_item, null);
        return getCustomView(convertView, position, false);
    }

    private View getCustomView(View convertView, int position, boolean dropdown) {

        SourceAdapterItem item = getItem(position);

        TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
        switch (item) {
            case SEARCH:
                textView.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(R.drawable.icon_search), null, null, null);
                break;
            case USER:
                textView.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(R.drawable.icon_user), null, null, null);
                break;
            case TAG:
                textView.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(R.drawable.icon_tag), null, null, null);
                break;
            case GROUP:
                textView.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(R.drawable.icon_group), null, null, null);
                break;
            case FAVORITES:
                textView.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(R.drawable.icon_favorite), null, null, null);
                break;
            case INTERESTINGNESS:
                textView.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(R.drawable.thumb_up), null, null, null);
                break;
        }
        textView.setText(mContext.getString(item.getTitleResource()));
        textView.setCompoundDrawablePadding(Utils.convertDPItoPixels(mContext, 5));
        if (dropdown) {
            textView.setHeight(Utils.convertDPItoPixels(mContext, 40));
        }

        return convertView;
    }

    public void reload() {

        clear();
        addAll(SourceAdapterItem.getFilteredEntries());


    }
}
