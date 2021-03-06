package org.tensorflow.yolo;


import  android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import android.content.Intent;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.navigation.NavigationView;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.LineString;
import com.mapbox.mapboxsdk.Mapbox;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.api.Places;

import org.tensorflow.yolo.view.ClassifierActivity;

import static com.mapbox.core.constants.Constants.PRECISION_6;


public class MapFragmentActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener,
        MapboxMap.OnMapClickListener {


    BackgroundWorker backgroundWorker;
    //??????????????? ??? ??????
    private DrawerLayout mDrawerlayout;
    private ActionBarDrawerToggle mToggle;


    private MapView mapView;
    private MapboxMap mapboxMap;

    private PermissionsManager permissionsManager;

    private LocationEngine locationEngine;
    private long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;

    private MapFragmentActivityLocationCallback callback = new MapFragmentActivityLocationCallback(this);
    private static final String Tag = "MapFragmentActivity_location";

    //navigation
    private Point originPosition;
    private Point destinatonPosition;
    private Marker destinationMarker;
    private Button startButton;
    private NavigationMapRoute navigationMapRoute;
    public static DirectionsRoute currentRoute;
    private MapboxDirections client;
    private static final String TAG = "MapFragmentActivity_navigation";
    //bookMark
    static BookMarkList bookMarkList;
    static Button btn_add;
    static Button btn_delete;
    static String user_id, place_mark, type, buttonState;

    EditText editText;

    double destinationX; // longitude
    double destinationY; // latitude
    public static double La;          //latitude
    public static double Lo;          // longitude

    //String TAG = "placeautocomplete";
    static TextView txtView;
    //STT
    static AutocompleteSupportFragment STT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.eyJ1IjoiamFja3NwYXJyb3cwMTMxIiwiYSI6ImNrcHdkZ2NuODAwZ3kyd3AwNDRrNGw2OW8ifQ.DM8he5yBLvFF1JlHigUwDA");

        setContentView(R.layout.fragment_map);
        Log.e(TAG, "onCreate ??????");
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);


        startButton = findViewById(R.id.startButton);//??????????????? ????????? ??????
        editText = (EditText) findViewById(R.id.txtDestination);
        Button search_Button = findViewById(R.id.btnStartLoc);

        startButton.setOnClickListener(new View.OnClickListener() { //??????????????? ?????? ????????????+ar ????????????
            @Override
            public void onClick(View v) {
                startButton.setEnabled(false);


                NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                        .directionsRoute(currentRoute)
                        .build();
                // Call this method with Context from within an Activity
                NavigationLauncher.startNavigation(MapFragmentActivity.this, options);
                //??????????????? ?????? (MapActivity??????)
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MapFragmentActivity.this, ClassifierActivity.class);
                        startActivity(intent);
                    }
                }, 7000);
            }
        });


        //.loginSession
        //??? ????????? ????????? ?????? + ?????? ?????? ????????? ?????????
        search_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(La, Lo)) // Sets the new camera position
                        .zoom(13) // Sets the zoom , ??? ?????? ????????? ????????? ????????? ??????
                        .bearing(180) // Rotate the camera , ????????? ??????(????????? 0) ???????????? ?????????????????? ??????
                        .tilt(0) // Set the camera tilt , ??????
                        .build(); // Creates a CameraPosition from the builder
                //????????? ????????????
                mapboxMap.animateCamera(CameraUpdateFactory
                        .newCameraPosition(position), 7000);
                Toast.makeText(getApplicationContext(), String.format("            ????????? \n?????? : " + La + "\n?????? : " + Lo), Toast.LENGTH_SHORT).show();
            }
        });

        mDrawerlayout = (DrawerLayout) findViewById(R.id.drawer);

        mToggle = new ActionBarDrawerToggle(this, mDrawerlayout, R.string.open, R.string.close);
        mDrawerlayout.addDrawerListener(mToggle);
        mToggle.syncState();
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        getSupportActionBar().setHomeButtonEnabled(true);




        //?????? ????????????
        Button search = (Button) findViewById(R.id.btnSearch);
        search.bringToFront();
        txtView = findViewById(R.id.txtDestination);
        // Initialize Places.
        Places.initialize(getApplicationContext(), "AIzaSyBv-e4yH0jQvSVd-uEWiF-uJRznblHb9QU");
        // Create a new Places client instance.
        PlacesClient placesClient = Places.createClient(this);
        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        STT = autocompleteFragment;
        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));
        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {

            @Override
            public void onPlaceSelected(Place place) {

                // TODO: Get info about the selected place.
                txtView.setText(place.getName());
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });
        //.?????? ????????????
        //Speak to Text ??????

        Button sttButton = (Button) findViewById(R.id.btn_stt);
        sttButton.bringToFront();
        sttButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH);
                try {
                    startActivityForResult(intent, 200);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(), "Intent problem", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //bookMark
        //userid = (TextView)findViewById(R.id.userid);
        //place = (EditText)findViewById(R.id.place);




    }
    //onCreate ???


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                editText.setText(result.get(0));
                STT.setText(result.get(0));
                startButton.setEnabled(true);
                getPointFromGeoCoder(editText.getText().toString());
                Point origin = Point.fromLngLat(Lo, La);
                Point destination = Point.fromLngLat(destinationX, destinationY);
                getRoute_walking(origin, destination);//???????????? ?????????
                getRoute_navi_walking(origin, destination);
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            if (mapboxMap.getStyle() != null) {

            }
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void showDialog2(View _view) //???????????? ????????? ???????????????
    {
        final CharSequence[] oItems = {"??????"};
        AlertDialog.Builder oDialog = new AlertDialog.Builder(this,
                android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
        oDialog.setTitle("????????? ???????????????")
                .setItems(oItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            //?????? ????????? ??????
                            getPointFromGeoCoder(editText.getText().toString());
                            Point origin = Point.fromLngLat(Lo, La);
                            Point destination = Point.fromLngLat(destinationX, destinationY);
                            getRoute_walking(origin, destination);//?????? ?????? ??? ?????? ?????? ??????
                            getRoute_navi_walking(origin, destination);//??????????????? ?????? ??????

                            startButton.setEnabled(true);

                        }else {
                            Toast.makeText(getApplicationContext(), "?????? ??????", Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setCancelable(false) //??????????????? ?????? ??????
                .show();
    }




    public void onMark2(View view) {
        place_mark = txtView.getText().toString();
        type = "bookmark";
        buttonState = "delete";
        BackgroundWorker backgroundWorker = new BackgroundWorker(this);
        backgroundWorker.execute(type, user_id, place_mark, buttonState);
    }

    class MapFragmentActivityLocationCallback implements LocationEngineCallback<LocationEngineResult> {
        private final WeakReference<MapFragmentActivity> activityWeakReference;

        MapFragmentActivityLocationCallback(MapFragmentActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @Override
        public void onSuccess(LocationEngineResult result) {
            Log.e(TAG, "onSuccess ??????");
            MapFragmentActivity activity = activityWeakReference.get();
            if (activity != null) {
                Location location = result.getLastLocation();
                if (location == null) {
                    return;
                }
                // Create a Toast which displays the new location's coordinates
                La = result.getLastLocation().getLatitude();
                Lo = result.getLastLocation().getLongitude();

                // Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                }
            }
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can not be captured
         *
         * @param exception the exception message
         */
        @Override
        public void onFailure(@NonNull Exception exception) {
            Log.e("LocationChangeActivity", exception.getLocalizedMessage());
            MapFragmentActivity activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getRoute_walking(Point origin, Point destination) {
        Log.e(TAG, "getRoute ??????");
        client = MapboxDirections.builder()
                .origin(origin)//????????? ?????? ??????
                .destination(destination)//????????? ?????? ??????
                .overview(DirectionsCriteria.OVERVIEW_FULL)//?????? ???????????? ??????
                .profile(DirectionsCriteria.PROFILE_WALKING)//????????? ??????(??????,?????????,?????????)
                .accessToken(getString(R.string.access_token))
                .build();

        client.enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                Log.e(TAG, "onResponse ??????");
                System.out.println(call.request().url().toString());
                // You can get the generic HTTP info about the response
                Log.e(TAG, "Response code: " + response.code());
                if (response.body() == null) {
                    Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                    return;
                } else if (response.body().routes().size() < 1) {
                    Log.e(TAG, "No routes found");
                    return;
                }
                // Print some info about the route
                currentRoute = response.body().routes().get(0);
                Log.e(TAG, "Distance: " + currentRoute.distance());

                int time = (int) (currentRoute.duration() / 60);
                //?????? ????????????????????? ?????????
                double distants = (currentRoute.distance() / 1000);
                //?????????????????? ????????? m??? ?????????

                distants = Math.round(distants * 100) / 100.0;
                //Math.round() ????????? ????????? ?????????????????? ??????????????? ????????? ?????????
                //?????? ?????? 100????????? round ?????? ??? ?????? 100?????? ????????? -> ?????????????????? ??????

                Toast.makeText(getApplicationContext(), String.format("?????? ?????? : " + String.valueOf(time) + " ??? \n" +
                        "????????? ?????? : " + distants + " km"), Toast.LENGTH_LONG).show();
                // Draw the route on the map
                // drawRoute(currentRoute);
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Log.e(TAG, "Error: " + throwable.getMessage());
                Toast.makeText(MapFragmentActivity.this, "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getRoute_CYCLING(Point origin, Point destination) {
        Log.e(TAG, "getRoute ??????");
        client = MapboxDirections.builder()
                .origin(origin)//????????? ?????? ??????
                .destination(destination)//????????? ?????? ??????
                .overview(DirectionsCriteria.OVERVIEW_FULL)//?????? ???????????? ??????
                .profile(DirectionsCriteria.PROFILE_CYCLING)//????????? ??????(??????,?????????,?????????)
                .accessToken(getString(R.string.access_token))
                .build();

        client.enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                Log.e(TAG, "onResponse ??????");
                System.out.println(call.request().url().toString());
                // You can get the generic HTTP info about the response
                Log.e(TAG, "Response code: " + response.code());
                if (response.body() == null) {
                    Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                    return;
                } else if (response.body().routes().size() < 1) {
                    Log.e(TAG, "No routes found");
                    return;
                }
                // Print some info about the route
                currentRoute = response.body().routes().get(0);
                Log.e(TAG, "Distance: " + currentRoute.distance());

                int time = (int) (currentRoute.duration() / 60);
                //?????? ????????????????????? ?????????
                double distants = (currentRoute.distance() / 1000);
                //?????????????????? ????????? m??? ?????????
                distants = Math.round(distants * 100) / 100.0;

                //Math.round() ????????? ????????? ?????????????????? ??????????????? ????????? ?????????
                //?????? ?????? 100????????? round ?????? ??? ?????? 100?????? ????????? -> ?????????????????? ??????
                Toast.makeText(getApplicationContext(), String.format("?????? ?????? : " + String.valueOf(time) + " ??? \n" +
                        "????????? ?????? : " + distants + " km"), Toast.LENGTH_LONG).show();
                // Draw the route on the map
                // drawRoute(currentRoute);
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Log.e(TAG, "Error: " + throwable.getMessage());
                Toast.makeText(MapFragmentActivity.this, "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void getRoute_DRIVING(Point origin, Point destination) {
        Log.e(TAG, "getRoute ??????");
        client = MapboxDirections.builder()
                .origin(origin)//????????? ?????? ??????
                .destination(destination)//????????? ?????? ??????
                .overview(DirectionsCriteria.OVERVIEW_FULL)//?????? ???????????? ??????
                .profile(DirectionsCriteria.PROFILE_DRIVING)//????????? ??????(??????,?????????,?????????)
                .accessToken(getString(R.string.access_token))
                .build();

        client.enqueueCall(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                Log.e(TAG, "onResponse ??????");
                System.out.println(call.request().url().toString());
                // You can get the generic HTTP info about the response
                Log.e(TAG, "Response code: " + response.code());
                if (response.body() == null) {
                    Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                    return;
                } else if (response.body().routes().size() < 1) {
                    Log.e(TAG, "No routes found");
                    return;
                }
                // Print some info about the route
                currentRoute = response.body().routes().get(0);
                Log.e(TAG, "Distance: " + currentRoute.distance());

                int time = (int) (currentRoute.duration() / 60);
                //?????? ????????????????????? ?????????
                double distants = (currentRoute.distance() / 1000);
                //?????????????????? ????????? m??? ?????????
                distants = Math.round(distants * 100) / 100.0;
                //Math.round() ????????? ????????? ?????????????????? ??????????????? ????????? ?????????
                //?????? ?????? 100????????? round ?????? ??? ?????? 100?????? ????????? -> ?????????????????? ??????
                Toast.makeText(getApplicationContext(), String.format("?????? ?????? : " + String.valueOf(time) + " ??? \n" +
                        "????????? ?????? : " + distants + " km"), Toast.LENGTH_LONG).show();
                // Draw the route on the map
                //drawRoute(currentRoute);
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                Log.e(TAG, "Error: " + throwable.getMessage());
                Toast.makeText(MapFragmentActivity.this, "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawRoute(DirectionsRoute route) { //??????????????? ????????? ???????????? ???????????? ??? ??????
        Log.e(TAG, "drawRoute ??????");
        // Convert LineString coordinates into LatLng[]
        LineString lineString = LineString.fromPolyline(route.geometry(), PRECISION_6);
        List<Point> coordinates = lineString.coordinates();
        LatLng[] points = new LatLng[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            points[i] = new LatLng(
                    coordinates.get(i).latitude(),
                    coordinates.get(i).longitude());
            // Log.e(TAG, "Error: " + points[i]);
        }
        // Draw Points on MapView
//        mapboxMap.clear();
//      mapboxMap.addPolyline(new PolylineOptions().add(points).color(Color.parseColor("#3bb2d0")).width(5));
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        Log.e(Tag, "onMapReady");
        this.mapboxMap = mapboxMap;
        mapboxMap.addOnMapClickListener(this);//??? ?????? ????????? ??????
        //??? ?????? ?????? ????????? ??????
        mapboxMap.setStyle(getString(R.string.navigation_guidance_day), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
            }
        });
    }

    /**
     * Initialize the Maps SDK's LocationComponent
     */
    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        Log.e(TAG, "enableLocationComponent ??????");
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Set the LocationComponent activation options
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .useDefaultLocationEngine(false)
                            .build();

            // Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(locationComponentActivationOptions);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);

            initLocationEngine();

        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        Log.e(TAG, "initLocationEngine ??????");
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);
        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();
        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }


    public void map_search(View view) {
        Log.e(TAG, "map_search ??????");
        showDialog2(view);
    }

    // ????????? ???????????? ?????? ????????? ?????? ????????? ???????????? ??????
    public void getPointFromGeoCoder(String destinationxy) {
        Log.e(TAG, "???????????? ??????");
        Geocoder geocoder = new Geocoder(this);
        List<Address> listAddress = null;
        try {
            listAddress = geocoder.getFromLocationName(destinationxy, 1);
            destinationX = listAddress.get(0).getLongitude();
            destinationY = listAddress.get(0).getLatitude();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent leaks
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates(callback);
        }
        mapView.onDestroy();
    }

    @Override
    //?????? ????????? ?????? ?????????
    public boolean onMapClick(@NonNull LatLng point) {
        if (destinationMarker != null) {
            mapboxMap.removeMarker(destinationMarker);
        }
        destinationMarker = mapboxMap.addMarker(new MarkerOptions().position(point));//?????? ??????
        destinatonPosition = Point.fromLngLat(point.getLongitude(), point.getLatitude());//??????????????? ??????
        originPosition = Point.fromLngLat(Lo, La);//?????? ??????
        getRoute_walking(originPosition, destinatonPosition);   //?????? ?????????
        getRoute_navi_walking(originPosition, destinatonPosition);//?????? ???????????????
        startButton.setEnabled(true);   //??????????????? ?????? ?????????
        return false;
    }

    private void getRoute_navi_walking(Point origin, Point destinaton) {
        NavigationRoute.builder(this).accessToken(Mapbox.getAccessToken())
                .profile(DirectionsCriteria.PROFILE_WALKING)//?????? ?????????
                .origin(origin)//?????????
                .destination(destinaton).//?????????
                build().
                getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.body() == null) {
                            return;
                        } else if (response.body().routes().size() == 0) {
                            return;
                        }
                        currentRoute = response.body().routes().get(0);
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                    }
                });
    }

}


