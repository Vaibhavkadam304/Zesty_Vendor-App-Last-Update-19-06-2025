import { NativeModules, NativeEventEmitter, Platform } from "react-native";
const { StripeTapToPay } = NativeModules;
const emitter = new NativeEventEmitter(StripeTapToPay);

export function initialize() {
  if (Platform.OS !== "android") {
    return Promise.reject(new Error("Tap to Pay only on Android"));
  }
  return StripeTapToPay.initialize();
}

export function startTapToPay(clientSecret) {
  if (Platform.OS !== "android") {
    return Promise.reject(new Error("Tap to Pay only on Android"));
  }
  return StripeTapToPay.startTapToPay(clientSecret);
}

export function addListener(event, handler) {
  return emitter.addListener(event, handler);
}

export function removeAllListeners(event) {
  emitter.removeAllListeners(event);
}
