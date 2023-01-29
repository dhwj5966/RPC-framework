package github.starry.extension;


import github.starry.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 为了提高可扩展性，用工厂方法创建实例。
 * 比如需要获取ServiceDiscovery接口的实例，
 * 则可以ServiceDiscovery zookeeperServiceDiscovery =
 * ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zk");
 * @param <T>
 */
@Slf4j
public final class ExtensionLoader<T> {


    //存放SPI接口实现类信息的路径
    private static final String SERVICE_DIRECTORY = "META-INF/extensions/";

    //ExtensionLoader的缓存，存放{类 -> ExtensionLoader}
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

    /**
     * 实例的缓存,存放{类 -> 实例}，比如zk全限定类名 -> zk实例,eureka全限定类名 -> eureka实例
     */
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    //该ExtensionLoader对应的SPI接口的class
    private final Class<?> type;

    /**
     * {zk -> zkServiceDiscovery实例}
     */
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<>();

    /**
     * 存放的是{"zk" -> zk对应的文本的class对象}
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }

    /**
     * 获取接口的ExtensionLoader。
     * @param type 接口的Class对象。
     * @return 接口对应的ExtensionLoader。
     */
    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type) {
        //合法性校验，需要传入的类型不为空，是接口，且有SPI注解。
        if (type == null) {
            throw new IllegalArgumentException("Extension type should not be null.");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type must be an interface.");
        }
        if (type.getAnnotation(SPI.class) == null) {
            throw new IllegalArgumentException("Extension type must be annotated by @SPI");
        }
        //从ExtensionLoader的缓存中获取接口对应的ExtensionLoader对象，如果不存在就new一个。
        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        if (extensionLoader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<>(type));
            extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }
        return extensionLoader;
    }

    /**
     * 根据name，获取类的实例。
     * @param name
     * @return
     */
    public T getExtension(String name) {
        if (StringUtil.isBlank(name)) {
            throw new IllegalArgumentException("Extension name should not be null or empty.");
        }
        // 从缓存中取，如果未命中就创建
        Holder<Object> objectHolder = cachedInstances.get(name);
        if (objectHolder == null) {
            cachedInstances.putIfAbsent(name, new Holder<>());
            objectHolder = cachedInstances.get(name);
        }
        //
        Object instance = objectHolder.get();
        if (instance == null) {
            synchronized (objectHolder) {
                instance = objectHolder.get();
                if (instance == null) {
                    instance = createExtension(name);
                    objectHolder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * 根据name，创建新的实例。
     * @param name
     * @return
     */
    private T createExtension(String name) {
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw new RuntimeException("No such extension of name " + name);
        }
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if (instance == null) {
            try {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return instance;
    }

    //
    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = new HashMap<>();
                    // load all extensions from our extensions directory
                    loadDirectory(classes);
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    //读取指定路径的文件
    private void loadDirectory(Map<String, Class<?>> extensionClasses) {
        //根据固定前缀名 + 接口的全限定类名，拼接出文件名
        String fileName = ExtensionLoader.SERVICE_DIRECTORY + type.getName();
        try {
            //根据文件名拿到URL
            Enumeration<URL> urls;
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            urls = classLoader.getResources(fileName);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL resourceUrl = urls.nextElement();
                    loadResource(extensionClasses, classLoader, resourceUrl);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    //根据URL，拿到流，并读取内容。
    private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL resourceUrl) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream(), UTF_8))) {
            String line;
            // read every line
            while ((line = reader.readLine()) != null) {
                // get index of comment
                final int ci = line.indexOf('#');
                if (ci >= 0) {
                    // string after # is comment so we ignore it
                    line = line.substring(0, ci);
                }
                line = line.trim();
                if (line.length() > 0) {
                    try {
                        final int ei = line.indexOf('=');
                        String name = line.substring(0, ei).trim();
                        String clazzName = line.substring(ei + 1).trim();
                        if (name.length() > 0 && clazzName.length() > 0) {
                            Class<?> clazz = classLoader.loadClass(clazzName);
                            extensionClasses.put(name, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error(e.getMessage());
                    }
                }

            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
