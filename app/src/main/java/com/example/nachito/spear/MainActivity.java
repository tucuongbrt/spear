package com.example.nachito.spear;

import android.Manifest.permission;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ExpandedMenuView;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.LocationSource.OnLocationChangedListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayItem.HotspotPlace;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.util.constants.MapViewConstants;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.Goto;
import pt.lsts.imc.Loiter;
import pt.lsts.imc.Maneuver;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanDB;
import pt.lsts.imc.StationKeeping;
import pt.lsts.imc.Teleoperation;
import pt.lsts.imc.VehicleState;
import pt.lsts.imc.net.Consume;
import pt.lsts.neptus.messages.listener.Periodic;
import pt.lsts.util.WGS84Utilities;
import static android.os.Build.VERSION_CODES.M;
import static com.example.nachito.spear.R.id.bottomsheet;
import static com.example.nachito.spear.R.id.imageView;


@EActivity

public class MainActivity extends AppCompatActivity
        implements  MapViewConstants, OnLocationChangedListener,SharedPreferences.OnSharedPreferenceChangeListener,  View.OnClickListener,   LocationListener {

    Context context = this;
    @ViewById(R.id.dive)
    Button dive;
    @ViewById(R.id.near)
    Button near;
    @ViewById(R.id.startplan)
    Button start;
    @ViewById(R.id.KeepStation)
    Button keep;
    @ViewById(R.id.servicebar)
    TextView servicebar;
    @Bean
    IMCGlobal imc;
    @ViewById(imageView)
    ImageView wifi;
    @ViewById(R.id.imageView2)
    ImageView nowifi;
    TeleOperation teleop2;

    List<String> vehicleList;
    List<String> planList;
    String selected;
    List<VehicleState> states;
    @ViewById(R.id.map)
    MapView map;
    MyLocationNewOverlay mLocationOverlay;
    @ViewById(R.id.teleop)
    Button teleop;
    @ViewById(R.id.stop)
    Button stop;
    CompassOverlay mCompassOverlay;
    LocationManager locationManager;
    double orientation2;
    IMapController mapController;
    OSMHandler updateHandler;
    OverlayItem lastPosition = null;
    OsmMapsItemizedOverlay mItemizedOverlay;
    int tamanhoLista;
    double latVeiculo;
    double lonVeiculo;
    double latitude;
    double longitude;
    boolean doubleBackToExitPressedOnce = false;
    @ViewById(R.id.velocity)
    TextView velocity;
    double speed;
    int duration;
    double radius;
    double depth;
    Bitmap target;
    int color = Color.parseColor("#39B7CD"), pressed_color = Color.parseColor("#568203");
    GeoPoint posicaoVeiculo;
   Button done;
@ViewById(R.id.bottomsheet)
    LinearLayout bottom;
Line line;
    Press trans;
    Marker nodeMarker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setClickable(true);
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(context), map);
        mLocationOverlay.enableMyLocation();
        map.getOverlays().add(this.mLocationOverlay);
        mCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), map);
        mCompassOverlay.enableCompass();
        map.getOverlays().add(this.mCompassOverlay);
        velocity.bringToFront();
        setupSharedPreferences();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowHomeEnabled(false);
        }
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        init();
        Accelerate accelerate = (Accelerate) findViewById(R.id.accelerate);
        accelerate.setVisibility(View.INVISIBLE);
        Decelerate decelerate = (Decelerate) findViewById(R.id.decelerate);
        decelerate.setVisibility(View.INVISIBLE);
        Joystick joystick = (Joystick) findViewById(R.id.joystick);
        joystick.setVisibility(View.INVISIBLE);
        nowifi.setVisibility(View.INVISIBLE);
        StopTeleop stopTeleop = (StopTeleop) findViewById(R.id.stopTeleop);
        stopTeleop.setVisibility(View.INVISIBLE);
        trans=(Press)findViewById(R.id.transparente);
        imc.register(this);
        if (android.os.Build.VERSION.SDK_INT >= M) {
            checkLocationPermission();
        }
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } catch (Exception ignored) {
        }

        /* location manager */
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        updateHandler = new OSMHandler(this);
        Location location = null;
        for (String provider : locationManager.getProviders(true)) {
            location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                locationManager.requestLocationUpdates(provider, 0, 0, updateHandler);
                break;
            }
        }
        //Adiciona localizaçao Android
        if (location == null) {
            location = new Location(LocationManager.GPS_PROVIDER);
        }
        this.map.getOverlays().add(this.mLocationOverlay);
        mLocationOverlay.enableMyLocation();
        map.invalidate();
        mapController = map.getController();
        mapController.setZoom(15);
        //(Done) Centrar na localizacao do android
        mapController.setCenter(new GeoPoint(location));
        done = (Button) findViewById(R.id.done);
        done.setVisibility(View.INVISIBLE);


    }

    public void updatePosition(GeoPoint aPoint) {
        if (mItemizedOverlay == null) {
            return;
        }
        OverlayItem overlayItem;
        overlayItem = new OverlayItem("Center", "Center", aPoint);
        lastPosition = overlayItem;
        mItemizedOverlay.addOverlay(overlayItem);
        map.getOverlays().add(mItemizedOverlay);
        map.getController().animateTo(aPoint);}
//Refreshing the map to draw the new overlay

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }
    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user  timer.start()
                // sees the explanation, try again to request the permission.
                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        registerForContextMenu(servicebar);
        openContextMenu(servicebar);
    }


    private void setupSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        loadFromPrefs(sharedPreferences);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }


    private void loadFromPrefs(SharedPreferences sharedPreferences) {
        speed = Float.parseFloat(sharedPreferences.getString(getString(R.string.pref_speed_key),getString(R.string.pref_speed_default)));
        duration = (int) Float.parseFloat(sharedPreferences.getString(getString(R.string.pref_duration_key),getString(R.string.pref_duration_default)));
        radius = Float.parseFloat(sharedPreferences.getString(getString(R.string.pref_radius_key),getString(R.string.pref_radius_default)));
        depth = Float.parseFloat(sharedPreferences.getString(getString(R.string.pref_depth_key),getString(R.string.pref_depth_default)));    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_speed_key))) {
            loadFromPrefs(sharedPreferences);

        } else if (key.equals(getString(R.string.pref_duration_key))) {
            loadFromPrefs(sharedPreferences);

        } else if (key.equals(getString(R.string.pref_radius_key))) {
            loadFromPrefs(sharedPreferences);
        } else if (key.equals(getString(R.string.pref_depth_key))) {
            loadFromPrefs(sharedPreferences);
        }
    }


    @UiThread
    public void init() {

        teleop.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (imc.selectedvehicle == null) {
                    Toast.makeText(MainActivity.this, "Select a vehicle first", Toast.LENGTH_SHORT).show();
                } else {
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                    Builder builder = alertDialogBuilder
                            .setMessage("Connect to " + imc.selectedvehicle + "?")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new OnClickListener() {

                                //se carregarmos em yes abre a TeleOp class
                                public void onClick(DialogInterface dialog, int id) {
                                    if (teleop2 == null)
                                        teleop2 = new TeleOperation();
                                    teleop2.setImc(imc);
                                    Joystick joystick = (Joystick) findViewById(R.id.joystick);
                                    joystick.setOnJoystickMovedListener(teleop2);
                                    getSupportFragmentManager().beginTransaction()
                                            .replace(R.id.activity_maps, teleop2).addToBackStack("tag").commit();
                                    dive.setVisibility(View.INVISIBLE);
                                    teleop.setVisibility(View.INVISIBLE);
                                    start.setVisibility(View.INVISIBLE);
                                    near.setVisibility(View.INVISIBLE);
                                    keep.setVisibility(View.INVISIBLE);
                                    Accelerate accelerate = (Accelerate) findViewById(R.id.accelerate);
                                    accelerate.setVisibility(View.VISIBLE);
                                    accelerate.setOnAccelerate(teleop2);
                                    Decelerate decelerate = (Decelerate) findViewById(R.id.decelerate);
                                    decelerate.setVisibility(View.VISIBLE);
                                    decelerate.setOnDec(teleop2);
                                    stop.setVisibility(View.INVISIBLE);
                                    StopTeleop stopTeleop = (StopTeleop) findViewById(R.id.stopTeleop);
                                    stopTeleop.setVisibility(View.VISIBLE);
                                    stopTeleop.setOnStop(teleop2);
                                    timer.start();
                                    joystick.setVisibility(View.VISIBLE);
                                    PlanControl pc = new PlanControl();
                                    Teleoperation teleoperationMsg = new Teleoperation();
                                    teleoperationMsg.setCustom("src=" + imc.getLocalId());
                                    pc.setArg(teleoperationMsg);
                                    pc.setType(PlanControl.TYPE.REQUEST);
                                    pc.setOp(PlanControl.OP.START);
                                    pc.setFlags(0);
                                    pc.setRequestId(0);
                                    pc.setPlanId("teleoperation-mode");
                                    imc.sendMessage(pc);
                                }
                            })
                            .setNegativeButton("No", new OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // if this button is clicked, just close
                                    // the dialog box and do nothing

                                    dialog.cancel();
                                }
                            });
                    // create alert dialog
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    // show it
                    alertDialog.show();
                }

            }
        });


//carregar no StartPlan
        start.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                start.setBackgroundColor(pressed_color);

                if (imc.selectedvehicle == null) {
                    Toast.makeText(MainActivity.this, "Select a vehicle first", Toast.LENGTH_SHORT).show();
                    start.setBackgroundColor(color);

                } else {
                    requestPlans();
                    start.setBackgroundColor(color);

                }

            }

        });


        dive.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dive.setBackgroundColor(pressed_color);

                if (imc.selectedvehicle == null) {
                    Toast.makeText(MainActivity.this, "Select a vehicle first", Toast.LENGTH_SHORT).show();
                    dive.setBackgroundColor(color);

                } else {
                    dive();
                    dive.setBackgroundColor(color);

                }
            }
        });

        near.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                near.setBackgroundColor(pressed_color);

                if (imc.selectedvehicle == null) {
                    Toast.makeText(MainActivity.this, "Select a vehicle first", Toast.LENGTH_SHORT).show();
                    near.setBackgroundColor(color);

                } else {
                    near();
                    near.setBackgroundColor(color);

                }

            }

        });

//carregar no stop
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (imc.selectedvehicle == null) {
                    Toast.makeText(MainActivity.this, "Select a vehicle first", Toast.LENGTH_SHORT).show();
                } else {
                    stopPlan();
                    map.getOverlays().clear();
                    mCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), map);
                    mCompassOverlay.enableCompass();
                    map.getOverlays().add(mCompassOverlay);

                }
            }


        });

        keep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keep.setBackgroundColor(pressed_color);

                if (imc.selectedvehicle == null) {
                    Toast.makeText(MainActivity.this, "Select a vehicle first", Toast.LENGTH_SHORT).show();
                    keep.setBackgroundColor(color);

                } else {
                    keepStation();
                    keep.setBackgroundColor(color);
                }
            }
        });


    }


    @Override
    public void onBackPressed() {

            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }
            if (teleop2 != null) {
                teleop2.finish();

                PlanControl pc = new PlanControl();
                pc.setType(PlanControl.TYPE.REQUEST);
                pc.setOp(PlanControl.OP.STOP);
                pc.setRequestId(1);
                pc.setFlags(0);
                pc.setPlanId("stopTeleOp");

                getFragmentManager().popBackStack();
                dive.setVisibility(View.VISIBLE);
                teleop.setVisibility(View.VISIBLE);
                start.setVisibility(View.VISIBLE);
                near.setVisibility(View.VISIBLE);
                keep.setVisibility(View.VISIBLE);
                stop.setVisibility(View.VISIBLE);
                Accelerate accelerate = (Accelerate) findViewById(R.id.accelerate);
                accelerate.setVisibility(View.INVISIBLE);
                Decelerate decelerate = (Decelerate) findViewById(R.id.decelerate);
                decelerate.setVisibility(View.INVISIBLE);
                StopTeleop stopTeleop = (StopTeleop) findViewById(R.id.stopTeleop);
                stopTeleop.setVisibility(View.INVISIBLE);
                Joystick joystick = (Joystick) findViewById(R.id.joystick);
                joystick.setVisibility(View.INVISIBLE);
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press BACK again to exit", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 500);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    //(DONE) andar back settings
    //(DONE) botoes acelerar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            try {
                startActivity(new Intent(this, SettingsActivity.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        else if(id==R.id.edit){

            done.setVisibility(View.VISIBLE);
            bottom.setVisibility(View.INVISIBLE);
            line= new Line();
            trans.setonPress(line);



        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister MainActv as an OnPreferenceChangedListener to avoid any memory leaks.
        android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);

        imc.stop();
    }

    LinkedHashMap<String, EstimatedState> estates = new LinkedHashMap<>();

    @Background
    @Consume
    public void receive(final EstimatedState state) {
        synchronized (estates) {
            estates.put(state.getSourceName(), state);
        }
    }






    @Background
    @Periodic(500)
    public void updateMap() {
        map.getOverlays().clear();
        map.getOverlays().add(this.mCompassOverlay);
        synchronized (estates) {
            for (EstimatedState state : estates.values()) {
                paintState(state);
            }

        }
    }


    @Background
    public void paintState(final EstimatedState state) {
        final String vname = state.getSourceName();
        //(DONE) por todas as posiçoes
        double[] lld = WGS84Utilities.toLatLonDepth(state);
        final ArrayList<OverlayItem> items2 = new ArrayList<OverlayItem>();

        OverlayItem marker2 = new OverlayItem("markerTitle", "markerDescription", new GeoPoint(lld[0], lld[1]));
        marker2.setMarkerHotspot(HotspotPlace.TOP_CENTER);
        items2.add(marker2);

        orientation2 = state.getPsi();
        int ori2 = (int) Math.round(Math.toDegrees(orientation2));
        ori2 = ori2 - 180;

        Bitmap source2 = BitmapFactory.decodeResource(this.getResources(), R.drawable.arrow_blue);

        if (vname.equals(imc.getSelectedvehicle())) {
            posicaoVeiculo = new GeoPoint(lld[0], lld[1]);
            latVeiculo = Math.toRadians(lld[0]);
            lonVeiculo = Math.toRadians(lld[1]);
            source2 = BitmapFactory.decodeResource(this.getResources(), R.drawable.arrow_green);
            DecimalFormat df2 = new DecimalFormat("#.##");
            final String vel = df2.format(Math.sqrt((state.getVx() * state.getVx()) + (state.getVy() * state.getVy()) + (state.getVz() * state.getVz())));
            final String dept = df2.format(state.getDepth()); //(DONE) por so 2 casa decimais
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    velocity.setText("Speed:" + " " + vel + " " + "m/s" + "\n" + "Depth:" + " " + dept + "\n");

                }
            });

        }


        target = RotateMyBitmap(source2, ori2);
        Drawable marker_ = new BitmapDrawable(getResources(), target);
        ItemizedIconOverlay markersOverlay2 = new ItemizedIconOverlay<>(items2, marker_, null, context);
        map.getOverlays().add(markersOverlay2);


    }


    public static Bitmap RotateMyBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }




public  void dive() {
    Loiter dive = new Loiter();
    dive.setLon(lonVeiculo);
    dive.setLat(latVeiculo);
    dive.setZ(depth);
    dive.setZUnits(Loiter.Z_UNITS.DEPTH);
    dive.setSpeed(speed);
    dive.setSpeedUnits(Loiter.SPEED_UNITS.RPM);
    dive.setRadius(radius);
    dive.setDuration(duration);
    dive.setBearing(0);
    String planid = "Dive";
    startManeuver(planid, dive);
}// /.neptus.sh mra

    public  void keepStation() {
        StationKeeping stationKeepingmsg = new StationKeeping();
        stationKeepingmsg.setLat(latVeiculo);
        stationKeepingmsg.setLon(lonVeiculo);
        stationKeepingmsg.setSpeed(speed);
        stationKeepingmsg.setSpeedUnits(StationKeeping.SPEED_UNITS.RPM);
        stationKeepingmsg.setDuration(duration);
        stationKeepingmsg.setRadius(radius);
        stationKeepingmsg.setZ(depth);
        stationKeepingmsg.setZUnits(StationKeeping.Z_UNITS.DEPTH);
        String planid = " StationKeeping";
        startManeuver(planid, stationKeepingmsg);
    }

    public  void near() {
        if(latitude == 0 & longitude == 0){
            return;
        }
        Goto go = new Goto();
        go.setLat(latitude);
        go.setLon(longitude);
        go.setZ(0);
        go.setZUnits(Goto.Z_UNITS.DEPTH);
        go.setSpeed(speed);
        go.setSpeedUnits(Goto.SPEED_UNITS.RPM);
        String planid = "ComeNear";
        startManeuver(planid, go);
    }


    public  void startManeuver(String planid, Maneuver maneuver) {
        PlanControl pc = new PlanControl();
        pc.setArg(maneuver);
        pc.setType(PlanControl.TYPE.REQUEST);
        pc.setOp(PlanControl.OP.START);
        pc.setFlags(0);
        pc.setRequestId(0);
        pc.setPlanId(planid);
        imc.sendMessage(pc);

    }


    public  void Go(GeoPoint p){
        Goto go2 = new Goto();
        double lat = Math.toRadians(p.getLatitude());
        double lon = Math.toRadians(p.getLongitude());
        go2.setLat(lat);
        go2.setLon(lon);
        go2.setZ(depth);
        go2.setZUnits(Goto.Z_UNITS.DEPTH);
        go2.setSpeed(speed);
        go2.setSpeedUnits(Goto.SPEED_UNITS.RPM);
        String planid = "GoToPoint";
        startManeuver(planid, go2);
    }
    public  void stopPlan() {
        PlanControl pc = new PlanControl();
        pc.setType(PlanControl.TYPE.REQUEST);
        pc.setOp(PlanControl.OP.STOP);
        pc.setRequestId(1);
        pc.setFlags(0);
        pc.setPlanId("teleoperation-mode");
        imc.sendMessage(pc);
    }




    @Override
    public void onStart() {
        timer.start();
        super.onStart();
    }

    //if there is wifi show an imageview, when the wifi is off change it to another
    private boolean isConnectedToWifi(Context context) {

        ConnectedWifi = false;
        try {
            ConnectivityManager nConManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (nConManager != null) {
                NetworkInfo nNetworkinfo = nConManager.getActiveNetworkInfo();
                if (nNetworkinfo.isConnected()) {
                    ConnectedWifi = true;
                    return ConnectedWifi;
                }
            }
        } catch (Exception e) {
        }
        return ConnectedWifi;
    }

    private static boolean ConnectedWifi;
    //Set a timer to check if is connected to a Wifi Network
    TimerWifi timer = new TimerWifi(new Runnable() {
        @Override
        public void run() {
            // Check if is connected to a Wifi Network, if not popups a informative toast
            if (!isConnectedToWifi(MainActivity.this)) {
                wifi.setVisibility(View.INVISIBLE);
                nowifi.setVisibility(View.VISIBLE);
            } else {
                nowifi.setVisibility(View.INVISIBLE);
                wifi.setVisibility(View.VISIBLE);
            }
        }
    }, 3000);



    //ContextMenu para os planos (start button)


    //(DONE) Adicionar lista de planos

    public void requestPlans() {
        if (imc.selectedvehicle == null) {
            Toast.makeText(MainActivity.this, "Select a vehicle first", Toast.LENGTH_SHORT).show();
            return;
        } else {
            PlanDB pdb = new PlanDB();
            pdb.setOp(PlanDB.OP.GET_INFO);
            imc.sendMessage(pdb);
            registerForContextMenu(start);
            openContextMenu(start);
        }
    }



    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
if(v.getId()==R.id.startplan) {
    planList = new ArrayList<>();
    if (imc.allPlans() == null) {
        Toast.makeText(this, "No plans available", Toast.LENGTH_SHORT).show();
        return;
    }
    for (int i = 0; i < imc.allPlans().size(); i++) {
        planList.addAll(imc.allPlans());
        menu.add(i, i, i, planList.get(i));
        menu.setHeaderTitle("Plan List");

    }

    tamanhoLista = planList.size();
}else if(v.getId()==R.id.servicebar){
    vehicleList = new ArrayList<>();
    states = imc.connectedVehicles();
    if(imc.connectedVehicles()==null)
        Toast.makeText(this, "No vehicles available", Toast.LENGTH_SHORT).show();
    for (VehicleState state : states) {
        vehicleList.add(state.getSourceName() + ":" + state.getOpMode());    }
    for(int i=0; i<vehicleList.size(); i++){
        String connectedvehicles= vehicleList.toString();
        String[] getName = connectedvehicles.split(",");
        getName[0] = getName[0].substring(1);
            getName[getName.length-1] = getName[getName.length-1].substring(0,getName[getName.length-1].length()-1);
        String selectedName= getName[i];
        menu.add(i,i,i,selectedName );


    }

}
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(!(item.toString().contains(":"))) {
            PlanControl pc = new PlanControl();
            pc.setType(PlanControl.TYPE.REQUEST);
            pc.setOp(PlanControl.OP.START);
            pc.setFlags(0);
            pc.setRequestId(0);
            pc.setPlanId(item.toString());
            imc.sendMessage(pc);
        }else {
            selected = item.toString();
            String[] getName2 = selected.split(":");
            String selectedName2 = getName2[0];
            imc.setSelectedvehicle(selectedName2.trim());
            servicebar.setText(selectedName2);
        }
    //TODO se em teleoperacao mudar para command
    if (teleop2 != null) {
        teleop2.finish();
    }

        return super.onContextItemSelected(item);
    }

    @Periodic(500)
    @Override
    public void onLocationChanged(Location location) {
        final ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
        latitude = Math.toRadians(location.getLatitude());
        longitude = Math.toRadians(location.getLongitude());
        GeoPoint posicao = new GeoPoint(location.getLatitude(), location.getLongitude());


        OverlayItem marker = new OverlayItem("markerTitle", "markerDescription", posicao);
        marker.setMarkerHotspot(OverlayItem.HotspotPlace.TOP_CENTER);
        items.add(marker);
        Drawable newMarker = ResourcesCompat.getDrawable(getResources(), R.drawable.arrow_red, null);
        ItemizedIconOverlay markersOverlay = new ItemizedIconOverlay<>(items, newMarker, null, context);
        map.getOverlays().add(markersOverlay);
        mCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), map);
        mCompassOverlay.enableCompass();
        map.getOverlays().add(this.mCompassOverlay);


    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


}

//TODO 2 joysticks
//todo map offline
//todo cores
//TODO se desligar veiculo selecionado tem de ficar em branco
//TODO recentrar no veic ou no android
