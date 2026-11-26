package dev.flagon.platform.browser;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Resources;
import dev.flagon.platform.browser.js.JsArrays;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

final class ResourcePack {

    private ResourcePack() {
    }

    static void install(ConsoleSink console) {
        Int8Array fetched = supplied();
        if (fetched == null) {
            Resources.clear();
            console.warn("no resource pack beside this bundle; getResourceAsStream reads nothing");
            return;
        }
        try {
            Resources.install(over(fetched), ResourcePack::fetchApart);
            console.info("flagon: the pack carries " + Resources.count() + " resources");
            if (servesApart()) {
                wantApart(String.join(",", Resources.apart()));
            }
        } catch (IllegalArgumentException broken) {
            Resources.clear();
            console.warn("the resource pack is unreadable: " + broken.getMessage());
        } finally {
            // the pack is held from here on, so the page lets go of it
            dropSupplied();
        }
    }

    /** reads the pack where it landed, copying out only the resources asked for */
    private static Resources.Source over(Int8Array pack) {
        return new Resources.Source() {
            @Override
            public int length() {
                return pack.getLength();
            }

            @Override
            public byte get(int at) {
                return pack.get(at);
            }

            @Override
            public void copy(int from, byte[] into, int to, int count) {
                byte[] part = JsArrays.bytesOf(Int8Array.create(pack.getBuffer(),
                        pack.getByteOffset() + from, count));
                System.arraycopy(part, 0, into, to, count);
            }
        };
    }

    /** the file once the page has fetched it; a read never waits, so it is null till then */
    private static byte[] fetchApart(String hash) {
        if (!servesApart()) {
            return null;
        }
        ArrayBuffer arrived = takeApart(hash);
        return arrived == null || JSObjects.isUndefined(arrived) ? null
                : JsArrays.bytesOf(new Int8Array(arrived));
    }

    @JSBody(script = "return typeof self.__flagonApart !== 'undefined';")
    private static native boolean servesApart();

    @JSBody(params = "hash", script = "return self.__flagonApart.take(hash);")
    private static native ArrayBuffer takeApart(String hash);

    /** the page fetches these, joined with commas, and drops cached files it is not given */
    @JSBody(params = "hashes", script = "self.__flagonApart.want(hashes);")
    private static native void wantApart(String hashes);

    interface Packed extends JSObject {
        @JSProperty("__flagonResources")
        ArrayBuffer getResources();
    }

    @JSBody(script = "self.__flagonResources = undefined;")
    private static native void dropSupplied();

    private static Int8Array supplied() {
        ArrayBuffer pack = HostBridges.scope().<Packed>cast().getResources();
        return pack == null || JSObjects.isUndefined(pack) ? null : new Int8Array(pack);
    }
}
