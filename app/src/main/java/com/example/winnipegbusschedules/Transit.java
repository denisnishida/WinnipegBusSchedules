package com.example.winnipegbusschedules;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Denis on 12/11/2017.
 * Class that contains the classes related to the
 * Winnipeg Transit classes
 */

public class Transit
{
  // Class that represents a bus
  public class Bus
  {
    public String key;
    public String number;
    public String variantName;
    public String scheduledTime;
    public String estimatedTime;
    public String stopId;

    public Bus()
    {
      key = number = variantName = scheduledTime = estimatedTime = "";
    }

    public Date getEstimatedTimeAsDate() throws ParseException
    {
      DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.CANADA);
      return format.parse(estimatedTime);
    }

//    @Override
//    public boolean equals(Object obj)
//    {
//      // To Do: comparison by time
//      return super.equals(obj);
//    }
  }

  // Class that represents a stop
  public class Stop
  {
    public String key;
    public String number;
    public String name;
    public double latitude;
    public double longitude;

    public Stop()
    {
      key = number = name = "";
      latitude = longitude = 0;
    }

//    @Override
//    public boolean equals(Object obj)
//    {
//      return super.equals(obj);
//    }
  }
}
