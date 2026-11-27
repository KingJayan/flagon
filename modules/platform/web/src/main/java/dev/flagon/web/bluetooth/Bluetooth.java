package dev.flagon.web.bluetooth;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSBoolean;
import org.teavm.jso.core.JSPromise;

/** https://developer.mozilla.org/docs/Web/API/Bluetooth */
public abstract class Bluetooth implements JSObject {

    public static boolean isSupported() {
        return hasNavigator() && hasBluetooth();
    }

    /** @return navigator.bluetooth, or null without it */
    public static Bluetooth get() {
        return isSupported() ? bluetooth() : null;
    }

    /** shows the device chooser, which needs a click or key press to start it */
    public abstract JSPromise<BluetoothDevice> requestDevice(RequestDeviceOptions options);

    /** @return a promise of whether the computer has a bluetooth adapter */
    public abstract JSPromise<JSBoolean> getAvailability();

    /** @return the devices the page may already use */
    public abstract JSPromise<JSArray<BluetoothDevice>> getDevices();

    @JSBody(script = "return typeof navigator !== 'undefined';")
    private static native boolean hasNavigator();

    @JSBody(script = "return navigator.bluetooth != null;")
    private static native boolean hasBluetooth();

    @JSBody(script = "return navigator.bluetooth;")
    private static native Bluetooth bluetooth();
}
