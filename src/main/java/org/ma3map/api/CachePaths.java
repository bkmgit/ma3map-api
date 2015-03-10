package org.ma3map.api;

import java.io.IOException;
import java.util.ArrayList;
import java.lang.System;
import java.lang.Object;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;

import org.ma3map.api.handlers.Database;
import org.ma3map.api.handlers.Data;
import org.ma3map.api.handlers.Log;
import org.ma3map.api.listeners.ProgressListener;
import org.ma3map.api.carriers.Stop;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * @author Jason Rogena <jasonrogena@gmail.com>
 * @since 2015-02-13
 *
 * This class implements the /cache_paths endpoint for the ma3map API.
 * The /cache_paths endpoint calculates the best path from each of the stops to each of the other
 * stops
 */
@Path("/cache_paths")
public class CachePaths {

    private static final String TAG = "ma3map.CachePaths";


    /**
    * Entry point for the /cache_paths endpoint
    *
    * @return Stringified JSONArray of the alternative paths
    */
    @GET
    @Produces("application/json")
    public String start(@QueryParam("from") String fromString, @QueryParam("to") String toString, @QueryParam("no_from_stops") String noFromStops, @QueryParam("no_to_stops") String noToStops) {
        Log.i(TAG, "API called");
        Data dataHandler = new Data();
        GetStopsProgressListener getStopsProgressListener = new GetStopsProgressListener();
        dataHandler.addProgressListener(getStopsProgressListener);
        dataHandler.getStopData();
        return "DONE";
    }

    private class GetStopsProgressListener implements ProgressListener {

        @Override
        public void onProgress(int progress, int end, String message, int flag) {
        }

        @Override
        public void onDone(Object output, String message, int flag) {
            if(output != null) {
                ArrayList<Stop> stops = (ArrayList<Stop>) output;
                Log.d(TAG, "Number of stops = "+String.valueOf(stops.size()));
                Data dataHandler = new Data();
                for(int i = 0; i < stops.size(); i++) {
                    Log.i(TAG, "Currently at "+String.valueOf(i+1)+" of "+String.valueOf(stops.size())+" stops");
                    for(int j = 0; j < stops.size(); j++) {
                        if(i != j) {
                            JSONObject pathObject = dataHandler.getPaths(stops.get(i).getLatLng(), 1, stops.get(j).getLatLng(), 2);
                            try {
                                JSONArray paths = pathObject.getJSONArray("paths");
                                String rawTime = pathObject.getString("time_taken");
                                double time = Double.parseDouble(rawTime.replace("ms", ""));
                                //TODO: run query for commute
                                //commute(id serial primary key, start_id varchar, destination_id varchar, processing_time double precision)
                                String cQuery = "insert into commute(start_id, destination_id, processing_time) values(";
                                cQuery = cQuery + "'" + stops.get(i).getId() + "'";
                                cQuery = cQuery + ", '" + stops.get(j).getId() + "'";
                                cQuery = cQuery + ", " + String.valueOf(time) + ")";
                                Log.d(TAG, cQuery);
                                int commuteId = 0;
                                for(int pI = 0; pI < paths.length(); pI++) {
                                    JSONObject currPath = paths.getJSONObject(pI);
                                    double score = currPath.getDouble("score");
                                    String pQuery = "insert into commute_path(score, commute_id) values("+score+","+commuteId+")";
                                    Log.d(TAG, pQuery);
                                    JSONArray steps = currPath.getJSONArray("steps");
                                    int stepSeq = 0;
                                    int pathId = 0;
                                    for(int sI = 0; sI < steps.length(); sI++) {
                                        JSONObject currStep = steps.getJSONObject(sI);
                                        if(currStep.getString("type").equals("matatu")) {
                                            //commute_step(id serial primary key, commute_path_id integer references commute_path(id), text varchar, sequence integer, start_id varchar, destination_id varchar, route_id varchar);
                                            String sQuery = "insert into commute_step(commute_path_id, text, sequence, start_id, destination_id, route_id) values(";
                                            sQuery = sQuery + pathId;
                                            sQuery = sQuery + ", '" + currStep.getString("text") + "'"; 
                                            sQuery = sQuery + ", " + stepSeq; 
                                            sQuery = sQuery + ", '" + currStep.getJSONObject("start").getString("id") + "'"; 
                                            sQuery = sQuery + ", '" + currStep.getJSONObject("destination").getString("id") + "')"; 
                                            sQuery = sQuery + ", '" + currStep.getJSONObject("route").getString("id") + "')"; 
                                            stepSeq++;
                                            Log.d(TAG, sQuery);
                                        }
                                    }
                                }
                            } catch(JSONException e) {
                                Log.e(TAG, "An error occurred while trying to ");
                            }
                        }
                    }
                }
            }
            else {
                Log.e(TAG, "Could not fetch stop data from the server");
            }
        }
    }
}
