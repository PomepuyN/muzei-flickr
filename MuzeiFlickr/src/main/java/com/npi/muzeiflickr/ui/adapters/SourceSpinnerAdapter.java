package com.npi.muzeiflickr.ui.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.npi.muzeiflickr.R;
import com.npi.muzeiflickr.utils.Utils;


/**
 * Created by nicolas on 21/02/14.
 */
public class SourceSpinnerAdapter extends ArrayAdapter<CharSequence> {

    private final Context mContext;

    public SourceSpinnerAdapter(Context context, int resource, CharSequence[] objects) {
        super(context, resource, objects);
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

        TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
        switch (position) {
            case 0:
                textView.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(R.drawable.icon_search), null, null, null);
                break;
            case 1:
                textView.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(R.drawable.icon_user), null, null, null);
                break;
            case 2:
                textView.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(R.drawable.icon_tag), null, null, null);
                break;
            case 3:
                textView.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(R.drawable.icon_group), null, null, null);
                break;
        }
        textView.setText(getItem(position));
        textView.setCompoundDrawablePadding(Utils.convertDPItoPixels(mContext, 5));
        if (dropdown) {
            textView.setHeight(Utils.convertDPItoPixels(mContext, 40));
        }

        return convertView;
    }
}
