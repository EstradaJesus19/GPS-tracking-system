package com.project.trackit

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.Socket
import java.net.DatagramSocket
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var longitudeText: TextView
    private lateinit var latitudeText: TextView
    private lateinit var dateText: TextView
    private lateinit var timeText: TextView
    private lateinit var sendButton_UDP: Button
    private lateinit var sendButton_TCP: Button

    private var currentData: String = ""
    private var isSendingUDP = false
    private val handler = Handler(Looper.getMainLooper())
    private val sendInterval: Long = 1000 // 1 segundos (orlando)
    private var lastGpsUpdateTime: Long = 0
    private var lastNetworkUpdateTime: Long = 0

    companion object {
        const val LOCATION_PERMISSION_CODE = 101
        const val UDP_PORT = 60001
        const val TCP_PORT = 60000
        const val IP_ADDRESS_1 = "trackit1.ddns.net" // Servidor casa Jesús
        const val IP_ADDRESS_2 = "trackit2.ddns.net" // Servidor casa tía mavi
        const val IP_ADDRESS_3 = "trackit3.ddns.net" // Servidor casa Edwin
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        latitudeText = findViewById(R.id.latitudeValue)
        longitudeText = findViewById(R.id.longitudeValue)
        dateText = findViewById(R.id.dateValue)
        timeText = findViewById(R.id.timeValue)
        sendButton_UDP = findViewById(R.id.sendButton_UDP)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (!checkLocationPermissions()) {
            requestLocationPermissions()
        } else {
            startLocationUpdates()
        }

        sendButton_UDP.setOnClickListener {
            if (!isSendingUDP) {
                startSendingUDP()
            } else {
                stopSendingUDP()
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        val provider = location.provider
        val locationTime = location.time

        // Verifica qué proveedor envió la actualización y guarda el tiempo
        when (provider) {
            LocationManager.GPS_PROVIDER -> lastGpsUpdateTime = locationTime
            LocationManager.NETWORK_PROVIDER -> lastNetworkUpdateTime = locationTime
        }

        // Verifica cuál de los dos proveedores tiene la información más reciente
        if (lastGpsUpdateTime > lastNetworkUpdateTime) {
            // Usa los datos del GPS
            if (provider == LocationManager.GPS_PROVIDER) {
                updateUIWithLocation(lat, lon, locationTime, provider)
            }
        } else {
            // Usa los datos de la red
            if (provider == LocationManager.NETWORK_PROVIDER) {
                updateUIWithLocation(lat, lon, locationTime, provider)
            }
        }

        Log.d("LocationUpdate", "Provider: $provider, Lat: $lat, Lon: $lon, Time: $locationTime")
    }

    // Función auxiliar para actualizar la interfaz con los datos de la ubicación
    private fun updateUIWithLocation(lat: Double, lon: Double, locationTime: Long, provider: String) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val TimeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getDefault()
        TimeFormat.timeZone = TimeZone.getDefault()
        val localDate = dateFormat.format(Date(locationTime))
        val localTime = TimeFormat.format(Date(locationTime))
        currentData = "Lat: $lat, Lon: $lon, Date: $localDate, Time: $localTime, Provider: $provider"

        latitudeText.text = "$lat"
        longitudeText.text = "$lon"
        dateText.text = "$localDate"
        timeText.text = "$localTime"

        Log.d("LocationUpdate", "Using data from provider: $provider, Data: $currentData")
    }

    private fun startSendingUDP() {
        isSendingUDP = true
        sendButton_UDP.text = "Stop sending"

        handler.post(object : Runnable {
            override fun run() {
                if (isSendingUDP && currentData.isNotEmpty()) {
                    sendLocationData_UDP(IP_ADDRESS_1)
                    sendLocationData_UDP(IP_ADDRESS_2)
                    sendLocationData_UDP(IP_ADDRESS_3)
                    //Toast.makeText(this@MainActivity, "UDP: data sent", Toast.LENGTH_SHORT).show()
                }
                if (isSendingUDP) {
                    handler.postDelayed(this, sendInterval)
                }
            }
        })
    }

    private fun stopSendingUDP() {
        isSendingUDP = false
        sendButton_UDP.text = "Start sending"
        handler.removeCallbacksAndMessages(null)
    }

    private fun sendLocationData_UDP(ipAddress: String) {
        Thread {
            try {
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
                    //Toast.makeText(this, "Error sending data: ${e.message}", Toast.LENGTH_LONG).show()
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
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, this)
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
