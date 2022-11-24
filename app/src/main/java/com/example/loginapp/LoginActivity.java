package com.example.loginapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class LoginActivity extends AppCompatActivity implements SensorEventListener {

    private static final int REQUEST_CODE_PERMISSION_CONTACTS = 900;
    private static final int MANUALLY_CONTACTS_PERMISSION_REQUEST_CODE = 124;
    private final String PASSWORD = "12345";
    private final String NAME_CONTACT = "אאבא";

    private EditText password;
    private Button login;
    private ProgressBar loading;
    private TextView info;

    private float proxiFlout;
    private SensorManager sensorManager;

    //    private ArrayList<Contact> contactsArray;
    private boolean isExist = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        findViews();
        // Obtain references to the SensorManager and the Light Sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            Sensor proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            if (proximitySensor != null) {
                sensorManager.registerListener(this, proximitySensor, sensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        requestPermissionContacts();

        //On click Login
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                info.setText("hii");
                login.setVisibility(View.INVISIBLE);
                loading.setVisibility(View.VISIBLE);
                Log.d("pttt", "password: " + password.getText().toString());

                if (ContextCompat.checkSelfPermission(LoginActivity.this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
                    checkPassword(password.getText().toString());
            }
        });
    }

    private void checkPassword(String password) {
        info.setText("");
        if (password.equals(PASSWORD)) {
            checkIfObjectNear();
        } else {
            info.setText("The password incorrect");
            info.setVisibility(View.VISIBLE);
            loading.setVisibility(View.INVISIBLE);
            login.setVisibility(View.VISIBLE);
        }
    }

    private void checkIfObjectNear() {
        Log.d("pttt", "proxiFlout: " + proxiFlout);

        if (proxiFlout <= 0) {
            checkPhoneVolume();
        } else {
            info.setText("The Login incorrect (proximity)");
            info.setVisibility(View.VISIBLE);
            loading.setVisibility(View.INVISIBLE);
            login.setVisibility(View.VISIBLE);
        }
    }

    private void checkPhoneVolume() {
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVolumePercentage = 100 * currentVolume / maxVolume;

        Log.d("pttt", "currentVolume: " + currentVolume);
        Log.d("pttt", "maxVolume: " + maxVolume);
        Log.d("pttt", "volume Percentage: " + currentVolumePercentage);

        if (currentVolumePercentage > 50) {
            checkContactExist();
        } else {
            info.setText("The Login incorrect (volume)");
            info.setVisibility(View.VISIBLE);
            loading.setVisibility(View.INVISIBLE);
            login.setVisibility(View.VISIBLE);
        }
    }

    private void checkContactExist() {
        Log.d("pttt", "Contact: ");
        //read all the contacts until name to find
        readContactsAndCheck();

        if (isExist) {
            startActivity(new Intent(LoginActivity.this, AfterLoginActivity.class));
            finish();
        } else {
            info.setText("The Login incorrect (no Exist Contacts)");
            info.setVisibility(View.VISIBLE);
            loading.setVisibility(View.INVISIBLE);
            login.setVisibility(View.VISIBLE);
        }
    }

    private void requestPermissionContacts() {
        //request Permission to contact
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CODE_PERMISSION_CONTACTS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d("pttt", "onRequestPermissionsResult");
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("pttt", "PERMISSION_GRANTED");
                    //i will read only when it goes to the contact level
//                    readContactsAndCheck();
                } else {
                    Log.d("pttt", "PERMISSION_DENIED");
//                    Toast.makeText(LoginActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                    requestPermissionWithRationaleCheck();
                }
            }
        }
    }

    private void requestPermissionWithRationaleCheck() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this, Manifest.permission.READ_CONTACTS)) {
            Log.d("pttt", "shouldShowRequestPermissionRationale = true");
            // Show user description for what we need the permission

            String message = "Contact permission is request";
            AlertDialog alertDialog =
                    new AlertDialog.Builder(LoginActivity.this)
                            .setMessage(message)
                            .setPositiveButton(getString(android.R.string.ok),
                                    (dialog, which) -> {
                                        requestPermissionContacts();
                                        dialog.cancel();
                                    })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // disabled functions due to denied permissions
                                    info.setText("you unable to Login if Contact permission deny");
                                }
                            })
                            .show();
            alertDialog.setCanceledOnTouchOutside(true);
        } else {
            Log.d("pttt", "shouldShowRequestPermissionRationale = false");
            openPermissionSettingDialog();
        }
    }

    private void openPermissionSettingDialog() {
        String message = "Setting screen if user have permanently disable the permission by clicking Don't ask again checkbox.";
        AlertDialog alertDialog =
                new AlertDialog.Builder(LoginActivity.this)
                        .setMessage(message)
                        .setPositiveButton(getString(android.R.string.ok),
                                (dialog, which) -> {
                                    openSettingsManually();
                                    dialog.cancel();
                                }).show();
        alertDialog.setCanceledOnTouchOutside(true);
    }

    private void openSettingsManually() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, MANUALLY_CONTACTS_PERMISSION_REQUEST_CODE);
    }

    @SuppressLint("Range")
    private void readContactsAndCheck() {
        Log.d("pttt", "readContacts");

        ContentResolver cr = this.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
//        contactsArray = new ArrayList<>();

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur.moveToNext()) {

                int hasPhoneNumber = Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));
                if (hasPhoneNumber > 0) {
//                    String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    Log.i("pttt", "Name: " + name);
                    if (name.equals(NAME_CONTACT)) {
                        Log.i("pttt", "Name: " + name + "NAMECON: " + NAME_CONTACT);
                        isExist = true;
                        return;
                    }
//                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
//                            null,
//                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
//                            new String[]{id}, null);

//                    while (pCur.moveToNext()) {
//                        String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
////                        Log.i("pttt", "Name: " + name + " Phone Number: " + phoneNo);
////                        contactsArray.add(new Contact(id,name, phoneNo));
//                    }
//                    pCur.close();
                }
            }
        }
        if (cur != null) {
            cur.close();
        }
    }

    private void findViews() {
        password = findViewById(R.id.password);
        login = findViewById(R.id.login);
        loading = findViewById(R.id.loading);
        info = findViewById(R.id.info);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            //More then 0 -> object is far
            //Less then 0 -> object is near
            proxiFlout = sensorEvent.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}