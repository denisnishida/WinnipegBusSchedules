package com.example.winnipegbusschedules;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity
        implements GoogleMap.OnMyLocationButtonClickListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnInfoWindowClickListener
{
  public static final int MY_LOCATION_REQUEST_CODE = 1;

  public static final String API_KEY = "api-key=rQ8lXW4lpLR9CwiYqK";
  public static final String BEGIN_URL = "https://api.winnipegtransit.com/v2/";
  public static final String JSON_APPEND = ".json";
  public static final String STATUS_SCHEDULE_REQUEST = "statuses/schedule";
  public static final String STOP_SCHEDULE_REQUEST_BEGIN = "stops";
  public static final String STOP_SCHEDULE_REQUEST_END = "/schedule";

  public static final String STOP_NUMBER_KEY = "StopNumber";

  private GoogleMap mMap;
  private GoogleApiClient mGoogleApiClient;
  private Location mLastLocation;
  private DBHelper dbHelper;

  String requestUrl;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);
    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);

    // Create an instance of GoogleAPIClient.
    if (mGoogleApiClient == null)
    {
      mGoogleApiClient = new GoogleApiClient.Builder(this)
              .addConnectionCallbacks(this)
              .addOnConnectionFailedListener(this)
              .addApi(LocationServices.API)
              .build();
    }

    mLastLocation = null;

    dbHelper = new DBHelper(this);
    ArrayList<Transit.Stop> test = dbHelper.loadData();
    for (int i = 0; i < test.size(); i++)
      Log.d("Database", test.get(i).name + " | " + test.get(i).number);
  }


  /**
   * Manipulates the map once available.
   * This callback is triggered when the map is ready to be used.
   * This is where we can add markers or lines, add listeners or move the camera.
   */
  @Override
  public void onMapReady(GoogleMap googleMap)
  {
    mMap = googleMap;

    // turning on the my location layer (runtime location permission is required)
    if (ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    {
      mMap.setMyLocationEnabled(true);
      mMap.setOnMyLocationButtonClickListener(this);
      mMap.setOnCameraIdleListener(this);
      mMap.setOnInfoWindowClickListener(this);
    }
    else
    {
      // Show rationale and request permission.
      ActivityCompat.requestPermissions(MapsActivity.this, new String[]
              {android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }
  }

  @Override
  public void onInfoWindowClick(Marker marker)
  {
    mLastLocation = createNewLocation(marker.getPosition().latitude, marker.getPosition().longitude);

    // Open the stop activity for the clicked bus stop
    Intent intent = new Intent(MapsActivity.this, StopActivity.class);
    intent.putExtra("StopNumber", marker.getTitle().split(": ")[1]);
    startActivity(intent);
  }

  private Location createNewLocation(double lat, double lon)
  {
    Location newLocation = new Location("new location");
    newLocation.setLatitude(lat);
    newLocation.setLongitude(lon);

    return newLocation;
  }

  @Override
  public void onCameraIdle()
  {
    CameraPosition cameraPosition = mMap.getCameraPosition();
    LatLng cameraLatLng = cameraPosition.target;

    double lat = cameraLatLng.latitude;
    double lon = cameraLatLng.longitude;

    Location newLocation = createNewLocation(lat, lon);

    if (mLastLocation.distanceTo(newLocation) > 300)
    {
      mLastLocation = newLocation;
      requestStopsNearby(lat, lon);
    }


  }

  private void requestStopsNearby(double lat, double lon)
  {
    // URL to request the stops
    // ttp://api.winnipegtransit.com/v2/stops.json?distance=500&lat=49.895&lon=-97.138&api-key=rQ8lXW4lpLR9CwiYqK
    requestUrl = BEGIN_URL + STOP_SCHEDULE_REQUEST_BEGIN + JSON_APPEND
            + "?distance=500" + "&lat=" + lat + "&lon=" + lon + "&" + API_KEY;

    processRequest();
  }

  // Event Handler when the location button is clicked
  @Override
  public boolean onMyLocationButtonClick()
  {
    // Return false so that we don't consume the event and the default behavior still occurs
    // (the camera animates to the user's current position).
    return false;
  }

  // handles the result of the location permission request by implementing the
  // ActivityCompat.OnRequestPermissionsResultCallback
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
  {
    if (requestCode == MY_LOCATION_REQUEST_CODE)
    {
      if (permissions.length == 1 &&
              permissions[0].equals(android.Manifest.permission.ACCESS_FINE_LOCATION) &&
              grantResults[0] == PackageManager.PERMISSION_GRANTED)
      {
        // either check to see if permission is available, or handle a potential
        // SecurityException before calling mMap.setMyLocationEnabled
        try
        {
          mMap.setMyLocationEnabled(true);
        }
        catch (SecurityException e)
        {
          Log.d("INFO", "SecurityException in MapsActivity.onRequestPermissionsResult: " +
                  e.getMessage());
        }
      }
      else
      {
        // Permission was denied. Display an error message.
        Toast.makeText(MapsActivity.this, "Permission to access your location was denied so your location cannot be displayed on the map.",
                Toast.LENGTH_LONG).show();
      }
    }
  }

  @Override
  protected void onStart()
  {
    mGoogleApiClient.connect();
    super.onStart();
  }

  @Override
  protected void onStop()
  {
    mGoogleApiClient.disconnect();
    super.onStop();
  }

  @Override
  public void onConnected(Bundle connectionHint)
  {
    if (mLastLocation == null)
    {
      if (ActivityCompat.checkSelfPermission(this,
                      android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                      && ActivityCompat.checkSelfPermission(this,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
      {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        return;
      }

      mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    if (mLastLocation != null)
    {
      double lat = mLastLocation.getLatitude();
      double lon = mLastLocation.getLongitude();

      LatLng currentLocation = new LatLng(lat, lon);

      mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16));

      requestStopsNearby(lat, lon);
    }
  }

  @Override
  public void onConnectionSuspended(int i)
  {

  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
  {

  }

  public void processRequest()
  {
    if (Helper.isNetworkAvailable(this))
    {
      // create and execute AsyncTask
      ProcessingTask task = new ProcessingTask();
      task.execute();
    }
    else
    {
      Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
    }
  }

  class ProcessingTask extends AsyncTask
  {
    @Override
    protected Object doInBackground(Object[] objects)
    {
      // Create URL object to RSS file
      URL url = null;

      try
      {
        url = new URL(requestUrl);
      }
      catch (MalformedURLException e)
      {
        e.printStackTrace();
      }

      // Create and open HTTP connection
      HttpURLConnection connection = null;
      try
      {
        connection = (HttpURLConnection) url.openConnection();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }

      try
      {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder stringBuilder = new StringBuilder();
        String line;

        while ((line = bufferedReader.readLine()) != null)
        {
          stringBuilder.append(line);
        }

        bufferedReader.close();

        return stringBuilder.toString();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
      finally
      {
        connection.disconnect();
      }

      return null;
    }

    // On the end of the thread
    @Override
    protected void onPostExecute(Object o)
    {
      if (o != null)
      {
        String response = o.toString();

        Log.i("INFO", o.toString());

        parseJSON(response);
      }
      else
      {
        Toast.makeText(MapsActivity.this,
                "Sorry, it was not possible to get the schedule information",
                Toast.LENGTH_SHORT).show();
      }
    }

    // Verify the request and get the values
    private void parseJSON(String response)
    {
      try
      {
        // Using orj.json, get the file string and convert it to an object
        JSONObject object = (JSONObject) new JSONTokener(response).nextValue();

        // The Winnipeg Transit JSON results usually have nested values
        // We can identify the request by the first key of the first level

        // The method names() will retrieve an JSONArray with the key names
        JSONArray objectNames = object.names();

        // Retrieve the first key of the first level
        String firstKey = objectNames.getString(0);

        if (firstKey.equals("stops"))
        {
          extractStops(object.getJSONArray(firstKey));
        }
      }
      catch (JSONException e)
      {
        e.printStackTrace();
      }
    }

    // Get the information from the status request
    private void extractStops(JSONArray stopsArray) throws JSONException
    {
      for (int i = 0; i < stopsArray.length(); i++)
      {
        JSONObject stopObj = (JSONObject)stopsArray.get(i);
        String snippet = stopObj.getString("name");
        String title = "Stop Number: " + stopObj.getString("number");

        JSONObject geographicObj = stopObj.getJSONObject("centre").getJSONObject("geographic");
        double latitude = geographicObj.getDouble("latitude");
        double longitude = geographicObj.getDouble("longitude");

        // Add a marker in each bus stop
        LatLng busStop = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions()
                          .position(busStop)
                          .title(title)
                          .snippet(snippet)
                          .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
      }
    }
  }
}
