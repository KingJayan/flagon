package dev.flagon.platform.browser;

import org.teavm.jso.JSBody;

final class HostBridges {

    private HostBridges() {
    }

    // a malformed bridge is reported then treated as absent
    @JSBody(script = """
            var host = window.__flagonHost;
            if (!host) {
                return null;
            }
            var required = ['log', 'status', 'ready', 'exited', 'fatal'];
            for (var i = 0; i < required.length; i++) {
                if (typeof host[required[i]] !== 'function') {
                    console.warn('[flagon] ignoring window.__flagonHost: missing '
                        + required[i] + '()');
                    return null;
                }
            }
            return host;
            """)
    static native HostBridge installed();
}
