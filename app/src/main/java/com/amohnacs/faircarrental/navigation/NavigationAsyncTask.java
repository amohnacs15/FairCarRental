package com.amohnacs.faircarrental.navigation;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;

/**
 * Created by adrianmohnacs on 4/23/18.
 */
@SuppressLint("StaticFieldLeak") //possible leak from RecyclerView handled by killing this task in Activity's onStop()
public class NavigationAsyncTask extends AsyncTask<NavigationRequestObject, Void, DirectionsResult> {

    private final GeoApiContext context;
    private final GoogleMap map;
    private final RecyclerView recyclerView;

    NavigationAsyncTask(GeoApiContext context, GoogleMap map, RecyclerView recyclerView) {
        this.context = context;
        this.map = map;
        this.recyclerView = recyclerView;
    }

    @Override
    protected DirectionsResult doInBackground(NavigationRequestObject... navigationRequestObjects) {
        DateTime now = new DateTime();
        NavigationRequestObject requestObject = navigationRequestObjects[0];

        try {
            return DirectionsApi.newRequest(context)
                    .mode(requestObject.getMode())
                    .origin(requestObject.getOriginLatLng())
                    .destination(requestObject.getDestinationLatLng())
                    .departureTime(now)
                    .await();
        } catch (ApiException | InterruptedException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override //called on the UI thread
    protected void onPostExecute(DirectionsResult directionsResult) {
        super.onPostExecute(directionsResult);

        if (directionsResult != null) {
            addMarkersToMap(directionsResult, map);
            addPolyline(directionsResult, map);
            positionCamera(directionsResult.routes[0], map);

            recyclerView.setAdapter(new StepsAdapter(
                    directionsResult.routes[0].legs[0].steps));
        }
    }

    private void addMarkersToMap(DirectionsResult results, GoogleMap uiThreadMap) {
        uiThreadMap.addMarker(new MarkerOptions().position(
                new LatLng(
                        results.routes[0].legs[0].startLocation.lat,results.routes[0].legs[0].startLocation.lng
                ))
                .title(results.routes[0].legs[0].startAddress)
        );
        uiThreadMap.addMarker(new MarkerOptions().position(
                new LatLng(
                        results.routes[0].legs[0].endLocation.lat,results.routes[0].legs[0].endLocation.lng
                ))
                .title(results.routes[0].legs[0].startAddress)
                .snippet(getEndLocationTitle(results))
        );
    }

    private String getEndLocationTitle(DirectionsResult results){
        return  "Time :"+ results.routes[0].legs[0].duration.humanReadable
                + " Distance :" + results.routes[0].legs[0].distance.humanReadable;
    }

    /**
     * You can use PolyUtil in the Google Maps Android API Utility Library to encode a sequence of latitude/longitude coordinates ('LatLngs') into an encoded path string, and to decode an encoded path string into a sequence of LatLngs. This will ensure interoperability with the Google Maps APIs web services.
     * @param results
     * @param uiThreadMap
     */
    private void addPolyline(DirectionsResult results, GoogleMap uiThreadMap) {

        List<LatLng> decodedPath = PolyUtil.decode(results.routes[0].overviewPolyline.getEncodedPath());
        uiThreadMap.addPolyline(new PolylineOptions().addAll(decodedPath));
    }

    private void positionCamera(DirectionsRoute route, GoogleMap mMap) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(route.legs[0].startLocation.lat, route.legs[0].startLocation.lng), 10)
        );
    }
}
