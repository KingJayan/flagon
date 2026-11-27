package dev.flagon.web.usb;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;

/** the setup packet of a control transfer */
public interface USBControlTransferParameters extends JSObject {
    /**
     * @param requestType standard, class or vendor
     * @param recipient device, interface, endpoint or other
     */
    static USBControlTransferParameters create(String requestType, String recipient,
            int request, int value, int index) {
        USBControlTransferParameters setup = JSObjects.create();
        setup.setRequestType(requestType);
        setup.setRecipient(recipient);
        setup.setRequest(request);
        setup.setValue(value);
        setup.setIndex(index);
        return setup;
    }

    @JSProperty
    void setRequestType(String requestType);

    @JSProperty
    void setRecipient(String recipient);

    @JSProperty
    void setRequest(int request);

    @JSProperty
    void setValue(int value);

    @JSProperty
    void setIndex(int index);
}
