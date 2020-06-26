package net.jitsi.sdktest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetView;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.emitter.Emitter;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

public class MainActivity extends AppCompatActivity {

    public String doctorId = "5ec24dc94654ca56795b7e4c";
    public String individualId = "5ea65976bacf51402fc32ff8";
    public String roomId = doctorId;
    private EditText mInputMessageView;
    private Button callButton;

    private Socket mSocket;

    Button call_received, call_end;
    AnimationDrawable callReceivedAnimation;
    private String token = "";
    EditText et_token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setCameraPermissions();

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        // to wake up screen
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
        wakeLock.acquire();

        if (getIntent().getStringExtra("incoming")!=null){
            String id = getIntent().getStringExtra("incoming");
           joinRoom(id);
        }

        et_token = findViewById(R.id.et_token);

        IO.Options opts = new IO.Options();
        opts.reconnection = true;

//        try {
//            mSocket = IO.socket("https://api.mefy.care?profileId=5ea65976bacf51402fc32ff8&&role=individual", opts);
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }


        callButton = (Button) findViewById(R.id.callbutton);

        // Initialize default options for Jitsi Meet conferences.
        URL serverURL;
        try {
            serverURL = new URL("https://meet.mefy.care");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid server URL!");
        }
        JitsiMeetConferenceOptions defaultOptions
                = new JitsiMeetConferenceOptions.Builder()
                .setServerURL(serverURL)
                .setWelcomePageEnabled(false)
                .build();
        JitsiMeet.setDefaultConferenceOptions(defaultOptions);

        callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeCall();
            }
        });

        initFirebase();

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter("CallIntent"));







        // to release screen lock
        KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();

    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");  //get the type of message from MyGcmListenerService 1 - lock or 0 -Unlock
            Log.d("Intent Message",message);
            try {
                final JSONObject object = new JSONObject(message);
                String msg = object.getString("msg");
                Log.d("+++++++++++++++++++++++",msg);
                if( msg.equals("CALL_REJECTED")){
                    callButton.setText("call");
                }else if(msg.equals("CALL_ACCEPTED")){
                    callButton.setText("call");
                    joinRoom(roomId);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public void makeCall() {
        callButton.setText("calling");
        try {

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            JSONObject caller = new JSONObject();
            try {
                caller.accumulate("_id", MainActivity.this.individualId);
                caller.accumulate("role", "individual");

            } catch (JSONException e) {
                e.printStackTrace();
            }


            JSONObject receiver = new JSONObject();
            try {
                receiver.accumulate("_id", MainActivity.this.doctorId);
                receiver.accumulate("role", "doctor");

            } catch (JSONException e) {
                e.printStackTrace();
            }

            JSONObject requestData = new JSONObject();
            try {
                requestData.accumulate("caller", caller);
                requestData.accumulate("receiver", receiver);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            final String url = "https://api.mefy.care/eConsult/makecall";
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, requestData,
                    response -> {
                        try {
                            JSONObject obj = response;
                            Boolean msg = obj.getBoolean("error");
                            if(msg == false){
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Call has been made",
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }else{
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Can not make call",
                                        Toast.LENGTH_SHORT);
                                toast.show();
                                callButton.setText("call");
                            }
                        } catch (JSONException e) {
                            // If an error occurs, this prints the error to the log
                            e.printStackTrace();
                            callButton.setText("call");
                        }
                    }, error -> {
                System.out.println("Error getting response------------------------");
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Can not make call",
                        Toast.LENGTH_SHORT);
                toast.show();
                callButton.setText("call");
            }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }

                @Override
                public String getBodyContentType() {
                    return "application/json";
                }

                @Override
                public byte[] getBody() {
                    try {
                        return requestData.toString().getBytes("UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
            requestQueue.add(jsonObjectRequest);

        } catch (Exception e) {
            e.printStackTrace();
            callButton.setText("call");
        }
    }


    public void initFirebase() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("message", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        token = task.getResult().getToken();
                        et_token.setText(token);

                        // Log and toast
//                        String msg = getString(R.string., token);
                        Log.d("message", token);
                       // Toast.makeText(MainActivity.this, token, Toast.LENGTH_SHORT).show();
                        saveFirebaseToken();
                    }
                });
    }


    public void saveFirebaseToken() {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JSONObject json = new JSONObject();
        try {
            json.accumulate("_id", MainActivity.this.individualId);
            json.accumulate("socketId", MainActivity.this.token);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        final String url = "https://api.mefy.care/individual/individualProfileUpdate";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, url, json,
                response -> {
                    try {
                        JSONObject obj = response;
                        Boolean msg = obj.getBoolean("error");
                        if(msg == false){
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "Firebase Token has been saved to server",
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }else{
                            Toast toast = Toast.makeText(getApplicationContext(),
                                    "Firebase Token could not saved to server",
                                    Toast.LENGTH_SHORT);
                            toast.show();
                        }
                    } catch (JSONException e) {
                        // If an error occurs, this prints the error to the log
                        e.printStackTrace();
                    }
                }, error -> {
            System.out.println("Error getting response------------------------");
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Firebase Token could not saved to server",
                    Toast.LENGTH_SHORT);
            toast.show();
        }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");
                return headers;
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() {
                try {
                    return json.toString().getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        requestQueue.add(jsonObjectRequest);
    }


    private void joinRoom(String roomId) {
        URL serverURL;
        try {
            serverURL = new URL("https://meet.mefy.care");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid server URL!");
        }
        JitsiMeetConferenceOptions options
                = new JitsiMeetConferenceOptions.Builder()
                .setRoom(roomId)
                .build();
        JitsiMeetActivity.launch(MainActivity.this, options);
    }


    public void setCameraPermissions() {

        Dexter.withActivity(MainActivity.this).withPermissions(Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (!report.areAllPermissionsGranted()) {


                    Toast.makeText(getApplicationContext(), "Please set the Camera permission to make a video call", Toast.LENGTH_LONG).show();


                }

            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                token.continuePermissionRequest();
            }
        }).check();



    }


}
