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
    private static String WD_ENDPOINT = "https://query.wikidata.org/sparql";
    private static final String WD_PREFIXES =
            "PREFIX bd: <http://www.bigdata.com/rdf#>\n" +
            "PREFIX cc: <http://creativecommons.org/ns#>\n" +
            "PREFIX dct: <http://purl.org/dc/terms/>\n" +
            "PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n" +
            "PREFIX ontolex: <http://www.w3.org/ns/lemon/ontolex#>\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX p: <http://www.wikidata.org/prop/>\n" +
            "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>\n" +
            "PREFIX pqn: <http://www.wikidata.org/prop/qualifier/value-normalized/>\n" +
            "PREFIX pqv: <http://www.wikidata.org/prop/qualifier/value/>\n" +
            "PREFIX pr: <http://www.wikidata.org/prop/reference/>\n" +
            "PREFIX prn: <http://www.wikidata.org/prop/reference/value-normalized/>\n" +
            "PREFIX prov: <http://www.w3.org/ns/prov#>\n" +
            "PREFIX prv: <http://www.wikidata.org/prop/reference/value/>\n" +
            "PREFIX ps: <http://www.wikidata.org/prop/statement/>\n" +
            "PREFIX psn: <http://www.wikidata.org/prop/statement/value-normalized/>\n" +
            "PREFIX psv: <http://www.wikidata.org/prop/statement/value/>\n" +
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX schema: <http://schema.org/>\n" +
            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n" +
            "PREFIX wd: <http://www.wikidata.org/entity/>\n" +
            "PREFIX wdata: <http://www.wikidata.org/wiki/Special:EntityData/>\n" +
            "PREFIX wdno: <http://www.wikidata.org/prop/novalue/>\n" +
            "PREFIX wdref: <http://www.wikidata.org/reference/>\n" +
            "PREFIX wds: <http://www.wikidata.org/entity/statement/>\n" +
            "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n" +
            "PREFIX wdtn: <http://www.wikidata.org/prop/direct-normalized/>\n" +
            "PREFIX wdv: <http://www.wikidata.org/value/>\n" +
            "PREFIX wikibase: <http://wikiba.se/ontology#>\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>";

    public static void getLocalMonuments() {

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

//        Query query = QueryFactory.create(WD_PREFIXES + "\n" + queryString);
//        QueryExecution qExec = QueryExecutionFactory.sparqlService(WD_ENDPOINT, query);

        // Define the Wikibase prefixes
        // How to do so from: https://stackoverflow.com/a/32125863

        Prologue queryPrologue = new Prologue();
        queryPrologue.setPrefix("wd", "http://www.wikidata.org/entity/");
        queryPrologue.setPrefix("wdt", "http://www.wikidata.org/prop/direct/");
        queryPrologue.setPrefix("p", "http://www.wikidata.org/prop/");
        queryPrologue.setPrefix("pq", "http://www.wikidata.org/prop/qualifier/");
        queryPrologue.setPrefix("ps", "http://www.wikidata.org/prop/statement/");
        queryPrologue.setPrefix("wikibase", "http://wikiba.se/ontology#");
        queryPrologue.setPrefix("bd", "http://www.bigdata.com/rdf#");
        queryPrologue.setPrefix("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
        queryPrologue.setPrefix("wd", "http://www.wikidata.org/entity/");

        // Make the query, inc. the prefix defs
        // How to make query from: https://stackoverflow.com/questions/36535702/use-jena-to-query-wikidata
        Log.i("MAR", "Going to build query!");
        Query query = QueryFactory.parse(new Query(queryPrologue), queryString, null, null);
        QueryExecution qExec = QueryExecutionFactory.sparqlService(WD_ENDPOINT, query);
        Log.i("MAR", "Built query!");


        try {
            ResultSet results = qExec.execSelect();
            ResultSetFormatter.out(System.out, results, query);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        } finally {
            qExec.close();
        }
    }
}
