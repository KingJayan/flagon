package dev.flagon.platform.browser.display;

import java.util.ArrayList;
import java.util.List;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.EventTarget;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.events.Touch;
import org.teavm.jso.dom.events.TouchEvent;
import org.teavm.jso.dom.events.WheelEvent;
import org.teavm.jso.dom.html.HTMLCanvasElement;

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

    @JSFunctor
    interface Noticed extends JSObject {
        void happened();
    }

    private DomInput() {
    }

    /**
     * wires every listener at once and remembers them so {@link #detach} can undo it
     */
    static Listeners attach(JSObject canvas, KeyHandler key, ButtonHandler button,
                            MoveHandler move, WheelHandler wheel, PresenceHandler presence,
                            FocusHandler focus) {
        return new Listeners(canvas.cast(), key, button, move, wheel, presence, focus);
    }

    static void detach(Listeners listeners) {
        if (listeners != null) {
            listeners.undo();
        }
    }

    /** options for addEventListener and focus */
    interface Options extends JSObject {
        @JSProperty
        void setPassive(boolean wanted);

        @JSProperty
        void setPreventScroll(boolean wanted);
    }

    /** the canvas box in fractional css pixels */
    interface Box extends JSObject {
        @JSProperty
        double getLeft();

        @JSProperty
        double getTop();

        @JSProperty
        double getWidth();

        @JSProperty
        double getHeight();
    }

    /** the listeners detach must remove */
    static final class Listeners {

        private static final String[] KEYS_THE_BROWSER_KEEPS = {"F5", "F11", "F12"};

        private final HTMLCanvasElement canvas;
        private final boolean borrowedTabIndex;
        private final List<Wired> wired = new ArrayList<>();

        private final JSObject sizes;
        private Box measured;

        private int touched = -1;
        private int lastX;
        private int lastY;

        private record Wired(EventTarget target, String type, EventListener<Event> listener) {
        }

        Listeners(HTMLCanvasElement canvas, KeyHandler key, ButtonHandler button, MoveHandler move,
                  WheelHandler wheel, PresenceHandler presence, FocusHandler focus) {
            this.canvas = canvas;
            this.borrowedTabIndex = !canvas.hasAttribute("tabindex");
            if (borrowedTabIndex) {
                canvas.setAttribute("tabindex", "0");
            }

            Window page = Window.current();
            // reading the box forces layout
            Noticed forget = () -> measured = null;
            this.sizes = watchSize(canvas, forget);
            EventListener<Event> whenMoved = event -> measured = null;
            onWindow(page, "resize", whenMoved);
            onWindow(page, "scroll", whenMoved);
            EventListener<Event> onMove = event -> {
                MouseEvent moved = event.cast();
                Box box = box();
                move.onMove(atX(moved.getClientX()), atY(moved.getClientY()),
                        scaled(moved.getMovementX(), canvas.getWidth(), box.getWidth()),
                        scaled(moved.getMovementY(), canvas.getHeight(), box.getHeight()),
                        isLocked());
            };
            onWindow(page, "mousemove", onMove);
            onWindow(page, "mouseup", event -> {
                MouseEvent up = event.cast();
                button.onButton(up.getButton(), false, atX(up.getClientX()), atY(up.getClientY()));
                event.preventDefault();
            });

            onCanvas("keydown", event -> {
                KeyboardEvent pressed = event.cast();
                if (pressed.isCtrlKey() || pressed.isMetaKey() || kept(pressed.getCode())) {
                    return;
                }
                if ("Tab".equals(pressed.getCode()) && !isLocked()) {
                    return;
                }
                if (key.onKey(pressed.getCode(), pressed.getKey(), true, pressed.isRepeat())) {
                    event.preventDefault();
                }
            });
            onCanvas("keyup", event -> {
                KeyboardEvent released = event.cast();
                if (key.onKey(released.getCode(), released.getKey(), false, false)) {
                    event.preventDefault();
                }
            });
            onCanvas("mousedown", event -> {
                MouseEvent down = event.cast();
                takeFocus();
                button.onButton(down.getButton(), true, atX(down.getClientX()),
                        atY(down.getClientY()));
                event.preventDefault();
            });
            onCanvas("wheel", event -> {
                WheelEvent turned = event.cast();
                wheel.onWheel(turned.getDeltaY(), turned.getDeltaMode());
                event.preventDefault();
            }, false);
            onCanvas("touchstart", event -> {
                if (touched >= 0) {
                    return;
                }
                Touch touch = ((TouchEvent) event.cast()).getChangedTouches().get(0);
                touched = touch.getIdentifier();
                lastX = atX(touch.getClientX());
                lastY = atY(touch.getClientY());
                takeFocus();
                presence.onPresence(true);
                move.onMove(lastX, lastY, 0, 0, false);
                button.onButton(0, true, lastX, lastY);
                event.preventDefault();
            }, false);
            onCanvas("touchmove", event -> {
                Touch touch = tracked(event.cast());
                if (touch == null) {
                    return;
                }
                int x = atX(touch.getClientX());
                int y = atY(touch.getClientY());
                move.onMove(x, y, x - lastX, y - lastY, false);
                lastX = x;
                lastY = y;
                event.preventDefault();
            }, false);
            EventListener<Event> onTouchEnd = event -> {
                Touch touch = tracked(event.cast());
                if (touch == null) {
                    return;
                }
                touched = -1;
                button.onButton(0, false, atX(touch.getClientX()), atY(touch.getClientY()));
                presence.onPresence(false);
                event.preventDefault();
            };
            onCanvas("touchend", onTouchEnd, false);
            onCanvas("touchcancel", onTouchEnd, false);
            onCanvas("mouseenter", event -> presence.onPresence(true));
            onCanvas("mouseleave", event -> presence.onPresence(false));
            onCanvas("focus", event -> focus.onFocus(true));
            onCanvas("blur", event -> focus.onFocus(false));
            onCanvas("contextmenu", Event::preventDefault);
            takeFocus();
        }

        void undo() {
            stopWatching(sizes);
            for (Wired one : wired) {
                one.target().removeEventListener(one.type(), one.listener());
            }
            wired.clear();
            if (borrowedTabIndex) {
                canvas.removeAttribute("tabindex");
            }
        }

        private void onWindow(Window page, String type, EventListener<Event> listener) {
            page.addEventListener(type, listener);
            wired.add(new Wired(page, type, listener));
        }

        private void onCanvas(String type, EventListener<Event> listener) {
            canvas.addEventListener(type, listener);
            wired.add(new Wired(canvas, type, listener));
        }

        private void onCanvas(String type, EventListener<Event> listener, boolean passive) {
            Options options = JSObjects.create();
            options.setPassive(passive);
            listen(canvas, type, listener, options);
            wired.add(new Wired(canvas, type, listener));
        }

        private Touch tracked(TouchEvent event) {
            JSArrayReader<Touch> changed = event.getChangedTouches();
            for (int at = 0; at < changed.getLength(); at++) {
                if (changed.get(at).getIdentifier() == touched) {
                    return changed.get(at);
                }
            }
            return null;
        }

        private void takeFocus() {
            Options options = JSObjects.create();
            options.setPreventScroll(true);
            focusWithout(canvas, options);
        }

        private boolean isLocked() {
            return isPointerLocked(canvas);
        }

        private static boolean kept(String code) {
            for (String held : KEYS_THE_BROWSER_KEEPS) {
                if (held.equals(code)) {
                    return true;
                }
            }
            return false;
        }

        // css pixels are not backing-store pixels: the canvas is sized by the page
        private int atX(double clientX) {
            Box box = box();
            double scale = box.getWidth() > 0 ? canvas.getWidth() / box.getWidth() : 1;
            return (int) Math.round((clientX - box.getLeft()) * scale);
        }

        private int atY(double clientY) {
            Box box = box();
            double scale = box.getHeight() > 0 ? canvas.getHeight() / box.getHeight() : 1;
            return (int) Math.round((clientY - box.getTop()) * scale);
        }

        private Box box() {
            if (measured == null) {
                measured = canvas.getBoundingClientRect().cast();
            }
            return measured;
        }

        private static int scaled(double movement, double size, double boxSize) {
            return (int) Math.round((long) movement * (boxSize > 0 ? size / boxSize : 1));
        }
    }

    @JSBody(params = {"target", "type", "listener", "options"},
            script = "target.addEventListener(type, listener, options);")
    private static native void listen(JSObject target, String type, JSObject listener,
                                      JSObject options);

    private static JSObject watchSize(JSObject element, Noticed watcher) {
        JSObject watching = observerFor(watcher);
        observe(watching, element);
        return watching;
    }

    @JSBody(params = "watcher", script = "return new ResizeObserver(watcher);")
    private static native JSObject observerFor(Noticed watcher);

    @JSBody(params = {"watching", "element"}, script = "watching.observe(element);")
    private static native void observe(JSObject watching, JSObject element);

    @JSBody(params = "watching", script = "watching.disconnect();")
    private static native void stopWatching(JSObject watching);

    @JSBody(params = {"element", "options"}, script = "element.focus(options);")
    private static native void focusWithout(JSObject element, JSObject options);

    /** pointer lock needs a user gesture and resolves later, so this reports acceptance only. */
    static boolean requestPointerLock(JSObject canvas) {
        try {
            Promises.warnIfRefused(askPointerLock(canvas), "pointer lock");
            return true;
        } catch (RuntimeException refused) {
            Promises.warn("[flagon] pointer lock refused: " + refused.getMessage());
            return false;
        }
    }

    @JSBody(params = "canvas", script = "return canvas.requestPointerLock();")
    private static native JSObject askPointerLock(JSObject canvas);

    static void exitPointerLock() {
        if (isPointerLockHeld()) {
            leavePointerLock();
        }
    }

    @JSBody(script = "return document.pointerLockElement != null;")
    private static native boolean isPointerLockHeld();

    @JSBody(script = "document.exitPointerLock();")
    private static native void leavePointerLock();

    @JSBody(params = "canvas", script = "return document.pointerLockElement === canvas;")
    static native boolean isPointerLocked(JSObject canvas);
}
