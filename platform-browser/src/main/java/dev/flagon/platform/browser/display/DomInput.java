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

    @JSFunctor
    interface FocusHandler extends JSObject {
        void onFocus(boolean focused);
    }

    private DomInput() {
    }

    /**
     * wires every listener at once and remembers them so {@link #detach} can undo it
     */
    @JSBody(params = {"canvas", "key", "button", "move", "wheel", "presence", "focus"}, script = """
            var locked = function () { return document.pointerLockElement === canvas; };
            // css pixels are not backing-store pixels: the canvas is sized by the page
            var atX = function (event) {
                var box = canvas.getBoundingClientRect();
                var scale = box.width > 0 ? canvas.width / box.width : 1;
                return Math.round((event.clientX - box.left) * scale);
            };
            var atY = function (event) {
                var box = canvas.getBoundingClientRect();
                var scale = box.height > 0 ? canvas.height / box.height : 1;
                return Math.round((event.clientY - box.top) * scale);
            };
            var scaled = function (movement, size, boxSize) {
                return Math.round((movement | 0) * (boxSize > 0 ? size / boxSize : 1));
            };

            var state = {
                canvas: canvas,
                borrowedTabIndex: !canvas.hasAttribute('tabindex'),
                keydown: function (event) {
                    if (event.ctrlKey || event.metaKey || event.code === 'F5'
                            || event.code === 'F11' || event.code === 'F12') {
                        return;
                    }
                    if (event.code === 'Tab' && !locked()) {
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
                    canvas.focus({preventScroll: true});
                    button(event.button, true, atX(event), atY(event));
                    event.preventDefault();
                },
                mouseup: function (event) {
                    button(event.button, false, atX(event), atY(event));
                    event.preventDefault();
                },
                mousemove: function (event) {
                    var box = canvas.getBoundingClientRect();
                    move(atX(event), atY(event),
                            scaled(event.movementX, canvas.width, box.width),
                            scaled(event.movementY, canvas.height, box.height),
                            locked());
                },
                wheel: function (event) {
                    wheel(event.deltaY, event.deltaMode | 0);
                    event.preventDefault();
                },
                touched: null,
                touchstart: function (event) {
                    if (state.touched !== null) {
                        return;
                    }
                    var touch = event.changedTouches[0];
                    state.touched = touch.identifier;
                    state.lastX = atX(touch);
                    state.lastY = atY(touch);
                    canvas.focus({preventScroll: true});
                    presence(true);
                    move(state.lastX, state.lastY, 0, 0, false);
                    button(0, true, state.lastX, state.lastY);
                    event.preventDefault();
                },
                touchmove: function (event) {
                    var touch = state.tracked(event);
                    if (!touch) {
                        return;
                    }
                    var x = atX(touch);
                    var y = atY(touch);
                    move(x, y, x - state.lastX, y - state.lastY, false);
                    state.lastX = x;
                    state.lastY = y;
                    event.preventDefault();
                },
                touchend: function (event) {
                    var touch = state.tracked(event);
                    if (!touch) {
                        return;
                    }
                    state.touched = null;
                    button(0, false, atX(touch), atY(touch));
                    presence(false);
                    event.preventDefault();
                },
                lastX: 0,
                lastY: 0,
                tracked: function (event) {
                    for (var i = 0; i < event.changedTouches.length; i++) {
                        if (event.changedTouches[i].identifier === state.touched) {
                            return event.changedTouches[i];
                        }
                    }
                    return null;
                },
                enter: function () { presence(true); },
                leave: function () { presence(false); },
                focused: function () { focus(true); },
                blurred: function () { focus(false); },
                contextmenu: function (event) { event.preventDefault(); }
            };

            if (state.borrowedTabIndex) {
                canvas.setAttribute('tabindex', '0');
            }
            window.addEventListener('mousemove', state.mousemove);
            window.addEventListener('mouseup', state.mouseup);
            canvas.addEventListener('keydown', state.keydown);
            canvas.addEventListener('keyup', state.keyup);
            canvas.addEventListener('mousedown', state.mousedown);
            canvas.addEventListener('wheel', state.wheel, {passive: false});
            canvas.addEventListener('touchstart', state.touchstart, {passive: false});
            canvas.addEventListener('touchmove', state.touchmove, {passive: false});
            canvas.addEventListener('touchend', state.touchend, {passive: false});
            canvas.addEventListener('touchcancel', state.touchend, {passive: false});
            canvas.addEventListener('mouseenter', state.enter);
            canvas.addEventListener('mouseleave', state.leave);
            canvas.addEventListener('focus', state.focused);
            canvas.addEventListener('blur', state.blurred);
            canvas.addEventListener('contextmenu', state.contextmenu);
            canvas.focus({preventScroll: true});
            return state;
            """)
    static native JSObject attach(JSObject canvas, KeyHandler key, ButtonHandler button,
                                  MoveHandler move, WheelHandler wheel, PresenceHandler presence,
                                  FocusHandler focus);

    @JSBody(params = "state", script = """
            if (!state) {
                return;
            }
            window.removeEventListener('mousemove', state.mousemove);
            window.removeEventListener('mouseup', state.mouseup);
            state.canvas.removeEventListener('keydown', state.keydown);
            state.canvas.removeEventListener('keyup', state.keyup);
            state.canvas.removeEventListener('mousedown', state.mousedown);
            state.canvas.removeEventListener('wheel', state.wheel);
            state.canvas.removeEventListener('touchstart', state.touchstart);
            state.canvas.removeEventListener('touchmove', state.touchmove);
            state.canvas.removeEventListener('touchend', state.touchend);
            state.canvas.removeEventListener('touchcancel', state.touchend);
            state.canvas.removeEventListener('mouseenter', state.enter);
            state.canvas.removeEventListener('mouseleave', state.leave);
            state.canvas.removeEventListener('focus', state.focused);
            state.canvas.removeEventListener('blur', state.blurred);
            state.canvas.removeEventListener('contextmenu', state.contextmenu);
            if (state.borrowedTabIndex) {
                state.canvas.removeAttribute('tabindex');
            }
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
