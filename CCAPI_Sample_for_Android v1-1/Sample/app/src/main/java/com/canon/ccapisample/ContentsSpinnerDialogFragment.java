package com.canon.ccapisample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.json.JSONException;
import org.json.JSONObject;

import static com.canon.ccapisample.Constants.CCAPI.Field.ACTION;
import static com.canon.ccapisample.Constants.CCAPI.Field.VALUE;
import static com.canon.ccapisample.Constants.CCAPI.Method.PUT;
import static com.canon.ccapisample.Constants.CCAPI.Value.ARCHIVE;
import static com.canon.ccapisample.Constants.CCAPI.Value.PROTECT;
import static com.canon.ccapisample.Constants.CCAPI.Value.RATING;
import static com.canon.ccapisample.Constants.CCAPI.Value.ROTATE;
import static com.canon.ccapisample.Constants.ContentsViewer.SAVE_ARRAY;
import static com.canon.ccapisample.Constants.ContentsViewer.SAVE_EMBEDDED;
import static com.canon.ccapisample.Constants.ContentsViewer.SAVE_DISPLAY;
import static com.canon.ccapisample.Constants.ContentsViewer.SAVE_ORIGINAL;

public class ContentsSpinnerDialogFragment extends DialogFragment {
    enum ActionType{
        ACTION_SAVE,
        ACTION_ROTATE,
        ACTION_PROTECT,
        ACTION_ARCHIVE,
        ACTION_RATING
    }
    private static final String[] ROTATE_ARRAY = {"0", "90", "180", "270"};
    private static final String[] PROTECT_ARRAY = {"enable", "disable"};
    private static final String[] ARCHIVE_ARRAY = {"enable", "disable"};
    private static final String[] RATING_ARRAY = {"off", "1", "2", "3", "4", "5"};

    private static final String TAG = ContentsSpinnerDialogFragment.class.getSimpleName();
    private static final String ACTION_TYPE = "Action";
    private static final String STORAGE_CONTENTS_DATA_SET = "ContentsDataSet";
    private WebAPI mWebAPI;
    private ActionType mAction;
    private ContentsDataSet mContentsDataSet;
    private Spinner mSpinner;

    public static ContentsSpinnerDialogFragment newInstance(Fragment target, ActionType action, ContentsDataSet contentsDataSet){
        ContentsSpinnerDialogFragment instance = new ContentsSpinnerDialogFragment();
        instance.setTargetFragment(target, 0);
        Bundle arguments = new Bundle();
        arguments.putSerializable(ACTION_TYPE, action);
        arguments.putSerializable(STORAGE_CONTENTS_DATA_SET, contentsDataSet);
        instance.setArguments(arguments);
        return instance;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle arguments) {
        Log.d(TAG, "onCreateDialog");

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        mWebAPI = WebAPI.getInstance();

        LinearLayout layout = new LinearLayout(getActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        mSpinner = new Spinner(getActivity());

        if (getArguments() != null) {
            String[] array = {};
            mAction = (ActionType) getArguments().getSerializable(ACTION_TYPE);
            mContentsDataSet = (ContentsDataSet) getArguments().getSerializable(STORAGE_CONTENTS_DATA_SET);

            if( mAction != null && mContentsDataSet != null) {
                switch (mAction) {
                    case ACTION_SAVE:
                        array = SAVE_ARRAY;
                        break;
                    case ACTION_ROTATE:
                        array = ROTATE_ARRAY;
                        break;
                    case ACTION_PROTECT:
                        array = PROTECT_ARRAY;
                        break;
                    case ACTION_ARCHIVE:
                        array = ARCHIVE_ARRAY;
                        break;
                    case ACTION_RATING:
                        array = RATING_ARRAY;
                        break;
                    default:
                        break;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, array);
                mSpinner.setAdapter(adapter);

                dialogBuilder.setTitle(mContentsDataSet.getName() + " : " + mAction.toString());
            }
        }

        layout.addView(mSpinner);
        dialogBuilder.setView(layout);

        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (mAction) {
                    case ACTION_SAVE:
                        downloadContents((String) mSpinner.getSelectedItem());
                        break;
                    case ACTION_ROTATE:
                    case ACTION_PROTECT:
                    case ACTION_ARCHIVE:
                    case ACTION_RATING:
                        putAction((String) mSpinner.getSelectedItem());
                        break;
                    default:
                        break;
                }
            }
        });

        return dialogBuilder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        // The minHeight in the custom layout of dialogs is 48dp.
        // Therefore unnecessary space is displayed.
        // So, disable it immediately before displaying a dialog.
        if(getDialog() != null) {
            FrameLayout frameLayout = getDialog().findViewById(android.R.id.custom);
            if (frameLayout != null && frameLayout.getParent() != null) {
                ((FrameLayout) frameLayout.getParent()).setMinimumHeight(0);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    private void downloadContents(String type){
        String url = mContentsDataSet.getUrl();
        String name = "";

        switch (type){
            case SAVE_ORIGINAL:
                name = mContentsDataSet.getName();
                break;
            case SAVE_DISPLAY:
                url += "?kind=display";
                name = mContentsDataSet.getNameNoExtension() + ".JPG";
                break;
            case SAVE_EMBEDDED:
                url += "?kind=embedded";
                name = mContentsDataSet.getNameNoExtension() + ".JPG";
                break;
            default:
                name = mContentsDataSet.getName();
                break;
        }

        ContentsDownloader contentsDownloader = new ContentsDownloader(new Handler());
        contentsDownloader.execute(getActivity(), name, url);
    }

    private void putAction(String value){
        JSONObject body = new JSONObject();
        try {
            String action = "";

            switch (mAction) {
                case ACTION_ROTATE:
                    action = ROTATE;
                    break;
                case ACTION_PROTECT:
                    action = PROTECT;
                    break;
                case ACTION_ARCHIVE:
                    action = ARCHIVE;
                    break;
                case ACTION_RATING:
                    action = RATING;
                    break;
                default:
                    break;
            }

            body.put(ACTION, action);
            body.put(VALUE, value);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        Log.d(TAG, body.toString());
        Bundle args = new Bundle();
        String[] params = new String[]{PUT, mContentsDataSet.getUrl(), body.toString()};
        args.putStringArray(Constants.RequestCode.ACT_WEB_API.name(), params);
        mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.ACT_WEB_API, args, (WebAPIResultListener) getTargetFragment()));
    }
}
