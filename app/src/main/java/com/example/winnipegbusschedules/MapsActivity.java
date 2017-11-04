package com.example.winnipegbusschedules;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends AppCompatActivity
        implements GoogleMap.OnMyLocationButtonClickListener,
                   OnMapReadyCallback
{
  final int MY_LOCATION_REQUEST_CODE = 1;

  private GoogleMap mMap;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);
    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);
    mapFragment.getMapAsync(this);
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
      Log.d("Jody", "enabling my location");
      mMap.setMyLocationEnabled(true);
      mMap.setOnMyLocationButtonClickListener(this);
    }
    else
    {
      // Show rationale and request permission.
      ActivityCompat.requestPermissions(MapsActivity.this, new String[]
              {android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    // Add a marker in Sydney and move the camera
    LatLng sydney = new LatLng(-34, 151);
    mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
    mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
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
}
