package cz.lastaapps.black;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

public class OverlayPermissionActivity extends Activity {
    /** code to post/handler request for permission */
    public final static int REQUEST_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permission_layout);

        findViewById(R.id.got_it).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkDrawOverlayPermission();
            }
        });
    }

    public void checkDrawOverlayPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            /** check if we already  have permission to draw over other apps */
            if (!Settings.canDrawOverlays(this)) {
                /** if not construct intent to request permission */
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                /** request permission via start activity for result */
                startActivityForResult(intent, REQUEST_CODE);
            } else setServiceEnabled(true);
        } else setServiceEnabled(true);
    }

    @Override
    @TargetApi(23)
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        /** check if received result code
         is equal our requested code for draw permission  */
        if (requestCode == REQUEST_CODE) {
            /*if so check once again if we have permission */
            if (Settings.canDrawOverlays(this)) {
                setServiceEnabled(true);
                finish();
            } else {
                setServiceEnabled(false);
            }
        }
    }

    private void setServiceEnabled(boolean state) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            return;

        PackageManager pm = this.getPackageManager();
        ComponentName name = new ComponentName(this, MyTileService.class);
        if (state) {
            pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0);
            finish();
        } else {
            pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
        }
    }

    public static boolean isEnabled() {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            return Settings.canDrawOverlays(App.getAppContext());
        }
        else return true;
    }
}
