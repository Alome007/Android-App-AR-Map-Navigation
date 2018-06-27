package com.arjo129.artest;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.Toast;

import com.arjo129.artest.device.WifiLocation;
//import com.arjo129.artest.places.Routing;
import com.arjo129.artest.places.Routing;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.turf.TurfJoins;
import com.mapbox.android.core.location.LocationEngine;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static com.mapbox.mapboxsdk.style.expressions.Expression.exponential;
import static com.mapbox.mapboxsdk.style.expressions.Expression.interpolate;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.mapbox.mapboxsdk.style.expressions.Expression.zoom;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

public class MapActivity extends AppCompatActivity implements LocationEngineListener,
        OnMapReadyCallback, PermissionsListener{
    private MapView mapView;
    private List<Point> boundingBox;
    private GeoJsonSource indoorBuildingSource;
    private List<List<Point>> boundingBoxList;
    private Icon green_icon;
    private List<Marker> routeDrawn;

    private MapboxMap map;
    private View levelButtons;

    private LocationLayerPlugin locationLayerPlugin;
    private LocationEngine locationEngine;
    private PermissionsManager permissionsManager;
    private Location originLocation;

    private Routing mapRouting;


    private Marker startMarker, destinationMarker;
    private LatLng startCoord, destinationCoord;
    private int floor = -1; //Keep track of the floor
    private String TAG = "MapActivity"; // Used for log.d

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        setLevelButtons();

        mapRouting = new Routing(this);

        Button route_button = findViewById(R.id.start_route_buttton);
        route_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Log.d("MapActivity", "Clicked route button");
                if(startMarker != null){
                    map.removeMarker(startMarker);
                }

                // Real starting position:

                // Remember to enable the location plugin!!
//                locationLayerPlugin.setLocationLayerEnabled(false);
//                startCoord = new LatLng(originLocation.getLatitude(), originLocation.getLongitude());


//                green_icon = IconFactory.getInstance(MapActivity.this).fromResource(R.drawable.green_marker);

                // Mock starting position:
                startCoord = new LatLng(1.295252,103.7737);

                startMarker = map.addMarker(new MarkerOptions()
                        .position(startCoord)
//                        .icon(green_icon)
                );

                // To check for out of bound markers
//                if(destinationCoord != null && !mapRouting.withinPolygon(destinationCoord)){
//                    Toast.makeText(MapActivity.this, "Out of COM1!", Toast.LENGTH_SHORT).show();
//                    return;
//                }

                // drawing route on map
                if(destinationMarker != null){
                    if(routeDrawn!= null && !routeDrawn.isEmpty()){
                        // erase route if has been drawn
                        for(Marker marker: routeDrawn){
                            map.removeMarker(marker);
                        }
                    }
                    routeDrawn = new ArrayList<>();
                    List<Node> drawNodes = mapRouting.getRoute(startCoord, destinationCoord);
                    drawRoute(drawNodes);
//                    route_button.setEnabled(false);
                }
                else{
                    Log.d("MapActivity", "no route to plot");
                }

            }
        });
    }

    private void drawRoute(List<Node> waypoints){
        if(waypoints == null || waypoints.size() <= 0)return;
        map.removeMarker(startMarker);

        Icon blue_icon = IconFactory.getInstance(MapActivity.this).fromResource(R.drawable.blue_marker);
        for(int i=0; i<waypoints.size();i++){
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(waypoints.get(i).coordinate)
                    .setTitle(String.valueOf(i)+" || "+waypoints.get(i).bearing)
//                    .icon(blue_icon)
            );
            routeDrawn.add(marker);
        }
    }

    private void setLevelButtons(){
        Button buttonZeroLevel = findViewById(R.id.zero_level_button);
        Button buttonFirstLevel = findViewById(R.id.first_level_button);
        Button buttonSecondLevel = findViewById(R.id.second_level_button);
        buttonSecondLevel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                floor = 2;
                initializeNewLevel(floor);
//                buttonSecondLevel.setBackgroundColor(Color.GREEN);
//                buttonFirstLevel.set
            }
        });
        buttonFirstLevel.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                floor = 1;
                initializeNewLevel(floor);
            }
        });
        buttonZeroLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                floor = 0;
                initializeNewLevel(floor);
            }
        });
    }

    private void hideLevelButton(){
        AlphaAnimation animation = new AlphaAnimation(1.0f,0.0f);
        animation.setDuration(500); // millisecs
        levelButtons.startAnimation(animation);
        levelButtons.setVisibility(View.GONE);
    }
    private void showLevelButton(){
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        levelButtons.startAnimation(animation);
        levelButtons.setVisibility(View.VISIBLE);
    }

    private void initializeNewLevel(int level){
        String filename = "com1floor"+String.valueOf(level)+".geojson";
        indoorBuildingSource.setGeoJson(loadJsonFromAsset(filename));
//        map.removeAnnotations();
//        featureCollection = null;
//        try {
//            getFeatureCollectionFromJson(level);
//        }catch (Exception e){
//            Log.e("MapActivity","onCreate: "+e);
//        }
//        List<Feature> featureList = featureCollection.features();
//
//
//        Log.d("Mapactivity","Building features list");
//        for(int i=0; i<featureList.size(); i++){
//            Feature singleLocation = featureList.get(i);
//            if( singleLocation.hasProperty("name")){
//                String name = singleLocation.getStringProperty("name");
//                Double stringLng = ((Point)singleLocation.geometry()).coordinates().get(0);
//                Double stringLat = ((Point)singleLocation.geometry()).coordinates().get(1);
////                Log.d("MapActivity", "feature: " +name);
//                LatLng locationLatLng = new LatLng(stringLat, stringLng);
//
//                map.addMarker(new MarkerOptions()
//                        .position(locationLatLng)
//                        .title(name));
//            }
//        }
    }

    private void loadBuildingLayer(){
        FillLayer indoorBuildingLayer = new FillLayer("indoor-building-fill","indoor-building").withProperties(
                fillColor(Color.parseColor("#eeeeee")), fillOpacity(interpolate(exponential(1f),zoom(),
                        stop(17f, 1f),
                        stop(16.5f, 0.5f),
                        stop(16f,0f))));

        map.addLayer(indoorBuildingLayer);
        Log.d("MainActtttivity", "main layer built");
        LineLayer indoorBuildingLineLayer = new LineLayer("indoor-building-line","indoor-building")
                .withProperties(lineColor(Color.parseColor("#50667f")),
                        lineWidth(0.5f),
                        lineOpacity(interpolate(exponential(1f), zoom(),
                                stop(17f,1f),
                                stop(16.5f, 0.5f),
                                stop(16f,0f))));
        map.addLayer(indoorBuildingLineLayer);
        Log.d("MainActtttivity", "line layer built");
    }
    private String loadJsonFromAsset(String filename){
        try{
            Log.d("LoadJson", "loading....");
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            Log.d("LOadJson", filename);
            return new String(buffer, "UTF-8");
        } catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        map = mapboxMap;
        map.addOnMapClickListener(new MapboxMap.OnMapClickListener(){

            @Override
            public void onMapClick(@NonNull LatLng point) {

                if(destinationMarker != null){
                    mapboxMap.removeMarker(destinationMarker);
                }
                destinationCoord = point;
                destinationMarker = mapboxMap.addMarker(new MarkerOptions()
                        .position(destinationCoord)
                        .setTitle(point.toString())
                );


                /*if(mapRouting.withinPolygon(point)){
                    Toast.makeText(MapActivity.this, "Inside Polygon", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MapActivity.this, "Outside Polygon", Toast.LENGTH_SHORT).show();
                }*/

                // TODO: Launch the polyline to go
            }
        });


        levelButtons = findViewById(R.id.floor_level_buttons);
        boundingBox = new ArrayList<>();
        boundingBox.add(Point.fromLngLat(103.775,1.2925)); // 1.295, 103.774
        boundingBox.add(Point.fromLngLat(103.775,1.2969));
        boundingBox.add(Point.fromLngLat(103.773,1.2969));
        boundingBox.add(Point.fromLngLat(103.773,1.2925));
        boundingBoxList = new ArrayList<>();
        boundingBoxList.add(boundingBox);

        mapboxMap.addOnCameraMoveListener(new MapboxMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {

                if(mapboxMap.getCameraPosition().zoom > 16){
                    if(TurfJoins.inside(Point.fromLngLat(mapboxMap.getCameraPosition().target.getLongitude(),
                            mapboxMap.getCameraPosition().target.getLatitude()), Polygon.fromLngLats(boundingBoxList))){

                        if(levelButtons.getVisibility()!=View.VISIBLE){
                            showLevelButton();
                        }
                    } else{
                        if(levelButtons.getVisibility() ==View.VISIBLE){
                            Log.d("CameraMove", "Outside the polygon");
                            hideLevelButton();
                        }
                    }
                } else if (levelButtons.getVisibility() == View.VISIBLE){
                    Log.d("CameraMove", "Too far");
                    hideLevelButton();
                }
            }
        });

        // TODO: Enable location but not animate camera sometimes
//        enableLocationPlugin();

        Intent before = getIntent();
        if(before.hasExtra("lat") && before.hasExtra("lng") && before.hasExtra("place_name") && before.hasExtra("level")){
            double lat = before.getDoubleExtra("lat", 0);
            double lng = before.getDoubleExtra("lng", 0);
            String place_name = before.getStringExtra("place_name");
            int level = before.getIntExtra("level",0);

            // Load map layout for that level
            indoorBuildingSource = new GeoJsonSource("indoor-building", loadJsonFromAsset("com1floor"+level+".geojson"));
            mapboxMap.addSource(indoorBuildingSource);
            loadBuildingLayer();


            // Place destination marker
            if(destinationMarker != null){
                mapboxMap.removeMarker(destinationMarker);
            }
            destinationCoord = new LatLng(lat,lng);
            destinationMarker = mapboxMap.addMarker(new MarkerOptions()
                    .position(destinationCoord)
                    .setTitle(place_name)
            );
            // TODO: Launch the polyline to go

            return;
        } else{
            indoorBuildingSource = new GeoJsonSource("indoor-building", loadJsonFromAsset("com1floor1.geojson"));
            mapboxMap.addSource(indoorBuildingSource);
            loadBuildingLayer();
        }
    }

    public void enableLocationPlugin(){
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            initializeLocationEngine();
            initializeLocationLayer();
            getLifecycle().addObserver(locationLayerPlugin);
        }
        else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }


    private void initializeLocationEngine(){
        locationEngine = new DBLocationEngine(this);
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();
        Location lastlocation = locationEngine.getLastLocation();
        if(lastlocation!=null){
            originLocation = lastlocation;
//            setCameraPosition(lastlocation);
        }
        locationEngine.addLocationEngineListener(this);
    }

    private void initializeLocationLayer(){
        locationLayerPlugin = new LocationLayerPlugin(mapView, map,locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.COMPASS);
        getLifecycle().addObserver(locationLayerPlugin);
        Log.d(TAG, "intialized location layer");
    }

    private void setCameraPosition(Location location){
        Log.d("Cam position", String.valueOf(location.getLatitude())+", "+String.valueOf(location.getLongitude()));
        panningTo(location.getLatitude(), location.getLongitude());
    }

    private void panningTo(double lat, double lng){
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(lat, lng), 16));
    }


    @Override
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location!= null){
            originLocation = location;
            setCameraPosition(location);
            int floor = (int)location.getAltitude();
            initializeNewLevel(floor);
            //locationEngine.removeLocationEngineListener(this);
        }
    }


    public void onStart() {
        super.onStart();
        if(locationEngine != null){
            locationEngine.requestLocationUpdates();
        }
        if(locationLayerPlugin != null){
            locationLayerPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(locationEngine != null){
            locationEngine.removeLocationUpdates();
        }
        if(locationLayerPlugin != null){
            locationLayerPlugin.onStop();
        }
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
        mapView.onDestroy();
        if(locationEngine!= null){
            locationEngine.deactivate();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        // User denies the first time, this is the 2nd time permission is presented
        // Present toast or dialog to explain why permission is needed
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if(granted) enableLocationPlugin();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
