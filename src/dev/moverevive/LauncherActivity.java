package dev.moverevive;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * Library entry for Move, whose own tile came from Meta's catalog. Opens it and exits.
 * Finishing straight away times out vrshell's display resolver, so it stays up briefly.
 */
public final class LauncherActivity extends Activity {
    private static final long FINISH_DELAY_MS = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent()
                .setComponent(new ComponentName(
                        "com.oculus.fitnesstracker",
                        "com.oculus.fitnesstracker.FitnessTrackerActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, FINISH_DELAY_MS);
    }
}
