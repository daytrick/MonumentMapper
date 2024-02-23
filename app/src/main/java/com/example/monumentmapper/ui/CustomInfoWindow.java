package com.example.monumentmapper.ui;

import com.example.monumentmapper.R;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

public class CustomInfoWindow extends MarkerInfoWindow {

    /**
     * @param mapView
     */
    public CustomInfoWindow(MapView mapView) {
        super(R.layout.bonuspack_bubble_custom, mapView);
    }

}
