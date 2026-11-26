package dev.flagon.platform.browser.workers;

import dev.flagon.platform.UnsupportedCapabilityException;
import dev.flagon.platform.browser.js.JsArrays;
import dev.flagon.platform.workers.Channel;
import dev.flagon.platform.workers.Message;
import dev.flagon.platform.workers.Workers;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.Int8Array;

/** {@link Workers} over web workers, through the self.__flagonWorkers of web/src/workers.ts */
public final class BrowserWorkers implements Workers {

    interface Mail extends JSObject {

        @JSProperty
        String getText();

        @JSProperty
        Int8Array getData();
    }

    @JSFunctor
    interface Received extends JSObject {
        void call(String text, Int8Array data, boolean ended);
    }

    private Channel parent;
    private boolean askedParent;

    @Override
    public boolean isAvailable() {
        return installed() && available();
    }

    @Override
    public Channel spawn(String program, String... arguments) {
        if (!isAvailable()) {
            throw new UnsupportedCapabilityException("this page cannot start bundle workers;"
                    + " the flagon bundle loader installs them where the browser has Worker");
        }
        JSArray<JSString> passed = JSArray.create();
        for (String argument : arguments) {
            passed.push(JSString.valueOf(argument));
        }
        return new Port(start(program, passed));
    }

    @Override
    public Channel parent() {
        if (!askedParent) {
            askedParent = true;
            int id = installed() ? parentId() : -1;
            parent = id < 0 ? null : new Port(id);
        }
        return parent;
    }

    private static final class Port implements Channel {

        private final int id;

        Port(int id) {
            this.id = id;
        }

        @Override
        public void send(Message message) {
            byte[] data = message.data();
            post(id, message.text(), data == null ? null : JsArrays.bytes(data, data.length));
        }

        @Override
        public Message receive() {
            return next(id);
        }

        @Override
        public Message poll() {
            Mail mail = take(id);
            return mail == null ? null : message(mail.getText(), mail.getData());
        }

        @Override
        public boolean isOpen() {
            return open(id);
        }

        @Override
        public void close() {
            end(id);
        }
    }

    private static Message message(String text, Int8Array data) {
        return new Message(text, data == null ? null : JsArrays.bytesOf(data));
    }

    @Async
    private static native Message next(int id);

    private static void next(int id, AsyncCallback<Message> callback) {
        receive(id, (text, data, ended) -> callback.complete(ended ? null : message(text, data)));
    }

    @JSBody(script = "return typeof self.__flagonWorkers !== 'undefined';")
    private static native boolean installed();

    @JSBody(script = "return self.__flagonWorkers.available();")
    private static native boolean available();

    @JSBody(params = {"program", "args"},
            script = "return self.__flagonWorkers.spawn(program, args);")
    private static native int start(String program, JSArray<JSString> args);

    @JSBody(script = "return self.__flagonWorkers.parent();")
    private static native int parentId();

    @JSBody(params = {"id", "text", "data"}, script = "self.__flagonWorkers.send(id, text, data);")
    private static native void post(int id, String text, Int8Array data);

    @JSBody(params = {"id", "done"}, script = "self.__flagonWorkers.receive(id, done);")
    private static native void receive(int id, Received done);

    @JSBody(params = "id", script = "return self.__flagonWorkers.take(id);")
    private static native Mail take(int id);

    @JSBody(params = "id", script = "return self.__flagonWorkers.isOpen(id);")
    private static native boolean open(int id);

    @JSBody(params = "id", script = "self.__flagonWorkers.close(id);")
    private static native void end(int id);
}
