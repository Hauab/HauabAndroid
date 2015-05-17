package org.kaufer.matthew.hauab;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.EstimoteSDK;
import com.estimote.sdk.Region;
import com.estimote.sdk.cloud.CloudCallback;
import com.estimote.sdk.cloud.EstimoteCloud;
import com.estimote.sdk.cloud.model.BeaconInfo;
import com.estimote.sdk.exception.EstimoteServerException;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.app.PendingIntent.getActivities;


public class Home extends Activity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {



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

    private long vibTime = 250;
    private long[] exitPattern  = {0, vibTime, vibTime, vibTime, vibTime};

    private Button button;
    private Vibrator vibrator;


    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

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

    public void alert(String message){
        boolean appInForeground = isAppInForeground(getApplicationContext());
        if(appInForeground){
            //text, etc.
            textView.setText(message);
        } else {
            //notification
            postNotification(message);
            vibrator.vibrate(vibTime);
        }
    }

    private String getColor(BeaconInfo b){
        return "color " + b.color;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

//        EstimoteSDK.initialize(getApplicationContext(), "hauab", "e5377749255f04840acbfe1c8ea8acfa");




        textView = (TextView)findViewById(R.id.output);
        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        final TextView title = (TextView)findViewById(R.id.title);
        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/PaytoneOne.ttf");
        title.setTypeface(typeFace);
        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);


        buildGoogleApiClient();

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        updateLocation(mLastLocation);

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
//        title.setText("----");


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
                double lat = -1;
                double lon = -1;
                if(mLastLocation != null){
                    lat = mLastLocation.getLatitude();
                    lon = mLastLocation.getLongitude();
                }
                AloneZone zone = new AloneZone(lat, lon, val);
//                ((TextView)findViewById(R.id.title)).setText(lat + ":" + lon);
                HashMap<String, AloneZone> map = new HashMap<String, AloneZone>();
                map.put(key, zone);
                aloneZones.child(key).setValue(zone);

                setButtonToggleState(!val);
            }
        });
        beaconManager = new BeaconManager(this);
        beaconManager.setBackgroundScanPeriod(
                TimeUnit.SECONDS.toMillis(1), 0);
        Log.d(TAG, "READY");
//        textView.setText("WOOT");

        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> beacons) {
                final String key = createKey(beacons.get(0));


                aloneZones.child(key).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
//                        System.out.println(snapshot.getValue());
//                        ((TextView)findViewById(R.id.debug2)).setText("Entered region " + key + " " + snapshot.getValue());
//                        if(snapshot == null)
//                            return;
//
//                        Log.e(TAG, snapshot.getKey());
//                        AloneZone snap = (AloneZone)snapshot.getValue();
//                        Log.e(TAG, snapshot.toString());
//                        Log.e(TAG, snap.toString());

                        HashMap<String, Object> snap = (HashMap<String, Object>)snapshot.getValue();


                        currentBeaconAlone = false;
                        if(snap == null){
//                            ((TextView)findViewById(R.id.title)).setText("Null map");

                        } else {

                            currentBeaconAlone = (boolean) snap.get("zone");
                        }

                        setButtonVisibility(true);
                        output(currentBeaconAlone, key,"");
//                        EstimoteCloud.getInstance().fetchBeaconDetails(currentBeacon.getMacAddress(), new CloudCallback<BeaconInfo>() {
//                            @Override
//                            public void success(BeaconInfo beaconInfo) {
//                                String color = getColor(beaconInfo);
//                                output(currentBeaconAlone, key, color);
//                            }
//
//                            @Override
//                            public void failure(EstimoteServerException e) {
//                                output(currentBeaconAlone, key, "");
//                            }
//                        });



                    }

                    @Override
                    public void onCancelled(FirebaseError firebaseError) {
                        System.out.println("The read failed: " + firebaseError.getMessage());
                    }
                });

                currentBeacon = beacons.get(0);

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
                    alert("You've left AloneZone " + createKey(currentBeacon));

                } else
                    alert("Left a zone.");

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

//    private void updateLocation(Location location) {
//        TextView title = (TextView)findViewById(R.id.title);
//        if (location != null) {
//
////            mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
////            mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
////            title.setText(location.getLatitude() + ":" + location.getLongitude());
//        } else {
//            title.setText("Hauab--" + Math.random());
//        }
//    }
//
    private void output(boolean currentBeaconAlone, String key, String color) {
        if(currentBeaconAlone) {
            alert("Warning, entering AloneZone " + key + " " + color);
            setButtonToggleState(false);
        } else {

            alert("Entered a zone, but not an AloneZone, " + key + " " + color);
            setButtonToggleState(true);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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
                .setSmallIcon(R.drawable.halt)
                .setContentTitle("Hauab")
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

    @Override
    public void onConnected(Bundle bundle) {
        createLocationRequest();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
//        updateLocation(location);
    }

    protected void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }
//    protected void startLocationUpdates() {
//
//
//    }
}
