package edu.estgp.pilight;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

import edu.estgp.njoy.NJoyActivity;

public class Pilight {
    private static int port = 0;
    private static String server = null;
    private static Socket socket = null;
    private static PrintStream printStream = null;
    private static BufferedReader bufferedReader = null;
    private static boolean isConnected = false;
    private static PilightMonitor pilitghThread = null;

    public static HashMap<String, Boolean> devices = new HashMap<String, Boolean>();

    public static void connect(final NJoyActivity mainActivity) {
        if (isConnected) return;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Socket socket = null;

                if (port == 0 || server == null) {
                    if (!findPilight()) {
                        return;
                    }
                }

                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(server, port), 1000);

                    try {
                        printStream = new PrintStream(socket.getOutputStream(), false);
                        printStream.print("{\"action\":\"identify\",\"options\":{\"core\": 0,\"receiver\":0,\"config\": 1,\"forward\": 0}}\n");
                        printStream.flush();

                        bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"), 1025);
                        String result = bufferedReader.readLine();
                        isConnected = result.contains("success");

                        if (isConnected) {
                            //Log.d("NJOY-PILIGHT","request values...");
                            printStream.print("{\"action\": \"request values\"}\n");
                            start(mainActivity);
                        }

                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        Log.d("NJOY-PILIGHT", "failed to write messages to server");
                    }
                }
                catch (UnknownHostException e) {
                    Log.d("NJOY-PILIGHT", "don't know about host");
                }
                catch (IOException e) {
                    Log.d("NJOY-PILIGHT", "could not get I/O for connection");
                }
            }
        });

        thread.start();
    }

    public static boolean isConnected() {
        return isConnected;
    }

    public static boolean start(NJoyActivity mainActivity) {
        if (!isConnected) {
            return false;
        }

        if (pilitghThread == null) {
            pilitghThread = new PilightMonitor(mainActivity, Pilight.bufferedReader);
        }
        pilitghThread.start();

        return true;
    }

    public static boolean stop() {
        if (pilitghThread != null) {
            pilitghThread.stop();
            return true;
        }

        return false;
    }


    public static boolean toogleDevice(final String device) {
        if (!isConnected) {
            return false;
        }

        final Boolean state = devices.get(device);
        if (state == null) {
            return false;
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                String action = "{\"action\": \"control\", \"code\": { \"device\": \"" + device + "\", \"state\": \"" + (!state ? "on" : "off") + "\"}}\n";
                printStream.print(action);
                printStream.flush();

                String result = null;
                try {
                    result = bufferedReader.readLine();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

        return true;
    }


    private static boolean findPilight() {

        /*
        String line = null;
        DatagramSocket ssdp = null;

        String msg = "M-SEARCH * HTTP/1.1\r\n"
                + "Host:239.255.255.250:1900\r\n"
                + "ST:urn:schemas-upnp-org:service:pilight:1\r\n"
                + "Man:\"ssdp:discover\"\r\n"
                + "MX:3\r\n\r\n";

        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        try {
                            ssdp = new DatagramSocket(new InetSocketAddress(inetAddress.getHostAddress().toString(), 0));
                            byte[] buff = msg.getBytes();
                            DatagramPacket sendPack = new DatagramPacket(buff, buff.length);
                            sendPack.setAddress(InetAddress.getByName("239.255.255.250"));
                            sendPack.setPort(1900);

                            try {
                                ssdp.send(sendPack);
                                ssdp.setSoTimeout(1000);
                                boolean loop = true;
                                while (loop) {
                                    DatagramPacket recvPack = new DatagramPacket(new byte[1024], 1024);
                                    ssdp.receive(recvPack);
                                    byte[] recvData = recvPack.getData();
                                    InputStreamReader recvInput = new InputStreamReader(new ByteArrayInputStream(recvData), Charset.forName("UTF-8"));
                                    StringBuilder recvOutput = new StringBuilder();
                                    for (int value; (value = recvInput.read()) != -1; ) {
                                        recvOutput.append((char) value);
                                    }
                                    BufferedReader bufReader = new BufferedReader(new StringReader(recvOutput.toString()));
                                    Pattern pattern = Pattern.compile("Location:([0-9.]+):(.*)");
                                    while ((line = bufReader.readLine()) != null) {
                                        Matcher matcher = pattern.matcher(line);
                                        if (matcher.matches()) {
                                            server = matcher.group(1);
                                            port = Integer.parseInt(matcher.group(2));
                                            loop = false;
                                            break;
                                        }
                                    }
                                }
                            }
                            catch (SocketTimeoutException e) {
                                e.printStackTrace();
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                                Log.d("NJOY-PILIGHT", "no pilight ssdp connections found");
                                ssdp.close();
                                return false;
                            }
                        }
                        catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        catch (SocketException e) {
            e.printStackTrace();
        }
        */

        server = "192.168.1.101";
        port = 5000;

        if (server == null || port == 0) {
            Log.d("NJOY-PILIGHT", "no pilight ssdp connections found");
            return false;
        }
        else {
            Log.d("NJOY-PILIGHT","pilight found at ip: " + server + ", port: " + port);
            return true;
        }
    }

}
