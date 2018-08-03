package edu.estgp.pilight;

/**
 * Created by vrealinho on 10/03/18.
 */

public interface DeviceStatusInterface {
    public void onInitialDeviceStatus(String device, boolean state);
    public void onDeviceStatusChanged(String device, boolean state);
}
