import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import type { ViewProps } from 'react-native';

interface NativeProps extends ViewProps {
  origin?: [number, number];
  destination?: [number, number];
  waypoints?: number[][];
  color?: string;
}

export default codegenNativeComponent<NativeProps>('MapboxNavigationView');
