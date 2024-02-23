package com.example.monumentmapper.ui;

import com.example.monumentmapper.R;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

/**
 * Extend MarkerInfoWindow to look nicer.
 *
 * Idea from: https://stackoverflow.com/a/66195184
 */
public class CustomInfoWindow extends MarkerInfoWindow {

    /**
     * @param mapView
     */
    public CustomInfoWindow(MapView mapView) {
        super(R.layout.bonuspack_bubble_custom, mapView);
    }

}
