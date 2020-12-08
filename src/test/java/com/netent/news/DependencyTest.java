package com.netent.news;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DependencyTest {

    public static final String COM_NETENT_NEWS = "com.netent.news.";
    public static final String PORT_OUT = COM_NETENT_NEWS + "application.port.out";
    public static final String DOMAIN = COM_NETENT_NEWS + "domain";
    private static final String ADAPTER_IN = COM_NETENT_NEWS + "adapter.in";
    private static final String PORT_IN = COM_NETENT_NEWS + "port.in";
    private static Map<String, Set<String>> collectDependencies;

    @BeforeAll
    static void beforeAll() throws IOException {
        File start = new File("src/main/java");
        String currentPath = start.getAbsolutePath();
        String[] path = currentPath.split("/");
        // Visit all Java files and check dependencies
        collectDependencies = new HashMap<>();
        visitJavaFiles(start, collectDependencies);
        collectDependencies
                .keySet()
                .stream()
                .sorted()
                .forEach(key -> System.out.println(key + ": " + collectDependencies.get(key)));
    }

    @Test
    void domain_must_not_depend_on_other_packages_in_the_system() {
        Set<String> keys = collectDependencies.keySet().stream().filter(k -> k.startsWith(DOMAIN)).collect(Collectors.toSet());
        assertTrue(keys.size() > 0);

        keys.forEach(k -> assertEquals(collectDependencies.get(k).size(), 0, "Package " + k + " depends on " + collectDependencies.get(k)));
    }

    @Test
    void application_must_only_depend_on_domain_and_port_out() {
        Set<String> keys = collectDependencies.keySet().stream().filter(k -> k.startsWith(COM_NETENT_NEWS + "application")).collect(Collectors.toSet());
        assertTrue(keys.size() > 0);

        for (String k : keys) {
            Set<String> illicitInPackage = collectDependencies.get(k).stream()
                    .filter(p -> !p.startsWith(DOMAIN))
                    .filter(p -> !p.startsWith(PORT_OUT))
                    .collect(Collectors.toSet());
            assertEquals(illicitInPackage.size(), 0, "Package " + k + " depends on " + illicitInPackage);
        }
    }

    @Test
    void adapter_out_must_only_depend_on_domain_and_port_out() {
        Set<String> keys = collectDependencies.keySet().stream().filter(k -> k.startsWith(COM_NETENT_NEWS + "adapter.out")).collect(Collectors.toSet());
        assertTrue(keys.size() > 0);

        for (String k : keys) {
            Set<String> illicitInPackage = collectDependencies.get(k).stream()
                    .filter(p -> !p.startsWith(DOMAIN))
                    .filter(p -> !p.startsWith(PORT_OUT))
                    .collect(Collectors.toSet());
            assertEquals(illicitInPackage.size(), 0, "Package " + k + " depends on " + illicitInPackage);
        }
    }

    @Test
    void port_in_must_only_depend_on_domain_and_adapter_in() {
        Set<String> keys = collectDependencies.keySet().stream().filter(k -> k.startsWith(COM_NETENT_NEWS + "port.in")).collect(Collectors.toSet());
        assertTrue(keys.size() > 0);

        for (String k : keys) {
            Set<String> illicitInPackage = collectDependencies.get(k).stream()
                    .filter(p -> !p.startsWith(DOMAIN))
                    .filter(p -> !p.startsWith(ADAPTER_IN))
                    .collect(Collectors.toSet());
            assertEquals(illicitInPackage.size(), 0, "Package " + k + " depends on " + illicitInPackage);
        }
    }

    @Test
    void adapter_in_must_only_depend_on_domain_and_port_in() {
        Set<String> keys = collectDependencies.keySet().stream().filter(k -> k.startsWith(ADAPTER_IN)).collect(Collectors.toSet());
        assertTrue(keys.size() > 0);

        for (String k : keys) {
            Set<String> illicitInPackage = collectDependencies.get(k).stream()
                    .filter(p -> !p.startsWith(DOMAIN))
                    .filter(p -> !p.startsWith(PORT_IN))
                    .collect(Collectors.toSet());
            assertEquals(illicitInPackage.size(), 0, "Package " + k + " depends on " + illicitInPackage);
        }
    }

    private static void visitJavaFiles(File current, Map<String, Set<String>> collectDependencies) throws IOException {
        if (current.isFile()) {
            if (current.getPath().endsWith("java")) {
                String javaPackage = getJavaPackage(current);
                Set<String> soFar = collectDependencies.getOrDefault(javaPackage, new HashSet<>());
                soFar.addAll(visitJavaFile(current));
                collectDependencies.put(javaPackage, soFar);
            }
        }
        if (current.isDirectory()) {
            File[] files = current.listFiles();
            for (File f : Objects.requireNonNull(files)) {
                visitJavaFiles(f, collectDependencies);
            }
        }
    }

    private static Set<String> visitJavaFile(File current) throws IOException {
        return Files.lines(current.toPath()).filter(s -> s.startsWith("import " + COM_NETENT_NEWS)).map(DependencyTest::extractDependency).collect(Collectors.toSet());
    }

    private static String getJavaPackage(File current) throws IOException {
        return Files.lines(current.toPath()).filter(s -> s.startsWith("package " + COM_NETENT_NEWS)).findFirst().map(s -> s.substring("package ".length(), s.indexOf(';'))).orElse("");
    }

    private static String extractDependency(String importStatement) {
        return importStatement.substring("import ".length(), importStatement.lastIndexOf('.'));
    }
}
