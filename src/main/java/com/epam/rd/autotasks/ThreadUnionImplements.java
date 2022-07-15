package com.epam.rd.autotasks;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreadUnionImplements implements ThreadUnion {
    private static final Logger logger = Logger.getLogger(ThreadUnionImplements.class.getName());
    private String name;
    private int totalSize;
    private List<Thread> listThreads = new ArrayList<>();
    private boolean isShutdown;
    private List<FinishedThreadResult> finishedThreads = new ArrayList<>();

    public ThreadUnionImplements(String name) {
        this.name = name;
    }

    @Override
    public int totalSize() {
        return totalSize;
    }

    @Override
    public int activeSize() {
        return (int) listThreads.stream().filter(Thread::isAlive).count();
    }

    @Override
    public void shutdown() {
        listThreads.forEach(Thread::interrupt);
        isShutdown = true;
    }

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public void awaitTermination() {
        for (Thread thread : listThreads) {
            try {
                    thread.join();
            } catch (InterruptedException e) {
                logger.log(Level.INFO, "Interrupted tread", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isFinished() {
        return isShutdown && activeSize() == 0;
    }

    @Override
    public List<FinishedThreadResult> results() {
        return finishedThreads;
    }

    @Override
    public synchronized Thread newThread(Runnable r) {
        if (isShutdown) {
            throw new IllegalStateException("isShutdown return true");
        }
        Thread thread = new Thread(r) {
            @Override
            public void run() {
                super.run();
                synchronized (finishedThreads) {
                    finishedThreads.add(new FinishedThreadResult(this.getName()));
                }
            }
        };
        thread.setName(String.format("%s-worker-%d", name, totalSize++));
        thread.setUncaughtExceptionHandler((t, e) -> {
            synchronized (finishedThreads) {
                finishedThreads.add(new FinishedThreadResult(t.getName(), e));
            }
        });
        listThreads.add(thread);
        return thread;
    }
}
