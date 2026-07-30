package dev.flagon.platform.browser.display;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * dom event plumbing, kept apart from the input state it feeds
 */
final class DomInput {

    @JSFunctor
    interface KeyHandler extends JSObject {
        /** @param code a {@code KeyboardEvent.code}; {@code key} is the text it produced */
        boolean onKey(String code, String key, boolean pressed, boolean repeat);
    }

    @JSFunctor
    interface ButtonHandler extends JSObject {
        void onButton(int button, boolean pressed, int x, int y);
    }

    @JSFunctor
    interface MoveHandler extends JSObject {
        void onMove(int x, int y, int movementX, int movementY, boolean locked);
    }

    @JSFunctor
    interface WheelHandler extends JSObject {
        void onWheel(double delta, int deltaMode);
    }

    @JSFunctor
    interface PresenceHandler extends JSObject {
        void onPresence(boolean inside);
    }

    private DomInput() {
    }

    /**
     * wires every listener at once and remembers them so {@link #detach} can undo it
     */
    @JSBody(params = {"canvas", "key", "button", "move", "wheel", "presence"}, script = """
            var state = {
                canvas: canvas,
                keydown: function (event) {
                    if (event.ctrlKey || event.metaKey || event.code === 'F5'
                            || event.code === 'F11' || event.code === 'F12') {
                        return;
                    }
                    if (key(event.code, event.key, true, event.repeat === true)) {
                        event.preventDefault();
                    }
                },
                keyup: function (event) {
                    if (key(event.code, event.key, false, false)) {
                        event.preventDefault();
                    }
                },
                mousedown: function (event) {
                    var box = canvas.getBoundingClientRect();
                    button(event.button, true,
                            Math.round(event.clientX - box.left),
                            Math.round(event.clientY - box.top));
                    event.preventDefault();
                },
                mouseup: function (event) {
                    var box = canvas.getBoundingClientRect();
                    button(event.button, false,
                            Math.round(event.clientX - box.left),
                            Math.round(event.clientY - box.top));
                    event.preventDefault();
                },
                mousemove: function (event) {
                    var box = canvas.getBoundingClientRect();
                    move(Math.round(event.clientX - box.left),
                            Math.round(event.clientY - box.top),
                            event.movementX | 0, event.movementY | 0,
                            document.pointerLockElement === canvas);
                },
                wheel: function (event) {
                    wheel(event.deltaY, event.deltaMode | 0);
                    event.preventDefault();
                },
                enter: function () { presence(true); },
                leave: function () { presence(false); },
                contextmenu: function (event) { event.preventDefault(); }
            };

            window.addEventListener('keydown', state.keydown);
            window.addEventListener('keyup', state.keyup);
            window.addEventListener('mousemove', state.mousemove);
            window.addEventListener('mouseup', state.mouseup);
            canvas.addEventListener('mousedown', state.mousedown);
            canvas.addEventListener('wheel', state.wheel, {passive: false});
            canvas.addEventListener('mouseenter', state.enter);
            canvas.addEventListener('mouseleave', state.leave);
            canvas.addEventListener('contextmenu', state.contextmenu);
            return state;
            """)
    static native JSObject attach(JSObject canvas, KeyHandler key, ButtonHandler button,
                                  MoveHandler move, WheelHandler wheel, PresenceHandler presence);

    @JSBody(params = "state", script = """
            if (!state) {
                return;
            }
            window.removeEventListener('keydown', state.keydown);
            window.removeEventListener('keyup', state.keyup);
            window.removeEventListener('mousemove', state.mousemove);
            window.removeEventListener('mouseup', state.mouseup);
            state.canvas.removeEventListener('mousedown', state.mousedown);
            state.canvas.removeEventListener('wheel', state.wheel);
            state.canvas.removeEventListener('mouseenter', state.enter);
            state.canvas.removeEventListener('mouseleave', state.leave);
            state.canvas.removeEventListener('contextmenu', state.contextmenu);
            """)
    static native void detach(JSObject state);

    /** pointer lock needs a user gesture and resolves later, so this reports acceptance only. */
    @JSBody(params = "canvas", script = """
            try {
                var result = canvas.requestPointerLock();
                if (result && typeof result.catch === 'function') {
                    result.catch(function (error) {
                        console.warn('[flagon] pointer lock refused: ' + error);
                    });
                }
                return true;
            } catch (error) {
                console.warn('[flagon] pointer lock refused: ' + error);
                return false;
            }
            """)
    static native boolean requestPointerLock(JSObject canvas);

    @JSBody(script = """
            if (document.pointerLockElement != null) {
                document.exitPointerLock();
            }
            """)
    static native void exitPointerLock();

    @JSBody(params = "canvas", script = "return document.pointerLockElement === canvas;")
    static native boolean isPointerLocked(JSObject canvas);
}
