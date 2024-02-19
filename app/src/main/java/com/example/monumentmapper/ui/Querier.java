package com.example.monumentmapper.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.example.monumentmapper.R;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.sparql.core.Prologue;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class Querier {

    /**
     * WikiData SPARQL endpoint URL.
     * From: https://stackoverflow.com/questions/36535702/use-jena-to-query-wikidata
     */
    private static final String WD_ENDPOINT = "https://query.wikidata.org/sparql";
    private static final Prologue WD_PREFIXES = new Prologue();
    private static final Pattern NAME_REGEX =
            Pattern.compile("^(?<name>.+)@[a-z]+$");
    private static final Pattern POINT_REGEX =
            Pattern.compile("Point[(](?<long>-?[0-9]+.[0-9]+) (?<lat>-?[0-9]+.[0-9]+)[)]");
    /**
     * Max wait time in milliseconds for an image-loading thread to complete.
     */
    private static final long WAIT_TIME = 500;

    private static Drawable photoful;
    private static Drawable photoless;
    private static MapView mapView;

    /**
     * Pass the Drawables for marker icons in.
     *
     * Best practice for accessing Resources from a non-activity class from:
     * https://stackoverflow.com/questions/7666589/using-getresources-in-non-activity-class
     *
     * @param photoful      Drawable of the marker for if there is a photo
     * @param photoless     Drawable of the marker for if there isn't a photo
     */
    public static void setMarkerIcons(Drawable photoful, Drawable photoless) {
        Querier.photoful = photoful;
        Querier.photoless = photoless;
    }

    public static void init(MapView mapView) {

        // Define the Wikibase prefixes
        // How to do so from: https://stackoverflow.com/a/32125863
        WD_PREFIXES.setPrefix("wd", "http://www.wikidata.org/entity/");
        WD_PREFIXES.setPrefix("wdt", "http://www.wikidata.org/prop/direct/");
        WD_PREFIXES.setPrefix("p", "http://www.wikidata.org/prop/");
        WD_PREFIXES.setPrefix("pq", "http://www.wikidata.org/prop/qualifier/");
        WD_PREFIXES.setPrefix("ps", "http://www.wikidata.org/prop/statement/");
        WD_PREFIXES.setPrefix("wikibase", "http://wikiba.se/ontology#");
        WD_PREFIXES.setPrefix("bd", "http://www.bigdata.com/rdf#");
        WD_PREFIXES.setPrefix("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
        WD_PREFIXES.setPrefix("wd", "http://www.wikidata.org/entity/");

        // Set the map
        Querier.mapView = mapView;

    }



    /**
     * Query Wikidata for local monuments on a new thread,
     * to avoid the NetworkOnMainThreadException.
     *
     * Thread safety tips from: https://android-developers.googleblog.com/2009/05/painless-threading.html
     * Note to avoid AsyncTask from: https://stackoverflow.com/a/6343299
     */
    public static void getLocalMonuments(double longitude, double latitude) {

        new Thread(new Runnable() {

            @Override
            public void run() {

                Log.i("MAR", "In a new thread!");
                queryLocalMonuments(longitude, latitude);

            }

        }).start();

    }

    /**
     * Query Wikidata for local monuments.
     */
    private static void queryLocalMonuments(double longitude, double latitude) {

        String queryString =
                "SELECT DISTINCT ?building ?buildingLabel ?coords ?image WHERE \n" +
                "  {\n" +
                "    # Accepted buildings from: https://www.wikilovesmonuments.org.uk/eligible-buildings\n" +
                "    # How to do an OR from: https://stackoverflow.com/a/17600298\n" +
                "    VALUES (?status) { (wd:Q10729054) (wd:Q10729125) (wd:Q10729142) }\n" +
                "    ?building wdt:P1435 ?status .\n" +
                "    \n" +
                "    # And get their images if they have them\n" +
                "    OPTIONAL { ?building wdt:P18 ?image } .\n" +
                "\n" +
                "  SERVICE wikibase:around {\n" +
                "    ?building wdt:P625 ?coords .\n" +
                "    # How to pass a specific location in from: https://stackoverflow.com/a/49315478\n" +
                "    bd:serviceParam wikibase:center \"Point(" + longitude + " " + latitude + ")\"^^geo:wktLiteral .\n" +
                "    bd:serviceParam wikibase:radius \"2\" .\n" +
                "  }\n" +
                "\n" +
                "  SERVICE wikibase:label {\n" +
                "    bd:serviceParam wikibase:language \"en\" .\n" +
                "  }\n" +
                "}\n";


        // Make the query, inc. the prefix defs
        // How to make query from: https://stackoverflow.com/questions/36535702/use-jena-to-query-wikidata
        Log.i("MAR", "Going to build query!");
        Query query = QueryFactory.parse(new Query(WD_PREFIXES), queryString, null, null);

//        ParameterizedSparqlString pss = new ParameterizedSparqlString(queryString);
//        pss.setNsPrefixes(WD_PREFIXES.getPrefixMapping());
//        pss.setLiteral("long", longitude);
//        pss.setLiteral("lat", latitude);
//        Log.i("QUERY", pss.toString());
//        Query query = pss.asQuery();

        QueryExecution qExec = QueryExecutionFactory.sparqlService(WD_ENDPOINT, query);
        Log.i("MAR", "Built query!");

        try {
            Log.i("MAR", "Trying to select results!");
            ResultSet results = qExec.execSelect();

            while (results.hasNext()) {

                QuerySolution qs = results.nextSolution();
                Log.i("POINT", qs.get("coords").toString());
                processMonumentQuery(qs);
                //addMarker(monumentData);

            }

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            Log.i("MAR", "Error getting query results: " + ex);
        } finally {
            qExec.close();
        }
    }


    /**
     * Process a single query solution (result)
     * for the query for getting local monuments.
     *
     * @param qs the query solution
     * @return
     */
    private static void processMonumentQuery(QuerySolution qs) {

        // Extract monument name and location
        Matcher nameMatcher = NAME_REGEX.matcher(qs.get("buildingLabel").toString());
        Matcher locMatcher = POINT_REGEX.matcher(qs.get("coords").toString());

        while (nameMatcher.find() && locMatcher.find()) {

            try {

                String name = nameMatcher.group("name");
                double latitude = Double.parseDouble(locMatcher.group("lat"));
                double longitude = Double.parseDouble(locMatcher.group("long"));
                String imageURL = null;
                if (qs.contains("image")) {
                    imageURL = qs.get("image").toString();
                    Log.i("IMAGE", qs.get("image").toString());
                }
                Log.i("IMAGE", imageURL);

                addMarker(name, latitude, longitude, imageURL);

            } catch (NumberFormatException | NullPointerException e) {
                Log.i("POINT", "Could not extract coordinates");
            }

        }
        Log.i("MAR", "Done finding matches!");

    }


    /**
     * Add a marker to the map using OSMBonus.
     *
     * How to do so from: https://stackoverflow.com/a/55707403
     *
     * @param name      name of the monument
     * @param latitude  latitude of the monument
     * @param longitude longitude of the monument
     */
    private static void addMarker(String name, double latitude, double longitude, String imageURL) {

        // Create marker
        Marker marker = new Marker(mapView);

        // Set position and name
        marker.setPosition(new GeoPoint(latitude, longitude));
        marker.setTitle(name);

        // Set marker image and allow image to be loaded upon clicking, if URL is provided
        // How to set marker icon from: https://stackoverflow.com/q/60301641
        if (imageURL != null) {
            marker.setOnMarkerClickListener(loadMonumentImage(imageURL));
            marker.setIcon(photoful);
        } else {
            marker.setIcon(photoless);
        }

        // Actually add the marker
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        Log.i("MAR", "Added a point!");

    }


    /**
     * Custom Marker.OnMarkerClickListener for loading the image only when requested,
     * so that there aren't potentially thousands of images being requested and loaded unnecessarily.
     *
     * How to set custom listener from: https://stackoverflow.com/q/47167058
     *
     * @param imageURL  the image's URL
     * @return          the listener
     */
    private static Marker.OnMarkerClickListener loadMonumentImage(String imageURL) {

        return new Marker.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {

                Log.i("TAP", "Tapped " + marker.getTitle());

                // Toggle display
                if (!marker.isInfoWindowOpen()) {
                    marker.showInfoWindow();
                }
                else {
                    marker.getInfoWindow().close();
                }


                // Only get the image if it hasn't already been loaded
                if (marker.getImage() == null && imageURL != null) {

                    // Use a new Thread to avoid networking on the main thread
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                    Drawable image = getDrawableFromURL(imageURL);
                                    marker.setImage(image);
                                    Log.i("TAP", "Displaying image for " + marker.getTitle());

                            }
                            catch (IOException e) {
                                Log.i("TAP", "Couldn't get image: " + e.getMessage());
                            }
                        }
                    });

                    // Run the thread
                    thread.start();

//                    // Wait for the req to go through
//                    try {
//
//                        thread.join(WAIT_TIME);
//
//                        // Reload the marker if user is still looking at it
//                        if (marker.isInfoWindowOpen()) {
//                            marker.showInfoWindow();
//                        }
//
//                    } catch (InterruptedException e) {
//                        Log.i("TAP", "Interrupted");
//                        // Doesn't really matter - just won't see the image
//                        // throw new RuntimeException(e);
//                    }

                }
                else {
                    Log.i("TAP", "Marker already has an image: " + marker.getImage().toString());
                    Log.i("TAP", String.valueOf(marker.isInfoWindowOpen()));
                }

                return false;

            }

        };

    }


    /**
     * Get a Drawable (image) from a URL.
     *
     * How to do so from: https://stackoverflow.com/a/9490060
     *
     * @param url   the URL
     * @return      the Drawable
     * @throws IOException
     */
    private static Drawable getDrawableFromURL(String url) throws IOException {

        url = secureURL(url);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.connect();

        InputStream input = conn.getInputStream();
        Bitmap image = BitmapFactory.decodeStream(input);

        return new BitmapDrawable(Resources.getSystem(), image);

    }


    /**
     * Change insecure (cleartext) HTTP URLs to secure HTTPS URLs,
     * because my permissions aren't set to allow cleartext ones.
     *
     * Explanation from: https://stackoverflow.com/a/50834600
     *
     * @param url   the insecure URL
     * @return      the secure URL
     */
    private static String secureURL(String url) {

        return url.replace("http://", "https://");

    }

}
