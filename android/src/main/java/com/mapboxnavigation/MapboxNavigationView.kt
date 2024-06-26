package com.mapboxnavigation

import android.view.LayoutInflater
import android.content.Context
import android.widget.FrameLayout
import com.mapbox.maps.MapView
import android.location.Location as AndroidLocation
import com.mapbox.common.location.Location
import android.location.LocationManager
import com.mapbox.maps.Style
import com.mapbox.maps.ImageHolder
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.mapboxnavigation.databinding.NavigationViewBinding
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.mapbox.navigation.core.MapboxNavigationProvider
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.mapboxnavigation.R
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.formatter.DistanceFormatterOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.replay.route.ReplayProgressObserver
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.tripdata.progress.api.MapboxTripProgressApi
import com.mapbox.navigation.tripdata.progress.model.DistanceRemainingFormatter
import com.mapbox.navigation.tripdata.progress.model.EstimatedTimeToArrivalFormatter
import com.mapbox.navigation.tripdata.progress.model.TimeRemainingFormatter
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.tripdata.speedlimit.api.MapboxSpeedInfoApi
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.voice.api.MapboxSpeechApi
import com.mapbox.navigation.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.model.SpeechError
import com.mapbox.navigation.voice.model.SpeechValue
import com.mapbox.navigation.voice.model.SpeechVolume
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxNavigationView(context: ReactContext, private val accessToken: String?) : FrameLayout(context) {
    private var origin: Point? = null
    private var waypoints: List<Point>? = null
    private var destination: Point? = null
    private lateinit var mapboxNavigation: MapboxNavigation
    private var shouldSimulateRoute = false
    private val binding: NavigationViewBinding = NavigationViewBinding.inflate(LayoutInflater.from(context), this, true)
    private val maneuverApi by lazy {
        MapboxManeuverApi(MapboxDistanceFormatter(
            DistanceFormatterOptions.Builder(context).build()
        ))
    }
    private val locale = Locale.getDefault()
    private lateinit var speechApi: MapboxSpeechApi
    private val replayRouteMapper = ReplayRouteMapper()
    private val routeCoordinates = listOf(
        Point.fromLngLat(-58.46, -34.58),
        Point.fromLngLat(-58.49, -34.65),
    )
    private val navigationLocationProvider = NavigationLocationProvider()
    private val options: MapboxRouteLineViewOptions by lazy {
        MapboxRouteLineViewOptions.Builder(context)
            .routeLineBelowLayerId("road-label-navigation")
            .build()
    }
    private val routeLineView by lazy {
        MapboxRouteLineView(options)
    }
    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(MapboxRouteLineApiOptions.Builder().build())
    }
    private val tripProgressFormatter: TripProgressUpdateFormatter by lazy {
        val distanceFormatterOptions =
            DistanceFormatterOptions.Builder(context).build()
        TripProgressUpdateFormatter.Builder(context)
            .distanceRemainingFormatter(DistanceRemainingFormatter(distanceFormatterOptions))
            .timeRemainingFormatter(TimeRemainingFormatter(context))
            .estimatedTimeToArrivalFormatter(EstimatedTimeToArrivalFormatter(context))
            .build()
    }
    private val tripProgressApi: MapboxTripProgressApi by lazy {
        MapboxTripProgressApi(tripProgressFormatter)
    }
    private val speedInfoApi: MapboxSpeedInfoApi by lazy {
        MapboxSpeedInfoApi()
    }
    private val distanceFormatterOptions: DistanceFormatterOptions by lazy {
           DistanceFormatterOptions.Builder(context).build()
       }
   init {
   onCreate()
   }
    override fun requestLayout() {
        super.requestLayout()
        post(measureAndLayout)
    }
    private val measureAndLayout = Runnable {
        measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
        layout(left, top, right, bottom)
    }


    private fun sendErrorToReact(error: String?) {
         val reactContext = context as ReactContext
         val event = Arguments.createMap()
         event.putString("error", error)
         reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java).emit("onError", event)
     }
    @SuppressLint("MissingPermission")
    fun onCreate() {
        mapboxNavigation = if (MapboxNavigationProvider.isCreated()) {
            MapboxNavigationProvider.retrieve()
        } else if (shouldSimulateRoute) {
            MapboxNavigationProvider.create(
                NavigationOptions.Builder(context)
                    .build()
            )
        } else {
            MapboxNavigationProvider.create(
                NavigationOptions.Builder(context)
                    .build()
            )
        }
        binding.mapView.location.apply {
            setLocationProvider(navigationLocationProvider)
            locationPuck = LocationPuck2D(
                bearingImage = ImageHolder.Companion.from(
                    R.drawable.mapbox_navigation_puck_icon
                )
            )
            puckBearingEnabled = true
            enabled = true
        }
        binding.mapView.mapboxMap.loadStyle(NavigationStyles.NAVIGATION_DAY_STYLE) {
            startRoute()
        }
    }
    private fun startRoute() {
          mapboxNavigation.registerRoutesObserver(routesObserver)
          mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
          mapboxNavigation.registerLocationObserver(locationObserver)
          mapboxNavigation.startTripSession()
          if (origin == null || destination == null) {
             sendErrorToReact("origin and destination are required")
             return
          }
          val coordinatesList = mutableListOf<Point>()
          this.origin?.let { coordinatesList.add(it) }
          this.waypoints?.let { coordinatesList.addAll(it) }
          this.destination?.let { coordinatesList.add(it) }
          fetchRoute(coordinatesList)
        }
     private fun setNavigationRoutes(routes: List<NavigationRoute>) {
        mapboxNavigation.setNavigationRoutes(routes)
        binding.tripProgressView.isVisible = false
    }

    private fun updateCamera(point: Point, bearing: Double? = null) {
        binding.mapView.camera.flyTo(
            CameraOptions.Builder()
                .center(point)
                .bearing(bearing)
                .zoom(17.0)
                .pitch(0.0)
                .build(),
            MapAnimationOptions.mapAnimationOptions {
                duration(1000L)
            }
        )
    }
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
        mapboxNavigation.stopTripSession()
        mapboxNavigation.onDestroy()
    }
    private val routesObserver: RoutesObserver = RoutesObserver { routeUpdateResult ->
        routeLineApi.setNavigationRoutes(
            routeUpdateResult.navigationRoutes
        ) { value ->
            binding.mapView.mapboxMap.style?.apply {
                routeLineView.renderRouteDrawData(this, value)
            }
        }
    }
    private fun setCameraPositionToOrigin() {
        val startingLocation = AndroidLocation(LocationManager.GPS_PROVIDER)
        sendErrorToReact("here2 $startingLocation")
    }
    private val locationObserver = object : LocationObserver {

        override fun onNewRawLocation(rawLocation: Location) {
            // Not implemented in this example. However, if you want you can also
            // use this callback to get location updates, but as the name suggests
            // these are raw location updates which are usually noisy.
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            navigationLocationProvider.changePosition(
                enhancedLocation,
                locationMatcherResult.keyPoints,
            )
            updateCamera(
                Point.fromLngLat(
                    enhancedLocation.longitude,
                    enhancedLocation.latitude
                ),
                enhancedLocation.bearing
            )
            val info = speedInfoApi.updatePostedAndCurrentSpeed(
                locationMatcherResult,
                distanceFormatterOptions
            )
            if (info != null) {
                binding.speedLimitView.isVisible = true
                binding.speedLimitView.requestLayout()
                binding.speedLimitView.invalidate()
                binding.speedLimitView.render(info)
            }
        }
    }
    private val arrivalObserver = object : ArrivalObserver {

        override fun onWaypointArrival(routeProgress: RouteProgress) {
            // do something when the user arrives at a waypoint
        }

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            // do something when the user starts a new leg
        }

        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            val reactContext = context as ReactContext
            val event = Arguments.createMap()
            event.putString("onArrive", "")
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java).emit("onArrive", event)
        }
    }

    private val routeProgressObserver = RouteProgressObserver { progress ->
        val tripProgress = tripProgressApi.getTripProgress(progress)
        binding.tripProgressView.render(tripProgress)
        val maneuvers = maneuverApi.getManeuvers(progress)
        maneuvers.fold(
            { error ->
                Toast.makeText(
                    context,
                    error.errorMessage,
                    Toast.LENGTH_SHORT
                ).show()
            },
            {
                binding.maneuverView.visibility = View.VISIBLE
                binding.maneuverView.renderManeuvers(maneuvers)
            }
        )
    }

      override fun onAttachedToWindow() {
          super.onAttachedToWindow()
          mapboxNavigation.registerRoutesObserver(routesObserver)
          mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
          mapboxNavigation.registerLocationObserver(locationObserver)
          mapboxNavigation.registerArrivalObserver(arrivalObserver)
          startRoute()
      }
    fun setOrigin(origin: Point?) {
        this.origin = origin
    }

    fun setDestination(destination: Point?) {
        this.destination = destination
    }
    fun setWaypoints(waypoints: List<Point>) {
            this.waypoints = waypoints
    }
    fun setShouldSimulateRoute(shouldSimulateRoute: Boolean) {
            this.shouldSimulateRoute = shouldSimulateRoute
        }
    private fun fetchRoute(coordinates: List<Point>) {
            mapboxNavigation.requestRoutes(
                RouteOptions.builder()
                    .applyDefaultNavigationOptions()
                    .alternatives(false)
                    .coordinatesList(coordinates)
                    .steps(true)
                    .build(),

                object : NavigationRouterCallback {
                    override fun onRoutesReady(
                        routes: List<NavigationRoute>,
                        @RouterOrigin routerOrigin: String
                    ) {
                        Log.d("MapboxNavigationView", "Routes fetched successfully")
                        sendErrorToReact("Route request $routes")
                         sendErrorToReact("here2 $origin $destination")
                        setNavigationRoutes(routes)
                    }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    Log.e("MapboxNavigationView", "Route request failed: $reasons")
                    sendErrorToReact("Route request failed: $reasons")
                }

                override fun onCanceled(
                    routeOptions: RouteOptions,
                    @RouterOrigin routerOrigin: String
                ) {
                    Log.d("MapboxNavigationView", "Route request canceled")
                    sendErrorToReact("Route request canceled")
                }
            }
        )
    }
}
