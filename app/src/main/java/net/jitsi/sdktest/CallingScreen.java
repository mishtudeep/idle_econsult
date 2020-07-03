package net.jitsi.sdktest;

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class CallingScreen extends AppCompatActivity {
    ProgressDialog mDialog;
    public String doctorId = "5ec24dc94654ca56795b7e4c";
    public String individualId = "5ea65976bacf51402fc32ff8";
    public String roomId = doctorId;
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Button call_received, call_end;
    AnimationDrawable callReceivedAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE); //will hide the title
        getSupportActionBar().hide(); // hide the title bar

        super.onCreate(savedInstanceState);

        Log.e("Colling screnn","111");

        setContentView(R.layout.activity_calling_screen);

        NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancel(1002);

//        String sessionId = getIntent().getStringExtra("EXTRA_SESSION_ID");
//        Log.d("SESSION_ID",sessionId);
//        doctorId  = sessionId;
//        roomId = sessionId;


//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
//                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
//                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
//                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);


//        Window window = getWindow();
//        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
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



        // to release screen lock
        KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();

        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        Ringtone ringtoneSound = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
        //System.out.println("Main | Play.onClick | ringtone:" +ringtoneSound);


        if (ringtoneSound != null) {
            ringtoneSound.play();
            Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vib.vibrate(500);
        }

        call_received = (Button) findViewById(R.id.call_received);
        call_end = (Button) findViewById(R.id.call_end);
//        call_received.setBackgroundResource(R.drawable.call_received_animation_);
//        callReceivedAnimation = (AnimationDrawable) call_received.getBackground();

//        String call_details = getIntent().getStringExtra("CALL_DETAILS");

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter("NotificationIntent"));

        mVisible = true;

        call_received.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ringtoneSound != null) {
                    ringtoneSound.stop();
                }

                receiveCall();
//                finish();
            }
        });

        call_end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ringtoneSound != null) {
                    ringtoneSound.stop();
                }

                rejectCall();
                finish();
            }
        });

    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");  //get the type of message from MyGcmListenerService 1 - lock or 0 -Unlock
            Log.d("Intent Message",message);
            try {
                final JSONObject object = new JSONObject(message);
                doctorId = object.getString("caller");
                roomId = doctorId;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
//        callReceivedAnimation.start();
    }


    public void receiveCall() {
        showDialog();
        try {

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            JSONObject caller = new JSONObject();
            try {
                caller.accumulate("_id", CallingScreen.this.doctorId);
                caller.accumulate("role", "doctor");

            } catch (JSONException e) {
                e.printStackTrace();
                cancelDialog();
            }


            JSONObject receiver = new JSONObject();
            try {
                receiver.accumulate("_id", CallingScreen.this.individualId);
                receiver.accumulate("role", "individual");

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

            final String url = "https://api.mefy.care/eConsult/acceptcall";
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, requestData,
                    response -> {
                        cancelDialog();
                        try {
                            JSONObject obj = response;
                            Boolean msg = obj.getBoolean("error");
                            if(msg == false){
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Call has been received",
                                        Toast.LENGTH_SHORT);
                                toast.show();
                               // joinRoom(roomId); // nur
                                startActivity(new Intent(CallingScreen.this,MainActivity.class).putExtra("incoming",roomId));
                                finish();
                            }else{
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Can not receive call",
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        } catch (JSONException e) {
                            // If an error occurs, this prints the error to the log
                            e.printStackTrace();
                        }
                    }, error -> {
                cancelDialog();
                System.out.println("Error getting response------------------------");
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Can not receive call",
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
        }
    }

    public void rejectCall() {
        try {

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            JSONObject caller = new JSONObject();
            try {
                caller.accumulate("_id", CallingScreen.this.doctorId);
                caller.accumulate("role", "doctor");

            } catch (JSONException e) {
                e.printStackTrace();
            }


            JSONObject receiver = new JSONObject();
            try {
                receiver.accumulate("_id", CallingScreen.this.individualId);
                receiver.accumulate("role", "individual");

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

            final String url = "https://api.mefy.care/eConsult/rejectcall";
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, requestData,
                    response -> {
                        try {
                            JSONObject obj = response;
                            Boolean msg = obj.getBoolean("error");
                            if(msg == false){
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Call has been rejected",
                                        Toast.LENGTH_SHORT);
                                toast.show();
                                finish();
                            }else{
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Can not reject call",
                                        Toast.LENGTH_SHORT);
                                toast.show();
                                finish();
                            }
                        } catch (JSONException e) {
                            // If an error occurs, this prints the error to the log
                            e.printStackTrace();
                        }
                    }, error -> {
                System.out.println("Error getting response------------------------");
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Can not reject call",
                        Toast.LENGTH_SHORT);
                toast.show();
                finish();
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
        }
    }

    public void disconnectCall() {
        JSONObject object = new JSONObject();
        try {
//            object.put("self", MainActivity.this.individualId);
//            object.put("selfRole", "individual");
//            object.put("peer", MainActivity.this.doctorId);
//            object.put("peerRole", "doctor");
//            mSocket.emit("callDisconnected", object);
//            callButton.setText("Idle");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        JitsiMeetActivity.launch(CallingScreen.this, options);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
//        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
//        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
//        mVisible = true;
//
//        // Schedule a runnable to display UI elements after a delay
//        mHideHandler.removeCallbacks(mHidePart2Runnable);
//        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
//        mHideHandler.removeCallbacks(mHideRunnable);
//        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public void showDialog() {
        mDialog = new ProgressDialog(CallingScreen.this, ProgressDialog.THEME_HOLO_LIGHT);
        mDialog.setMessage("Please wait...");
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDialog.setIndeterminate(true);
        mDialog.setCancelable(false);
        mDialog.show();
    }

    public void cancelDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }

    }

}
