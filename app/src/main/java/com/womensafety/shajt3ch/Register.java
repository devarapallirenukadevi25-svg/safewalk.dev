package com.womensafety.shajt3ch;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import static android.content.Context.MODE_PRIVATE;

public class Register extends Fragment implements View.OnClickListener {

    private static final int REQUEST_CONTACT = 1;
    private static final int PERMISSION_CONTACTS = 2;

    private EditText name, number, keyword, myUserNumber;
    private SQLiteDatabase db;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.contacts_register,
                container,
                false
        );

        name = view.findViewById(R.id.editText2);
        number = view.findViewById(R.id.editText3);
        keyword = view.findViewById(R.id.editTextKeyword);
        myUserNumber = view.findViewById(R.id.editTextMyNumber); // Ensure this ID exists in your XML

        Button btnPickContact = view.findViewById(R.id.btnPickContact);
        btnPickContact.setOnClickListener(v -> checkContactsPermission());

        Button save = view.findViewById(R.id.save);
        save.setOnClickListener(this);

        // Load existing "My Number" if it exists
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        if (myUserNumber != null) {
            myUserNumber.setText(prefs.getString("my_registered_number", ""));
        }

        return view;
    }

    private void checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_CONTACTS);
        } else {
            pickContact();
        }
    }

    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, REQUEST_CONTACT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickContact();
            } else {
                Toast.makeText(getActivity(), "Contacts permission is required to pick a contact.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONTACT && resultCode == Activity.RESULT_OK && data != null) {
            Uri contactUri = data.getData();
            String[] projection = new String[]{
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
            };

            try (Cursor cursor = requireContext().getContentResolver().query(contactUri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                    String contactName = cursor.getString(nameIndex);
                    String contactNumber = cursor.getString(numberIndex);

                    name.setText(contactName);
                    number.setText(contactNumber);
                }
            } catch (Exception e) {
                Log.e("Register", "Failed to get contact data", e);
            }
        }
    }

    @Override
    public void onClick(View view) {
        String str_name = name.getText().toString();
        String str_number = number.getText().toString();
        String str_keyword = keyword.getText().toString().trim().toLowerCase();
        String str_my_num = (myUserNumber != null) ? myUserNumber.getText().toString().trim() : "";

        if (str_name.isEmpty() || str_number.isEmpty() || str_keyword.isEmpty()) {
            Toast.makeText(getActivity(), "Please enter name, number, and keyword", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save User's specific SIM number for SOS verification
        if (!str_my_num.isEmpty()) {
            SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            prefs.edit().putString("my_registered_number", str_my_num).apply();
        }

        try {
            db = requireActivity().openOrCreateDatabase("NumberDB", MODE_PRIVATE, null);
            
            db.execSQL("CREATE TABLE IF NOT EXISTS details(Pname TEXT, number TEXT, keyword TEXT);");
            
            try {
                db.rawQuery("SELECT keyword FROM details LIMIT 1", null).close();
            } catch (Exception e) {
                db.execSQL("ALTER TABLE details ADD COLUMN keyword TEXT;");
            }

            Cursor c = db.rawQuery("SELECT * FROM details WHERE keyword = ?", new String[]{str_keyword});
            if (c != null && c.getCount() > 0) {
                db.execSQL("UPDATE details SET Pname = ?, number = ? WHERE keyword = ?", 
                        new Object[]{str_name, str_number, str_keyword});
                Toast.makeText(getActivity(), "Contact updated for: " + str_keyword, Toast.LENGTH_SHORT).show();
            } else {
                db.execSQL("INSERT INTO details (Pname, number, keyword) VALUES(?, ?, ?)", 
                        new Object[]{str_name, str_number, str_keyword});
                Toast.makeText(getActivity(), "Contact saved successfully", Toast.LENGTH_SHORT).show();
            }
            if (c != null) c.close();
            db.close();
            
            name.setText("");
            number.setText("");
            keyword.setText("");
            
        } catch (Exception e) {
            Log.e("Register", "Database Error", e);
            Toast.makeText(getActivity(), "Error saving contact: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static String getNumber(SQLiteDatabase db2) {
        String phone_num = "";
        try {
            db2.execSQL("CREATE TABLE IF NOT EXISTS details(Pname TEXT, number TEXT, keyword TEXT);");
            try (Cursor c = db2.rawQuery("SELECT number FROM details LIMIT 1", null)) {
                if (c != null && c.moveToFirst()) {
                    phone_num = c.getString(0);
                }
            }
        } catch (Exception e) {
            Log.e("Register", "Error getting number", e);
        }
        return phone_num;
    }

    public static String getNumberByKeyword(SQLiteDatabase db, String spokenText) {
        if (spokenText == null || spokenText.isEmpty()) return null;
        
        String phone_num = null;
        String cleanSpoken = spokenText.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", " ").trim();
        
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS details(Pname TEXT, number TEXT, keyword TEXT);");
            try (Cursor c = db.rawQuery("SELECT number, keyword FROM details", null)) {
                if (c != null) {
                    while (c.moveToNext()) {
                        String dbNumber = c.getString(0);
                        String dbKeyword = c.getString(1);
                        
                        if (dbKeyword != null && !dbKeyword.isEmpty()) {
                            String cleanKeyword = dbKeyword.toLowerCase().trim();
                            if (cleanSpoken.matches(".*\\b" + cleanKeyword + "\\b.*") || cleanSpoken.contains(cleanKeyword)) {
                                phone_num = dbNumber;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Register", "Error matching keyword", e);
        }
        return phone_num;
    }
}
