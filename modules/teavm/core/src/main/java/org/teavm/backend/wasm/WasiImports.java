// flagon: under wasi, each javascript import becomes a function that calls java in its place
package org.teavm.backend.wasm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import org.teavm.backend.wasm.generate.WasmGCDeclarationsGenerator;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

final class WasiImports {
    static final String PREVIEW1 = "wasi_snapshot_preview1";
    private static final String WASI = "org.teavm.runtime.wasi.Wasi";
    private static final String MATH = "org.teavm.runtime.wasi.WasiMath";
    private static final Map<String, MethodReference> TARGETS = new LinkedHashMap<>();

    static {
        put("teavmConsole.putcharStdout", WASI, "putCharStdout", ValueType.CHARACTER, ValueType.VOID);
        put("teavmConsole.putcharStderr", WASI, "putCharStderr", ValueType.CHARACTER, ValueType.VOID);
        put("teavmDate.currentTimeMillis", WASI, "currentTimeMillis", ValueType.DOUBLE);
        put("teavmMemory.notifyHeapResized", WASI, "heapResized", ValueType.VOID);
        put("teavmFiber.start", WASI, "noFiberStack", ValueType.VOID);
        put("teavmFiber.park", WASI, "noFiberStack", ValueType.INTEGER, ValueType.VOID);
        put("teavmFiber.wake", WASI, "noFiberStack", ValueType.INTEGER, ValueType.VOID);
        put("teavmMath.random", WASI, "random", ValueType.DOUBLE);
        for (var name : new String[] { "sin", "cos", "tan", "asin", "acos", "atan", "exp", "log", "sqrt",
                "ceil", "floor" }) {
            put("teavmMath." + name, MATH, name, ValueType.DOUBLE, ValueType.DOUBLE);
        }
        put("teavmMath.atan2", MATH, "atan2", ValueType.DOUBLE, ValueType.DOUBLE, ValueType.DOUBLE);
        put("teavmMath.pow", MATH, "pow", ValueType.DOUBLE, ValueType.DOUBLE, ValueType.DOUBLE);
    }

    private WasiImports() {
    }

    private static void put(String imported, String className, String name, ValueType... signature) {
        TARGETS.put(imported, new MethodReference(className, name, signature));
    }

    static void link(DependencyAnalyzer analyzer) {
        analyzer.linkMethod(new MethodReference(WASI, "run", ValueType.VOID)).use();
        for (var target : TARGETS.values()) {
            analyzer.linkMethod(target).use();
        }
    }

    static void declare(WasmGCDeclarationsGenerator declarations) {
        for (var target : TARGETS.values()) {
            declarations.functions().forStaticMethod(target);
        }
    }

    /** @throws IllegalStateException naming each import that only javascript could give */
    static void retarget(WasmModule module, WasmGCDeclarationsGenerator declarations, DependencyInfo reached) {
        var unknown = new ArrayList<String>();
        var converted = new ArrayList<WasmFunction>();
        var functions = new ArrayList<WasmFunction>();
        for (var function : module.functions) {
            functions.add(function);
        }
        for (var function : functions) {
            var importModule = function.getImportModule();
            if (importModule == null || importModule.equals(PREVIEW1)) {
                continue;
            }
            var key = importModule + "." + function.getImportName();
            var target = TARGETS.get(key);
            if (target == null) {
                unknown.add(key);
                continue;
            }
            becomeCall(function, declarations.functions().forStaticMethod(target));
            converted.add(function);
        }
        // a defined function comes after every import, and the collection numbers them again
        module.functions.removeIf(converted::contains);
        for (var function : converted) {
            module.functions.add(function);
        }
        if (!unknown.isEmpty()) {
            throw new IllegalStateException("a wasi bundle cannot import from javascript, but it needs "
                    + String.join(", ", unknown) + ", reached from " + String.join(", ", callersOfJso(reached))
                    + "; that code runs only in a page");
        }
    }

    /** the methods outside teavm whose calls lead into org.teavm.jso, nearest first */
    private static TreeSet<String> callersOfJso(DependencyInfo reached) {
        var graph = reached.getCallGraph();
        var queue = new ArrayDeque<MethodReference>();
        var seen = new HashSet<MethodReference>();
        for (var method : reached.getReachableMethods()) {
            if (method.getClassName().startsWith("org.teavm.jso.") && seen.add(method)) {
                queue.add(method);
            }
        }
        var found = new TreeSet<String>();
        while (!queue.isEmpty() && found.size() < 8) {
            var node = graph.getNode(queue.remove());
            if (node == null) {
                continue;
            }
            for (var site : node.getCallerCallSites()) {
                for (var caller : site.getCallers()) {
                    var method = caller.getMethod();
                    if (!seen.add(method)) {
                        continue;
                    }
                    if (method.getClassName().startsWith("org.teavm.")) {
                        queue.add(method);
                    } else {
                        found.add(method.getClassName() + "." + method.getName());
                    }
                }
            }
        }
        return found;
    }

    private static void becomeCall(WasmFunction function, WasmFunction target) {
        function.setImportModule(null);
        function.setImportName(null);
        var builder = function.getBody().builder();
        for (var type : function.getType().getParameterTypes()) {
            var local = new WasmLocal(type);
            function.add(local);
            builder.getLocal(local);
        }
        builder.call(target);
    }
}
