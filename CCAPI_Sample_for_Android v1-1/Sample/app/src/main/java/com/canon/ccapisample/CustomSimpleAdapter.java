package com.canon.ccapisample;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleAdapter;

import java.util.List;
import java.util.Map;

class CustomSimpleAdapter extends SimpleAdapter {
    private List<Boolean> mEditableFrom;
    private List<Boolean> mGrayOutFrom;

    CustomSimpleAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to, List<Boolean> editableFrom, List<Boolean> grayOutFrom) {
        super(context, data, resource, from, to);
        mEditableFrom = editableFrom;
        mGrayOutFrom = grayOutFrom;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        ImageView editableImage = view.findViewById(R.id.EditableImageView);
        if(mEditableFrom != null) {
            if (mEditableFrom.size() > position && mEditableFrom.get(position)) {
                editableImage.setVisibility(View.VISIBLE);
            }
            else {
                editableImage.setVisibility(View.GONE);
            }
        }
        else {
            editableImage.setVisibility(View.GONE);
        }

        if(mGrayOutFrom != null) {
            if (mGrayOutFrom.size() > position && mGrayOutFrom.get(position)) {
                view.setBackgroundColor(Color.LTGRAY);
            }
            else{
                view.setBackgroundColor(Color.TRANSPARENT);
            }
        }

        return view;
    }
}
