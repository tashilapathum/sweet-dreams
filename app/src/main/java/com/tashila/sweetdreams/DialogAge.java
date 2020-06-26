package com.tashila.sweetdreams;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class DialogAge extends DialogFragment {
    View view;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        view = getActivity().getLayoutInflater().inflate(R.layout.dialog_age, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setView(view)
                .setIcon(R.drawable.ic_birthday)
                .setCancelable(false)
                .setTitle("When is your birthday?");

        return builder.create();
    }
}
