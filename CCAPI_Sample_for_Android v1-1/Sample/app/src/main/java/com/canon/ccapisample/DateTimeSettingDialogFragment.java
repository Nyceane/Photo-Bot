package com.canon.ccapisample;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeSettingDialogFragment extends DialogFragment implements WebAPIResultListener, View.OnClickListener{
    private static final String TAG = DateTimeSettingDialogFragment.class.getSimpleName();
    static final String[] WEEK_ITEMS = {
            "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };
    static final String[] MONTH_ITEMS = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private WebAPI mWebAPI;
    private TextView mDateTextView;
    private TextView mTimeTextView;
    private Spinner mTimeZoneSignSpinner;
    private EditText mTimeZoneEdit;
    private CheckBox mDstCheckBox;
    private DatePickerDialog.OnDateSetListener mOnDateSetListener;
    private TimePickerDialog.OnTimeSetListener mOnTimeSetListener;

    public DateTimeSettingDialogFragment() {
        // Required empty public constructor
    }

    public static DateTimeSettingDialogFragment newInstance(Fragment target) {
        DateTimeSettingDialogFragment fragment = new DateTimeSettingDialogFragment();
        fragment.setTargetFragment(target, 0);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle arguments) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        mWebAPI = WebAPI.getInstance();

        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_date_time_setting, null);

        LinearLayout layout = view.findViewById(R.id.DateTimeSetting);
        view.findViewById(R.id.DateEditButton).setOnClickListener(this);
        view.findViewById(R.id.TimeEditButton).setOnClickListener(this);

        mDateTextView = view.findViewById(R.id.DateText);
        mTimeTextView = view.findViewById(R.id.TimeText);
        mTimeZoneSignSpinner = view.findViewById(R.id.TimeZoneSignSpinner);
        mTimeZoneEdit = view.findViewById(R.id.TimeZoneEdit);
        mDstCheckBox = view.findViewById(R.id.DstCheckBox);

        List<String> items = new ArrayList<String>();
        items.add("+");
        items.add("-");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, items.toArray(new String[0]));
        mTimeZoneSignSpinner.setAdapter(adapter);
        mTimeZoneSignSpinner.setSelection(0);

        mOnDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mDateTextView.setText(year + "-" + String.format(Locale.US, "%02d", (monthOfYear + 1)) + "-" + String.format(Locale.US, "%02d", dayOfMonth));
            }
        };

        mOnTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                mTimeTextView.setText(String.format(Locale.US, "%02d", hourOfDay) + ":" + String.format(Locale.US, "%02d", minute) + ":00");
            }
        };

        dialog.setTitle("datetime");
        dialog.setView(layout);
        dialog.setPositiveButton("Set", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                setDateTime();
            }
        });

        mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.GET_FUNCTIONS_DATETIME, null, this));

        return dialog.create();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    public void onClick(View v){
        if (v != null) {
            switch (v.getId()) {
                case R.id.DateEditButton:
                    String dateText = mDateTextView.getText().toString();
                    String[] dateList = dateText.split("-");
                    int year = Integer.valueOf(dateList[0]);
                    int month = Integer.valueOf(dateList[1]) - 1; // 0-11
                    int day = Integer.valueOf(dateList[2]);

                    DatePickerDialog dateDialog = new DatePickerDialog(
                            getActivity(),
                            mOnDateSetListener,
                            year,
                            month,
                            day);
                    dateDialog.show();
                    break;

                case R.id.TimeEditButton:
                    String timeText = mTimeTextView.getText().toString();
                    String[] timeList = timeText.split(":");
                    int hour = Integer.valueOf(timeList[0]);
                    int minute = Integer.valueOf(timeList[1]);
                    TimePickerDialog timeDialog = new TimePickerDialog(
                            getActivity(),
                            mOnTimeSetListener,
                            hour,
                            minute,
                            true);
                    timeDialog.show();
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * Callback from execute WebAPI
     * @param result HTTP Request result
     */
    @Override
    public void onWebAPIResult(WebAPIResultDataSet result){
        Log.d(TAG, String.format("%s onWebAPIResult", String.valueOf(result.getRequestCode())));
        Context context = getActivity();

        // Do nothing, if life cycle of the fragment is finished.
        if(context != null) {
            if (result.isError()) {
                Toast.makeText(context, result.getErrorMsg(), Toast.LENGTH_SHORT).show();
            }
            else {
                switch (result.getRequestCode()) {
                    case GET_FUNCTIONS_DATETIME:
                        Pattern p = Pattern.compile("^\\D{3},\\s(\\d{2})\\s(\\D{3})\\s(\\d{4})\\s(\\d{2}:\\d{2}:\\d{2})\\s(\\D)(\\d{4})");
                        Matcher m = p.matcher(result.getResponseBodyFromKey("datetime"));

                        if (m.find()) {
                            String month = String.format(Locale.US, "%02d", Arrays.asList(MONTH_ITEMS).indexOf(m.group(2)) + 1);
                            mDateTextView.setText(m.group(3) + "-" + month + "-" + m.group(1));
                            mTimeTextView.setText(m.group(4));
                            mTimeZoneEdit.setText(m.group(6));

                            if(m.group(5).equals("+")) {
                                mTimeZoneSignSpinner.setSelection(0);
                            }
                            else{
                                mTimeZoneSignSpinner.setSelection(1);
                            }

                            mDstCheckBox.setChecked(Boolean.valueOf(result.getResponseBodyFromKey("dst")));
                        }
                        break;
                    case PUT_FUNCTIONS_DATETIME:
                        Toast.makeText(getActivity(), "Success.", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void setDateTime(){
        Calendar cal = Calendar.getInstance();
        String[] date = mDateTextView.getText().toString().split("-");
        cal.set(Integer.valueOf(date[0]), Integer.valueOf(date[1]) - 1, Integer.valueOf(date[2]));

        String week = WEEK_ITEMS[cal.get(Calendar.DAY_OF_WEEK) - 1];
        String day = String.format(Locale.US, "%02d", Integer.valueOf(date[2]));
        String month = MONTH_ITEMS[Integer.valueOf(date[1]) - 1];
        String year = date[0];
        String time = mTimeTextView.getText().toString();
        String sign = mTimeZoneSignSpinner.getSelectedItem().toString();;
        String timezone = sign + mTimeZoneEdit.getText().toString();

        String datetime = week + ", " + day + " " + month + " " + year + " " + time + " " + timezone;
        String dst = String.valueOf(mDstCheckBox.isChecked());

        String[] params = { datetime, dst };

        Bundle args = new Bundle();
        args.putStringArray(Constants.RequestCode.PUT_FUNCTIONS_DATETIME.name(), params);

        // Execute the API.
        // The onWebAPIResult() of caller Fragment process the execution result, because the dialog is closed.
        mWebAPI.enqueueRequest(new WebAPIQueueDataSet(Constants.RequestCode.PUT_FUNCTIONS_DATETIME, args, new WebAPIResultListener() {
            @Override
            public void onWebAPIResult(WebAPIResultDataSet result) {
                if (getTargetFragment() instanceof WebAPIResultListener) {
                    ((WebAPIResultListener) getTargetFragment()).onWebAPIResult(result);
                }
            }
        }));
    }
}
