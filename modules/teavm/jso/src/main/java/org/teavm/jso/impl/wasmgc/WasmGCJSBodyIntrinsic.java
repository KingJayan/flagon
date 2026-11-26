/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.jso.impl.wasmgc;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsicContext;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.jso.impl.JSBodyAstEmitter;
import org.teavm.jso.impl.JSBodyEmitter;
import org.teavm.model.ValueType;

class WasmGCJSBodyIntrinsic implements WasmGCInlineIntrinsic {
    private JSBodyEmitter emitter;
    private boolean inlined;
    private WasmGCJsoCommonGenerator commonGen;
    private WasmGlobal global;
    private WasmGCJSFunctions jsFunctions;

    WasmGCJSBodyIntrinsic(JSBodyEmitter emitter, boolean inlined, WasmGCJsoCommonGenerator commonGen,
            WasmGCJSFunctions jsFunctions) {
        this.emitter = emitter;
        this.inlined = inlined;
        this.commonGen = commonGen;
        this.jsFunctions = jsFunctions;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        if (emitter instanceof JSBodyAstEmitter) {
            var lowering = new WasmGCJSBodyLowering(commonGen, jsFunctions);
            var wantsValue = invocation.getMethod().getReturnType() != ValueType.VOID;
            var written = lowering.apply((JSBodyAstEmitter) emitter, inlined, wantsValue, invocation,
                    context, builder);
            if (written != null) {
                if (written == WasmGCJSBodyLowering.Result.VALUE && !wantsValue) {
                    builder.drop();
                }
                return;
            }
        }
        if (global == null) {
            System.err.println("flagon: " + invocation.getMethod()
                    + " keeps its javascript text, so this bundle needs unsafe-eval");
            global = commonGen.addJSBody(emitter, inlined);
        }
        var caller = jsFunctions.getFunctionCaller(invocation.getArguments().size());
        builder.getGlobal(global);
        for (var arg : invocation.getArguments()) {
            context.generate(builder, arg);
        }
        builder.call(caller);
        if (invocation.getMethod().getReturnType() == ValueType.VOID) {
            builder.drop();
        }
    }
}
