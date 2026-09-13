package dev.flagon.platform.browser;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Resources;
import dev.flagon.platform.browser.js.JsArrays;

import org.teavm.jso.JSBody;
import org.teavm.jso.typedarrays.Int8Array;

final class ResourcePack {

    private ResourcePack() {
    }

    static void install(ConsoleSink console) {
        Int8Array fetched = supplied();
        if (fetched == null) {
            Resources.install(null);
            console.warn("no resource pack beside this bundle; getResourceAsStream reads nothing");
            return;
        }
        try {
            Resources.install(JsArrays.bytesOf(fetched));
            console.info("flagon: the pack carries " + Resources.count() + " resources");
        } catch (IllegalArgumentException broken) {
            Resources.install(null);
            console.warn("the resource pack is unreadable: " + broken.getMessage());
        }
    }

    @JSBody(script = "var pack = window.__flagonResources;"
            + "return pack ? new Int8Array(pack) : null;")
    private static native Int8Array supplied();
}
