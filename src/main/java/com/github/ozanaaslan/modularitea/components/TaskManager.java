package com.github.ozanaaslan.modularitea.components;

import lombok.Getter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.*;

public class TaskManager {

    @Getter private static final TaskManager instance = new TaskManager();
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "TaskWorker");
                t.setDaemon(true);
                return t;
            }
    );

    private final Map<Object, ScheduledFuture<?>> activeTasks = new ConcurrentHashMap<>();

    private TaskManager() {}

    /**
     * Annotation to mark a method for periodic execution.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Task {
        long delay() default 0;
        long interval();
        TimeUnit unit() default TimeUnit.SECONDS;
        boolean async() default true;
    }

    /**
     * Scans an object for @Task annotations and schedules them.
     */
    public void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Task.class)) {
                Task meta = method.getAnnotation(Task.class);
                method.setAccessible(true);

                Runnable runnable = () -> {
                    try {
                        method.invoke(listener);
                    } catch (Exception e) {
                        System.err.println("Task execution failed: " + method.getName());
                        e.printStackTrace();
                    }
                };

                ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                        runnable, 
                        meta.delay(), 
                        meta.interval(), 
                        meta.unit()
                );

                activeTasks.put(method.getName() + listener.hashCode(), future);
            }
        }
    }

    /**
     * Gracefully shuts down the scheduler.
     */
    public void stopAll() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}