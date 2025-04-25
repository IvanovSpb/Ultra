package com.vladimir.ultra.scheduler;

import com.vladimir.ultra.interfaces.Scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ComputationScheduler implements Scheduler {
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    @Override
    public void execute(Runnable task) {
        executor.submit(task);
    }
}