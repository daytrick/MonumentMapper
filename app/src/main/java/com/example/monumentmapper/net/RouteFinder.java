package com.example.monumentmapper.net;

import android.os.AsyncTask;
import android.widget.Toast;

import com.example.monumentmapper.ui.CustomInfoWindow;

import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;

/**
 * Helper for finding routes and displaying them.
 *
 * Helpful: https://github.com/MKergall/osmbonuspack/wiki/Tutorial_1
 */
public class RouteFinder {

    private static MapView mapView;
    private static RoadManager rm;

    private static ArrayList<Marker> stops;
    private static ArrayList<GeoPoint> waypoints;
    private static GeoPoint currentLocation;
    private static Polyline roadOverlay = null;

    /**
     * Set the map view that the RouteFinder will use.
     * @param mapView the map view
     */
    public static void init(MapView mapView) {

        RouteFinder.mapView = mapView;

        // User agent is basically who's using the OSMDroid server: https://stackoverflow.com/a/48841423
        rm = new OSRMRoadManager(mapView.getContext(), "Monument Mapper");

        // Want walking routes
        ((OSRMRoadManager) rm).setMean(OSRMRoadManager.MEAN_BY_FOOT);

        // Initialise with no stops
        stops = new ArrayList<>();
        waypoints = new ArrayList<>();

    }

    public static void updateLoc(GeoPoint currLoc) {
        currentLocation = currLoc;
    }


    /**
     * Add a stop to the route.
     * @param marker marker for the stop
     */
    public static void addStop(Marker marker) {
        stops.add(marker);
        waypoints.add(marker.getPosition());
        getRoute();
    }

    /**
     * Remove a waypoint from the route.
     * @param marker marker for the stop
     */
    public static void removeStop(Marker marker) {
        stops.remove(marker);
        waypoints.remove(marker.getPosition());
        getRoute();
    }


    /**
     * Clear the route.
     */
    public static void clearRoute() {

        // Inform user
        Toast.makeText(mapView.getContext(), "Clearing route", Toast.LENGTH_SHORT).show();

        // Change markers back
        for (Marker marker : stops) {
            ((CustomInfoWindow) marker.getInfoWindow()).resetMarker();
        }

        // Clear stops
        stops.clear();
        waypoints.clear();

        // Clear route
        mapView.getOverlays().remove(roadOverlay);
        roadOverlay = null;

    }

    /**
     * Work out the route.
     */
    private static void getRoute() {

        // Remove old route, if need to
        if (roadOverlay != null) {
            mapView.getOverlays().remove(roadOverlay);
        }

        // Check if need to work out a route
        if (!waypoints.isEmpty()) {

            // Get the route async
            AsyncTask<Object, Void, Road> task = new UpdateRoadTask();
            task.execute(waypoints);

        }

    }


    /**
     * AsyncTask to do route-finding (which uses the network) not on the main thread.
     *
     * How to do so from: https://stackoverflow.com/q/76203045
     */
    private static class UpdateRoadTask extends AsyncTask<Object, Void, Road> {

        /**
         * @param objects
         * @deprecated
         */
        @Override
        protected Road doInBackground(Object... objects) {
            ArrayList<GeoPoint> stops = (ArrayList<GeoPoint>) waypoints.clone();
            stops.add(0, currentLocation);
            return rm.getRoad(stops);
        }

        protected void onPostExecute(Road result) {

            // Check whether there's a route
            if (result.mStatus != Road.STATUS_OK) {
                Toast.makeText(
                        mapView.getContext(),
                        "Could not generate route.",
                        Toast.LENGTH_SHORT)
                    .show();
            }

            // Build a Polyline with the road (so it can be displayed)
            roadOverlay = RoadManager.buildRoadOverlay(result);

            // Add the Polyline to the map
            mapView.getOverlays().add(roadOverlay);

            // Refresh the map
            mapView.invalidate();

        }
    }

}
