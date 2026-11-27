package dev.flagon.web.usb;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSUndefined;
import org.teavm.jso.typedarrays.ArrayBufferView;

/** https://developer.mozilla.org/docs/Web/API/USBDevice */
public interface USBDevice extends JSObject {
    @JSProperty
    int getVendorId();

    @JSProperty
    int getProductId();

    @JSProperty
    String getProductName();

    @JSProperty
    String getManufacturerName();

    @JSProperty
    String getSerialNumber();

    @JSProperty
    boolean isOpened();

    /** @return the selected configuration, or null before one is */
    @JSProperty
    USBConfiguration getConfiguration();

    JSPromise<JSUndefined> open();

    JSPromise<JSUndefined> close();

    JSPromise<JSUndefined> reset();

    JSPromise<JSUndefined> selectConfiguration(int configurationValue);

    JSPromise<JSUndefined> claimInterface(int interfaceNumber);

    JSPromise<JSUndefined> releaseInterface(int interfaceNumber);

    JSPromise<JSUndefined> selectAlternateInterface(int interfaceNumber, int alternateSetting);

    JSPromise<USBInTransferResult> controlTransferIn(USBControlTransferParameters setup,
            int length);

    JSPromise<USBOutTransferResult> controlTransferOut(USBControlTransferParameters setup,
            ArrayBufferView data);

    JSPromise<USBInTransferResult> transferIn(int endpointNumber, int length);

    JSPromise<USBOutTransferResult> transferOut(int endpointNumber, ArrayBufferView data);

    /** @param direction in or out */
    JSPromise<JSUndefined> clearHalt(String direction, int endpointNumber);
}
