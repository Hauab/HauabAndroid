package org.kaufer.matthew.hauab;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.app.PendingIntent.getActivities;


public class Home extends ActionBarActivity {



    private NotificationManager notificationManager;


    private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private static final Region ALL_ESTIMOTE_BEACONS = new Region("regionId", ESTIMOTE_PROXIMITY_UUID, null, null);

    private BeaconManager beaconManager;

    private String TAG = "Hauab";
    private int NID = 1;
    private TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        beaconManager = new BeaconManager(this);
        beaconManager.setBackgroundScanPeriod(
                TimeUnit.SECONDS.toMillis(1), 0);
        textView = (TextView)findViewById(R.id.debug);
        Log.d(TAG, "READY");
        textView.setText("WOOT");
        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> beacons) {

                ((TextView)findViewById(R.id.debug2)).setText("Entered region " + beacons.get(0).getMajor() + ":" + beacons.get(0).getMinor() );

                if (isAppInForeground(
                        getApplicationContext())) {
                    toastAlert("Entered region");
                    textView.setText("In the region!");
                } else {
                    postNotification("In the region!");
                    textView.setText("AAA");
                }
                System.out.println("ENTER");


            }

            @Override
            public void onExitedRegion(Region region) {

                if (isAppInForeground(
                        getApplicationContext())) {
                    toastAlert("Exited region");
                    textView.setText("Out of the region!");

                } else {
                    postNotification("Out of the region!");
                }
                System.out.println("LEAVE");


            }
        });
    }



    public boolean isAppInForeground(
            Context context) {
        List<ActivityManager.RunningTaskInfo> task = ((ActivityManager)
                context.getSystemService(
                        Context.ACTIVITY_SERVICE))
                .getRunningTasks(1);
        if (task.isEmpty()) {
            return false;
        }
        return task
                .get(0)
                .topActivity
                .getPackageName()
                .equalsIgnoreCase(
                        context.getPackageName());
    }

    public void toastAlert(String s, int duration){
        Toast.makeText(getApplicationContext(), s, duration).show();
    }

    public void toastAlert(String s){
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startMonitoring(ALL_ESTIMOTE_BEACONS);
                } catch (RemoteException e) {
                    Log.e(TAG, "Cannot start monitoring", e);
                }
            }
        });
    }
    private void postNotification(String msg) {
        Intent notifyIntent = new Intent(Home.this, Home.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivities(
                Home.this,
                0,
                new Intent[]{notifyIntent},
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(Home.this)
                .setContentTitle("Notify Demo")
                .setContentText(msg)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notificationManager.notify(NID, notification);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStop(){
        try {
//            beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS);
            beaconManager.stopMonitoring(ALL_ESTIMOTE_BEACONS);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot stop but it does not matter now", e);
        }
        super.onStop();
    }

    @Override
    public void onDestroy(){
        beaconManager.disconnect();
        super.onDestroy();
    }
}
