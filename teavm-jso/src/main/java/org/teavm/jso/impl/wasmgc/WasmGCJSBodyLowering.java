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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsicContext;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.jso.impl.JSBodyAstEmitter;
import org.teavm.rhino.javascript.Token;
import org.teavm.rhino.javascript.ast.AstNode;
import org.teavm.rhino.javascript.ast.AstRoot;
import org.teavm.rhino.javascript.ast.Block;
import org.teavm.rhino.javascript.ast.ExpressionStatement;
import org.teavm.rhino.javascript.ast.FunctionCall;
import org.teavm.rhino.javascript.ast.InfixExpression;
import org.teavm.rhino.javascript.ast.KeywordLiteral;
import org.teavm.rhino.javascript.ast.Name;
import org.teavm.rhino.javascript.ast.NewExpression;
import org.teavm.rhino.javascript.ast.NumberLiteral;
import org.teavm.rhino.javascript.ast.ObjectLiteral;
import org.teavm.rhino.javascript.ast.ParenthesizedExpression;
import org.teavm.rhino.javascript.ast.PropertyGet;
import org.teavm.rhino.javascript.ast.ReturnStatement;
import org.teavm.rhino.javascript.ast.StringLiteral;

/**
 * Writes a simple JSBody as the operations the backend already imports, so that the page does not
 * build a function from text. A body this class does not know falls back to the text.
 */
final class WasmGCJSBodyLowering {

    /** what the written body left on the stack, or null when this class does not know the body */
    enum Result {
        VALUE,
        NOTHING
    }

    private final WasmGCJsoCommonGenerator commonGen;
    private final WasmGCJSFunctions jsFunctions;

    private final Map<String, WasmLocal> arguments = new HashMap<>();

    WasmGCJSBodyLowering(WasmGCJsoCommonGenerator commonGen, WasmGCJSFunctions jsFunctions) {
        this.commonGen = commonGen;
        this.jsFunctions = jsFunctions;
    }

    Result apply(JSBodyAstEmitter emitter, boolean inlined, boolean wantsValue, InvocationExpr invocation,
            WasmGCInlineIntrinsicContext context, WasmInstructionBuilder builder) {
        if (emitter.imports().length > 0) {
            return null;
        }
        var names = new ArrayList<String>();
        if (!emitter.isStatic()) {
            names.add("__this__");
        }
        names.addAll(List.of(emitter.parameterNames()));
        if (names.size() != invocation.getArguments().size()) {
            return null;
        }
        var body = statementsOf(emitter, inlined);
        if (body == null || !known(body, names) || wantsValue && !endsWithValue(body)) {
            return null;
        }

        var taken = new ArrayList<WasmLocal>();
        for (var at = 0; at < names.size(); ++at) {
            var local = context.tempVars().acquire(WasmType.EXTERN);
            taken.add(local);
            arguments.put(names.get(at), local);
            context.generate(builder, invocation.getArguments().get(at));
            builder.setLocal(local);
        }

        var result = write(body, builder);

        for (var local : taken) {
            context.tempVars().release(local);
        }
        return result;
    }

    /** the statements of a body, with a lone expression counted as one return */
    private static List<AstNode> statementsOf(JSBodyAstEmitter emitter, boolean inlined) {
        var ast = emitter.ast;
        if (inlined) {
            var one = new ReturnStatement();
            one.setReturnValue(ast);
            return List.of(one);
        }
        var found = new ArrayList<AstNode>();
        if (ast instanceof Block) {
            for (var node : (Block) ast) {
                found.add((AstNode) node);
            }
        } else if (ast instanceof AstRoot) {
            for (var node : (AstRoot) ast) {
                found.add((AstNode) node);
            }
        } else {
            found.add(ast);
        }
        return found;
    }

    private static boolean endsWithValue(List<AstNode> body) {
        if (body.isEmpty()) {
            return false;
        }
        var last = body.get(body.size() - 1);
        return last instanceof ReturnStatement && ((ReturnStatement) last).getReturnValue() != null;
    }

    private boolean known(List<AstNode> body, List<String> names) {
        for (var at = 0; at < body.size(); ++at) {
            var statement = body.get(at);
            if (statement instanceof ReturnStatement) {
                if (at != body.size() - 1) {
                    return false;
                }
                var value = ((ReturnStatement) statement).getReturnValue();
                if (value != null && !known(value, names)) {
                    return false;
                }
            } else if (statement instanceof ExpressionStatement) {
                if (!isPut(((ExpressionStatement) statement).getExpression(), names)) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    /** a write to a property, which is the only statement this class knows apart from a return */
    private boolean isPut(AstNode node, List<String> names) {
        if (!(node instanceof InfixExpression) || node.getType() != Token.ASSIGN) {
            return false;
        }
        var assignment = (InfixExpression) node;
        return assignment.getLeft() instanceof PropertyGet
                && known(assignment.getLeft(), names)
                && known(assignment.getRight(), names);
    }

    private boolean known(AstNode node, List<String> names) {
        if (node instanceof Name) {
            return true;
        }
        if (node instanceof StringLiteral || node instanceof NumberLiteral) {
            return true;
        }
        if (node instanceof KeywordLiteral) {
            var kind = node.getType();
            return kind == Token.THIS && names.contains("__this__")
                    || kind == Token.TRUE || kind == Token.FALSE || kind == Token.NULL;
        }
        if (node instanceof ObjectLiteral) {
            return ((ObjectLiteral) node).getElements().isEmpty();
        }
        if (node instanceof ParenthesizedExpression) {
            return known(((ParenthesizedExpression) node).getExpression(), names);
        }
        if (node instanceof PropertyGet) {
            return known(((PropertyGet) node).getTarget(), names);
        }
        if (node instanceof NewExpression) {
            var made = (NewExpression) node;
            return made.getTarget() instanceof Name && knownAll(made.getArguments(), names);
        }
        if (node instanceof FunctionCall) {
            var call = (FunctionCall) node;
            var target = call.getTarget();
            if (target instanceof PropertyGet) {
                if (!known(((PropertyGet) target).getTarget(), names)) {
                    return false;
                }
            } else if (!(target instanceof Name)) {
                return false;
            }
            return knownAll(call.getArguments(), names);
        }
        if (node instanceof InfixExpression) {
            var kind = node.getType();
            if (kind != Token.SHEQ && kind != Token.SHNE && kind != Token.EQ && kind != Token.NE) {
                return false;
            }
            var comparison = (InfixExpression) node;
            return known(comparison.getLeft(), names) && known(comparison.getRight(), names);
        }
        if (node instanceof org.teavm.rhino.javascript.ast.UnaryExpression) {
            var unary = (org.teavm.rhino.javascript.ast.UnaryExpression) node;
            return unary.getType() == Token.TYPEOF && known(unary.getOperand(), names);
        }
        return false;
    }

    private boolean knownAll(List<AstNode> nodes, List<String> names) {
        for (var node : nodes) {
            if (!known(node, names)) {
                return false;
            }
        }
        return true;
    }

    private Result write(List<AstNode> body, WasmInstructionBuilder builder) {
        var result = Result.NOTHING;
        for (var statement : body) {
            if (statement instanceof ReturnStatement) {
                var value = ((ReturnStatement) statement).getReturnValue();
                if (value != null) {
                    write(value, builder);
                    result = Result.VALUE;
                }
            } else {
                var assignment = (InfixExpression) ((ExpressionStatement) statement).getExpression();
                var target = (PropertyGet) assignment.getLeft();
                write(target.getTarget(), builder);
                builder.getGlobal(commonGen.jsStringConstant(target.getProperty().getIdentifier()));
                write(assignment.getRight(), builder);
                builder.call(jsFunctions.getSetter());
            }
        }
        return result;
    }

    private void write(AstNode node, WasmInstructionBuilder builder) {
        if (node instanceof ParenthesizedExpression) {
            write(((ParenthesizedExpression) node).getExpression(), builder);
        } else if (node instanceof Name) {
            writeName(((Name) node).getIdentifier(), builder);
        } else if (node instanceof StringLiteral) {
            builder.getGlobal(commonGen.jsStringConstant(((StringLiteral) node).getValue()));
        } else if (node instanceof NumberLiteral) {
            builder.f64Const(((NumberLiteral) node).getNumber());
            builder.call(jsFunctions.getNumberWrapper());
        } else if (node instanceof KeywordLiteral) {
            writeKeyword(node.getType(), builder);
        } else if (node instanceof ObjectLiteral) {
            builder.call(jsFunctions.getOperation("emptyObject", 0));
        } else if (node instanceof PropertyGet) {
            var get = (PropertyGet) node;
            write(get.getTarget(), builder);
            builder.getGlobal(commonGen.jsStringConstant(get.getProperty().getIdentifier()));
            builder.call(jsFunctions.getGet());
        } else if (node instanceof NewExpression) {
            var made = (NewExpression) node;
            write(made.getTarget(), builder);
            for (var argument : made.getArguments()) {
                write(argument, builder);
            }
            builder.call(jsFunctions.getNew(made.getArguments().size()));
        } else if (node instanceof FunctionCall) {
            writeCall((FunctionCall) node, builder);
        } else if (node instanceof org.teavm.rhino.javascript.ast.UnaryExpression) {
            write(((org.teavm.rhino.javascript.ast.UnaryExpression) node).getOperand(), builder);
            builder.call(jsFunctions.getOperation("typeOf", 1));
        } else {
            writeComparison((InfixExpression) node, builder);
        }
    }

    private void writeComparison(InfixExpression comparison, WasmInstructionBuilder builder) {
        write(comparison.getLeft(), builder);
        write(comparison.getRight(), builder);
        var operation = switch (comparison.getType()) {
            case Token.SHEQ -> "strictEquals";
            case Token.SHNE -> "strictNotEquals";
            case Token.EQ -> "looseEquals";
            default -> "looseNotEquals";
        };
        builder.call(jsFunctions.getOperation(operation, 2));
    }

    private void writeCall(FunctionCall call, WasmInstructionBuilder builder) {
        var target = call.getTarget();
        String method;
        if (target instanceof PropertyGet) {
            var get = (PropertyGet) target;
            write(get.getTarget(), builder);
            method = get.getProperty().getIdentifier();
        } else {
            // a name on its own is a call on the page itself, which the loader looks up
            builder.nullConst(WasmType.EXTERN);
            method = ((Name) target).getIdentifier();
        }
        builder.getGlobal(commonGen.jsStringConstant(method));
        for (var argument : call.getArguments()) {
            write(argument, builder);
        }
        builder.call(jsFunctions.getMethodCaller(call.getArguments().size()));
    }

    private void writeKeyword(int kind, WasmInstructionBuilder builder) {
        if (kind == Token.THIS) {
            writeName("__this__", builder);
        } else if (kind == Token.NULL) {
            builder.nullConst(WasmType.EXTERN);
        } else {
            builder.i32Const(kind == Token.TRUE ? 1 : 0);
            builder.call(jsFunctions.getBooleanWrapper());
        }
    }

    private void writeName(String identifier, WasmInstructionBuilder builder) {
        var local = arguments.get(identifier);
        if (local != null) {
            builder.getLocal(local);
        } else {
            builder.getGlobal(commonGen.jsStringConstant(identifier));
            builder.call(jsFunctions.getGlobalReader());
        }
    }
}
