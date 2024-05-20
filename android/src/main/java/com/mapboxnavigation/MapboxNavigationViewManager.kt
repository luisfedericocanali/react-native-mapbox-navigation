package com.mapboxnavigation

import android.content.pm.PackageManager
import com.mapbox.geojson.Point
import android.util.Log
import com.facebook.react.uimanager.SimpleViewManager
import android.graphics.Color
import com.mapbox.common.MapboxOptions
import com.facebook.react.bridge.ReadableArray
import com.mapbox.common.TileStore
import com.facebook.react.common.MapBuilder
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class MapboxNavigationViewManager(private var mCallerContext: ReactApplicationContext)  : SimpleViewManager<MapboxNavigationView>() {
    private var accessToken: String? = null
    init {
        mCallerContext.runOnUiQueueThread {
           try {
              val app = mCallerContext.packageManager.getApplicationInfo(mCallerContext.packageName, PackageManager.GET_META_DATA)
              val bundle = app.metaData
              accessToken = bundle.getString("MAPBOX_ACCESS_TOKEN")
              accessToken?.let {
                  MapboxOptions.accessToken = it
              } ?: Log.e("MapboxNavManager", "Access token not found")
          } catch (e: PackageManager.NameNotFoundException) {
              e.printStackTrace()
          }
        }
      }
    override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Map<String, String>>? {
        return MapBuilder.of<String, Map<String, String>>(
          "onLocationChange", MapBuilder.of("registrationName", "onLocationChange"),
          "onError", MapBuilder.of("registrationName", "onError"),
          "onCancelNavigation", MapBuilder.of("registrationName", "onCancelNavigation"),
          "onArrive", MapBuilder.of("registrationName", "onArrive"),
          "onRouteProgressChange", MapBuilder.of("registrationName", "onRouteProgressChange"),
        )
      }


    override fun getName(): String = NAME

    override fun createViewInstance(context: ThemedReactContext): MapboxNavigationView {
        return MapboxNavigationView(context, accessToken)
    }
    @ReactProp(name = "origin")
      fun setOrigin(view: MapboxNavigationView, sources: ReadableArray?) {
        if (sources == null) {
          view.setOrigin(null)
          return
        }
        view.setOrigin(Point.fromLngLat(sources.getDouble(0), sources.getDouble(1)))
      }
      @ReactProp(name = "shouldSimulateRoute")
       fun setShouldSimulateRoute(view: MapboxNavigationView, shouldSimulateRoute: Boolean) {
         view.setShouldSimulateRoute(shouldSimulateRoute)
       }

      @ReactProp(name = "destination")
      fun setDestination(view: MapboxNavigationView, sources: ReadableArray?) {
        if (sources == null) {
          view.setDestination(null)
          return
        }
        view.setDestination(Point.fromLngLat(sources.getDouble(0), sources.getDouble(1)))
      }
    @ReactProp(name = "color")
     fun setColor(view: MapboxNavigationView?, color: String?) {
        color?.let {
            try {
                // Parse the color string to an Int and set it
                val parsedColor = Color.parseColor(it)
                view?.setBackgroundColor(parsedColor)
            } catch (e: IllegalArgumentException) {
                // Handle potential color parsing errors
                Log.e("MapboxNavigationViewManager", "Error parsing color string", e)
            }
        }
    }

    companion object {
        const val NAME = "MapboxNavigationView"
    }
}
