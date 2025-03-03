import { Button, View, Text, Dimensions } from "react-native";
import { useLocationService } from "./LocationServiceModule";

const { height, width } = Dimensions.get("window");

export default function App() {
  const { status, start, stop } = useLocationService();

  return (
    <View
      style={{
        paddingVertical: height * 0.2,
        paddingHorizontal: width * 0.1,
        gap: height * 0.1,
      }}
    >
      <Text>
        Location Service Status:{" "}
        <Text style={{ fontWeight: "600" }}>{status}</Text>
      </Text>
      <Button title="Start Location Service" onPress={() => start("123123-tracking-token")} />
      <Button title="Stop Location Service" onPress={stop} />
    </View>
  );
}
