package com.tashila.sweetdreams;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class DialogSleepingTime extends DialogFragment {
    private View view;
    private SharedPreferences sharedPref;
    private EditText etTime;
    private AlertDialog dialog;
    private TextInputLayout tilTime;


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        view = getActivity().getLayoutInflater().inflate(R.layout.dialog_sleeping_time, null);
        sharedPref = getActivity().getSharedPreferences("myPref", Context.MODE_PRIVATE);

        tilTime = view.findViewById(R.id.time);
        etTime = tilTime.getEditText();

        dialog = new MaterialAlertDialogBuilder(getActivity())
                .setTitle("How long does it take for you to fall asleep?")
                .setView(view)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        etTime.setText(String.valueOf(sharedPref.getInt("sleepingTime", 14)));

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save();
            }
        });
    }

    private void save() {
        String txtTime = etTime.getText().toString();
        if (txtTime.isEmpty())
            tilTime.setError("Required");
        else {
            tilTime.setError(null);
            sharedPref.edit().putInt("sleepingTime", Integer.parseInt(txtTime)).apply();
            Toast.makeText(getActivity(), "Saved", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }
    }
}
