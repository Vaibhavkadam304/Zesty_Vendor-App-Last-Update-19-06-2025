import { NativeModules, NativeEventEmitter, Platform } from "react-native";

const { StripeTapToPay } = NativeModules;
const emitter = new NativeEventEmitter(StripeTapToPay);

/**
 * Initialize the native module. No args.
 */
export function initialize() {
  if (Platform.OS !== "android") {
    return Promise.reject(new Error("StripeTapToPay is Android-only"));
  }
  return StripeTapToPay.initialize();
}

/**
 * Discover on-device readers.
 * @param locationId your Stripe Terminal location ID
 * @param onUpdate callback(readersArray)
 */
export function discoverReaders(locationId, onUpdate) {
  emitter.removeAllListeners("readersDiscovered");
  emitter.addListener("readersDiscovered", onUpdate);
  return StripeTapToPay.discoverReaders(locationId);
}

/** Connect to a reader by its ID */
export function connectReader(locationId) {
  if (!locationId) {
    return Promise.reject(new Error("Location ID is required"));
  }
  return StripeTapToPay.connectReader(locationId);
}

/** Collect and process a payment in one go */
export function collectAndProcessPayment(
  amount,
  currency = "usd",
  skipTipping = true
) {
  return StripeTapToPay.collectAndProcessPayment(amount, currency, skipTipping);
}
