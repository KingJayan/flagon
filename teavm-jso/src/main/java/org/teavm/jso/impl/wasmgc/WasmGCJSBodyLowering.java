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

import java.util.ArrayList;
import java.util.List;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsicContext;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.jso.impl.JSBodyAstEmitter;
import org.teavm.rhino.javascript.ast.AstNode;
import org.teavm.rhino.javascript.ast.AstRoot;
import org.teavm.rhino.javascript.ast.Block;
import org.teavm.rhino.javascript.ast.ExpressionStatement;
import org.teavm.rhino.javascript.ast.FunctionCall;
import org.teavm.rhino.javascript.ast.Name;
import org.teavm.rhino.javascript.ast.NewExpression;
import org.teavm.rhino.javascript.ast.PropertyGet;
import org.teavm.rhino.javascript.ast.ReturnStatement;
import org.teavm.rhino.javascript.ast.StringLiteral;

/**
 * Writes a simple JSBody as backend operations. A body this class does not know keeps its text.
 */
final class WasmGCJSBodyLowering {

    private final WasmGCJsoCommonGenerator commonGen;
    private final WasmGCJSFunctions jsFunctions;

    private List<String> names;

    WasmGCJSBodyLowering(WasmGCJsoCommonGenerator commonGen, WasmGCJSFunctions jsFunctions) {
        this.commonGen = commonGen;
        this.jsFunctions = jsFunctions;
    }

    boolean apply(JSBodyAstEmitter emitter, boolean inlined, InvocationExpr invocation,
            WasmGCInlineIntrinsicContext context, WasmInstructionBuilder builder) {
        if (emitter.imports().length > 0) {
            return false;
        }
        var value = valueOf(emitter, inlined);
        if (value == null) {
            return false;
        }
        names = new ArrayList<>();
        if (!emitter.isStatic()) {
            names.add("__this__");
        }
        names.addAll(List.of(emitter.parameterNames()));
        if (names.size() != invocation.getArguments().size()) {
            return false;
        }
        if (!readsEveryArgumentInOrder(value)) {
            return false;
        }
        write(value, invocation, context, builder);
        return true;
    }

    /** the one expression a body gives back */
    private AstNode valueOf(JSBodyAstEmitter emitter, boolean inlined) {
        var ast = emitter.ast;
        if (inlined) {
            return ast;
        }
        var statements = statementsOf(ast);
        if (statements == null || statements.size() != 1) {
            return null;
        }
        var only = statements.get(0);
        if (only instanceof ReturnStatement) {
            return ((ReturnStatement) only).getReturnValue();
        }
        return null;
    }

    private static List<AstNode> statementsOf(AstNode ast) {
        var found = new ArrayList<AstNode>();
        if (ast instanceof Block) {
            for (var node : (Block) ast) {
                found.add((AstNode) node);
            }
        } else if (ast instanceof AstRoot) {
            for (var node : (AstRoot) ast) {
                found.add((AstNode) node);
            }
        } else if (ast instanceof ExpressionStatement) {
            found.add(ast);
        } else {
            return null;
        }
        return found;
    }

    /** a java argument must run one time, in order */
    private boolean readsEveryArgumentInOrder(AstNode value) {
        var read = new ArrayList<Integer>();
        if (!collect(value, read) || read.size() != names.size()) {
            return false;
        }
        for (var at = 0; at < read.size(); ++at) {
            if (read.get(at) != at) {
                return false;
            }
        }
        return true;
    }

    private boolean collect(AstNode node, List<Integer> read) {
        if (node instanceof Name) {
            var at = names.indexOf(((Name) node).getIdentifier());
            if (at >= 0) {
                read.add(at);
            }
            return true;
        }
        if (node instanceof StringLiteral) {
            return true;
        }
        if (node instanceof PropertyGet) {
            return collect(((PropertyGet) node).getTarget(), read);
        }
        if (node instanceof FunctionCall) {
            var call = (FunctionCall) node;
            var target = call.getTarget();
            if (target instanceof PropertyGet) {
                if (!collect(((PropertyGet) target).getTarget(), read)) {
                    return false;
                }
            } else if (!(target instanceof Name)) {
                return false;
            }
            for (var argument : call.getArguments()) {
                if (!collect(argument, read)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void write(AstNode node, InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        if (node instanceof Name) {
            writeName((Name) node, invocation, context, builder);
        } else if (node instanceof StringLiteral) {
            builder.getGlobal(commonGen.jsStringConstant(((StringLiteral) node).getValue()));
        } else if (node instanceof PropertyGet) {
            var get = (PropertyGet) node;
            write(get.getTarget(), invocation, context, builder);
            builder.getGlobal(commonGen.jsStringConstant(get.getProperty().getIdentifier()));
            builder.call(jsFunctions.getGet());
        } else if (node instanceof NewExpression) {
            var made = (NewExpression) node;
            write(made.getTarget(), invocation, context, builder);
            for (var argument : made.getArguments()) {
                write((AstNode) argument, invocation, context, builder);
            }
            builder.call(jsFunctions.getNew(made.getArguments().size()));
        } else {
            writeCall((FunctionCall) node, invocation, context, builder);
        }
    }

    private void writeCall(FunctionCall call, InvocationExpr invocation,
            WasmGCInlineIntrinsicContext context, WasmInstructionBuilder builder) {
        var target = call.getTarget();
        String method;
        if (target instanceof PropertyGet) {
            var get = (PropertyGet) target;
            write(get.getTarget(), invocation, context, builder);
            method = get.getProperty().getIdentifier();
        } else {
            // a bare name is a call on the page
            builder.nullConst(WasmType.EXTERN);
            method = ((Name) target).getIdentifier();
        }
        builder.getGlobal(commonGen.jsStringConstant(method));
        for (var argument : call.getArguments()) {
            write((AstNode) argument, invocation, context, builder);
        }
        builder.call(jsFunctions.getMethodCaller(call.getArguments().size()));
    }

    private void writeName(Name name, InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var at = names.indexOf(name.getIdentifier());
        if (at < 0) {
            builder.getGlobal(commonGen.jsStringConstant(name.getIdentifier()));
            builder.call(jsFunctions.getGlobalReader());
        } else {
            context.generate(builder, invocation.getArguments().get(at));
        }
    }
}
