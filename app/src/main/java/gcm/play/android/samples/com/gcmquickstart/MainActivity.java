/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gcm.play.android.samples.com.gcmquickstart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends AppCompatActivity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ProgressBar mRegistrationProgressBar;
    private TextView mInformationTextView;
    private boolean isReceiverRegistered;

    private Button sendButton;

    public static final String API_KEY = "AIzaSyCKdDeFtyOW8ZwmKtw-F8txIPDnhmyLlAQ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendButton = (Button)findViewById(R.id.send_button);
        mRegistrationProgressBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    mInformationTextView.setText(getString(R.string.gcm_send_message));
                } else {
                    mInformationTextView.setText(getString(R.string.token_error_message));
                }
            }
        };
        mInformationTextView = (TextView) findViewById(R.id.informationTextView);

        // Registering BroadcastReceiver
        registerReceiver();

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(){
                    public void run(){
                        Log.i("Thread","Thread is starting.");
                        try {
                            // Prepare JSON containing the GCM message content. What to send and where to send.
                            JSONObject jGcmData = new JSONObject();
                            JSONObject jData = new JSONObject();
                            jData.put("message", "Hello test gcm!");
                            // Where to send GCM message.
                            if (true) {
                                //jGcmData.put("to", "e0GunyxfHso:APA91bFG_T25p6KA9lKrpvtke7vyokVBe6R0ojGA8b43CXIAHSQB1Zhgnv0FDsPwQ5gcST91c5X3IyUeq8sNzTyxiBsB9wfRj7nDq1qQj8Q1lAJ66Nf5Y7Zr6z15Cxh9DF4XhnKmRpKz".trim());
                                jGcmData.put("to", "ewhEdfx61Yk:APA91bGdSLBiO3QSKv2OBk1G2WpvUeD0mQ9J0jU-HsRoW1C8jnr3M3EDPvV_r7sSC4GhByCiC4OI6Q0MH3PgyClX6NpkFHl7tvv0fuvr7OQxCQxCh8_mb9ZgSN47qFn_9izTnH5SA5f9".trim());
                            } else {
                                jGcmData.put("to", "/topics/global");
                            }
                            // What to send in GCM message.
                            jGcmData.put("data", jData);

                            // Create connection to send GCM Message request.
                            URL url = new URL("https://android.googleapis.com/gcm/send");
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestProperty("Authorization", "key=" + API_KEY);
                            conn.setRequestProperty("Content-Type", "application/json");
                            conn.setRequestMethod("POST");
                            conn.setDoOutput(true);

                            // Send GCM message content.
                            OutputStream outputStream = conn.getOutputStream();
                            outputStream.write(jGcmData.toString().getBytes());

                            // Read GCM response.
                            InputStream inputStream = conn.getInputStream();
                            String resp = IOUtils.toString(inputStream);
                            System.out.println(resp);
                            System.out.println("Check your device/emulator for notification or logcat for " +
                                    "confirmation of the receipt of the GCM message.");
                        } catch (IOException e) {
                            System.out.println("Unable to send GCM message.");
                            System.out.println("Please ensure that API_KEY has been replaced by the server " +
                                    "API key, and that the device's registration token is correct (if specified).");
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.i("Thread","Thread is finish.");

                    }
                }.start();

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        isReceiverRegistered = false;
        super.onPause();
    }

    private void registerReceiver(){
        if(!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
            isReceiverRegistered = true;
        }
    }
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

}
