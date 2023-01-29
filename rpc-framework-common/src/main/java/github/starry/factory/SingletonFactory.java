package github.starry.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Starry
 * @create 2023-01-27-7:18 PM
 * @Describe
 */
public class SingletonFactory {
    //工厂缓存
    private static final Map<String, Object> cache = new ConcurrentHashMap<>();

    public static <T> T getInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException();
        }
        String key = clazz.toString();
        if (cache.containsKey(key)) {
            return clazz.cast(cache.get(key));
        } else {
            return clazz.cast(cache.computeIfAbsent(key, k -> {
                try {
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }));
        }
    }
}
