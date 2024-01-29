package com.example.monumentmapper.ui;


import android.util.Log;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.sparql.core.Prologue;

public class Querier {

    /**
     * WikiData SPARQL endpoint URL.
     * From: https://stackoverflow.com/questions/36535702/use-jena-to-query-wikidata
     */
    private static final String WD_ENDPOINT = "https://query.wikidata.org/sparql";
    private static final Prologue WD_PREFIXES = new Prologue();

    public static void init() {

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
                "SELECT DISTINCT ?building ?coords ?image WHERE \n" +
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
            Log.i("MAR", "Printing results now!");
            ResultSetFormatter.out(System.out, results, query);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            Log.i("MAR", "Error getting query results: " + ex);
        } finally {
            qExec.close();
        }
    }
}
