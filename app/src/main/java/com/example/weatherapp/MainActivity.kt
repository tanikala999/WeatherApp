package com.example.weatherapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.*
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import org.json.JSONObject
import java.lang.Exception
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private var currentLocation: Location? = null
    private lateinit var locationManager: LocationManager

    private var locationByGps: Location? = null
    private var locationByNetwork: Location? = null
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var addresses: List<Address>? = null

    private lateinit var city: String
    val API: String = "ab994ab9dfeb6900f29b66b59b58f209"

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        isLocationPermissionGranted()

        weatherTask().execute()
    }

    private fun isLocationPermissionGranted(): Boolean {
        return if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) ,1
            )
            Log.i("location_message", "Couldn't get location")
            city = "New York"
            false
        } else {
            val lastKnownLocationByGps =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastKnownLocationByGps?.let {
                locationByGps = lastKnownLocationByGps
            }
            val lastKnownLocationByNetwork =
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            lastKnownLocationByNetwork?.let {
                locationByNetwork = lastKnownLocationByNetwork
            }

            if (locationByGps == null && locationByNetwork != null) {
                getLocationByNetwork()

            } else if (locationByNetwork == null && locationByGps != null) {
                getLocationByGPS()

            } else if (locationByGps != null && locationByNetwork != null) {
                if (locationByGps!!.accuracy > locationByNetwork!!.accuracy) {
                    getLocationByGPS()
                } else {
                    getLocationByNetwork()
                }
            }
            true
        }
    }

    private fun getLocationByGPS() {
        currentLocation = locationByGps
        latitude = currentLocation?.latitude
        longitude = currentLocation?.longitude
        val geocoder = Geocoder(this, Locale.getDefault())
        addresses = geocoder.getFromLocation(latitude!!, longitude!!, 1)
        city = addresses!![0].locality
        Log.i("location_message", "Could get location by GPS, city: $city, lat: $latitude, long: $longitude")
    }

    private fun getLocationByNetwork() {
        currentLocation = locationByNetwork
        latitude = currentLocation?.latitude
        longitude = currentLocation?.longitude
        val geocoder = Geocoder(this, Locale.getDefault())
        addresses = geocoder.getFromLocation(latitude!!, longitude!!, 1)
        city = addresses!![0].locality
        Log.i("location_message", "Could get location by network, city: $city, lat: $latitude, long: $longitude")
    }

    inner class weatherTask() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.main_container).visibility = View.GONE
            findViewById<TextView>(R.id.error_text).visibility = View.GONE
        }

        override fun doInBackground(vararg p0: String?): String? {
            var response: String?
            try {
                response = URL("https://api.openweathermap.org/data/2.5/weather?lat=$latitude&lon=$longitude&&units=metric&appid=$API")
                    .readText(Charsets.UTF_8)
            } catch(e: Exception) {
                response = null
            }
            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {
                val jsonObj = JSONObject(result)
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val wind = jsonObj.getJSONObject("wind")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                val updatedAt: Long = jsonObj.getLong("dt")
                val updatedAtText = "Updated at: ${SimpleDateFormat("dd/MM/yyyy hh:mm a", 
                    Locale.ENGLISH).format(Date(updatedAt * 1000))}"
                val tempInt = main.getInt("temp")
                val temp = "$tempInt°C"
                val tempMinInt = main.getInt("temp_min")
                val tempMin = "Min Temp: $tempMinInt°C"
                val tempMaxInt = main.getInt("temp_max")
                val tempMax = "Max Temp: $tempMaxInt°C"
                val pressure = main.getString("pressure")
                val humidity = main.getString("humidity")
                val sunrise: Long = sys.getLong("sunrise")
                val sunset: Long = sys.getLong("sunset")
                val windSpeed = wind.getString("speed")
                val weatherDescription = weather.getString("description")
                val address = jsonObj.getString("name") + ", " + sys.getString("country")

                findViewById<TextView>(R.id.address).text = address
                findViewById<TextView>(R.id.updated_at).text = updatedAtText
                findViewById<TextView>(R.id.status).text = weatherDescription.capitalize()
                findViewById<TextView>(R.id.temp).text = temp
                findViewById<TextView>(R.id.temp_min).text = tempMin
                findViewById<TextView>(R.id.temp_max).text = tempMax
                findViewById<TextView>(R.id.sunrise).text = SimpleDateFormat("hh:mm a",
                    Locale.ENGLISH).format(Date(sunrise * 1000))
                findViewById<TextView>(R.id.sunset).text = SimpleDateFormat("hh:mm a",
                    Locale.ENGLISH).format(Date(sunset * 1000))
                findViewById<TextView>(R.id.wind).text = windSpeed
                findViewById<TextView>(R.id.pressure).text = pressure
                findViewById<TextView>(R.id.humidity).text = humidity

                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<RelativeLayout>(R.id.main_container).visibility = View.VISIBLE
            } catch (e: Exception) {
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<TextView>(R.id.error_text).visibility = View.VISIBLE
            }
        }
    }
}