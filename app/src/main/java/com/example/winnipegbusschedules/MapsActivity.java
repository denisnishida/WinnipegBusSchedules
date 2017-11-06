package com.example.winnipegbusschedules;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

public class MapsActivity extends AppCompatActivity
        implements GoogleMap.OnMyLocationButtonClickListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnInfoWindowClickListener
{
  final int MY_LOCATION_REQUEST_CODE = 1;

  final String API_KEY = "api-key=rQ8lXW4lpLR9CwiYqK";
  final String BEGIN_URL = "https://api.winnipegtransit.com/v2/";
  final String JSON_APPEND = ".json";
  final String STATUS_SCHEDULE_REQUEST = "statuses/schedule";
  final String STOP_SCHEDULE_REQUEST_BEGIN = "stops";
  final String STOP_SCHEDULE_REQUEST_END = "/schedule";

  private GoogleMap mMap;
  private GoogleApiClient mGoogleApiClient;
  private Location mLastLocation;

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
  }


  /**
   * Manipulates the map once available.
   * This callback is triggered when the map is ready to be used.
   * This is where we can add markers or lines, add listeners or move the camera. In this case,
   * we just add a marker near Sydney, Australia.
   * If Google Play services is not installed on the device, the user will be prompted to install
   * it inside the SupportMapFragment. This method will only be triggered once the user has
   * installed Google Play services and returned to the app.
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
    Toast.makeText(this,
                    "This should open the stop schedule",
                     Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onCameraIdle()
  {
    CameraPosition cameraPosition = mMap.getCameraPosition();
    LatLng cameraLatLng = cameraPosition.target;

    double lat = cameraLatLng.latitude;
    double lon = cameraLatLng.longitude;

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
    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
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

    if (mLastLocation != null)
    {
      double lat = mLastLocation.getLatitude();
      double lon = mLastLocation.getLongitude();

      LatLng currentLocation = new LatLng(lat, lon);

      mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16));
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

  private boolean isNetworkAvailable()
  {
    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
  }

  public void processRequest()
  {
    if (isNetworkAvailable())
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
    // On the start of the thread
    @Override
    protected void onPreExecute()
    {
      super.onPreExecute();
    }

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
        String title = stopObj.getString("number") + ": " + stopObj.getString("name");

        JSONObject geographicObj = stopObj.getJSONObject("centre").getJSONObject("geographic");
        double latitude = geographicObj.getDouble("latitude");
        double longitude = geographicObj.getDouble("longitude");

        // Add a marker in each bus stop
        LatLng busStop = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(busStop).title(title));
      }
    }
  }
}
