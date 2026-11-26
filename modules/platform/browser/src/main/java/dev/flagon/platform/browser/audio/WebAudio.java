package dev.flagon.platform.browser.audio;

import org.teavm.jso.JSBody;

/** what a page has to offer before an {@link WebAudioDevice} can be built on it. */
final class WebAudio {

    private WebAudio() {
    }

    @JSBody(script = "return typeof AudioContext !== 'undefined';")
    static native boolean isAvailable();
}
