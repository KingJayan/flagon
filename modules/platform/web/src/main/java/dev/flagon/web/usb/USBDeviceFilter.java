package dev.flagon.web.usb;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** a device filter of USBDeviceRequestOptions */
public interface USBDeviceFilter extends JSObject {
    static USBDeviceFilter vendor(int vendorId) {
        USBDeviceFilter filter = JSObjects.create();
        filter.setVendorId(vendorId);
        return filter;
    }

    static USBDeviceFilter product(int vendorId, int productId) {
        USBDeviceFilter filter = vendor(vendorId);
        filter.setProductId(productId);
        return filter;
    }

    @JSProperty
    void setVendorId(int vendorId);

    @JSProperty
    void setProductId(int productId);

    @JSProperty
    void setClassCode(int classCode);

    @JSProperty
    void setSubclassCode(int subclassCode);

    @JSProperty
    void setProtocolCode(int protocolCode);

    @JSProperty
    void setSerialNumber(String serialNumber);
}
