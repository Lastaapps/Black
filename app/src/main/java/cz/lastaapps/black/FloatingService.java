package cz.lastaapps.black;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Date;

import static android.view.WindowManager.LayoutParams.*;

public class FloatingService extends Service {
    WindowManager wm;
    LinearLayout floating, screen, touch;
    BroadcastReceiver receiver;
    int extraPadding = 1000;
    View.OnKeyListener cancelKey = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, final int keyCode, final KeyEvent event) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(App.getAppContext(), "screen " + keyCode + " " + event, Toast.LENGTH_LONG).show();
                    floating.setVisibility(View.VISIBLE);
                    screen.setVisibility(View.GONE);
                    touch.setVisibility(View.GONE);
                }
            });
            return false;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 23) {
            if (Settings.canDrawOverlays(this) == false) {
                Intent i = new Intent(this, PermissionActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                stopSelf();
                return;
            }
        }
        createWindow();
        //createCallReceiver();
    }

    private void createWindow() {

        //---INIT---
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Point screenSize = new Point();
        wm.getDefaultDisplay().getSize(screenSize);


        //---FLOATING BUTTON---
        floating = new LinearLayout(this);
        //floating.setBackgroundColor(getColor());
        floating.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400));
        floating.setKeepScreenOn(true);

        int floatingLayoutParamsType = TYPE_APPLICATION_OVERLAY;
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            floatingLayoutParamsType = TYPE_PHONE;
        }
        final WindowManager.LayoutParams floatingParams = new WindowManager.LayoutParams(
                MainActivity.getSize(this), MainActivity.getSize(this), floatingLayoutParamsType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                0,
                PixelFormat.TRANSLUCENT);
        floatingParams.gravity = Gravity.CENTER | Gravity.CENTER;
        floatingParams.x = 0;
        floatingParams.y = 0;

        ImageView flImg = new ImageView(this);
        drawCircle(flImg);
        floating.addView(flImg);







        //---SCREEN OVERLAY---
        int screenLayoutParamsType = TYPE_APPLICATION_OVERLAY;
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            screenLayoutParamsType = TYPE_PHONE;
            screenLayoutParamsType = TYPE_SYSTEM_OVERLAY;
        }

        screen = new LinearLayout(this);
        screen.setBackgroundColor(getColor());
        screen.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        screen.setKeepScreenOn(true);

        WindowManager.LayoutParams screenParams = new WindowManager.LayoutParams(
                screenSize.x + extraPadding, screenSize.y + extraPadding,
                screenLayoutParamsType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |

                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION |
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS |
                        0,
                PixelFormat.TRANSLUCENT);
        screenParams.gravity = Gravity.CENTER | Gravity.CENTER;
        screenParams.x = -extraPadding/2;
        screenParams.y = -extraPadding/2;





        //---TOUCH---
        int touchLayoutParamsType = TYPE_APPLICATION_OVERLAY;
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            touchLayoutParamsType = TYPE_SYSTEM_OVERLAY;
            touchLayoutParamsType = TYPE_PHONE;
        }

        touch = new LinearLayout(this);
        touch.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        touch.setKeepScreenOn(true);

        WindowManager.LayoutParams touchParams = new WindowManager.LayoutParams(
                screenSize.x + extraPadding, screenSize.y + extraPadding,
                touchLayoutParamsType,
                //WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |

                        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
                        WindowManager.LayoutParams.FLAG_LOCAL_FOCUS_MODE |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                        0,
                PixelFormat.TRANSLUCENT);
        screenParams.gravity = Gravity.CENTER | Gravity.CENTER;
        screenParams.x = -extraPadding/2;
        screenParams.y = -extraPadding/2;

        touch.setFocusable(true);

        //--ADDING WINDOWS---
        wm.addView(floating, floatingParams);
        wm.addView(screen, screenParams);
        wm.addView(touch, touchParams);
        screen.setVisibility(View.GONE);
        touch.setVisibility(View.GONE);

        //---CHANGING OVERLAY STATE
        floating.setOnTouchListener(new View.OnTouchListener() {
            WindowManager.LayoutParams updatedParameters = floatingParams;
            double x, y;
            double pressedX, pressedY;
            long lastTouch = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        long currentTime = System.currentTimeMillis();
                        if (lastTouch + 500 > currentTime) {
                            floating.setVisibility(View.GONE);
                            screen.setVisibility(View.VISIBLE);
                            touch.setVisibility(View.VISIBLE);
                        }
                        lastTouch = currentTime;

                        x = updatedParameters.x;
                        y = updatedParameters.y;

                        pressedX = event.getRawX();
                        pressedY = event.getRawY();

                        break;

                    case MotionEvent.ACTION_MOVE:
                        updatedParameters.x = (int) (x + (event.getRawX() - pressedX));
                        updatedParameters.y = (int) (y + (event.getRawY() - pressedY));

                        wm.updateViewLayout(floating, updatedParameters);

                    default:
                        break;
                }

                return false;
            }
        });
        touch.setOnTouchListener(new View.OnTouchListener() {
            long lastTouch = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        long currentTime = System.currentTimeMillis();
                        if (lastTouch  + 500 > currentTime) {
                            floating.setVisibility(View.VISIBLE);
                            screen.setVisibility(View.GONE);
                            touch.setVisibility(View.GONE);
                        }
                        lastTouch = currentTime;

                        break;
                    default:
                        break;
                }

                return false;
            }
        });
        floating.setOnKeyListener(cancelKey);
        screen.setOnKeyListener(cancelKey);
        touch.setOnKeyListener(cancelKey);

        //--HELP
        Toast.makeText(this, R.string.expand, Toast.LENGTH_LONG).show();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Point screenSize = new Point();
        wm.getDefaultDisplay().getSize(screenSize);

        //---SCREEN ORIENTATION CHANGE---
        LinearLayout screenBackup = screen;
        screen = new LinearLayout(this);
        screen.setBackgroundColor(getColor());
        screen.setLayoutParams(screenBackup.getLayoutParams());
        screen.setKeepScreenOn(screenBackup.getKeepScreenOn());

        final WindowManager.LayoutParams screenLayoutParams = (WindowManager.LayoutParams)screenBackup.getLayoutParams();
        screenLayoutParams.width = screenSize.x + extraPadding;
        screenLayoutParams.height = screenSize.y + extraPadding;

        screen.setVisibility(screenBackup.getVisibility());

        wm.removeView(screenBackup);
        wm.addView(screen, screenLayoutParams);

        //---TOUCH ORIENTATION CHANGE---
        LinearLayout touchBackup = touch;
        touch = new LinearLayout(this);
        touch.setLayoutParams(touchBackup.getLayoutParams());
        touch.setKeepScreenOn(touchBackup.getKeepScreenOn());

        final WindowManager.LayoutParams touchLayoutParams = (WindowManager.LayoutParams)touchBackup.getLayoutParams();
        touchLayoutParams.width = screenSize.x + extraPadding;
        touchLayoutParams.height = screenSize.y + extraPadding;

        touch.setVisibility(touchBackup.getVisibility());

        wm.removeView(touchBackup);
        wm.addView(touch, touchLayoutParams);

        touch.setOnTouchListener(new View.OnTouchListener() {
            long lastTouch = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        long currentTime = System.currentTimeMillis();
                        if (lastTouch  + 500 > currentTime) {
                            floating.setVisibility(View.VISIBLE);
                            screen.setVisibility(View.GONE);
                            touch.setVisibility(View.GONE);
                        }
                        lastTouch = currentTime;

                        break;
                    default:
                        break;
                }

                return false;
            }
        });
    }

    private void drawCircle(ImageView imageView) {
        int size = MainActivity.getSize(this);
        Bitmap map = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Paint main = new Paint(0);
        main.setStyle(Paint.Style.FILL);
        main.setColor(FloatingService.getColor());


        Canvas c = new Canvas(map);
        c.drawCircle(size/2, size/2, size/2, main);

        imageView.setImageBitmap(map);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wm != null) {
            wm.removeView(floating);
            wm.removeView(screen);
        }
        stopSelf();
    }

    public static int getColor() {
        return App.getAppContext().getSharedPreferences("data", Context.MODE_PRIVATE)
                .getInt("color", Color.BLACK);
    }

    public static void setColor(int color) {
        if (color == getColor())
            return;
        App.getAppContext().getSharedPreferences("data", Context.MODE_PRIVATE)
                .edit().putInt("color", color).apply();

        restartService();
    }

    public static void restartService() {
        Intent intent = new Intent(App.getAppContext(), FloatingService.class);
        if (MainActivity.isMyServiceRunning(FloatingService.class)) {
            App.getAppContext().stopService(intent);
            App.getAppContext().startService(intent);
        }
    }

    private void createCallReceiver() {
        receiver = new PhonecallReceiver() {
            @Override
            protected void onIncomingCallReceived(Context ctx, String number, Date start) {
                floating.setVisibility(View.VISIBLE);
                screen.setVisibility(View.GONE);
            }
            @Override
            protected void onIncomingCallAnswered(Context ctx, String number, Date start) {}
            @Override
            protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {}
            @Override
            protected void onOutgoingCallStarted(Context ctx, String number, Date start) {}
            @Override
            protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {}
            @Override
            protected void onMissedCall(Context ctx, String number, Date start) {}
        };

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        this.registerReceiver(receiver, filter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //https://stackoverflow.com/questions/15563921/how-to-detect-incoming-calls-in-an-android-device
    public abstract static class PhonecallReceiver extends BroadcastReceiver {

        //The receiver will be recreated whenever android feels like it.  We need a static variable to remember data between instantiations

        private static int lastState = TelephonyManager.CALL_STATE_IDLE;
        private static Date callStartTime;
        private static boolean isIncoming;
        private static String savedNumber;  //because the passed incoming is only valid in ringing


        @Override
        public void onReceive(Context context, Intent intent) {

            //We listen to two intents.  The new outgoing call only tells us of an outgoing call.  We use it to get the number.
            if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
                savedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
            }
            else{
                String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
                String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                int state = 0;
                if(stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)){
                    state = TelephonyManager.CALL_STATE_IDLE;
                }
                else if(stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){
                    state = TelephonyManager.CALL_STATE_OFFHOOK;
                }
                else if(stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)){
                    state = TelephonyManager.CALL_STATE_RINGING;
                }


                onCallStateChanged(context, state, number);
            }
        }

        //Derived classes should override these to respond to specific events of interest
        protected abstract void onIncomingCallReceived(Context ctx, String number, Date start);
        protected abstract void onIncomingCallAnswered(Context ctx, String number, Date start);
        protected abstract void onIncomingCallEnded(Context ctx, String number, Date start, Date end);

        protected abstract void onOutgoingCallStarted(Context ctx, String number, Date start);
        protected abstract void onOutgoingCallEnded(Context ctx, String number, Date start, Date end);

        protected abstract void onMissedCall(Context ctx, String number, Date start);

        //Deals with actual events

        //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
        //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
        public void onCallStateChanged(Context context, int state, String number) {
            if(lastState == state){
                //No change, debounce extras
                return;
            }
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    isIncoming = true;
                    callStartTime = new Date();
                    savedNumber = number;
                    onIncomingCallReceived(context, number, callStartTime);
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                    if(lastState != TelephonyManager.CALL_STATE_RINGING){
                        isIncoming = false;
                        callStartTime = new Date();
                        onOutgoingCallStarted(context, savedNumber, callStartTime);
                    }
                    else
                    {
                        isIncoming = true;
                        callStartTime = new Date();
                        onIncomingCallAnswered(context, savedNumber, callStartTime);
                    }

                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                    if(lastState == TelephonyManager.CALL_STATE_RINGING){
                        //Ring but no pickup-  a miss
                        onMissedCall(context, savedNumber, callStartTime);
                    }
                    else if(isIncoming){
                        onIncomingCallEnded(context, savedNumber, callStartTime, new Date());
                    }
                    else{
                        onOutgoingCallEnded(context, savedNumber, callStartTime, new Date());
                    }
                    break;
            }
            lastState = state;
        }
    }
}

