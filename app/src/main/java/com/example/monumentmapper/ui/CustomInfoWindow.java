package com.example.monumentmapper.ui;

import android.content.Intent;
import android.view.View;
import android.widget.Button;

import com.example.monumentmapper.R;
import com.example.monumentmapper.ui.camera.CameraActivity;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

/**
 * Extend MarkerInfoWindow to look nicer.
 *
 * Idea from: https://stackoverflow.com/a/66195184
 */
public class CustomInfoWindow extends MarkerInfoWindow {

    private final Button cameraButton;

    /**
     * Construct a custom Info Window (larger, centred image + camera button).
     *
     * @param mapView the map the info window's marker is on
     */
    public CustomInfoWindow(MapView mapView) {

        super(R.layout.bonuspack_bubble_custom, mapView);
        this.cameraButton = (Button) mView.findViewById(R.id.bubble_camera_button);

        // Add onClickListener to camera button
        // How to do so from: https://stackoverflow.com/a/41389737
        this.cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), CameraActivity.class);
                view.getContext().startActivity(intent);
            }
        });
    }
//
//    private void onClickCameraButton() {
//
//        Intent intent = new Intent(mView.getContext(), CameraActivity.class);
//        mView.getContext().startActivity(intent);
//
//    }

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

}
