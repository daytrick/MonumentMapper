package com.example.monumentmapper

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.monumentmapper.databinding.ActivityMainBinding
import com.example.monumentmapper.ui.Querier
import com.github.pengrad.mapscaleview.MapScaleView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.hp.hpl.jena.query.ResultSet
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.collections.Map as Map1


class MainActivity : AppCompatActivity(), MapListener, LocationListener {

    // For nav sidebar
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    // For map view
    // From: https://medium.com/@mr.appbuilder/how-to-integrate-and-work-with-open-street-map-osm-in-an-android-app-kotlin-564b38590bfe
    lateinit var myMap: MapView
    lateinit var controller: IMapController
    lateinit var myLocationOverlay: MyLocationNewOverlay
    // From: https://medium.com/@hasperong/get-current-location-with-latitude-and-longtitude-using-kotlin-2ef6c94c7b76
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2
    private val defaultZoomLevel = 14.0
    private var currentZoomLevel = defaultZoomLevel

    // For scale
    lateinit var scaleView: MapScaleView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // FOR NAV BAR
        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // FOR MAP VIEW
        // @me check what this is for
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE)
        )
        myMap = binding.osmmap.osmmap
        myMap.setTileSource(TileSourceFactory.MAPNIK)
        myMap.mapCenter
        myMap.setMultiTouchControls(true)
        myMap.getLocalVisibleRect(Rect())
        scaleView = findViewById(R.id.scaleView)

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), myMap)
        controller = myMap.controller

        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
        myLocationOverlay.isDrawAccuracyEnabled = true

        controller.setZoom(defaultZoomLevel)
        Log.i("LOC", "onCreate:in ${controller.zoomIn()}")
        Log.i("LOC", "onCreate: out  ${controller.zoomOut()}")

        myLocationOverlay.runOnFirstFix {
            runOnUiThread {

                // FOR LOCATION
                getLocation()
                controller.setCenter(myLocationOverlay.myLocation)
                controller.animateTo(myLocationOverlay.myLocation)
                Log.i("LOC", "My location: ${myLocationOverlay.myLocation}")

                updateScale()

            }
        }
        myMap.overlays.add(myLocationOverlay)
        myMap.addMapListener(this)


        Log.i("MAR", "Called the Querier!")
        Querier.init(myMap);
        Querier.getLocalMonuments()
        Log.i("MAR", "Finished calling the Querier!")

//        Log.i("MAR", "Trying to add a point!");
//        addMarker(GeoPoint(56.3405, -2.81741))

    }


    /**
     * Add a marker to the map using OSMBonus.
     *
     * How to do so from: https://stackoverflow.com/a/55707403
     */
//    private fun addMarker(point: GeoPoint) {
//
//        var marker: Marker = Marker(myMap);
//        marker.position = point;
//        marker.title = "Test";
//        marker.snippet = "Description text testing"
//        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//        myMap.overlays.add(marker)
//        Log.i("MAR", "Added a point!")
//
//    }



//    public fun addMarker(monumentData: Map1<String, Object>) {
//
//        // Set up marker
//        var marker: Marker = Marker(myMap);
//
//        // Put the data in
//        try {
//            val latitude: Double = monumentData["lat"].toString().toDouble();
//            val longitude: Double = monumentData["long"].toString().toDouble();
//            marker.position = GeoPoint(latitude, longitude);
//            marker.title = monumentData.get("name").toString();
//            //marker.snippet = "Description text testing"
//            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//            myMap.overlays.add(marker)
//            Log.i("MAR", "Added a point!")
//        }
//        catch (e: Exception) {
//            Log.i("MAR", e.message.toString());
//        }
//
//    }





    /**
     * Resize the map scale to be accurate for the current location and zoom level.
     *
     * How to load scale from: https://github.com/pengrad/MapScaleView
     *                    and: https://stackoverflow.com/a/43537667
     */
    private fun updateScale() {

        myLocationOverlay.runOnFirstFix {
            runOnUiThread {
                scaleView.update(currentZoomLevel.toFloat(), myLocationOverlay.myLocation.latitude)
            }
        }

    }


    /**
     * Request a location update from the location manager.
     *
     * Based on code from: https://medium.com/@hasperong/get-current-location-with-latitude-and-longtitude-using-kotlin-2ef6c94c7b76
     */
    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        }
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                android.Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                this,
//                android.Manifest.permission.ACCESS_COARSE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return
//        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
        Log.i("LOC", "Got location!")
    }

    /**
     * Copied from: https://medium.com/@hasperong/get-current-location-with-latitude-and-longtitude-using-kotlin-2ef6c94c7b76
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onScroll(event: ScrollEvent?): Boolean {
        // Copied from: https://medium.com/@mr.appbuilder/how-to-integrate-and-work-with-open-street-map-osm-in-an-android-app-kotlin-564b38590bfe
        Log.i("LOC", "onCreate:la ${event?.source?.mapCenter?.latitude}")
        Log.i("LOC", "onCreate:lo ${event?.source?.mapCenter?.longitude}")
        updateScale()
        return true
    }

    override fun onZoom(event: ZoomEvent?): Boolean {
        // Copied from: https://medium.com/@mr.appbuilder/how-to-integrate-and-work-with-open-street-map-osm-in-an-android-app-kotlin-564b38590bfe
        Log.i("LOC", "onZoom zoom level: ${event?.zoomLevel}   source:  ${event?.source}")
        currentZoomLevel = event?.zoomLevel!!
        updateScale()
        return false
    }

    override fun onLocationChanged(p0: Location) {
        controller.setCenter(GeoPoint(p0.latitude, p0.longitude))

    }

    override fun onPause() {
        super.onPause()

        // Turning off location tracking when not using the app
        myLocationOverlay.disableMyLocation()
        myLocationOverlay.disableFollowLocation()

    }

}