package com.github.ozanaaslan.modularitea.components;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class EventManager {

    private final List<RegisteredListener> eventRegister = new ArrayList<>();

    /**
     * Dispatches an event to all registered listeners.
     */
    public <T extends Event> T dispatch(T event) {
        for (RegisteredListener registeredListener : eventRegister) {
            registeredListener.invoke(event);
        }
        return event;
    }

    /**
     * Registers all event-handling methods in a given listener instance.
     */
    @SneakyThrows
    public void register(Class<?> listenerClass) {
        this.eventRegister.add(new RegisteredListener(listenerClass));
    }

    public void registerInstance(Object instance) {
        this.eventRegister.add(new RegisteredListener(instance));
    }

    // Update the Inner Class constructor
    private static class RegisteredListener {
        private final Object listener;

        RegisteredListener(Object listener) {
            this.listener = listener;
        }

        RegisteredListener(Class<?> listenerClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
            this.listener = listenerClass.getDeclaredConstructor().newInstance();
        }

        @SneakyThrows
        <T extends Event> void invoke(T event) {
            for (Method method : listener.getClass().getMethods()) {
                if (method.isAnnotationPresent(EventBus.class) && method.getParameterCount() == 1) {
                    Class<?> paramType = method.getParameterTypes()[0];
                    if (paramType.isAssignableFrom(event.getClass()))
                        method.invoke(listener, event);
                }
            }
        }
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EventBus {

    }

    public abstract static class Event {
        @Getter
        @Setter
        private boolean cancelled;
    }

}