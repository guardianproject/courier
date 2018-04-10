package info.guardianproject.securereaderinterface;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;


public class PanicResponderActivity extends Activity {
    private static final String TAG = "PanicResponderActivity";

    public static final String PANIC_TRIGGER_ACTION = "info.guardianproject.panic.action.TRIGGER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null && PANIC_TRIGGER_ACTION.equals(intent.getAction())) {
            App.getInstance().socialReader.lockApp();
            LocalBroadcastManager.getInstance(this).sendBroadcastSync(new Intent(App.EXIT_BROADCAST_ACTION));
        }

        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        } else {
            finish();
        }
    }
}
