package cz.lastaapps.black;

import android.app.Activity;
import android.os.Build;
import android.support.constraint.solver.widgets.Rectangle;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import cz.lastaapps.black.autolock.LockPermissionActivity;

public class Black extends Activity {
    private RelativeLayout root;
    private long fullScreenOpened = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fullScreenOpened = System.currentTimeMillis();

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        if (Build.VERSION.SDK_INT >= 28)
        getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        }

        setContentView(R.layout.black);


        root = findViewById(R.id.root);
        root.setBackgroundColor(FloatingService.getColor());
        root.setKeepScreenOn(true);

        root.setOnTouchListener(new View.OnTouchListener() {
            long lastTouch = 0;
            int lastX = -1, lastY = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        boolean closeEnabled = true;

                        if (lastX != -1 && lastY != -1) {
                            int range = MainActivity.DOUBLE_CLICK_RANGE;
                            Rectangle rect = new Rectangle();
                            rect.setBounds(lastX - range, lastY - range,
                                    lastX + range, lastY + range);
                            closeEnabled = rect.contains((int) event.getX(), (int) event.getY());
                            //System.out.printf("%d %d %d %d\n", rect.x, rect.y, rect.width, rect.height);
                        }

                        long currentTime = System.currentTimeMillis();
                        lastX = (int) event.getX();
                        lastY = (int) event.getY();
                        //System.out.println(event.getX() + " " + event.getY());

                        if (closeEnabled == false)
                            break;

                        if (lastTouch + MainActivity.DOUBLE_CLICK_TIMEOUT > currentTime) {
                            //here moved to onDestroy because of back button
                            //MainActivity.playUnlockSound();
                            finish();
                            if (MainActivity.isAutoLocking())
                                if (fullScreenOpened + MainActivity.getLockTime() * 60 * 1000 < System.currentTimeMillis())
                                    LockPermissionActivity.lock();
                        }
                        lastTouch = currentTime;
                        break;
                }
                return false;
            }
        });



        Toast.makeText(App.getAppContext(), R.string.close, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            MainActivity.playUnlockSound();

            if (MainActivity.isAutoLocking()) {
                if (LockPermissionActivity.isLocked() == false) {
                    if (fullScreenOpened + MainActivity.getLockTime() * 60 * 1000 < System.currentTimeMillis())
                        LockPermissionActivity.lock();
                }
            }
        }
    }
}
