package dev.flagon.sandbox;

import dev.flagon.platform.ConsoleSink;
import dev.flagon.platform.Platforms;
import dev.flagon.runtime.Program;
import dev.flagon.web.Promises;
import dev.flagon.web.WebException;
import dev.flagon.web.bluetooth.Bluetooth;
import dev.flagon.web.codecs.CodecSupport;
import dev.flagon.web.codecs.EncodedVideoChunk;
import dev.flagon.web.codecs.VideoDecoder;
import dev.flagon.web.codecs.VideoDecoderConfig;
import dev.flagon.web.codecs.VideoDecoderInit;
import dev.flagon.web.codecs.VideoEncoder;
import dev.flagon.web.codecs.VideoEncoderConfig;
import dev.flagon.web.codecs.VideoEncoderEncodeOptions;
import dev.flagon.web.codecs.VideoEncoderInit;
import dev.flagon.web.codecs.VideoFrame;
import dev.flagon.web.codecs.VideoFrameBufferInit;
import dev.flagon.web.indexeddb.IndexedDb;
import dev.flagon.web.indexeddb.Store;
import dev.flagon.web.rtc.RTCDataChannel;
import dev.flagon.web.rtc.RTCPeerConnection;
import dev.flagon.web.rtc.RTCSessionDescriptionInit;
import dev.flagon.web.transport.WebTransport;
import dev.flagon.web.usb.USB;

import org.teavm.jso.core.JSPromise;
import org.teavm.jso.core.JSString;
import org.teavm.jso.core.JSUndefined;
import org.teavm.jso.typedarrays.Uint8Array;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** drives the platform-web bindings against the browser's own apis */
public final class WebApisProgram implements Program {

    private static final String DATABASE = "flagon-webapis";

    private ConsoleSink console;

    @Override
    public String id() {
        return "webapis";
    }

    @Override
    public String description() {
        return "stores and reads indexeddb, talks over a webrtc channel and encodes a video frame";
    }

    @Override
    public void run(String[] arguments) {
        console = Platforms.get().console();
        indexedDb();
        rtc();
        codecs();
        transport();
        devices();
        say("done");
    }

    private void indexedDb() {
        IndexedDb.delete(DATABASE);
        try (IndexedDb database = IndexedDb.open(DATABASE, 1, "files")) {
            Store files = database.store("files");
            files.put("a", new byte[] {1, 2, 3});
            files.put("b", "text");
            say("indexeddb: a holds " + Arrays.toString(files.get("a")) + " and b holds "
                    + files.getString("b"));
            say("indexeddb: the keys are " + files.keys() + ", " + files.size() + " of them");
            files.delete("a");
            say("indexeddb: a is gone " + !files.has("a") + " and nothing holds "
                    + files.get("nothing"));
            try {
                files.get("b");
            } catch (IllegalStateException refused) {
                say("indexeddb: bytes from a string are refused");
            }
            try {
                database.store("missing");
            } catch (WebException refused) {
                say("indexeddb: a missing store is refused with " + refused.name());
            }
        }
        int[] seen = new int[2];
        try (IndexedDb database = IndexedDb.open(DATABASE, 2, (schema, from, to) -> {
            seen[0] = from;
            seen[1] = to;
            schema.create("more");
        })) {
            say("indexeddb: version " + seen[0] + " upgrades to " + database.version()
                    + " with " + Arrays.toString(database.stores()) + " and b still holds "
                    + database.store("files").getString("b"));
        }
        try {
            IndexedDb.open(DATABASE, 3, (schema, from, to) -> {
                throw new IllegalArgumentException("a refused upgrade");
            });
        } catch (IllegalArgumentException refused) {
            try (IndexedDb database = IndexedDb.open(DATABASE, 2)) {
                say("indexeddb: '" + refused.getMessage() + "' leaves version "
                        + database.version());
            }
        }
        IndexedDb.delete(DATABASE);
    }

    private void rtc() {
        if (!RTCPeerConnection.isSupported()) {
            say("rtc: not offered");
            return;
        }
        RTCPeerConnection calling = new RTCPeerConnection();
        RTCPeerConnection answering = new RTCPeerConnection();
        calling.onIceCandidate(event -> {
            if (event.getCandidate() != null) {
                answering.addIceCandidate(event.getCandidate());
            }
        });
        answering.onIceCandidate(event -> {
            if (event.getCandidate() != null) {
                calling.addIceCandidate(event.getCandidate());
            }
        });
        JSPromise<JSString> heard = new JSPromise<>((resolve, reject) ->
                answering.onDataChannel(event -> event.getChannel().onMessage(message ->
                        resolve.accept(JSString.valueOf(message.getDataAsString())))));
        RTCDataChannel channel = calling.createDataChannel("chat");
        JSPromise<JSUndefined> opened = new JSPromise<>((resolve, reject) ->
                channel.onOpen(event -> resolve.accept(null)));

        RTCSessionDescriptionInit offer = Promises.await(calling.createOffer());
        Promises.await(calling.setLocalDescription(offer));
        Promises.await(answering.setRemoteDescription(offer));
        RTCSessionDescriptionInit answer = Promises.await(answering.createAnswer());
        Promises.await(answering.setLocalDescription(answer));
        Promises.await(calling.setRemoteDescription(answer));

        Promises.await(opened);
        channel.send("hello over " + channel.getLabel());
        say("rtc: the other peer hears '" + Promises.await(heard).stringValue() + "'");
        calling.close();
        answering.close();
    }

    private void codecs() {
        if (!VideoEncoder.isSupported()) {
            say("codecs: not offered");
            return;
        }
        VideoEncoderConfig config = VideoEncoderConfig.create("vp8", 16, 16);
        CodecSupport<VideoEncoderConfig> support =
                Promises.await(VideoEncoder.isConfigSupported(config));
        if (!support.isSupported()) {
            say("codecs: vp8 is not offered");
            return;
        }
        List<EncodedVideoChunk> chunks = new ArrayList<>();
        VideoEncoder encoder = new VideoEncoder(VideoEncoderInit.create(
                (chunk, metadata) -> chunks.add(chunk), this::refused));
        encoder.configure(config);
        VideoFrame frame = new VideoFrame(new Uint8Array(16 * 16 * 4),
                VideoFrameBufferInit.create("RGBA", 16, 16, 0));
        encoder.encode(frame, VideoEncoderEncodeOptions.keyFrame());
        frame.close();
        Promises.await(encoder.flush());
        encoder.close();
        say("codecs: one frame encodes to " + chunks.size() + " " + chunks.get(0).getType()
                + " chunk");

        List<String> decoded = new ArrayList<>();
        VideoDecoder decoder = new VideoDecoder(VideoDecoderInit.create(output -> {
            decoded.add(output.getCodedWidth() + "x" + output.getCodedHeight());
            output.close();
        }, this::refused));
        decoder.configure(VideoDecoderConfig.create("vp8"));
        decoder.decode(chunks.get(0));
        Promises.await(decoder.flush());
        decoder.close();
        say("codecs: the chunk decodes to " + decoded);
    }

    private void transport() {
        if (!WebTransport.isSupported()) {
            say("transport: not offered");
            return;
        }
        WebTransport transport = new WebTransport("https://127.0.0.1:4433/");
        // closed rejects too, and would be an unhandled rejection
        JSPromise<Object> closed = transport.getClosed().catchError(error -> null);
        transport.close();
        try {
            Promises.await(transport.getReady());
            say("transport: a closed session is ready");
        } catch (WebException refused) {
            say("transport: a session closed before it connects is refused with "
                    + refused.name());
        }
        Promises.await(closed);
    }

    /** the choosers would need a click, so only the calls that need none */
    private void devices() {
        Bluetooth bluetooth = Bluetooth.get();
        if (bluetooth == null) {
            say("bluetooth: not offered");
        } else {
            Promises.await(bluetooth.getAvailability());
            say("bluetooth: the adapter check answers");
        }
        USB usb = USB.get();
        if (usb == null) {
            say("usb: not offered");
        } else {
            say("usb: the page may use " + Promises.await(usb.getDevices()).getLength()
                    + " devices");
        }
    }

    private void refused(Object error) {
        say("codecs: refused " + WebException.of(error).getMessage());
    }

    private void say(String line) {
        console.info("webapis: " + line);
    }
}
