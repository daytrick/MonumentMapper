package com.example.monumentmapper

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import com.example.monumentmapper.databinding.ActivityMainBinding
import com.example.monumentmapper.ui.Querier
import com.example.monumentmapper.ui.login.LoginActivity
import com.github.pengrad.mapscaleview.MapScaleView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import org.osmdroid.api.IMapController
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


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
    private val LOCATION_PERMISSION_CODE = 2
    private val DEFAULT_ZOOM_LEVEL = 14.0
    private var currentZoomLevel = DEFAULT_ZOOM_LEVEL
    private val MIN_ZOOM_LEVEL = 3.0
    private val MAX_ZOOM_LEVEL = 20.0

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
            Log.i("MENU_CLICK", "onClickListener activated")
        }

        binding.appBarMain.toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener() {
            Log.i("MENU_CLICK", "In the binding")
            menuClickListener(it)
        })

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow, R.id.nav_account
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

        // Limit zoom levels so can't have weird parallel world tiles
        // Zoom level limit names from: https://code.google.com/archive/p/osmdroid/issues/418
        myMap.maxZoomLevel = MAX_ZOOM_LEVEL
        myMap.minZoomLevel = MIN_ZOOM_LEVEL
        myMap.setScrollableAreaLimitLatitude(
            MapView.getTileSystem().maxLatitude,
            MapView.getTileSystem().minLatitude,
            0)

        // Centre map view
        myMap.mapCenter
        myMap.setMultiTouchControls(true)
        myMap.getLocalVisibleRect(Rect())
        scaleView = findViewById(R.id.scaleView)

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), myMap)
        controller = myMap.controller

        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()
        myLocationOverlay.isDrawAccuracyEnabled = true

        controller.setZoom(DEFAULT_ZOOM_LEVEL)
        Log.i("LOC", "onCreate:in ${controller.zoomIn()}")
        Log.i("LOC", "onCreate: out  ${controller.zoomOut()}")

        try {
            val photoful = ResourcesCompat.getDrawable(resources, R.drawable.marker_photoful, null);
            val photoless = ResourcesCompat.getDrawable(resources, R.drawable.marker_photoless, null);
//            val photoful = SVG.getFromResource(resources, R.drawable.marker_photoful)
//            val photoless = SVG.getFromResource(resources, R.drawable.marker_photoless)
            Querier.setMarkerIcons(photoful, photoless)
        }
        catch (e: SVGParseException) {
            Log.i("ICON", "Couldn't parse SVGs: " + e.toString())
        }

        Querier.init(myMap)

        myLocationOverlay.runOnFirstFix {
            runOnUiThread {

                // FOR LOCATION
                getLocation()
                controller.setCenter(myLocationOverlay.myLocation)
                controller.animateTo(myLocationOverlay.myLocation)
                Log.i("LOC", "My location: ${myLocationOverlay.myLocation}")

                updateScale()

                Log.i("POINT", myLocationOverlay.myLocation.toString())
                Querier.getLocalMonuments(myLocationOverlay.myLocation.longitude, myLocationOverlay.myLocation.latitude)

            }
        }
        myMap.overlays.add(myLocationOverlay)
        myMap.addMapListener(this)

    }



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
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
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
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.i("MENU_CREATE", "Created options menu!")
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)

        return super.onCreateOptionsMenu(menu)
    }

    /**
     * For selecting a menu item in the nav bar.
     *
     * Documentation: https://developer.android.com/develop/ui/views/components/appbar/actions#handle-actions
     */
    fun menuClickListener(item: MenuItem): Boolean {

        Log.i("MENU_CLICK", "Detected a click!")

        if (item.itemId == R.id.nav_account) {
            // Show login page
            val intent = Intent(this, LoginActivity::class.java)
            Log.i("MENU_CLICK", "Showing login page!")
            startActivity(intent);
            return true;
        }
        else {
            Log.i("MENU_CLICK", "Something else selected!")
            super.onOptionsItemSelected(item)
            return false;
        }

    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        // Clicked on account
        R.id.nav_account -> {
            // Show login page
            val intent = Intent(this, LoginActivity::class.java)
            Log.i("MENU_CLICK", "Showing login page!")
            startActivity(intent)
            true;
        }

        // DK
        else -> {
            Log.i("MENU_CLICK", "Something else selected!")
            super.onOptionsItemSelected(item)
        }

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

    override fun onResume() {

        super.onResume()

        // Re-enable location tracking
        myLocationOverlay.enableMyLocation()
        myLocationOverlay.enableFollowLocation()

    }

}