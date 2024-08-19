package com.project.trackit

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.DatagramPacket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*
import java.net.Socket
import java.net.DatagramSocket

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var longitudeText: TextView
    private lateinit var latitudeText: TextView
    private lateinit var dateTimeText: TextView
    private lateinit var sendButton_UDP: Button
    private lateinit var sendButton_TCP: Button

    private var currentData: String = ""
    private var bestLocation: Location? = null

    companion object {
        const val LOCATION_PERMISSION_CODE = 101
        const val TCP_PORT = 60000
        const val UDP_PORT = 60001
        const val IP_ADDRESS_1 = "152.204.170.240"  // IP 1
        const val IP_ADDRESS_2 = "161.10.95.122"  // IP 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        latitudeText = findViewById(R.id.latitudeValue)
        longitudeText = findViewById(R.id.longitudeValue)
        dateTimeText = findViewById(R.id.dateTimeValue)
        sendButton_UDP = findViewById(R.id.sendButton_UDP)
        sendButton_TCP = findViewById(R.id.sendButton_TCP)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (!checkLocationPermissions()) {
            requestLocationPermissions()
        } else {
            startLocationUpdates()
        }

        sendButton_UDP.setOnClickListener {
            if (currentData.isNotEmpty()) {
                sendLocationData_UDP(IP_ADDRESS_1)
                sendLocationData_UDP(IP_ADDRESS_2)
                Toast.makeText(this, "UDP: Data sent", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No location data available", Toast.LENGTH_SHORT).show()
            }
        }

        sendButton_TCP.setOnClickListener {
            if (currentData.isNotEmpty()) {
                sendLocationData_TCP(IP_ADDRESS_1)
                sendLocationData_TCP(IP_ADDRESS_2)
                Toast.makeText(this, "TCP: Data sent", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No location data available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        if (isBetterLocation(location, bestLocation)) {
            bestLocation = location
            val lat = location.latitude
            val lon = location.longitude
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            dateTimeFormat.timeZone = TimeZone.getDefault()
            val localDateTime = dateTimeFormat.format(Date(location.time))
            currentData = "Lat: $lat, Lon: $lon, Date/Time: $localDateTime"

            // Update UI with current location
            latitudeText.text = "$lat"
            longitudeText.text = "$lon"
            dateTimeText.text = "$localDateTime"

            Log.d("LocationUpdate", "Provider: ${location.provider}, Data: $currentData")
        }
    }

    private fun isBetterLocation(newLocation: Location, currentBestLocation: Location?): Boolean {
        if (currentBestLocation == null) {
            return true
        }

        // Checking newest location
        val timeDelta = newLocation.time - currentBestLocation.time
        val isSignificantlyNewer = timeDelta > 120000  // 2 minutos
        val isSignificantlyOlder = timeDelta < -120000  // 2 minutos
        val isNewer = timeDelta > 0

        if (isSignificantlyNewer) {
            return true
        } else if (isSignificantlyOlder) {
            return false
        }

        // Checking precision
        val accuracyDelta = (newLocation.accuracy - currentBestLocation.accuracy).toInt()
        val isMoreAccurate = accuracyDelta < 0
        val isLessAccurate = accuracyDelta > 0

        // Checking provider
        val isFromSameProvider = newLocation.provider == currentBestLocation.provider

        // Best location determination
        return when {
            isMoreAccurate -> true
            isNewer && !isLessAccurate -> true
            isNewer && !isFromSameProvider -> true
            else -> false
        }
    }

    private fun sendLocationData_UDP(ipAddress: String) {
        Thread {
            try {
                // Send UDP
                val socket_udp = DatagramSocket()
                val address = InetAddress.getByName(ipAddress)
                val message_udp = currentData.toByteArray()
                val packet = DatagramPacket(message_udp, message_udp.size, address, UDP_PORT)
                socket_udp.send(packet)
                socket_udp.close()
                Log.d("UDP", "Data sent to $ipAddress")
            } catch (e: Exception) {
                Log.e("UDP", "Error sending data to $ipAddress: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this, "Error sending data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun sendLocationData_TCP(ipAddress: String) {
        Thread {
            try {
                // SEND TCP
                val socket_tcp = Socket(ipAddress, TCP_PORT)
                val outputStream = socket_tcp.getOutputStream()
                val message_tcp = currentData.toByteArray()
                outputStream.write(message_tcp)
                outputStream.flush()
                outputStream.close()
                socket_tcp.close()
                Log.d("TCP", "Data sent to $ipAddress")
            } catch (e: Exception) {
                Log.e("TCP", "Error sending data to $ipAddress: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this, "Error sending data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

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
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun checkLocationPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fineLocation == PackageManager.PERMISSION_GRANTED && coarseLocation == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_CODE)
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Solicitar actualizaciones de ambos proveedores
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, this)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, this)
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
