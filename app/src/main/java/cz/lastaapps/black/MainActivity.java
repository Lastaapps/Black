package cz.lastaapps.black;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import cz.lastaapps.black.autolock.LockPermissionActivity;

public class MainActivity extends AppCompatActivity {

    private static final String SF_SIZE = "SIZE";
    private static final String SF_AUTO_LOCK = "AUTO_LOCK";
    private static final String SF_LOCK_TIME = "LOCK_TIME";

    public static final long DOUBLE_CLICK_TIMEOUT = 250;
    public static final int DOUBLE_CLICK_RANGE = 100;//in fact, it will be 2x bigger

    SwitchCompat lockSwitcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (OverlayPermissionActivity.isEnabled() == false) {
            startActivity(new Intent(this, OverlayPermissionActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);



        String[] advicesList = getResources().getStringArray(R.array.advices);
        String advice = "";

        for (String s : advicesList) {
            advice += s;
            advice += "\n\n";
        }
        ((TextView)findViewById(R.id.advices)).setText(advice);

        findViewById(R.id.innerSc).setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                ((ScrollView)findViewById(R.id.outerSc))
                        .requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        findViewById(R.id.outerSc).setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                ((ScrollView)findViewById(R.id.innerSc))
                        .requestDisallowInterceptTouchEvent(false);
                return false;
            }
        });


        findViewById(R.id.color_circle).setOnClickListener(new View.OnClickListener() {
            int lastColor = FloatingService.getColor();
            @Override
            public void onClick(View v) {

                android.support.v7.app.AlertDialog dialog = ColorPickerDialogBuilder
                        .with(MainActivity.this)
                        .setTitle(getString(R.string.chose_color))
                        .initialColor(FloatingService.getColor())
                        .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                        .density(12)
                        .setOnColorSelectedListener(new OnColorSelectedListener() {
                            @Override
                            public void onColorSelected(int selectedColor) {

                            }
                        })
                        .setPositiveButton(getString(android.R.string.ok), new ColorPickerClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                FloatingService.setColor(selectedColor);
                                updateCanvas();
                            }
                        })
                        .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .build();

                dialog.show();/**/
            }

            private int getDimensionAsPx(Context context, int rid) {
                return (int) (context.getResources().getDimension(rid) + .5f);
            }
        });
        ((ImageView)findViewById(R.id.color_circle)).setScaleType(ImageView.ScaleType.FIT_CENTER);
        updateCanvas();


        initSizeBar();



        if (LockPermissionActivity.isLockEnabled() == false) setAutoLocking(false);

        lockSwitcher = (SwitchCompat)findViewById(R.id.lock_switch);
        lockSwitcher.setChecked(isAutoLocking());
        /*lockSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SwitchCompat switcher = ((SwitchCompat)v);
                boolean checked = switcher.isChecked();

                if (LockPermissionActivity.isLockEnabled()) {
                    setAutoLocking(checked);
                } else {
                    switcher.setChecked(false);
                    setAutoLocking(false);
                    LockPermissionActivity.enableActivity();
                }
            }
        });*/
        lockSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (LockPermissionActivity.isLockEnabled()) {
                    if (isChecked) {

                        int minutes = getLockTime() % 60;
                        int hours = (getLockTime() - minutes) / 60;



                        View root = View.inflate(MainActivity.this, R.layout.my_time_picker, null);

                        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface d, int which) {
                                        AlertDialog dialog = (AlertDialog) d;
                                        TimePicker timePicker = (TimePicker) root.findViewById(R.id.time_picker);

                                        int time;
                                        if (Build.VERSION.SDK_INT < 23)
                                            time = 60 * timePicker.getCurrentHour() + timePicker.getCurrentHour();
                                        else
                                            time = 60 * timePicker.getHour() + timePicker.getMinute();

                                        setLockTime(time);


                                        int min = time % 60;
                                        int hour = (time - min) / 60;

                                        Toast.makeText(App.getAppContext(), String.format(getString(R.string.auto_lock_set), hour, min), Toast.LENGTH_LONG).show();
                                    }
                                })
                                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        lockSwitcher.setChecked(false);
                                    }
                                })
                                .setCancelable(true)
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        lockSwitcher.setChecked(false);
                                    }
                                })
                                .setView(root)
                                .create();


                        TimePicker timePicker = (TimePicker)root.findViewById(R.id.time_picker);
                        timePicker.setIs24HourView(true);
                        if (Build.VERSION.SDK_INT < 23) {
                            timePicker.setCurrentHour(hours);
                            timePicker.setCurrentMinute(minutes);
                        } else {
                            timePicker.setHour(hours);
                            timePicker.setMinute(minutes);
                        }

                        dialog.show();
                    }
                    setAutoLocking(isChecked);
                } else {
                    lockSwitcher.setChecked(false);
                    setAutoLocking(false);
                    LockPermissionActivity.enableActivity();
                }
            }
        });


        updateButton();
        new AsyncTask<Object, Object, Object>(){
            @Override
            protected Object doInBackground(Object[] objects) {
                while(true) {
                    try { Thread.sleep(1000); } catch (InterruptedException e) { e.printStackTrace(); }
                    publishProgress();
                }
            }

            @Override
            protected void onProgressUpdate(Object... values) {
                updateButton();
            }
        }.execute();

        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Button) findViewById(R.id.start)).setText(!isMyServiceRunning(FloatingService.class) ? R.string.stop : R.string.start);

                Intent intent = new Intent(MainActivity.this, FloatingService.class);
                if (isMyServiceRunning(FloatingService.class)) {
                    stopService(intent);
                } else {
                    startService(intent);
                }
            }
        });

        findViewById(R.id.start_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Black.class));
            }
        });


        initIcons();


        ((TextView)findViewById(R.id.info)).setText(getString(R.string.version) + ": " +
                BuildConfig.VERSION_NAME + "  " +
                new DecimalFormat("000").format(BuildConfig.VERSION_CODE) + "  " +
                getString(R.string.company_name) + "  " +
                new SimpleDateFormat("yyyy").format(BuildConfig.BUILD_TIME));

    }

    private void initSizeBar() {
        SeekBar bar = findViewById(R.id.size_bar);
        bar.getProgressDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        if (Build.VERSION.SDK_INT >= 16)
            bar.getThumb().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

        Point scSize = getScreenSize(this);
        final int smaller = Math.min(scSize.x, scSize.y);

        final int min = (int)(smaller * 0.05), max = (int)(smaller * 0.3);
        int progress = getSize();
        progress = progress > max ? max : progress;

        bar.setMax(max - min);
        bar.setProgress(progress - min);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    progress += min;
                    setSize(progress);
                    updateCanvas();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                FloatingService.restartService();
            }
        });
    }

    private void initIcons() {
        findViewById(R.id.share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        findViewById(R.id.facebook).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://www.facebook.com/lastaapps/";
                Uri uri = Uri.parse(url);
                try {
                    ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo("com.facebook.katana", 0);
                    if (applicationInfo.enabled) {
                        uri = Uri.parse("fb://facewebmodal/f?href=" + url);
                    }
                } catch (PackageManager.NameNotFoundException ignored) {}

                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
        });
        findViewById(R.id.github).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Lastaapps/Black")));
            }
        });
    }

    public void updateCanvas() {
        int size = getSize();
        float rSize = 2 * size;
        Bitmap map = Bitmap.createBitmap((int)rSize, (int)rSize, Bitmap.Config.ARGB_8888);

        Paint main = new Paint(0);
        main.setStyle(Paint.Style.FILL);
        main.setColor(FloatingService.getColor());

        Paint circle = new Paint(0);
        circle.setStyle(Paint.Style.FILL);
        circle.setColor(Color.WHITE);

        Canvas c = new Canvas(map);
        c.drawCircle(rSize/2, rSize/2, rSize/2, circle);
        c.drawCircle(rSize/2, rSize/2, rSize/2 - (rSize/8), main);

        ((ImageView)findViewById(R.id.color_circle)).setImageBitmap(map);
        ((ImageView)findViewById(R.id.color_circle)).getLayoutParams().width = size;
        ((ImageView)findViewById(R.id.color_circle)).getLayoutParams().height = size;
    }

    public void updateButton() {
        ((Button) findViewById(R.id.start))
                .setText(isMyServiceRunning(FloatingService.class)
                        ? R.string.stop : R.string.start);
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) App.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void playUnlockSound() {
        MediaPlayer player = MediaPlayer.create(App.getAppContext(), R.raw.s3_unlock);
        player.start();
    }

    public static Point getScreenSize(Context context) {
        Point screenSize = new Point();
        ((WindowManager) context.getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getSize(screenSize);
        return screenSize;
    }

    public static void setSize(int size) {
        App.getAppContext().getSharedPreferences("data", Context.MODE_PRIVATE).edit()
                .putInt(SF_SIZE, size)
                .apply();
    }

    public static int getSize() {
        return App.getAppContext().getSharedPreferences("data", Context.MODE_PRIVATE)
                .getInt(SF_SIZE, 128);
    }

    public void setAutoLocking(boolean state) {
        App.getAppContext().getSharedPreferences("data", Context.MODE_PRIVATE).edit()
                .putBoolean(SF_AUTO_LOCK, state)
                .apply();
    }

    public static boolean isAutoLocking() {
        return App.getAppContext().getSharedPreferences("data", Context.MODE_PRIVATE)
                .getBoolean(SF_AUTO_LOCK, false);
    }

    public static void setLockTime(int time) {
        if (time < 0) time = 0;
        App.getAppContext().getSharedPreferences("data", Context.MODE_PRIVATE).edit()
                .putInt(SF_LOCK_TIME, time)
                .apply();
    }

    public static int getLockTime() {
        return App.getAppContext().getSharedPreferences("data", Context.MODE_PRIVATE)
                .getInt(SF_LOCK_TIME, 1);
    }


}
