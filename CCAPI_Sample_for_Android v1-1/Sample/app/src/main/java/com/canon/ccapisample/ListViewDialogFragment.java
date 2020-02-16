package com.canon.ccapisample;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.canon.ccapisample.Constants.CCAPI.Field.MAX;
import static com.canon.ccapisample.Constants.CCAPI.Field.MIN;
import static com.canon.ccapisample.Constants.CCAPI.Field.STEP;
import static com.canon.ccapisample.Constants.CCAPI.Field.VALUE;
import static com.canon.ccapisample.Constants.CCAPI.Method.DELETE;
import static com.canon.ccapisample.Constants.CCAPI.Method.PUT;
import static com.canon.ccapisample.Constants.Settings.Key.KEY;
import static com.canon.ccapisample.Constants.Settings.Key.NAME;

public class ListViewDialogFragment extends DialogFragment {
    private static final String TAG = ListViewDialogFragment.class.getSimpleName();
    private static final String SETTINGS_INFO = "SettingsInfo";
    private static final String LIST_VIEW_SETTINGS = "ListViewDataSet";
    private WebAPI mWebAPI;
    private JSONObject mSettingsInfo;
    private ListViewDataSet mListViewDataSet;
    private Map<String, EditText> mStringViewMap = new HashMap<>();
    private Map<String, Spinner> mEnumViewMap = new HashMap<>();
    private Map<String, EditText> mRangeViewMap = new HashMap<>();

    public static ListViewDialogFragment newInstance(Fragment target, JSONObject info, ListViewDataSet listViewDataSet){
        ListViewDialogFragment instance = new ListViewDialogFragment();
        instance.setTargetFragment(target, 0);
        Bundle arguments = new Bundle();
        arguments.putString(SETTINGS_INFO, info.toString());
        arguments.putSerializable(LIST_VIEW_SETTINGS, listViewDataSet);
        instance.setArguments(arguments);
        return instance;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle arguments) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        mWebAPI = WebAPI.getInstance();

        if (getArguments() != null) {
            String name = "";

            try {
                mSettingsInfo = new JSONObject(getArguments().getString(SETTINGS_INFO));
                name = mSettingsInfo.getString(NAME);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }

            mListViewDataSet = (ListViewDataSet) getArguments().getSerializable(LIST_VIEW_SETTINGS);

            // Generate a parent layout.
            LinearLayout parentLayout = new LinearLayout(getActivity());
            parentLayout.setOrientation(LinearLayout.VERTICAL);

            // Set the title.
            dialog.setTitle(name);

            // Generate a View of setting items.
            createLayout(parentLayout);

            // Set the dialog button.
            setDialogButton(dialog);

            dialog.setView(parentLayout);
        }

        return dialog.create();
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

    private void createLayout(LinearLayout parentLayout){
        Context context = getActivity();

        List<String> items = mListViewDataSet.getItems();
        Map<String, ListViewDataSet.DataType> type = mListViewDataSet.getDataType();

        for(String item : items) {
            LinearLayout subLayout = new LinearLayout(context);
            subLayout.setOrientation(LinearLayout.HORIZONTAL);

            if (items.size() > 1) {
                TextView textView = new TextView(context);
                textView.setText(item);
                textView.setGravity(Gravity.CENTER);
                subLayout.addView(textView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
            }

            switch (type.get(item)) {
                case STRING:
                    createStringLayout(subLayout, item);
                    break;
                case ENUM:
                    createEnumLayout(subLayout, item);
                    break;
                case RANGE:
                    createRangeLayout(subLayout, item);
                    break;
                default:
                    break;
            }

            parentLayout.addView(subLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }
    }

    private void createStringLayout(LinearLayout layout, String key) {
        String value = mListViewDataSet.getValue().get(key);
        EditText editText = new EditText(getActivity());
        editText.setText(value);
        layout.addView(editText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
        mStringViewMap.put(key, editText);
    }

    private void createEnumLayout(LinearLayout layout, String key){
        Map<String, List<String>> ability = mListViewDataSet.getEnumAbility();
        Map<String, String> current = mListViewDataSet.getValue();
        int index = 0;
        String[] list = {""};
        Spinner spinner = new Spinner(getActivity());

        if( ability.size() != 0 && ability.containsKey(key) ){
            list = ability.get(key).toArray(new String[0]);
        }
        else{
            spinner.setEnabled(false);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, list);
        spinner.setAdapter(adapter);

        for(String item:list){
            if(item.equals(current.get(key))){
                spinner.setSelection(index);
                break;
            }
            index++;
        }

        layout.addView(spinner, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
        mEnumViewMap.put(key, spinner);
    }

    private void createRangeLayout(LinearLayout layout, final String key){
        Map<String, Map<String, Integer>> ability = mListViewDataSet.getRangeAbility();
        final Map<String, String> value = mListViewDataSet.getValue();

        Context context = getActivity();
        LinearLayout rangeLayout = new LinearLayout(context);
        final EditText numberText = new EditText(context);
        Button plusButton = new Button(context);
        Button minusButton = new Button(context);
        TextView rangeText = new TextView(context);

        if( ability.size() != 0 && ability.containsKey(key) ) {
            Map<String, Integer> range = ability.get(key);
            final int min = range.get(MIN);
            final int max = range.get(MAX);
            final int step = range.get(STEP);
            rangeLayout.setOrientation(LinearLayout.HORIZONTAL);

            numberText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            numberText.setGravity(Gravity.CENTER);
            numberText.setText(value.get(key));
            plusButton.setText("+");
            minusButton.setText("-");
            rangeText.setText(String.format(Locale.US, "(%d-%d)", min, max));

            plusButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String str = numberText.getText().toString();
                    if (!str.isEmpty()) {
                        try {
                            int num = Integer.valueOf(str);
                            num += step;
                            if (num >= min && num <= max) {
                                if (num % step != 0) {
                                    num -= num % step;
                                }
                                numberText.setText(String.valueOf(num));
                            }
                            else if (num < min) {
                                numberText.setText(String.valueOf(min));
                            }
                            else {
                                numberText.setText(String.valueOf(max));
                            }
                        }
                        catch (NumberFormatException | ArithmeticException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            minusButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String str = numberText.getText().toString();
                    if (!str.isEmpty()) {
                        try {
                            int num = Integer.valueOf(str);
                            num -= step;
                            if (num >= min && num <= max) {
                                if (num % step != 0) {
                                    num -= num % step;
                                }
                                numberText.setText(String.valueOf(num));
                            }
                            else if (num < min) {
                                numberText.setText(String.valueOf(min));
                            }
                            else {
                                numberText.setText(String.valueOf(max));
                            }
                        }
                        catch (NumberFormatException | ArithmeticException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        else{
            numberText.setEnabled(false);
            minusButton.setEnabled(false);
            plusButton.setEnabled(false);
        }

        rangeLayout.addView(numberText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
        rangeLayout.addView(minusButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        rangeLayout.addView(plusButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        rangeLayout.addView(rangeText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));

        layout.addView(rangeLayout, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
        mRangeViewMap.put(key, numberText);
    }

    private void setDialogButton(AlertDialog.Builder dialog){
        String key = "";
        try {
            key = mSettingsInfo.getString(KEY);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        final APIDataSet api = mWebAPI.getAPIData(key);

        if(api != null) {
            if (api.isDeletable()) {
                dialog.setNegativeButton("Reset", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Bundle args = new Bundle();
                        String[] params = new String[]{DELETE, api.getUrl(), null};
                        args.putStringArray(Constants.RequestCode.ACT_WEB_API.name(), params);

                        // Execute the API.
                        // The onWebAPIResult() of caller Fragment process the execution result, because the dialog is closed.
                        mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.ACT_WEB_API, args, new WebAPIResultListener() {
                            @Override
                            public void onWebAPIResult(WebAPIResultDataSet result) {
                                if (getTargetFragment() instanceof WebAPIResultListener) {
                                    ((WebAPIResultListener) getTargetFragment()).onWebAPIResult(result);
                                }
                            }
                        }));
                    }
                });
            }

            if (api.isPutable()) {
                dialog.setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setParameter(api);
                    }
                });
            }
        }
    }

    void setParameter(APIDataSet api){
        List<String> items = mListViewDataSet.getItems();
        Bundle args = new Bundle();
        JSONObject body = new JSONObject();
        JSONObject valueSub = new JSONObject();

        try {
            if(mListViewDataSet.isSettable()) {
                Map<String, ListViewDataSet.DataType> type = mListViewDataSet.getDataType();

                for (String item : items) {
                    switch (type.get(item)) {
                        case STRING: {
                            body.put(item, mStringViewMap.get(item).getText().toString());
                            break;
                        }
                        case ENUM: {
                            String value = (String) mEnumViewMap.get(item).getSelectedItem();
                            if (items.size() > 1) {
                                valueSub.put(item, value);
                            } else {
                                body.put(VALUE, value);
                            }
                            break;
                        }
                        case RANGE: {
                            String value = mRangeViewMap.get(item).getText().toString();
                            if (!value.isEmpty()) {
                                try {
                                    if (items.size() > 1) {
                                        valueSub.put(item, Integer.valueOf(value));
                                    } else {
                                        body.put(VALUE, Integer.valueOf(value));
                                    }
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                        }
                        default:
                            break;
                    }
                }
            }
            else{
                // In the case of items which can not be changed.
                Map<String, String> valueMap = mListViewDataSet.getValue();

                if(valueMap.size() != 0){
                    // Generate JSON using values, if it have been got.
                    for (String item : items) {
                        String value = valueMap.get(item);
                        if (items.size() > 1) {
                            valueSub.put(item, value);
                        } else {
                            body.put(VALUE, value);
                        }
                    }
                }
                else{
                    body.put(VALUE, "");
                }
            }

            if (valueSub.length() > 0) {
                body.put(VALUE, valueSub);
            }

            Log.d(TAG, mListViewDataSet.getName() + " : " +  body.toString());

            String[] params = new String[]{PUT, api.getUrl(), body.toString()};
            args.putStringArray(Constants.RequestCode.ACT_WEB_API.name(), params);

            // Execute the API.
            // The onWebAPIResult() of caller Fragment process the execution result, because the dialog is closed.
            mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.ACT_WEB_API, args, new WebAPIResultListener() {
                @Override
                public void onWebAPIResult(WebAPIResultDataSet result) {
                    if (getTargetFragment() instanceof WebAPIResultListener) {
                        ((WebAPIResultListener) getTargetFragment()).onWebAPIResult(result);
                    }
                }
            }));
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
