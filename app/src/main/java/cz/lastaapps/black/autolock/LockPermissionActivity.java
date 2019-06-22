package cz.lastaapps.black.autolock;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import cz.lastaapps.black.App;
import cz.lastaapps.black.R;

public class LockPermissionActivity extends Activity {
    protected static final int REQUEST_ENABLE = 0;
    static DevicePolicyManager devicePolicyManager = (DevicePolicyManager) App.getAppContext()
            .getSystemService(Context.DEVICE_POLICY_SERVICE);
    static ComponentName adminComponent
            = new ComponentName(App.getAppContext(), Darclass.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permission_layout);

        Button button = (Button) findViewById(R.id.got_it);
        button.setOnClickListener(btnListener);

        ((TextView)findViewById(R.id.message)).setText(R.string.permission_admin);
    }

    Button.OnClickListener btnListener = new Button.OnClickListener() {
        public void onClick(View v) {

            if (!isLockEnabled()) {

                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
                startActivityForResult(intent, REQUEST_ENABLE);
            } else {
                //devicePolicyManager.lockNow();
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_ENABLE == requestCode) {
            super.onActivityResult(requestCode, resultCode, data);

            if (isLockEnabled())
                finish();
        }
    }

    public static boolean isLockEnabled() {
        return devicePolicyManager.isAdminActive(adminComponent);
    }

    public static void lock() {
        if (isLockEnabled()) {
            devicePolicyManager.lockNow();
        } else {
            Toast.makeText(App.getAppContext(), R.string.could_not_lock, Toast.LENGTH_LONG).show();
        }
    }

    public static void enableActivity() {
        if (!isLockEnabled()) {
            Intent intent = new Intent(App.getAppContext(), LockPermissionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            App.getAppContext().startActivity(intent);


        }
    }
}
