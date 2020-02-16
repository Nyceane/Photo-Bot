package com.canon.ccapisample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import static com.canon.ccapisample.Constants.CCAPI.Value.ARCHIVE;
import static com.canon.ccapisample.Constants.CCAPI.Value.PROTECT;
import static com.canon.ccapisample.Constants.CCAPI.Value.RATING;
import static com.canon.ccapisample.Constants.CCAPI.Value.ROTATE;

public class ContentsEditDialogFragment extends DialogFragment implements AdapterView.OnItemClickListener {
    private static final String[] EDIT_MENU = {ROTATE, PROTECT, ARCHIVE, RATING};
    private static final String STORAGE_CONTENTS_DATA_SET = "ContentsDataSet";
    private ContentsDataSet mContentsDataSet;

    public static ContentsEditDialogFragment newInstance(Fragment target, ContentsDataSet contentsDataSet){
        ContentsEditDialogFragment instance = new ContentsEditDialogFragment();
        instance.setTargetFragment(target, 0);
        Bundle arguments = new Bundle();
        arguments.putSerializable(STORAGE_CONTENTS_DATA_SET, contentsDataSet);
        instance.setArguments(arguments);
        return instance;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle arguments) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

        if (getArguments() != null) {
            mContentsDataSet = (ContentsDataSet) getArguments().getSerializable(STORAGE_CONTENTS_DATA_SET);
            if(mContentsDataSet != null) {
                dialogBuilder.setTitle(mContentsDataSet.getName());

                ListView listView = new ListView(getActivity());
                ArrayAdapter arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, EDIT_MENU);
                listView.setAdapter(arrayAdapter);
                listView.setOnItemClickListener(this);
                dialogBuilder.setView(listView);
            }
        }
        return dialogBuilder.create();
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ListView listView = (ListView) parent;
        String menu = (String) listView.getItemAtPosition(position);

        switch(menu){
            case ROTATE: {
                ContentsSpinnerDialogFragment dialog = ContentsSpinnerDialogFragment.newInstance(
                        getTargetFragment(),
                        ContentsSpinnerDialogFragment.ActionType.ACTION_ROTATE,
                        mContentsDataSet);
                dialog.show(getActivity().getSupportFragmentManager(), mContentsDataSet.getName());
                break;
            }
            case PROTECT: {
                ContentsSpinnerDialogFragment dialog = ContentsSpinnerDialogFragment.newInstance(
                        getTargetFragment(),
                        ContentsSpinnerDialogFragment.ActionType.ACTION_PROTECT,
                        mContentsDataSet);
                dialog.show(getActivity().getSupportFragmentManager(), mContentsDataSet.getName());
                break;
            }
            case ARCHIVE: {
                ContentsSpinnerDialogFragment dialog = ContentsSpinnerDialogFragment.newInstance(
                        getTargetFragment(),
                        ContentsSpinnerDialogFragment.ActionType.ACTION_ARCHIVE,
                        mContentsDataSet);
                dialog.show(getActivity().getSupportFragmentManager(), mContentsDataSet.getName());
                break;
            }
            case RATING: {
                ContentsSpinnerDialogFragment dialog = ContentsSpinnerDialogFragment.newInstance(
                        getTargetFragment(),
                        ContentsSpinnerDialogFragment.ActionType.ACTION_RATING,
                        mContentsDataSet);
                dialog.show(getActivity().getSupportFragmentManager(), mContentsDataSet.getName());
                break;
            }
            default:
                break;
        }

        dismiss();
    }
}
