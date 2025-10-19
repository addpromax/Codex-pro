package net.momirealms.craftengine.bukkit.plugin.classpath;

import net.momirealms.craftengine.core.plugin.classpath.ClassPathAppender;
import net.momirealms.craftengine.core.plugin.classpath.URLClassLoaderAccess;
import net.momirealms.craftengine.core.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Optional;

public class PaperClassPathAppender implements ClassPathAppender {
    public static final Class<?> clazz$PaperPluginClassLoader = ReflectionUtils.getClazz(
            "io.papermc.paper.plugin.entrypoint.classloader.PaperPluginClassLoader"
    );
    public static final Field field$PaperPluginClassLoader$libraryLoader = Optional.ofNullable(clazz$PaperPluginClassLoader)
            .map(it -> ReflectionUtils.getDeclaredField(it, URLClassLoader.class, 0))
            .orElse(null);
    private final URLClassLoaderAccess classLoaderAccess;

    public PaperClassPathAppender(ClassLoader classLoader) {
        try {
            if (clazz$PaperPluginClassLoader != null && clazz$PaperPluginClassLoader.isInstance(classLoader)) {
                URLClassLoader libraryClassLoader = (URLClassLoader) field$PaperPluginClassLoader$libraryLoader.get(classLoader);
                this.classLoaderAccess = URLClassLoaderAccess.create(libraryClassLoader);
            } else if (classLoader instanceof URLClassLoader) {
                this.classLoaderAccess = URLClassLoaderAccess.create((URLClassLoader) classLoader);
            } else {
                throw new IllegalStateException("ClassLoader is not instance of URLClassLoader");
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate PaperPluginClassLoader", e);
        }
    }

    @Override
    public void addJarToClasspath(Path file) {
        try {
            this.classLoaderAccess.addURL(file.toUri().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
