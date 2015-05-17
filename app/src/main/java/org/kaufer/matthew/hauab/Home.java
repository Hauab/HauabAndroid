package org.kaufer.matthew.hauab;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

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
    private Firebase ref, aloneZones;
    private Beacon currentBeacon;
    private boolean currentBeaconAlone = false;

    private Button button;

    private void setButtonVisibility(boolean visible){
        if(visible){//make it visible
            button.setVisibility(View.VISIBLE);
            button.setEnabled(true);
        } else {
            button.setVisibility(View.INVISIBLE);
            button.setEnabled(false);
        }
    }

    private void setButtonToggleState(boolean val){//true makes it green, with create, false is red
        if(val){
            button.setText("Enable Alone Zone");
            button.setTextColor(Color.BLACK);
        } else {
            button.setText("Disable Alone Zone");
            button.setTextColor(Color.RED);
        }
    }

    private String createKey(Beacon b){
        if(b == null)
            return "---";
        return b.getMajor() + ":" + b.getMinor();
    }

//    private final LocationListener locationListener = new LocationListener() {
//        public void onLocationChanged(Location location) {
//            textView.setText(location.getLatitude() + ":" + location.getLongitude());
//        }
//
//        @Override
//        public void onStatusChanged(String provider, int status, Bundle extras) {
//
//        }
//
//        @Override
//        public void onProviderEnabled(String provider) {
//
//        }
//
//        @Override
//        public void onProviderDisabled(String provider) {
//
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        textView = (TextView)findViewById(R.id.debug);


//        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
//        Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
//
//        if(location != null)
//            textView.setText(location.getLatitude() + ":" + location.getLongitude());
//        else
//            textView.setText("Location null");
//
//        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);

//        double longitude = location.getLongitude();
//        double latitude = location.getLatitude();




        Firebase.setAndroidContext(this);
        ref = new Firebase("https://boiling-inferno-9895.firebaseio.com/");
        aloneZones = ref.child("zones");
        button = (Button)findViewById(R.id.button);

        setButtonVisibility(false);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String key = createKey(currentBeacon);
//                aloneZones.child(currentBeacon.getMajor() + ":" + currentBeacon.getMinor()).setValue(true);//enable the zone
//                if(button.getBackground().equals(Color.RED)){//disable the alone zone
//                    aloneZones.child(createKey(currentBeacon)).setValue(false);
//                } else {

                //if green, set the alone zone as enabled, or true
                boolean val = button.getCurrentTextColor() == Color.BLACK;
                aloneZones.child(key).setValue(val);
                setButtonToggleState(!val);
            }
        });
        beaconManager = new BeaconManager(this);
        beaconManager.setBackgroundScanPeriod(
                TimeUnit.SECONDS.toMillis(1), 0);
        Log.d(TAG, "READY");
        textView.setText("WOOT");
        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> beacons) {
                final String key = createKey(beacons.get(0));


                aloneZones.child(key).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        System.out.println(snapshot.getValue());
//                        ((TextView)findViewById(R.id.debug2)).setText("Entered region " + key + " " + snapshot.getValue());
                        currentBeaconAlone = (snapshot.getValue() == true);
                        if(currentBeaconAlone) {
                            ((TextView) findViewById(R.id.debug2)).setText("Warning, entering AloneZone " + key);
                            setButtonToggleState(false);
                        } else {
                            setButtonToggleState(true);
                            ((TextView) findViewById(R.id.debug2)).setText("Entered a zone, but not an AloneZone");
                        }

//                        aloneZones.child(key)
                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        System.out.println("The read failed: " + firebaseError.getMessage());
                    }
                });

                currentBeacon = beacons.get(0);
                setButtonVisibility(true);
//                if (isAppInForeground(
//                        getApplicationContext())) {
//                    toastAlert("Entered region");
//                    textView.setText("In the region!");
//
//                } else {
//                    postNotification("In the region!");
//                    textView.setText("AAA");
//                }
//                System.out.println("ENTER");


            }

            @Override
            public void onExitedRegion(Region region) {
                button.setEnabled(false);
                button.setVisibility(View.INVISIBLE);
                if(currentBeaconAlone){
                    currentBeaconAlone = false;
                    ((TextView)findViewById(R.id.debug2)).setText("You've left AloneZone " + createKey(currentBeacon));
                }

                currentBeacon = null;

//                if (isAppInForeground(
//                        getApplicationContext())) {
//                    toastAlert("Exited region");
//                    textView.setText("Out of the region!");
//
//                } else {
//                    postNotification("Out of the region!");
//                }
//                System.out.println("LEAVE");


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
