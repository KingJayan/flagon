/*
 *  Copyright 2026 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
// flagon: an export of the program takes and gives numbers only, which wasm passes without glue
package org.teavm.backend.wasm.dependencies;

import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.model.CallLocation;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;
import org.teavm.dependency.DependencyAgent;
import org.teavm.interop.Export;

public class WasmGCExportDependencyListener extends AbstractDependencyListener {
    @Override
    public void classReached(DependencyAgent agent, String className) {
        for (var reader : agent.getClassSource().get(className).getMethods()) {
            var annotation = reader.getAnnotations().get(Export.class.getName());
            if (annotation != null) {
                if (!className.startsWith("org.teavm.")) {
                    check(agent, reader);
                }
                agent.linkMethod(reader.getReference()).use();
            }
        }
    }

    private static void check(DependencyAgent agent, MethodReader method) {
        var location = new CallLocation(method.getReference());
        if (!method.hasModifier(ElementModifier.STATIC)) {
            agent.getDiagnostics().error(location, "@Export {{m0}} must be static", method.getReference());
            return;
        }
        for (var type : method.getSignature()) {
            if (!(type instanceof ValueType.Primitive) && type != ValueType.VOID) {
                agent.getDiagnostics().error(location, "@Export {{m0}} takes or returns an object; an export "
                        + "passes only numbers and booleans", method.getReference());
                return;
            }
        }
    }
}
