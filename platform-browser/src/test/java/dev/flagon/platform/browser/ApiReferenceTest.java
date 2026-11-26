package dev.flagon.platform.browser;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import org.junit.jupiter.api.Test;

import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** the site's api reference names every public type and member of the modules it covers */
class ApiReferenceTest {

    private static final Path ROOT = Path.of("..");
    private static final List<String> SOURCES = List.of(
            "runtime/src/main/java",
            "platform-api/src/main/java",
            "platform-browser/src/main/java",
            "platform-wasi/src/main/java",
            "toolchain/reflect-policy/src/main/java/dev/flagon/reflect/Reflectable.java");
    /** members mirror a web api, which the reference links to instead */
    private static final Set<String> MIRRORS = Set.of("WebGL2");

    @Test
    void namesEveryPublicTypeAndMember() throws IOException {
        String reference = Files.readString(ROOT.resolve("web/docs/api.md"));
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager files = javac.getStandardFileManager(null, null, null);
        List<Path> sources = new ArrayList<>();
        for (String source : SOURCES) {
            try (Stream<Path> walk = Files.walk(ROOT.resolve(source))) {
                walk.filter(path -> path.toString().endsWith(".java")).forEach(sources::add);
            }
        }
        JavacTask task = (JavacTask) javac.getTask(null, files, diagnostic -> { },
                List.of("-proc:none"), null, files.getJavaFileObjectsFromPaths(sources));
        List<String> missing = new ArrayList<>();
        for (CompilationUnitTree unit : task.parse()) {
            for (Tree type : unit.getTypeDecls()) {
                if (type instanceof ClassTree declared
                        && declared.getModifiers().getFlags().contains(Modifier.PUBLIC)) {
                    check(reference, declared, "", missing);
                }
            }
        }
        assertEquals(List.of(), missing);
    }

    private static void check(String reference, ClassTree type, String outer,
                              List<String> missing) {
        String name = outer + type.getSimpleName();
        int start = reference.indexOf("\n### " + name + "\n");
        if (start < 0) {
            missing.add(name);
            return;
        }
        int end = reference.indexOf("\n### ", start + 1);
        String section = reference.substring(start, end < 0 ? reference.length() : end);
        boolean contract = type.getKind() == Tree.Kind.INTERFACE
                || type.getKind() == Tree.Kind.ANNOTATION_TYPE;
        for (Tree member : type.getMembers()) {
            String shown = null;
            if (member instanceof MethodTree method
                    && visible(method.getModifiers().getFlags(), contract)
                    && !overrides(method) && !method.getName().contentEquals("<init>")) {
                shown = " " + method.getName() + "(";
            } else if (member instanceof VariableTree field
                    && (visible(field.getModifiers().getFlags(), contract)
                    || type.getKind() == Tree.Kind.ENUM && field.getType() == null)) {
                shown = field.getName().toString();
            } else if (member instanceof ClassTree inner
                    && visible(inner.getModifiers().getFlags(), contract)) {
                check(reference, inner, name + ".", missing);
            }
            if (shown != null && !MIRRORS.contains(name) && !section.contains(shown)) {
                missing.add(name + shown.trim());
            }
        }
    }

    private static boolean visible(Set<Modifier> modifiers, boolean contract) {
        return modifiers.contains(Modifier.PUBLIC)
                || contract && !modifiers.contains(Modifier.PRIVATE);
    }

    private static boolean overrides(MethodTree method) {
        return method.getModifiers().getAnnotations().stream()
                .anyMatch(annotation -> "Override".equals(
                        annotation.getAnnotationType().toString()));
    }
}
