import * as React from 'react';

import {
  NativeEventEmitter,
  NativeModules,
  PermissionsAndroid,
  StyleSheet,
  View,
} from 'react-native';
import { MapboxNavigationView } from 'react-native-mapbox-navigation';
import { useEffect } from 'react';

export default function App() {
  const requestCameraPermission = async () => {
    try {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        {
          title: 'Cool Photo App Camera Permission',
          message:
            'Cool Photo App needs access to your camera ' +
            'so you can take awesome pictures.',
          buttonNeutral: 'Ask Me Later',
          buttonNegative: 'Cancel',
          buttonPositive: 'OK',
        }
      );
      if (granted === PermissionsAndroid.RESULTS.GRANTED) {
        console.log('You can use the camera');
      } else {
        console.log('Camera permission denied');
      }
    } catch (err) {
      console.warn(err);
    }
  };
  useEffect(() => {
    requestCameraPermission();
  });
  useEffect(() => {
    const eventEmitter = new NativeEventEmitter(NativeModules.ToastExample);
    let eventListener = eventEmitter.addListener('onError', (event) => {
      console.log(event.error); // "someValue"
    });
    let arrivalListener = eventEmitter.addListener('onArrive', (event) => {
      console.log(event, 'arrival'); // "someValue"
    });

    // Removes the listener once unmounted
    return () => {
      arrivalListener.remove();
      eventListener.remove();
    };
  }, []);
  return (
    <View style={styles.container}>
      <MapboxNavigationView
        origin={[-58.46, -34.58]}
        destination={[-58.49, -34.65]}
        shouldSimulateRoute
        color="#32a852"
        style={styles.box}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    flex: 1,
    height: '100%',
    width: '100%',
  },
});
