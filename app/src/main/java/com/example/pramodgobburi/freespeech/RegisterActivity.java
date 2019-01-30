package com.example.pramodgobburi.freespeech;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import org.w3c.dom.Text;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout mDisplayName;
    private TextInputLayout mEmail;
    private TextInputLayout mPassword;
    private Button mRegister;
    private ProgressDialog mDialog;

    private Toolbar mToolbar;

    private DatabaseReference mDatabase;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        mToolbar = (Toolbar) findViewById(R.id.register_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Register");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mDisplayName = (TextInputLayout) findViewById(R.id.register_activity_displayName);
        mEmail = (TextInputLayout) findViewById(R.id.register_activity_Email);
        mPassword = (TextInputLayout) findViewById(R.id.register_activity_Password);
        mRegister = (Button) findViewById(R.id.register_activity_button);
        mDialog = new ProgressDialog(this);
        mDialog.setTitle("Registering User");
        mDialog.setMessage("Processing...");
        mDialog.setCancelable(false);

        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String displayName = mDisplayName.getEditText().getText().toString();
                String email = mEmail.getEditText().getText().toString();
                String password = mPassword.getEditText().getText().toString();

                registerUser(displayName, email, password);
                mDialog.show();
            }
        });
    }

    private void registerUser(final String displayName, final String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {

                            FirebaseUser current_user = FirebaseAuth.getInstance().getCurrentUser();
                            String uid = current_user.getUid();



                            mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(uid);
                            String deviceToken = FirebaseInstanceId.getInstance().getToken();

                            HashMap<String, String> userMap = new HashMap<>();
                            userMap.put("name", displayName);
                            userMap.put("sort_name", displayName.toLowerCase());
                            userMap.put("status", "Hi, I'm using FreeSpeech.");
                            userMap.put("image", "default");
                            userMap.put("thumb_image", "default");
                            userMap.put("email", email.toString());
                            userMap.put("device_token", deviceToken);

                            mDatabase.setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()) {
                                        mDialog.dismiss();
                                        Toast.makeText(RegisterActivity.this, "Successfully registered user", Toast.LENGTH_SHORT).show();
                                        Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(mainIntent);
                                        finish();
                                    }
                                }
                            });



                        }
                        else {
                            mDialog.dismiss();
                            Toast.makeText(RegisterActivity.this, " "+task.getException().getMessage().toString(), Toast.LENGTH_LONG).show();
                        }

                        // ...
                    }
                });
    }
}
