package com.github.ozanaaslan.modularitea.components;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ServiceManager {
    @Getter private static final ServiceManager instance = new ServiceManager();
    private final Map<ServiceKey, Object> services = new ConcurrentHashMap<>();

    private ServiceManager() {}

    @AllArgsConstructor @EqualsAndHashCode
    private static class ServiceKey {
        private final Class<?> type;
        private final String name;
    }

    public <T> void register(Class<?> type, String name, Object implementation) {
        services.put(new ServiceKey(type, name.toLowerCase()), implementation);
    }

    public void registerBeans(Object provider) {
        // 1. Process Methods
        for (Method method : provider.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(ServiceBean.class)) {
                ServiceBean anno = method.getAnnotation(ServiceBean.class);
                try {
                    method.setAccessible(true);
                    Object bean = method.invoke(provider);
                    // Use annotation value if set, otherwise use method name
                    String name = anno.value().isEmpty() ? method.getName() : anno.value();
                    register(method.getReturnType(), name, bean);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }

        // 2. Process Fields
        for (Field field : provider.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(ServiceBean.class)) {
                ServiceBean anno = field.getAnnotation(ServiceBean.class);
                try {
                    field.setAccessible(true);
                    Object bean = field.get(provider);
                    // Use annotation value if set, otherwise use field name
                    String name = anno.value().isEmpty() ? field.getName() : anno.value();
                    register(field.getType(), name, bean);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    public void inject(Object target) {
        for (Field field : target.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(InjectService.class)) {
                InjectService anno = field.getAnnotation(InjectService.class);
                field.setAccessible(true);

                // Fallback: If @InjectService is empty, look for a bean named exactly like the field
                String targetName = anno.value().isEmpty() ? field.getName() : anno.value();
                Object service = services.get(new ServiceKey(field.getType(), targetName.toLowerCase()));

                try {
                    if (service != null) {
                        field.set(target, service);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public @interface ServiceBean {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface InjectService {
        String value() default "";
    }
}