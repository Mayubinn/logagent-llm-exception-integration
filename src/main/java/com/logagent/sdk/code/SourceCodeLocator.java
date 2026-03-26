package com.logagent.sdk.code;

import com.logagent.sdk.ExceptionAnalyzerConfig;
import com.logagent.sdk.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 定位异常堆栈对应的源码或反编译代码。
 */
final class SourceCodeLocator {

    private static final Logger log = LoggerFactory.getLogger(SourceCodeLocator.class);

    CodeSnippet locate(String label, StackTraceElement frame, ExceptionAnalyzerConfig config) {
        if (frame == null) {
            return new CodeSnippet(label, null, null, null, -1,
                    CodeSnippet.SourceType.UNAVAILABLE, null, null, "未找到堆栈帧");
        }

        String className = frame.getClassName();
        String methodName = frame.getMethodName();
        String fileName = frame.getFileName();
        int lineNumber = frame.getLineNumber();

        try {
            Class<?> targetClass = loadClass(className);
            String sourcePath = buildSourcePath(className, fileName);

            String classpathSource = readFromClasspath(targetClass, sourcePath);
            if (!TextUtils.isBlank(classpathSource)) {
                return buildSnippet(label, frame, CodeSnippet.SourceType.CLASSPATH_SOURCE,
                        sourcePath, classpathSource, config, false);
            }

            File sourceFile = resolveSourceFile(targetClass, sourcePath, config);
            if (sourceFile != null && sourceFile.isFile()) {
                String source = readFile(sourceFile);
                return buildSnippet(label, frame, CodeSnippet.SourceType.SOURCE_FILE,
                        sourceFile.getAbsolutePath(), source, config, false);
            }

            if (config.isSearchSourcesJar()) {
                JarSource jarSource = searchInSourcesJar(targetClass, sourcePath);
                if (jarSource != null && !TextUtils.isBlank(jarSource.source)) {
                    return buildSnippet(label, frame, CodeSnippet.SourceType.SOURCES_JAR,
                            jarSource.origin, jarSource.source, config, false);
                }
            }

            if (config.isDecompileWhenSourceMissing()) {
                String decompiled = tryDecompile(targetClass, className);
                if (!TextUtils.isBlank(decompiled)) {
                    return buildSnippet(label, frame, CodeSnippet.SourceType.DECOMPILED,
                            "CFR(" + className + ")", decompiled, config, true);
                }
            }

            String stub = buildReflectionStub(className, methodName, lineNumber);
            if (!TextUtils.isBlank(stub)) {
                return new CodeSnippet(label, className, methodName, fileName, lineNumber,
                        CodeSnippet.SourceType.REFLECTION_STUB, "reflection", stub,
                        "源码不可达，已退化为反射生成的方法骨架，仅供定位参考");
            }

            return new CodeSnippet(label, className, methodName, fileName, lineNumber,
                    CodeSnippet.SourceType.UNAVAILABLE, null, null,
                    "未能找到源码、sources.jar 或可反编译 class");
        } catch (Throwable ex) {
            log.debug("Locate source failed for {}: {}", className, ex.getMessage());
            return new CodeSnippet(label, className, methodName, fileName, lineNumber,
                    CodeSnippet.SourceType.UNAVAILABLE, null, null, ex.getMessage());
        }
    }

    private Class<?> loadClass(String className) throws ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            try {
                return Class.forName(className, false, classLoader);
            } catch (ClassNotFoundException ignore) {
                // continue fallback
            }
        }
        return Class.forName(className);
    }

    private String buildSourcePath(String className, String fileName) {
        int lastDot = className.lastIndexOf('.');
        String packagePath = lastDot < 0 ? "" : className.substring(0, lastDot).replace('.', '/');
        String fallbackFileName = className.substring(lastDot + 1);
        int innerClassIndex = fallbackFileName.indexOf('$');
        if (innerClassIndex > 0) {
            fallbackFileName = fallbackFileName.substring(0, innerClassIndex);
        }
        String javaFileName = !TextUtils.isBlank(fileName) ? fileName : fallbackFileName + ".java";
        return packagePath.isEmpty() ? javaFileName : packagePath + "/" + javaFileName;
    }

    private String readFromClasspath(Class<?> targetClass, String sourcePath) {
        if (targetClass == null || TextUtils.isBlank(sourcePath)) {
            return null;
        }
        ClassLoader classLoader = targetClass.getClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        if (classLoader == null) {
            return null;
        }
        InputStream in = classLoader.getResourceAsStream(sourcePath);
        if (in == null) {
            return null;
        }
        try {
            return readStream(in);
        } catch (IOException ex) {
            return null;
        }
    }

    private File resolveSourceFile(Class<?> targetClass, String sourcePath, ExceptionAnalyzerConfig config) {
        List<File> roots = new ArrayList<File>();
        Set<String> seen = new HashSet<String>();

        addCandidateRoots(roots, seen, config.getSourceRoots(), null);
        addCandidateRoots(roots, seen, config.getSourceRoots(), new File(System.getProperty("user.dir", ".")));

        File codeSource = resolveCodeSourceLocation(targetClass);
        if (codeSource != null) {
            if (codeSource.isDirectory()) {
                File projectRoot = detectProjectRootFromClassesDir(codeSource);
                if (projectRoot != null) {
                    addCandidateRoots(roots, seen, config.getSourceRoots(), projectRoot);
                }
            } else {
                File jarParent = codeSource.getParentFile();
                if (jarParent != null) {
                    addCandidateRoots(roots, seen, config.getSourceRoots(), jarParent);
                    if (jarParent.getParentFile() != null) {
                        addCandidateRoots(roots, seen, config.getSourceRoots(), jarParent.getParentFile());
                    }
                }
            }
        }

        for (File root : roots) {
            if (root == null) {
                continue;
            }
            File sourceFile = new File(root, sourcePath);
            if (sourceFile.isFile()) {
                return sourceFile;
            }
        }
        return null;
    }

    private void addCandidateRoots(List<File> roots, Set<String> seen, List<String> configuredRoots, File baseDir) {
        if (configuredRoots == null) {
            return;
        }
        for (String root : configuredRoots) {
            if (TextUtils.isBlank(root)) {
                continue;
            }
            File candidate = baseDir == null ? new File(root) : new File(baseDir, root);
            try {
                String path = candidate.getCanonicalPath();
                if (seen.add(path)) {
                    roots.add(candidate);
                }
            } catch (IOException ignore) {
                // skip invalid root
            }
        }
    }

    private File detectProjectRootFromClassesDir(File classesDir) {
        String path = classesDir.getAbsolutePath().replace('\\', '/');
        if (path.endsWith("/target/classes")) {
            return new File(path.substring(0, path.length() - "/target/classes".length()));
        }
        if (path.endsWith("/build/classes/java/main")) {
            return new File(path.substring(0, path.length() - "/build/classes/java/main".length()));
        }
        return classesDir.getParentFile();
    }

    private File resolveCodeSourceLocation(Class<?> targetClass) {
        try {
            if (targetClass != null
                    && targetClass.getProtectionDomain() != null
                    && targetClass.getProtectionDomain().getCodeSource() != null
                    && targetClass.getProtectionDomain().getCodeSource().getLocation() != null) {
                URL location = targetClass.getProtectionDomain().getCodeSource().getLocation();
                if ("file".equalsIgnoreCase(location.getProtocol())) {
                    return new File(location.toURI());
                }
            }
        } catch (Exception ignore) {
            // ignore
        }
        return null;
    }

    private JarSource searchInSourcesJar(Class<?> targetClass, String sourcePath) {
        List<File> candidates = new ArrayList<File>();
        File codeSource = resolveCodeSourceLocation(targetClass);
        if (codeSource != null && codeSource.isFile()) {
            String name = codeSource.getName();
            int dotIndex = name.lastIndexOf('.');
            String baseName = dotIndex > 0 ? name.substring(0, dotIndex) : name;
            File sibling = new File(codeSource.getParentFile(), baseName + "-sources.jar");
            if (sibling.isFile()) {
                candidates.add(sibling);
            }
            File plainSources = new File(codeSource.getParentFile(), "sources.jar");
            if (plainSources.isFile()) {
                candidates.add(plainSources);
            }
        }

        String classPath = System.getProperty("java.class.path", "");
        for (String path : classPath.split(File.pathSeparator)) {
            if (path.endsWith("-sources.jar")) {
                File jar = new File(path);
                if (jar.isFile()) {
                    candidates.add(jar);
                }
            }
        }

        for (File candidate : candidates) {
            JarSource source = readSourceFromZip(candidate, sourcePath);
            if (source != null) {
                return source;
            }
        }
        return null;
    }

    private JarSource readSourceFromZip(File zipFile, String sourcePath) {
        ZipFile zip = null;
        try {
            zip = new ZipFile(zipFile);
            ZipEntry directEntry = zip.getEntry(sourcePath);
            if (directEntry != null) {
                return new JarSource(readZipEntry(zip, directEntry), zipFile.getAbsolutePath() + "!" + sourcePath);
            }
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry == null || entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                if (entryName.endsWith(sourcePath)) {
                    return new JarSource(readZipEntry(zip, entry), zipFile.getAbsolutePath() + "!" + entryName);
                }
            }
            return null;
        } catch (Exception ex) {
            return null;
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    private String readZipEntry(ZipFile zip, ZipEntry entry) throws IOException {
        InputStream in = zip.getInputStream(entry);
        try {
            return readStream(in);
        } finally {
            in.close();
        }
    }

    private String tryDecompile(Class<?> targetClass, String className) {
        byte[] classBytes = readClassBytes(targetClass, className);
        if (classBytes == null || classBytes.length == 0) {
            return null;
        }
        return new OptionalCfrDecompiler().decompile(className, classBytes);
    }

    private byte[] readClassBytes(Class<?> targetClass, String className) {
        String classResource = className.replace('.', '/') + ".class";
        InputStream in = null;
        try {
            ClassLoader classLoader = targetClass == null ? null : targetClass.getClassLoader();
            if (classLoader != null) {
                in = classLoader.getResourceAsStream(classResource);
            }
            if (in == null) {
                in = ClassLoader.getSystemResourceAsStream(classResource);
            }
            if (in == null) {
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        } catch (IOException ex) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    private CodeSnippet buildSnippet(String label,
                                     StackTraceElement frame,
                                     CodeSnippet.SourceType sourceType,
                                     String origin,
                                     String source,
                                     ExceptionAnalyzerConfig config,
                                     boolean preferMethodExtract) {
        String excerpt = extractRelevantSnippet(source, frame, config, preferMethodExtract);
        String note = sourceType == CodeSnippet.SourceType.DECOMPILED
                ? "源码不可用，已使用 class 反编译后的 Java 代码辅助推断，行号仅作近似定位"
                : null;
        return new CodeSnippet(label,
                frame.getClassName(),
                frame.getMethodName(),
                frame.getFileName(),
                frame.getLineNumber(),
                sourceType,
                origin,
                excerpt,
                note);
    }

    private String extractRelevantSnippet(String source,
                                          StackTraceElement frame,
                                          ExceptionAnalyzerConfig config,
                                          boolean preferMethodExtract) {
        if (TextUtils.isBlank(source)) {
            return source;
        }
        List<String> lines = Arrays.asList(TextUtils.normalizeLineBreaks(source).split("\n", -1));
        int targetLine = frame.getLineNumber();
        if (!preferMethodExtract && targetLine > 0 && targetLine <= lines.size()) {
            return formatWindow(lines, targetLine, config.getCodeContextLinesBefore(), config.getCodeContextLinesAfter());
        }
        String methodExcerpt = extractAroundMethodName(lines, frame.getClassName(), frame.getMethodName(), config);
        if (!TextUtils.isBlank(methodExcerpt)) {
            return methodExcerpt;
        }
        int end = Math.min(lines.size(), config.getCodeContextLinesBefore() + config.getCodeContextLinesAfter() + 1);
        return formatWindow(lines, 1, 0, Math.max(0, end - 1));
    }

    private String extractAroundMethodName(List<String> lines,
                                           String className,
                                           String methodName,
                                           ExceptionAnalyzerConfig config) {
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        String constructorName = className;
        int dotIndex = constructorName.lastIndexOf('.');
        if (dotIndex >= 0) {
            constructorName = constructorName.substring(dotIndex + 1);
        }
        int innerIndex = constructorName.indexOf('$');
        if (innerIndex > 0) {
            constructorName = constructorName.substring(0, innerIndex);
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null) {
                continue;
            }
            String matchToken = methodName;
            if ("<init>".equals(methodName)) {
                matchToken = constructorName;
            }
            if (line.contains(matchToken + "(")) {
                int lineNumber = i + 1;
                return formatWindow(lines, lineNumber, config.getCodeContextLinesBefore(), config.getCodeContextLinesAfter());
            }
        }
        return null;
    }

    private String formatWindow(List<String> lines, int centerLine, int before, int after) {
        int total = lines == null ? 0 : lines.size();
        if (total == 0) {
            return "";
        }
        int start = Math.max(1, centerLine - before);
        int end = Math.min(total, centerLine + after);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            String marker = i == centerLine ? ">>>" : "   ";
            sb.append(marker)
                    .append(String.format("%5d | ", i))
                    .append(lines.get(i - 1))
                    .append('\n');
        }
        return sb.toString().trim();
    }

    private String buildReflectionStub(String className, String methodName, int lineNumber) {
        try {
            Class<?> clazz = loadClass(className);
            StringBuilder sb = new StringBuilder(512);
            sb.append("// Source unavailable, generated from reflection for exception location\n");
            sb.append(Modifier.toString(clazz.getModifiers())).append(' ')
                    .append(clazz.isInterface() ? "interface " : "class ")
                    .append(clazz.getName()).append(" {\n");

            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                if ("<init>".equals(methodName)) {
                    sb.append("    ")
                            .append(Modifier.toString(constructor.getModifiers())).append(' ')
                            .append(clazz.getSimpleName()).append('(')
                            .append(joinParameterTypes(constructor.getParameterTypes()))
                            .append(") { /* original source unavailable */ }\n");
                }
            }
            for (Method method : clazz.getDeclaredMethods()) {
                if (methodName.equals(method.getName())) {
                    sb.append("    ")
                            .append(Modifier.toString(method.getModifiers())).append(' ')
                            .append(method.getReturnType().getTypeName()).append(' ')
                            .append(method.getName()).append('(')
                            .append(joinParameterTypes(method.getParameterTypes()))
                            .append(") {\n")
                            .append("        // original line: ").append(lineNumber).append("\n")
                            .append("        throw new UnsupportedOperationException(\"Source unavailable\");\n")
                            .append("    }\n");
                }
            }
            sb.append("}\n");
            return sb.toString();
        } catch (Throwable ex) {
            return null;
        }
    }

    private String joinParameterTypes(Class<?>[] parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(parameterTypes[i].getTypeName()).append(" arg").append(i);
        }
        return sb.toString();
    }

    private String readFile(File file) throws IOException {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            FileInputStream in = new FileInputStream(file);
            try {
                byte[] data = new byte[(int) file.length()];
                int len = in.read(data);
                if (len <= 0) {
                    return "";
                }
                try {
                    return new String(data, 0, len, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return new String(data, 0, len, Charset.defaultCharset());
                }
            } finally {
                in.close();
            }
        }
    }

    private String readStream(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    private static final class JarSource {
        private final String source;
        private final String origin;

        private JarSource(String source, String origin) {
            this.source = source;
            this.origin = origin;
        }
    }
}
