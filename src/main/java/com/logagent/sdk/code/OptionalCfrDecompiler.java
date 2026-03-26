package com.logagent.sdk.code;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 通过反射调用 CFR，避免 SDK 对其 API 产生编译期硬耦合。
 */
final class OptionalCfrDecompiler {

    private static final Logger log = LoggerFactory.getLogger(OptionalCfrDecompiler.class);

    String decompile(String className, byte[] classBytes) {
        if (classBytes == null || classBytes.length == 0) {
            return null;
        }
        File tempDir = null;
        try {
            tempDir = Files.createTempDirectory("logagent-cfr-").toFile();
            File classFile = new File(tempDir, className.replace('.', File.separatorChar) + ".class");
            File parent = classFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return null;
            }
            FileOutputStream out = new FileOutputStream(classFile);
            try {
                out.write(classBytes);
            } finally {
                out.close();
            }
            return invokeCfr(classFile);
        } catch (Throwable ex) {
            log.debug("CFR decompile failed for {}: {}", className, ex.getMessage());
            return null;
        } finally {
            if (tempDir != null) {
                deleteQuietly(tempDir);
            }
        }
    }

    private String invokeCfr(File classFile) throws Exception {
        final StringBuilder javaSource = new StringBuilder();

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = OptionalCfrDecompiler.class.getClassLoader();
        }

        Class<?> cfrDriverClass = Class.forName("org.benf.cfr.reader.api.CfrDriver", true, classLoader);
        Class<?> builderClass = Class.forName("org.benf.cfr.reader.api.CfrDriver$Builder", true, classLoader);
        Class<?> outputSinkFactoryClass = Class.forName("org.benf.cfr.reader.api.OutputSinkFactory", true, classLoader);

        Object sinkFactory = Proxy.newProxyInstance(classLoader,
                new Class<?>[]{outputSinkFactoryClass},
                new OutputSinkFactoryHandler(classLoader, javaSource));

        Object builder = builderClass.getDeclaredConstructor().newInstance();
        Method withOptions = builderClass.getMethod("withOptions", Map.class);
        Method withOutputSink = builderClass.getMethod("withOutputSink", outputSinkFactoryClass);
        Method build = builderClass.getMethod("build");

        builder = withOptions.invoke(builder, Collections.singletonMap("silent", "true"));
        builder = withOutputSink.invoke(builder, sinkFactory);
        Object driver = build.invoke(builder);

        Method analyse = cfrDriverClass.getMethod("analyse", List.class);
        analyse.invoke(driver, Collections.singletonList(classFile.getAbsolutePath()));
        String result = javaSource.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private void deleteQuietly(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteQuietly(child);
                }
            }
        }
        if (!file.delete()) {
            file.deleteOnExit();
        }
    }

    private static final class OutputSinkFactoryHandler implements InvocationHandler {
        private final ClassLoader classLoader;
        private final StringBuilder javaSource;

        private OutputSinkFactoryHandler(ClassLoader classLoader, StringBuilder javaSource) {
            this.classLoader = classLoader;
            this.javaSource = javaSource;
        }

        @Override
        public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
            String methodName = method.getName();
            if ("getSupportedSinks".equals(methodName)) {
                return resolveSupportedSinks(args);
            }
            if ("getSink".equals(methodName)) {
                final Object sinkType = args[0];
                final Object sinkClass = args[1];
                final Class<?> sinkInterface = method.getReturnType();
                return Proxy.newProxyInstance(classLoader, new Class<?>[]{sinkInterface}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method sinkMethod, Object[] sinkArgs) throws Throwable {
                        if ("write".equals(sinkMethod.getName()) && sinkArgs != null && sinkArgs.length > 0) {
                            captureSinkValue(sinkType, sinkClass, sinkArgs[0]);
                        }
                        return null;
                    }
                });
            }
            return null;
        }

        private Object resolveSupportedSinks(Object[] args) {
            Collection<?> available = (Collection<?>) args[1];
            if (available == null || available.isEmpty()) {
                return Collections.emptyList();
            }
            Object javaDecompiled = findByName(available, "DECOMPILED");
            if (javaDecompiled != null) {
                return Collections.singletonList(javaDecompiled);
            }
            Object javaString = findByName(available, "STRING");
            if (javaString != null) {
                return Collections.singletonList(javaString);
            }
            return Collections.singletonList(available.iterator().next());
        }

        private Object findByName(Collection<?> available, String name) {
            for (Object candidate : available) {
                if (candidate != null && name.equals(String.valueOf(candidate))) {
                    return candidate;
                }
            }
            return null;
        }

        private void captureSinkValue(Object sinkType, Object sinkClass, Object sinkValue) {
            if (sinkValue == null) {
                return;
            }
            String sinkTypeName = String.valueOf(sinkType);
            String sinkClassName = String.valueOf(sinkClass);
            if (!"JAVA".equals(sinkTypeName) && !"STRING".equals(sinkClassName)) {
                return;
            }
            String candidate = tryExtractJava(sinkValue);
            if (candidate != null && !candidate.trim().isEmpty()) {
                javaSource.append(candidate);
                if (!candidate.endsWith("\n")) {
                    javaSource.append('\n');
                }
            }
        }

        private String tryExtractJava(Object sinkValue) {
            try {
                Method getJava = sinkValue.getClass().getMethod("getJava");
                Object value = getJava.invoke(sinkValue);
                return value == null ? null : String.valueOf(value);
            } catch (Exception ignore) {
                return String.valueOf(sinkValue);
            }
        }
    }
}
