package com.example.monumentmapper.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import com.example.monumentmapper.R;
import com.example.monumentmapper.net.RouteFinder;
import com.example.monumentmapper.ui.camera.CameraActivity;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

/**
 * Extend MarkerInfoWindow to look nicer.
 *
 * Idea from: <a href="https://stackoverflow.com/a/66195184">https://stackoverflow.com/a/66195184</a>
 */
public class CustomInfoWindow extends MarkerInfoWindow {

    public static final String NAME_KEY = "nameKey";
    private final Marker marker;
    private final Button cameraButton;
    private final Button addStopButton;
    private final Button removeStopButton;

    private Boolean photoful;

    /**
     * Construct a custom Info Window (larger, centred image + camera button).
     *
     * @param mapView the map the info window's marker is on
     */
    public CustomInfoWindow(MapView mapView, String monumentName, Marker marker, Boolean photoful) {

        super(R.layout.bonuspack_bubble_custom, mapView);

        // Save ref to the marker and photo
        this.marker = marker;
        this.photoful = photoful;

        // Camera button
        // How to add onClickListener to camera button from: https://stackoverflow.com/a/41389737
        this.cameraButton = (Button) mView.findViewById(R.id.bubble_camera_button);
        this.cameraButton.setOnClickListener(view -> onClickCameraButton(view, monumentName));

        // Add stop button
        this.addStopButton = (Button) mView.findViewById(R.id.bubble_add_stop_button);
        this.addStopButton.setOnClickListener(view -> onClickAddStopButton());

        // Add stop button
        this.removeStopButton = (Button) mView.findViewById(R.id.bubble_remove_stop_button);
        this.removeStopButton.setOnClickListener(view -> onClickRemoveStopButton());

    }

    /**
     * Check if user really does intend to take a photo of that location,
     * then open the camera up if they do.
     *
     * @param view          the map view
     * @param monumentName  the monument's name
     */
    private void onClickCameraButton(View view, String monumentName) {
        // How to create confirmation dialogue from: https://developer.android.com/develop/ui/views/components/dialogs#java

        // TODO: check that the user is within 100m of that location

        // Build the confirm alert
        AlertDialog.Builder adb = new AlertDialog.Builder(view.getContext());
        adb.setMessage("Are you sure you're taking a photo of " + monumentName + "?");

        // Set confirm action (opening up camera activity)
        adb.setPositiveButton(R.string.yes, (dialogInterface, i) -> {
            Intent intent = new Intent(view.getContext(), CameraActivity.class);
            intent.putExtra(NAME_KEY, monumentName);
            view.getContext().startActivity(intent);
        });

        // Set deny action
        adb.setNegativeButton(R.string.no, (dialogInterface, i) -> {
            // Do nothing?
        });

        // Actually create the confirmation alert
        AlertDialog ad = adb.create();
        ad.show();

    }

    /**
     * Show the camera button.
     */
    public void showCameraButton() {
        cameraButton.setVisibility(View.VISIBLE);
    }

    /**
     * Hide the camera button.
     */
    public void hideCameraButton() {
        cameraButton.setVisibility(View.GONE);
    }


    /**
     * Add the location to the route,
     * hide the add stop button, and show the remove stop button.
     */
    private void onClickAddStopButton() {

        // Add location to route
        RouteFinder.addStop(marker);

        // Toggle to remove stop
        addStopButton.setVisibility(View.GONE);
        removeStopButton.setVisibility(View.VISIBLE);

        // Toggle marker icon
        if (photoful) {
            marker.setIcon(getMapView().getContext().getDrawable(R.drawable.marker_visit_photoful));
        }
        else {
            marker.setIcon(getMapView().getContext().getDrawable(R.drawable.marker_visit_photoless));
        }

    }


    /**
     * Remove the location from the route,
     * show the add stop button, and hide the remove stop button.
     */
    private void onClickRemoveStopButton() {

        // Remove location from route
        RouteFinder.removeStop(marker);

        resetMarker();

    }


    /**
     * Reset the marker so that it isn't marked as a stop.
     * (Does not remove it from RouteFinder list though.)
     */
    public void resetMarker() {

        // Toggle to remove stop
        removeStopButton.setVisibility(View.GONE);
        addStopButton.setVisibility(View.VISIBLE);

        // Toggle marker icon
        if (photoful) {
            marker.setIcon(getMapView().getContext().getDrawable(R.drawable.marker_photoful));
        }
        else {
            marker.setIcon(getMapView().getContext().getDrawable(R.drawable.marker_photoless));
        }

    }

}
