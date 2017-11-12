package com.example.winnipegbusschedules;

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
    public String number;
    public String variantName;
    public String scheduledTime;
    public String estimatedTime;

    public Bus()
    {
      number = variantName = scheduledTime = estimatedTime = "";
    }

    @Override
    public boolean equals(Object obj)
    {
      // To Do: comparison by time
      return super.equals(obj);
    }
  }

  // Class that represents a stop
  public class Stop
  {
    public String number;
    public String name;
    public double latitude;
    public double longitude;

    public Stop()
    {
      number = name = "";
      latitude = longitude = 0;
    }

    @Override
    public boolean equals(Object obj)
    {
      return super.equals(obj);
    }
  }
}
