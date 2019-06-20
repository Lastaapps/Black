package cz.lastaapps.black;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

public class MainActivity extends AppCompatActivity {

    private static final String SF_SIZE = "SIZE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PermissionActivity.isEnabled() == false) {
            startActivity(new Intent(this, PermissionActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

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

        updateButton();
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

        initSizeBar();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            findViewById(R.id.tint_available).setVisibility(View.GONE);

        new AsyncTask<Object, Object, Object>(){
            @Override
            protected Object doInBackground(Object[] objects) {
                while(true) {
                    try { Thread.sleep(1000);
                    } catch (InterruptedException e) { e.printStackTrace(); }
                    publishProgress();
                }
            }

            @Override
            protected void onProgressUpdate(Object... values) {
                updateButton();
            }
        }.execute();

        ((ImageView)findViewById(R.id.color_circle)).setScaleType(ImageView.ScaleType.FIT_CENTER);
        updateCanvas();
    }

    private void initSizeBar() {
        SeekBar bar = findViewById(R.id.size_bar);
        bar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        if (Build.VERSION.SDK_INT >= 16)
            bar.getThumb().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);

        Point scSize = getScreenSize(this);
        final int smaller = Math.min(scSize.x, scSize.y);

        final int min = (int)(smaller * 0.1), max = (int)(smaller * 0.4);
        int progress = getSize(this);
        progress = progress > max ? max : progress;

        bar.setMax(max - min);
        bar.setProgress(progress - min);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    progress += min;
                    setSize(MainActivity.this, progress);
                    System.out.println(progress);
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

    public void updateCanvas() {
        int size = getSize(this);
        Bitmap map = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Paint main = new Paint(0);
        main.setStyle(Paint.Style.FILL);
        main.setColor(FloatingService.getColor());

        Paint circ = new Paint(0);
        circ.setStyle(Paint.Style.FILL);
        circ.setColor(Color.WHITE);

        Canvas c = new Canvas(map);
        c.drawCircle(size/2, size/2, size/2, circ);
        c.drawCircle(size/2, size/2, size/2 - (size/8), main);

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

    public static Point getScreenSize(Context context) {
        Point screenSize = new Point();
        ((WindowManager) context.getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getSize(screenSize);
        return screenSize;
    }

    public static void setSize(Context context, int size) {
        context.getSharedPreferences("data", Context.MODE_PRIVATE).edit().putInt(SF_SIZE, size).apply();
    }

    public static int getSize(Context context) {
        return context.getSharedPreferences("data", Context.MODE_PRIVATE).getInt(SF_SIZE, 256);
    }

}
