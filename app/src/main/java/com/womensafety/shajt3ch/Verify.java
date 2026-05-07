package com.womensafety.shajt3ch;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Verify extends Fragment implements View.OnClickListener {

    private TextInputLayout source_no;
    private static EditText phone;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.selfregister, container, false);

        source_no = view.findViewById(R.id.verify);
        phone = view.findViewById(R.id.editText1);

        Button submit = view.findViewById(R.id.button1);
        submit.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {

        String str_source_no = phone.getText().toString();

        SQLiteDatabase db =
                requireActivity().openOrCreateDatabase(
                        "NumDB",
                        Context.MODE_PRIVATE,
                        null
                );

        db.execSQL("CREATE TABLE IF NOT EXISTS source(number VARCHAR);");
        db.execSQL("INSERT INTO source VALUES('" + str_source_no + "');");

        Toast.makeText(
                getActivity(),
                str_source_no + " Successfully Saved",
                Toast.LENGTH_SHORT
        ).show();

        db.close();
    }
}