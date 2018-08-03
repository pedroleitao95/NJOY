package edu.estgp.pilight;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;

import edu.estgp.njoy.NJoyActivity;

class PilightMonitor extends Thread {
    private BufferedReader bufferedReader = null;
    private NJoyActivity mainActivity;

    public PilightMonitor(NJoyActivity mainActivity, BufferedReader bufferedReader) {
        this.bufferedReader = bufferedReader;
        this.mainActivity = mainActivity;
    }

    public void run() {
        while (true) {
            try {
                String result = bufferedReader.readLine();
                if (!result.isEmpty()) {
                    Log.d("NJOY-PILIGHT", "PILIGHT: " + result);
                    if (result.startsWith("{\"message\":")) {
                        Pilight.devices.clear();
                        try {
                            JSONObject json = new JSONObject(result);
                            JSONArray devices = json.getJSONArray("values");
                            for (int i = 0; i < devices.length(); i++) {
                                JSONObject device = (JSONObject) devices.get(i);
                                int type = device.getInt("type");
                                if (type == 1) {
                                    String deviceName = (String) device.getJSONArray("devices").get(0);
                                    JSONObject values = (JSONObject) device.get("values");
                                    Boolean state = ((String)values.getString("state")).equals("on");

                                    Pilight.devices.put(deviceName, state);
                                    mainActivity.onInitialDeviceStatus(deviceName, state);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (result.startsWith("{\"origin\":\"update\"")) {
                        try {
                            JSONObject device = new JSONObject(result);
                            int type = device.getInt("type");
                            if (type == 1) {
                                String deviceName = (String) device.getJSONArray("devices").get(0);
                                JSONObject values = (JSONObject) device.get("values");
                                Boolean state = ((String)values.getString("state")).equals("on");
                                Pilight.devices.put(deviceName, state);

                                mainActivity.onDeviceStatusChanged(deviceName, state);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                Log.e("NJOY-PILIGHT", "PILIGHT MONITOR", e);
            }
        }
    }
}