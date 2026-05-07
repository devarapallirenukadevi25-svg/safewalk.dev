package com.womensafety.shajt3ch;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class Display extends Fragment implements ContactAdapter.OnContactActionListener {

    private RecyclerView rvContacts;
    private ContactAdapter adapter;
    private List<ContactModel> contactList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.display, container, false);
        rvContacts = view.findViewById(R.id.rvContacts);
        rvContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        
        loadContacts();
        
        return view;
    }

    private void loadContacts() {
        contactList = new ArrayList<>();
        try {
            if (getActivity() == null) return;
            SQLiteDatabase db = getActivity().openOrCreateDatabase("NumberDB", Context.MODE_PRIVATE, null);
            
            db.execSQL("CREATE TABLE IF NOT EXISTS details(Pname TEXT, number TEXT, keyword TEXT);");

            Cursor c = db.rawQuery("SELECT * FROM details", null);
            if (c != null) {
                while (c.moveToNext()) {
                    contactList.add(new ContactModel(c.getString(0), c.getString(1), c.getString(2)));
                }
                c.close();
            }
            db.close();
            
            adapter = new ContactAdapter(contactList, this);
            rvContacts.setAdapter(adapter);
            
            if (contactList.isEmpty()) {
                Toast.makeText(getContext(), "No contacts found.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("Display", "Load Error", e);
        }
    }

    @Override
    public void onDelete(ContactModel contact) {
        try {
            SQLiteDatabase db = getActivity().openOrCreateDatabase("NumberDB", Context.MODE_PRIVATE, null);
            db.delete("details", "number = ? AND keyword = ?", new String[]{contact.number, contact.keyword});
            db.close();
            Toast.makeText(getContext(), "Contact Deleted", Toast.LENGTH_SHORT).show();
            loadContacts(); // Refresh list
        } catch (Exception e) {
            Log.e("Display", "Delete Error", e);
        }
    }
}
