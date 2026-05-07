package com.womensafety.shajt3ch;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PinActivity extends AppCompatActivity {

    private EditText etPin;
    private String correctPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        etPin = findViewById(R.id.etPinVerify);
        Button btnVerify = findViewById(R.id.btnVerifyPin);

        correctPin = getIntent().getStringExtra("CORRECT_PIN");

        btnVerify.setOnClickListener(v -> {
            String input = etPin.getText().toString();
            if (input.equals(correctPin)) {
                Intent intent = new Intent(this, MyService.class);
                intent.setAction(MyService.ACTION_PIN_SUCCESS);
                startService(intent);
                finish();
            } else {
                Toast.makeText(this, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Disable back button to force PIN entry
    }
}
