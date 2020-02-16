package com.canon.ccapisample;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

public class AuthenticateDialogFragment extends DialogFragment {

    private String mUserName = null;
    private String mPassword = null;
    private EditText mUserNameEditText;
    private EditText mPasswordEditText;

    public static AuthenticateDialogFragment newInstance(){
        AuthenticateDialogFragment instance = new AuthenticateDialogFragment();
        Bundle arguments = new Bundle();
        instance.setArguments(arguments);
        return instance;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle arguments) {
        Context context = getActivity();
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_dialog_authenticate, null);
        LinearLayout layout = view.findViewById(R.id.AuthDialogLayout);
        mUserNameEditText = view.findViewById(R.id.AuthUserNameEditText);
        mPasswordEditText = view.findViewById(R.id.AuthPasswordEditText);

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle("Authentication Required");
        dialog.setView(layout);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mUserName = mUserNameEditText.getText().toString();
                mPassword = mPasswordEditText.getText().toString();
            }
        });
        dialog.setNegativeButton("Cancel", null);

        return dialog.create();
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    public String getUserName() {
        return mUserName;
    }

    public String getPassword() {
        return mPassword;
    }
}
