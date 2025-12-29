package com.github.ozanaaslan.modularitea.components;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarFile;

public class ModuleManager {

    @Getter private List<Manifest> manifests = new ArrayList<>();
    private Set<String> loading = new HashSet<>();
    @Getter private File workingDirectory;
    @Getter private Configuration configuration;

    public ModuleManager(File workingDirectory) {
        this.workingDirectory = workingDirectory;
        init();
    }

    public ModuleManager(){
        this(new File(System.getProperty("user.dir"), "modules"));
    }

    @SneakyThrows
    private void init(){
        if(!workingDirectory.exists()) workingDirectory.mkdirs();
        this.configuration = new Configuration(new File(workingDirectory, "modules.properties"));
    }

    public void loadModules() {
        File[] files = workingDirectory.listFiles();
        if (files == null) return;

        Arrays.stream(files)
                .filter(file -> file.getName().endsWith(".jar"))
                .forEach(file -> this.manifests.add(new Manifest(file)));

        this.manifests.forEach(this::linkModuleLoader);
    }

    @SneakyThrows
    private void linkModuleLoader(Manifest manifest) {
        if (manifest.getClassLoader() != null) return;

        String name = manifest.getAttributes().getProperty("name");
        if (!loading.add(name)) return;

        ClassLoader parent = ModuleManager.class.getClassLoader();
        String depName = manifest.getAttributes().getProperty("depends");

        if (depName != null) {
            Manifest dep = getModuleWithName(depName);
            if (dep != null) {
                linkModuleLoader(dep);
                if (dep.getClassLoader() != null)
                    parent = dep.getClassLoader();
            }
        }

        manifest.initLoader(parent);
        loading.remove(name);
    }

    public void invokePrimaries(){ manifests.forEach(m -> m.getModule().invokePrimary()); }
    public void invokeSecondaries(){ manifests.forEach(m -> m.getModule().invokeSecondary()); }
    public void invokeTertiaries(){ manifests.forEach(m -> m.getModule().invokeTertiary()); }

    public Manifest getModuleWithName(String name) {
        return manifests.stream()
                .filter(m -> m.getAttributes().getProperty("name").equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private class Configuration {
        @Getter private File propertiesFile;
        @Getter private Properties properties;

        @SneakyThrows
        public Configuration(File file){
            this.propertiesFile = file;
            this.propertiesFile.getParentFile().mkdirs();
            this.propertiesFile.createNewFile();
            this.properties = new Properties();
            this.properties.load(Files.newInputStream(propertiesFile.toPath()));
        }

        @SneakyThrows
        public String set(String path, String value){
            properties.setProperty(path, value);
            properties.store(new FileWriter(propertiesFile), null);
            return value;
        }

    }

    public class Manifest implements Serializable {
        @Getter private Properties attributes;
        @Getter transient private File file;
        @Getter transient private URLClassLoader classLoader;

        private Module module;

        @SneakyThrows
        public Manifest(File file) {
            this.file = file;
            this.attributes = new Properties();
            try (JarFile jar = new JarFile(file)) {
                attributes.load(jar.getInputStream(jar.getEntry("manifest.properties")));
            }
        }

        public Module getModule() {
            if (module == null) module = new Module(this);
            return module;
        }

        public URLClassLoader initLoader(ClassLoader parent) throws MalformedURLException {
            if (this.classLoader == null)
                this.classLoader = new URLClassLoader(new URL[]{file.toURI().toURL()}, parent);
            return this.classLoader;
        }
    }

    public class Module {
        @Getter private Manifest manifest;
        @Getter private Object instance;
        @Getter private boolean excluded;
        @Getter private String exclusionKey;

        private Set<String> executedEntrypoints = new HashSet<>();

        @SneakyThrows
        public Module(Manifest manifest){
            this.manifest = manifest;

            this.exclusionKey = String.join(".", "module",
                    manifest.getAttributes().getProperty("name"),
                    manifest.getAttributes().getProperty("version"),
                    manifest.getAttributes().getProperty("author"),
                    "exclude"
            );

            this.excluded = Boolean.parseBoolean(
                    String.valueOf(configuration.getProperties().getProperty(this.exclusionKey, "false"))
            );

            if (this.instance == null) {
                String mainClass = manifest.getAttributes().getProperty("main");
                this.instance = Class.forName(mainClass, true, manifest.getClassLoader())
                        .getConstructor().newInstance();
            }

        }

        @SneakyThrows
        public Object load(String entrypoint){
            if(excluded || executedEntrypoints.contains(entrypoint)) return null;

            String depName = manifest.getAttributes().getProperty("depends");
            if (depName != null) {
                Manifest dep = getModuleWithName(depName);
                if (dep != null) dep.getModule().load(entrypoint);
            }

            Object o = ModuleManager.invoke(entrypoint, instance);
            executedEntrypoints.add(entrypoint);
            return o;
        }

        public void invokePrimary() { load("primaryEntrypoint"); }
        public void invokeSecondary() { load("secondaryEntrypoint"); }
        public void invokeTertiary() { load("tertiaryEntrypoint"); }
    }

    public interface JavaModule{
        void primaryEntrypoint();
        void secondaryEntrypoint();
        void tertiaryEntrypoint();
    }

    @SneakyThrows
    private static Object invoke(String methodName, Object instance) {
        try {
            Method m = instance.getClass().getDeclaredMethod(methodName);
            m.setAccessible(true);
            return m.invoke(instance);
        } catch (NoSuchMethodException e) {
        }
        return null;
    }
}
