import React, { useEffect, useState } from "react";
import {
  View,
  Text,
  Button,
  Alert,
  StyleSheet,
  PermissionsAndroid,
  Platform
} from "react-native";
import * as Tap from "../StripeTapToPay";

// Request necessary Android permissions
async function ensurePermissions() {
  if (Platform.OS !== "android") {
    return true;
  }
  const wanted = [
    PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
    PermissionsAndroid.PERMISSIONS.NFC,
  ].filter(p => !!p);

  const results = await PermissionsAndroid.requestMultiple(wanted);
  const allGranted = Object.values(results).every(
    status => status === PermissionsAndroid.RESULTS.GRANTED
  );
  if (!allGranted) {
    Alert.alert(
      "Permissions Required",
      "Location, Bluetooth, and NFC permissions are required for Tap to Pay."
    );
  }
  return allGranted;
}

export default function POSScreen({ route }) {
  // You can pass a clientSecret via navigation params or fetch from your backend
  const clientSecret = route?.params?.clientSecret;
  const [status, setStatus] = useState("Initializing…");

  useEffect(() => {
    ensurePermissions().then(granted => {
      if (!granted) return;
      // Initialize Tap to Pay
      Tap.initialize()
        .then(() => {
          // Subscribe to events
          Tap.removeAllListeners("tapToPayStarted");
          Tap.removeAllListeners("tapToPayFinished");
          Tap.removeAllListeners("tapToPayCancelled");
          Tap.removeAllListeners("tapToPayError");

          Tap.addListener("tapToPayStarted", () => setStatus("Ready to Tap"));
          Tap.addListener("tapToPayFinished", data => setStatus(`Success: ${data.status}`));
          Tap.addListener("tapToPayCancelled", () => setStatus("Cancelled"));
          Tap.addListener("tapToPayError", err => setStatus(`Error: ${err.message}`));

          setStatus("Initialized");
        })
        .catch(err => {
          Alert.alert("Init error", err.message);
          setStatus("Init error");
        });
    });
    // Clean up on unmount
    return () => {
      [
        "tapToPayStarted",
        "tapToPayFinished",
        "tapToPayCancelled",
        "tapToPayError"
      ].forEach(evt => Tap.removeAllListeners(evt));
    };
  }, []);

  const onTapToPayPress = () => {
    if (!clientSecret) {
      return Alert.alert("Error", "Missing clientSecret for PaymentIntent.");
    }
    setStatus("Waiting for tap…");
    Tap.startTapToPay(clientSecret).catch(e => {
      Alert.alert("Tap-to-Pay failed", e.message);
      setStatus("Error");
    });
  };

  return (
    <View style={styles.container}>
      <Text style={styles.status}>Status: {status}</Text>
      <Button
        title="Tap to Pay"
        onPress={onTapToPayPress}
        disabled={status !== "Initialized" && status !== "Ready to Tap"}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16, justifyContent: "center", backgroundColor: "#fff" },
  status: { fontSize: 16, marginBottom: 12 },
});
