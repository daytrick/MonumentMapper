package com.example.monumentmapper.ui;


import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static MapView mapView;

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
    public static void getLocalMonuments() {

        new Thread(new Runnable() {

            @Override
            public void run() {

                Log.i("MAR", "In a new thread!");
                queryLocalMonuments();

            }

        }).start();

    }

    /**
     * Query Wikidata for local monuments.
     */
    private static void queryLocalMonuments() {

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
                "    bd:serviceParam wikibase:center \"Point(-2.8175 56.3406)\"^^geo:wktLiteral .\n" +
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
        QueryExecution qExec = QueryExecutionFactory.sparqlService(WD_ENDPOINT, query);
        Log.i("MAR", "Built query!");

        try {
            Log.i("MAR", "Trying to select results!");
            ResultSet results = qExec.execSelect();

            while (results.hasNext()) {

                QuerySolution qs = results.nextSolution();
                Log.i("POINT", qs.get("coords").toString());
                Map<String, Object> monumentData = processMonumentQuery(qs);
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
    private static Map processMonumentQuery(QuerySolution qs) {

        Map<String, Object> monumentDict = new HashMap<>();

        // Extract monument name & location
        Matcher nameMatcher = NAME_REGEX.matcher(qs.get("buildingLabel").toString());
        Matcher locMatcher = POINT_REGEX.matcher(qs.get("coords").toString());
        while (nameMatcher.find() && locMatcher.find()) {

            try {

                String name = nameMatcher.group("name");
                double latitude = Double.parseDouble(locMatcher.group("lat"));
                double longitude = Double.parseDouble(locMatcher.group("long"));

                addMarker(name, latitude, longitude);

            } catch (NumberFormatException | NullPointerException e) {
                Log.i("POINT", "Could not extract coordinates");
            }

        }
        Log.i("MAR", "Done finding matches!");

        // Monument image
        monumentDict.put("image", qs.get("image"));

        return monumentDict;

    }


    /**
     * Add a marker to the map using OSMBonus.
     *
     * @param name      name of the monument
     * @param latitude  latitude of the monument
     * @param longitude longitude of the monument
     */
    private static void addMarker(String name, double latitude, double longitude) {

        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(latitude, longitude));
        marker.setTitle(name);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        Log.i("MAR", "Added a point!");

    }


    /**
     * Add a marker to the map using OSMBonus.
     *
     * How to do so from: https://stackoverflow.com/a/55707403
     *
     * @param monumentData dictionary containing the monument data
     */
//    private static void addMarker(Map<String, Object> monumentData) {
//
//        // Set up marker
//        Marker marker = new Marker(mapView);
//
//        // Put the data in
//        try {
//            double latitude = Double.parseDouble(monumentData.get("lat").toString());
//            double longitude = Double.parseDouble(monumentData.get("long").toString());
//            marker.setPosition(new GeoPoint(latitude, longitude));
//            marker.setTitle(monumentData.get("name").toString());
//            //marker.snippet = "Description text testing"
//            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
//            mapView.getOverlays().add(marker);
//            Log.i("MAR", "Added a point!");
//        }
//        catch (Exception e) {
//            Log.i("MAR", e.getMessage().toString());
//        }
//
//    }
}
