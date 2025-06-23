import React, { useEffect, useState } from "react";
import {
  View,
  Text,
  Button,
  FlatList,
  Alert,
  StyleSheet,
  PermissionsAndroid,
  Platform
} from "react-native";
import * as Tap from "../StripeTapToPay";

async function ensurePermissions() {
  if (Platform.OS !== "android") {
    return true;
  }

  // Build an array of the permission constants you want to ask for...
  const wanted = [
    PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
    PermissionsAndroid.PERMISSIONS.NFC,
  ];

  // ...but drop any that are undefined / null
  const toRequest = wanted.filter(p => !!p);

  // Now request only the valid ones
  const results = await PermissionsAndroid.requestMultiple(toRequest);

  // See if all were granted
  const allGranted = Object.values(results).every(
    status => status === PermissionsAndroid.RESULTS.GRANTED
  );
  if (!allGranted) {
    Alert.alert(
      "Permissions Required",
      "Location, Bluetooth, and NFC (if available) permissions are required for Tap-to-Pay."
    );
  }
  return allGranted;
}

export default function POSScreen({ route }) {
  // fallback if route.params is undefined
  const locationId = route?.params?.locationId ?? "tml_GFZlfAmzFCGtcQ";
  const [readers, setReaders] = useState([]);
  const [connectedReaderId, setConnectedReaderId] = useState(null);
  const [isProcessing, setProcessing] = useState(false);

  useEffect(() => {
    // 1) Ask for Android permissions, then initialize & discover
    ensurePermissions().then(granted => {
      if (!granted) return;
      Tap.initialize()
        .then(() => Tap.discoverReaders(locationId, setReaders))
        .catch(err => Alert.alert("Init error", err.message));
    });
  }, [locationId]);

  const handleConnect = () => {
    setProcessing(true);
    Tap.connectReader(locationId)  // Only pass locationId
      .then(() => {
        setConnectedReaderId("local-mobile");  // You can hardcode it for now
        Alert.alert("Connected", "Reader connected");
      })
      .catch(err => Alert.alert("Connect error", err.message))
      .finally(() => setProcessing(false));
  };

  const handlePayment = () => {
    if (!connectedReaderId) {
      return Alert.alert("No Reader", "Please connect first");
    }
    setProcessing(true);
    Tap.collectAndProcessPayment(1.0, "usd", true)
      .then(() => Alert.alert("Success", "Payment complete"))
      .catch(err => Alert.alert("Payment error", err.message))
      .finally(() => setProcessing(false));
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Available Readers</Text>
      <FlatList
        data={readers}
        keyExtractor={(item, index) => `reader-${index}`}
        renderItem={({ item }) => (
          <View style={styles.row}>
            <Text style={styles.label}>{item.label}</Text>
            <Button
              title={connectedReaderId ? "Connected" : "Connect"}
              disabled={connectedReaderId != null || isProcessing}
              onPress={handleConnect}
            />
          </View>
        )}
        ListEmptyComponent={<Text style={styles.empty}>No readers yet</Text>}
      />
      <View style={styles.footer}>
        <Button
          title="Tap to Pay $1.00"
          disabled={!connectedReaderId || isProcessing}
          onPress={handlePayment}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex:1, padding:16, backgroundColor:"#fff" },
  title: { fontSize:18, fontWeight:"600", marginBottom:12 },
  row: {
    flexDirection:"row",
    justifyContent:"space-between",
    paddingVertical:8,
    borderBottomWidth:1,
    borderBottomColor:"#eee"
  },
  label: { fontSize:16 },
  empty: { textAlign:"center", color:"#888", marginTop:20 },
  footer: { marginTop:24 },
});
