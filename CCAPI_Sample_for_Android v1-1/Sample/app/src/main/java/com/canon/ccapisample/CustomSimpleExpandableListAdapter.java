package com.canon.ccapisample;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;

import java.util.List;
import java.util.Map;

class CustomSimpleExpandableListAdapter extends SimpleExpandableListAdapter {
    private List<Boolean> mEditableFrom;

    CustomSimpleExpandableListAdapter(Context context,
                                      List<? extends Map<String, ?>> groupData,
                                      int groupLayout,
                                      String[] groupFrom,
                                      int[] groupTo,
                                      List<? extends List<? extends Map<String, ?>>> childData,
                                      int childLayout,
                                      String[] childFrom,
                                      int[] childTo,
                                      List<Boolean> editableFrom) {
        super(context, groupData, groupLayout, groupFrom, groupTo, childData, childLayout, childFrom, childTo);
        mEditableFrom = editableFrom;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        View v = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);

        ImageView editableImage = v.findViewById(R.id.EditableImageView);
        if(mEditableFrom.size() > groupPosition && mEditableFrom.get(groupPosition)) {
            editableImage.setVisibility(View.VISIBLE);
        }
        else{
            editableImage.setVisibility(View.GONE);
        }

        return v;
    }
}
