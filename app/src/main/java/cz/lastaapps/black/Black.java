package cz.lastaapps.black;

import android.app.Activity;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class Black extends Activity {
    RelativeLayout root;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;

        setContentView(R.layout.black);


        root = findViewById(R.id.root);
        root.setBackgroundColor(FloatingService.getColor());
        root.setKeepScreenOn(true);


        root.setOnClickListener(new View.OnClickListener() {
            long lastTouch = 0;
            @Override
            public void onClick(View v) {
                long currentTime = System.currentTimeMillis();
                if (lastTouch  + 500 > currentTime) {
                    finish();
                }
                lastTouch = currentTime;
            }
        });

        Toast.makeText(App.getAppContext(), R.string.close, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
