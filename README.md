# react-native-mapbox-navigation

Navigating using mapbox V3 core framework (android only for now)

## Installation

```sh
npm install react-native-mapbox-navigation
```

## Usage
Dentro de la carpeta android:
en gradle.properties añadir  {
 ```
   MAPBOX_DOWNLOADS_TOKEN=SECRET_TOKEN_HERE
```
}.

en gradle.build añadir {
```gradle
allprojects {
    repositories {
        maven {
            url 'https://api.mapbox.com/downloads/v2/releases/maven'
            authentication {
                basic(BasicAuthentication)
            }
            credentials {
                username = "mapbox"
                password = project.properties['MAPBOX_DOWNLOADS_TOKEN'] ?: ""
            }
        }
    }
}
```
}.

En  `android/app/src/main/AndroidManifest.xml` añadir

```xml
<!-- Tiene que estar dentro de application -->
<meta-data android:name="MAPBOX_ACCESS_TOKEN"
    android:value="PUBLIC_TOKEN_HERE" />
```

```js
import { MapboxNavigationView } from "react-native-mapbox-navigation";

// ...

<MapboxNavigationView origin={[]} destination={[]} waypoints={[]} />
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
