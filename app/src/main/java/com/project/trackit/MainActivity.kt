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
    private lateinit var dateTimeText: TextView
    private lateinit var sendButton_UDP: Button
    private lateinit var sendButton_TCP: Button

    private var currentData: String = ""
    private var isSendingUDP = false
    private var isSendingTCP = false
    private val handler = Handler(Looper.getMainLooper())
    private val sendInterval: Long = 10000 // 10 segundos

    companion object {
        const val LOCATION_PERMISSION_CODE = 101
        const val TCP_PORT = 60000
        const val UDP_PORT = 60001
        const val IP_ADDRESS_1 = "trackit1.ddns.net" // Servidor casa Jesús
        const val IP_ADDRESS_2 = "trackit2.ddns.net" // Servidor casa tía mavi
        const val IP_ADDRESS_3 = "trackit3.ddns.net" // Servidor casa amigo orlando
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
            if (!isSendingUDP) {
                startSendingUDP()
            } else {
                stopSendingUDP()
            }
        }

        sendButton_TCP.setOnClickListener {
            if (!isSendingTCP) {
                startSendingTCP()
            } else {
                stopSendingTCP()
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        dateTimeFormat.timeZone = TimeZone.getDefault()
        val localDateTime = dateTimeFormat.format(Date(location.time))
        currentData = "Lat: $lat, Lon: $lon, Date/Time: $localDateTime"

        latitudeText.text = "$lat"
        longitudeText.text = "$lon"
        dateTimeText.text = "$localDateTime"

        Log.d("LocationUpdate", "Provider: ${location.provider}, Data: $currentData")
    }

    private fun startSendingUDP() {
        isSendingUDP = true
        sendButton_UDP.text = "Stop UDP sending"
        sendButton_TCP.isEnabled = false

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
        sendButton_UDP.text = "Start UDP sending"
        sendButton_TCP.isEnabled = true
        handler.removeCallbacksAndMessages(null)
    }

    private fun startSendingTCP() {
        isSendingTCP = true
        sendButton_TCP.text = "Stop TCP sending"
        sendButton_UDP.isEnabled = false

        handler.post(object : Runnable {
            override fun run() {
                if (isSendingTCP && currentData.isNotEmpty()) {
                    sendLocationData_TCP(IP_ADDRESS_1)
                    sendLocationData_TCP(IP_ADDRESS_2)
                    sendLocationData_TCP(IP_ADDRESS_3)
                    //Toast.makeText(this@MainActivity, "TCP: data sent", Toast.LENGTH_SHORT).show()
                }
                if (isSendingTCP) {
                    handler.postDelayed(this, sendInterval)
                }
            }
        })
    }

    private fun stopSendingTCP() {
        isSendingTCP = false
        sendButton_TCP.text = "Start TCP sending"
        sendButton_UDP.isEnabled = true
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

    private fun sendLocationData_TCP(ipAddress: String) {
        Thread {
            try {
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this)
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, this)
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
