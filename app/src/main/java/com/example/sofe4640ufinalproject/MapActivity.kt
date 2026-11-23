package com.example.sofe4640ufinalproject

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import java.util.Locale

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var etMapSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "SOFE4640UFinalProject/1.0 (Educational Project)"

        enableEdgeToEdge()
        setContentView(R.layout.activity_map)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mapView = findViewById(R.id.mapView)
        val osmDeTileSource = XYTileSource(
            "OpenStreetMapDe",
            0, 20, 256, ".png",
            arrayOf(
                "https://tile.openstreetmap.de/"
            ),
            "© OpenStreetMap contributors"
        )
        mapView.setTileSource(osmDeTileSource)
        mapView.setMultiTouchControls(true)

        val mapController = mapView.controller
        mapController.setZoom(9.0)
        val defaultPoint = GeoPoint(43.6532, -79.3832) // Toronto / Ontario
        mapController.setCenter(defaultPoint)

        etMapSearch = findViewById(R.id.etMapSearch)
        etMapSearch.doOnTextChanged { _, _, _, _ -> loadListingsOnMap() }

        setupBottomNav()
        loadListingsOnMap()
    }

    override fun onResume() {
        super.onResume()
        ColorBlindHelper.apply(this)
        mapView.onResume()
        loadListingsOnMap()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    private fun loadListingsOnMap() {
        val query = etMapSearch.text.toString().trim()
        ListingStore.loadAll(this) { allListings ->
            val listings = if (query.isEmpty()) {
                allListings
            } else {
                allListings.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.category.contains(query, ignoreCase = true) ||
                            it.location.contains(query, ignoreCase = true)
                }
            }

            mapView.overlays.clear()

            // Map events receiver to hide popups when clicking off of markers
            val mapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    InfoWindow.closeAllInfoWindowsOn(mapView)
                    return false
                }
                override fun longPressHelper(p: GeoPoint?): Boolean {
                    return false
                }
            }
            mapView.overlays.add(MapEventsOverlay(mapEventsReceiver))

            val geocoder = Geocoder(this, Locale.getDefault())
            var firstPoint: GeoPoint? = null

            for (listing in listings) {
                val locStr = listing.location.trim()
                if (locStr.isBlank()) continue

                val point = parseLocationToGeoPoint(locStr, geocoder)
                if (point != null) {
                    if (firstPoint == null) firstPoint = point

                    val marker = Marker(mapView)
                    marker.position = point
                    marker.title = listing.title
                    marker.snippet = "${listing.price} - ${listing.category}<br/>${listing.location}"
                    marker.icon = ContextCompat.getDrawable(this, R.drawable.ic_dollar)
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.setOnMarkerClickListener { m, mv ->
                        if (m.isInfoWindowShown) {
                            // Second click: open listing menu/details
                            val intent = Intent(this, ListingDetailActivity::class.java).apply {
                                putExtra("listingId", listing.id)
                                putExtra("title", listing.title)
                                putExtra("description", listing.description)
                                putExtra("price", listing.price)
                                putExtra("category", listing.category)
                                putExtra("isSold", listing.isSold)
                                putExtra("mediaUri", listing.mediaUri)
                                putExtra("mediaType", listing.mediaType)
                                putExtra("ownerId", listing.ownerId)
                                putExtra("location", listing.location)
                            }
                            startActivity(intent)
                        } else {
                            // First click: show popup / info window
                            InfoWindow.closeAllInfoWindowsOn(mv)
                            m.showInfoWindow()
                            mv.controller.animateTo(m.position)
                        }
                        true
                    }
                    mapView.overlays.add(marker)
                }
            }
            mapView.invalidate()
            if (firstPoint != null && query.isNotEmpty()) {
                mapView.controller.setCenter(firstPoint)
            }
        }
    }

    private fun parseLocationToGeoPoint(loc: String, geocoder: Geocoder): GeoPoint? {
        val parts = loc.split(",")
        if (parts.size == 2) {
            val lat = parts[0].trim().toDoubleOrNull()
            val lon = parts[1].trim().toDoubleOrNull()
            if (lat != null && lon != null) {
                return GeoPoint(lat, lon)
            }
        }
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(loc, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                return GeoPoint(address.latitude, address.longitude)
            }
        } catch (_: Exception) {
            // ignore
        }
        return null
    }

    private fun setupBottomNav() {
        findViewById<Button>(R.id.btnMessages)?.setOnClickListener {
            startActivity(Intent(this, Messages::class.java))
            finish()
        }
        findViewById<Button>(R.id.btnMyListings)?.setOnClickListener {
            startActivity(Intent(this, myListings::class.java))
            finish()
        }
        findViewById<Button>(R.id.btnHome)?.setOnClickListener {
            startActivity(Intent(this, Homepage::class.java))
            finish()
        }
        findViewById<Button>(R.id.btnMap)?.setOnClickListener {
            // Already on Map
        }
        findViewById<Button>(R.id.btnPost)?.setOnClickListener {
            startActivity(Intent(this, postCreation::class.java))
        }
    }
}
