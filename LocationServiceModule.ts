import { useCallback, useEffect, useState } from "react";
import { NativeEventEmitter, NativeModules } from "react-native";

interface LocationService {
  getStatus: () => boolean;
  start: (token: string) => void;
  stop: () => void;
}

export const LocationServiceModule =
  NativeModules.LocationServiceModule as LocationService;
const eventEmitter = new NativeEventEmitter(
  NativeModules.LocationServiceModule
);

export function useLocationService() {
  const [serviceStatus, setServiceStatus] = useState("inactive");
  const [location, setLocation] = useState(null);

  useEffect(() => {
    const statusSub = eventEmitter.addListener(
      "onStatusChanged",
      (isActive) => {
        setServiceStatus(isActive ? "active" : "inactive");
        console.log("Service status changed:", isActive);
      }
    );
    const locationSub = eventEmitter.addListener("onLocationUpdate", (loc) => {
      console.log("Location updated:", loc);
      setLocation(loc);
    });

    return () => {
      statusSub.remove();
      locationSub.remove();
    };
  }, []);

  function start(token: string) {
    LocationServiceModule.start(token);
  }

  function stop() {
    LocationServiceModule.stop();
  }

  return {
    status: serviceStatus,
    location,
    start,
    stop,
  };
}
