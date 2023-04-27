package com.example.naurtdemoapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.naurt.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


class MainActivity : AppCompatActivity() {


    private lateinit var naurt: Naurt

    private lateinit var naurtAnalyticsButton: Button
    private lateinit var naurtLocationOutputText: TextView
    private lateinit var naurtValidatedText: TextView


    private val naurtPermissions =
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
        )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.naurtLocationOutputText = findViewById(R.id.naurtLocationOutputText)
        this.naurtValidatedText = findViewById(R.id.validatedText)
        this.naurtAnalyticsButton = findViewById(R.id.analyticsButton)

        // We can't start Naurt until permissions have been successfully granted.
        if (!hasPermissions()) {
            requestPermissions()
        } else{
            setupNaurt()
        }
    }


    private fun hasPermissions(): Boolean {

        for (permission in this.naurtPermissions) {
            if (ActivityCompat.checkSelfPermission(this.applicationContext, permission) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("Naurt", "does not have:${permission}")
                return false
            }
        }

        return true
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, this.naurtPermissions, 60);
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 60 && grantResults.all { it == 0 }) {
            Log.d("Naurt Example App", "Permissions successfully granted.")
            setupNaurt()
        } else{
            Log.e("Naurt Example App", "Naurt does not have the correct permissions nothing will happen.")
        }
    }



    private fun setupNaurt() {

        // Instantiate the naurt class.

        this.naurt = Naurt(
            BuildConfig.API_KEY,
            applicationContext,
            NaurtEngineType.Service // Change to Standalone if your app already runs a foreground service.
        )

        // Location callback
        this.naurt.on(
            NaurtEvents.NEW_LOCATION,
            NaurtEventListener<NaurtNewLocationEvent> {

                // Dispatch UI update to main thread.
                CoroutineScope(Dispatchers.Main).launch {

                    val loc = it.newPoint

                    // Rounding numbers for neater strings.
                    val roundedLat = String.format("%.6f", loc.latitude)
                    val roundedLon = String.format("%.6f", loc.longitude)
                    val roundedSpeed = String.format("%.2f", loc.speed)
                    val roundedAltitude = String.format("%.0f", loc.altitude)
                    val roundedHeading = String.format("%.1f", loc.heading)


                    naurtLocationOutputText.text =
                        "Lat: ${roundedLat}		Lon: ${roundedLon}		Alt: ${roundedAltitude}\n\nSpeed: ${roundedSpeed}		Heading: ${roundedHeading}\n\n Motion type: ${loc.motionFlag}		Environment type: ${loc.environmentFlag}\n\nBackground Status: ${loc.backgroundStatus}\n\nLocation origin: ${loc.locationOrigin}\n\nisMocked: ${loc.isMocked}    isMockedPrevented: ${loc.isMockedPrevented}"
                }
            }
        )

        // Validation callback.
        this.naurt.on(
            NaurtEvents.IS_VALIDATED,
            NaurtEventListener<NaurtIsValidatedEvent> {

                when (it.isValidated) {
                    NaurtValidationStatus.Valid -> {
                        naurtValidatedText.visibility = AdapterView.INVISIBLE
                    }
                    NaurtValidationStatus.ValidNoDataTransfer -> {
                        naurtValidatedText.visibility = AdapterView.INVISIBLE
                    }
                    NaurtValidationStatus.Invalid -> {
                        naurtValidatedText.text = "INVALID API KEY. GNSS/FUSED PROVIDED" // If key is invalid GNSS will be passed through.
                    }
                    NaurtValidationStatus.NotYetValidated -> {}
                }

            }
        )


        // Set up analytics sessions based on button press. These sessions are used to send Naurt
        // metadata about certain portions of data for later analysis.

        this.naurtAnalyticsButton.setOnClickListener{

            if (naurt.getMetadata() != null){

                val nullMeta: JsonObject? = null
                naurt.updateMetadata(nullMeta)
                naurtAnalyticsButton.text = "ADD METADATA"
                naurtAnalyticsButton.setBackgroundColor(Color.GREEN)

            } else{

                val element = buildJsonObject {
                    put("example_metadata", "Naurt Example App!")
                }

                naurt.updateMetadata(element)



                naurtAnalyticsButton.text = "REMOVE METADATA"
                naurtAnalyticsButton.setBackgroundColor(Color.RED)
            }
        }

    }
}