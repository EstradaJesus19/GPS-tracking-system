package com.project.trackit

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*


class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var longitudeText: TextView
    private lateinit var latitudeText: TextView
    private lateinit var dateTimeText: TextView
    private lateinit var phoneNumber: EditText
    private lateinit var sendButton: Button

    private var currentData: String = ""

    companion object {
        const val SMS_PERMISSION_CODE = 100
        const val LOCATION_PERMISSION_CODE = 101
    }

    // Handling location updates
    override fun onLocationChanged(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        dateTimeFormat.timeZone = TimeZone.getDefault()
        val localDateTime = dateTimeFormat.format(Date(location.time))
        currentData = "Lat: $lat, \nLon: $lon \nDate/Time: $localDateTime"
        latitudeText.text = "$lat"
        longitudeText.text = "$lon"
        dateTimeText.text = "$localDateTime"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initializing interface components
        latitudeText = findViewById(R.id.latitudeValue)
        longitudeText = findViewById(R.id.longitudeValue)
        dateTimeText = findViewById(R.id.dateTimeValue)
        phoneNumber = findViewById(R.id.phoneNumber)
        sendButton = findViewById(R.id.sendButton)

        // Initializing LocationManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Check and request location permissions if necessary
        if (!checkLocationPermissions()) {
            requestLocationPermissions()
        } else {
            startLocationUpdates()
        }

        // Check and request SMS permissions if necessary
        if (!checkSmsPermission()) {
            requestSmsPermission()
        }

        // Action when pressing the send button
        sendButton.setOnClickListener {
            val phone = phoneNumber.text.toString()

            if (phone.length < 10) {
                Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("PhoneNumber", "Number entered: $phone")

            if (phone.isNotEmpty()) {
                // Build message with current location and time
                val message = "$currentData"
                if (checkSmsPermission()) {
                    sendSMS(phone, message)
                } else {
                    Toast.makeText(this, "Permission to send SMS not granted", Toast.LENGTH_SHORT).show()
                    requestSmsPermission()
                }
            } else {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Send SMS with location and time
    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            Log.d("SMS", "Sending SMS to the phone number: $phoneNumber")
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("SMS", "SMS sent successfully")
            Toast.makeText(this, "SMS sent successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("SMS", "Error sending SMS: ${e.message}")
            Toast.makeText(this, "Error sending SMS: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Resulting permit request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            LOCATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Location permission granted.", Toast.LENGTH_SHORT).show()
                    startLocationUpdates()
                } else {
                    Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show()
                }
            }
            SMS_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "SMS permission granted.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "SMS permission denied.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // Check if location permissions are granted
    private fun checkLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fineLocation == PackageManager.PERMISSION_GRANTED && coarseLocation == PackageManager.PERMISSION_GRANTED
    }

    // Request location permissions
    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_CODE)
    }

    // Check if permission to send SMS is granted
    private fun checkSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    // Request permission to send SMS
    private fun requestSmsPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_CODE)
    }

    // Start location updates
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}


}