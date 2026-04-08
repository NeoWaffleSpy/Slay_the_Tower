package com.Team_Berry.Utils.Scheduler;

import java.util.concurrent.*;
import java.util.*;

public class KeyedScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public void schedule(String key, Runnable task, long delay, TimeUnit unit) {
        // Cancel existing task with same key (optional behavior)
        cancel(key);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                task.run();
            } finally {
                tasks.remove(key); // cleanup after execution
            }
        }, delay, unit);

        tasks.put(key, future);
    }

    public void cancel(String key) {
        ScheduledFuture<?> future = tasks.remove(key);
        if (future != null) {
            future.cancel(true); // true = interrupt if running
        }
    }

    public boolean isScheduled(String key) {
        ScheduledFuture<?> future = tasks.get(key);
        return future != null && !future.isDone();
    }
}