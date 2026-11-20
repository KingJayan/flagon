package dev.flagon.shims;

import dev.flagon.classfile.Loadable;

import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyListener;
import org.teavm.dependency.FieldDependency;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class KeepNamedClasses implements TeaVMPlugin, DependencyListener,
        ClassHolderTransformer {

    public static final String PATTERNS = "flagon.preserve";
    /** a classpath resource that lists one class per line */
    public static final String PRESERVED = "META-INF/flagon/preserved.txt";
    /** a class that only reaches its helpers through an absent library is left out */
    private static final int DEPTH = 1;

    private static final String INDEX = "dev.flagon.platform.KeptClasses";
    private static final MethodDescriptor NAMES =
            MethodDescriptor.parse("names()Ljava/lang/String;");

    private ClassLoader loader;
    private List<Pattern> wanted = List.of();
    private Set<String> listed = Set.of();
    private final List<String> kept = new ArrayList<>();

    @Override
    public void install(TeaVMHost host) {
        loader = BuildClasspath.of(host);
        wanted = patterns(host.getProperties().getProperty(PATTERNS, ""));
        listed = listed(loader);
        if (!wanted.isEmpty() || !listed.isEmpty()) {
            host.add((DependencyListener) this);
            host.add((ClassHolderTransformer) this);
        }
    }

    @Override
    public void started(DependencyAgent agent) {
        Loadable runnable = new Loadable(
                new CompiledClasses(loader, agent.getClassSource()));
        int skipped = 0;
        for (String name : classpath()) {
            if (!listed.contains(name)
                    && wanted.stream().noneMatch(one -> one.matcher(name).matches())) {
                continue;
            }
            if (runnable.test(name, DEPTH)) {
                agent.linkClass(name);
                kept.add(name);
            } else {
                skipped++;
            }
        }
        System.out.println("flagon: keeping " + kept.size() + " classes that " + PATTERNS
                + " names, and leaving out " + skipped + " that the classpath cannot run");
    }

    @Override
    public void transformClass(ClassHolder type, ClassHolderTransformerContext context) {
        if (!INDEX.equals(type.getName())) {
            return;
        }
        MethodHolder names = type.getMethod(NAMES);
        if (names != null) {
            names.setProgram(listing(String.join(",", kept)));
        }
    }

    private static Program listing(String names) {
        Program program = new Program();
        program.createVariable();
        BasicBlock block = program.createBasicBlock();

        Variable listed = program.createVariable();
        StringConstantInstruction text = new StringConstantInstruction();
        text.setReceiver(listed);
        text.setConstant(names);
        block.add(text);

        ExitInstruction give = new ExitInstruction();
        give.setValueToReturn(listed);
        block.add(give);
        return program;
    }

    @Override
    public void classReached(DependencyAgent agent, String type) {
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
    }

    @Override
    public void fieldReached(DependencyAgent agent, FieldDependency field) {
    }

    @Override
    public void completing(DependencyAgent agent) {
    }

    @Override
    public void complete() {
    }

    private static Set<String> listed(ClassLoader loader) {
        Set<String> names = new HashSet<>();
        try {
            Enumeration<URL> lists = loader.getResources(PRESERVED);
            while (lists.hasMoreElements()) {
                try (InputStream list = lists.nextElement().openStream()) {
                    for (String line : new String(list.readAllBytes(), StandardCharsets.UTF_8)
                            .split("\\R")) {
                        if (!line.isBlank()) {
                            names.add(line.trim());
                        }
                    }
                }
            }
        } catch (IOException unreadable) {
            throw new UncheckedIOException("cannot read " + PRESERVED, unreadable);
        }
        return names;
    }

    // * stops at a package boundary, ** does not
    private static List<Pattern> patterns(String globs) {
        List<Pattern> wanted = new ArrayList<>();
        for (String glob : globs.split("[,\\s]+")) {
            if (!glob.isBlank()) {
                wanted.add(Pattern.compile(regexOf(glob.trim())));
            }
        }
        return wanted;
    }

    private static String regexOf(String glob) {
        StringBuilder pattern = new StringBuilder();
        int at = 0;
        while (at < glob.length()) {
            char letter = glob.charAt(at);
            if (letter == '*') {
                boolean deep = at + 1 < glob.length() && glob.charAt(at + 1) == '*';
                pattern.append(deep ? ".*" : "[^.]*");
                at += deep ? 2 : 1;
            } else {
                pattern.append(Pattern.quote(String.valueOf(letter)));
                at++;
            }
        }
        return pattern.toString();
    }

    private List<String> classpath() {
        if (!(loader instanceof URLClassLoader urls)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (URL url : urls.getURLs()) {
            Path path = Path.of(url.getPath());
            try {
                if (Files.isDirectory(path)) {
                    readDirectory(path, names);
                } else if (path.toString().endsWith(".jar")) {
                    readJar(path, names);
                }
            } catch (IOException unreadable) {
                throw new UncheckedIOException(unreadable);
            }
        }
        return names;
    }

    private static void readJar(Path jar, List<String> names) throws IOException {
        try (JarFile opened = new JarFile(jar.toFile())) {
            Enumeration<JarEntry> entries = opened.entries();
            while (entries.hasMoreElements()) {
                named(entries.nextElement().getName(), names);
            }
        }
    }

    private static void readDirectory(Path root, List<String> names) throws IOException {
        try (Stream<Path> found = Files.walk(root)) {
            found.filter(Files::isRegularFile)
                    .forEach(file -> named(root.relativize(file).toString(), names));
        }
    }

    private static void named(String entry, List<String> names) {
        if (entry.endsWith(".class") && !entry.contains("-")) {
            names.add(entry.substring(0, entry.length() - ".class".length())
                    .replace('/', '.').replace(java.io.File.separatorChar, '.'));
        }
    }
}
