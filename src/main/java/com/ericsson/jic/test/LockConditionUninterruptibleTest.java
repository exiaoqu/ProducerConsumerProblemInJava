package com.ericsson.jic.test;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Producer Consumer Pattern: Using Lock and Condition
 */
public class LockConditionUninterruptibleTest {

    private static final int CAPACITY = 5;

    public static void main(String args[]) {

        Buffer buff = new Buffer();

        Thread producer1 = new Producer("P-1", buff);
        Thread consumer1 = new Consumer("C-1", buff, producer1);


        producer1.start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        consumer1.start();

    }

    public static class Buffer {

        private static final Lock lock = new ReentrantLock(); // * fair vs. non-fair detail
        private static final Condition condition = lock.newCondition();  // condition

        public Buffer() {
        }

        public void put(final String name) throws InterruptedException {
            // acquire the lock - dead wait problem
            lock.lock(); // trylock(), trylock(10, TimeUnit.MILLISECONDS), lockInterruptibly()
            try {
                System.out.println("Producer[" + name + "] enter condition.await(8s)!");
                boolean ret = condition.await(8, TimeUnit.SECONDS);
                System.out.println("Producer[" + name + "] exit condition.await(8s) normally! ret=" + ret);

            } catch(InterruptedException ie) {
                System.out.println("Producer[" + name + "] exit condition.await(8s) by interrupt!");
                throw ie;
            } finally {
                // release the lock
                lock.unlock();
            }
        }

        public void get(final String name,  Thread thread) throws InterruptedException {
            // acquire the lock  - dead wait problem
            lock.lock(); // trylock(), trylock(10, TimeUnit.MILLISECONDS), lockInterruptibly()
            try {
                System.out.println("Consumer[" + name + "] interrupt thread!");
                Thread.sleep(4000); // sleep 30s
                // awake other producers and consumers
                thread.interrupt();
                Thread.sleep(10000); // sleep 30s
                System.out.println("30 seconds slept, Consumer[" + name + "] signal thread!");
                condition.signal();
                Thread.sleep(10000); // sleep 30s
                System.out.println("another 30 seconds slept, Consumer[" + name + "] exit lock!");
            } finally {
                // release the lock
                lock.unlock();
            }
        }
    }

    public static class Producer extends Thread {

        private String name;
        private Buffer buff;

        public Producer(final String name, final Buffer buff) {
            this.name = name;
            this.buff = buff;
        }

        public void run() {
            try {
                buff.put(name);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }

        private static int e = 0;

        private static synchronized int getNextE() {
            return e++;
        }
    }

    public static class Consumer extends Thread {

        private String name;
        private Buffer buff;
        private Thread thread;

        public Consumer(final String name, final Buffer buff, Thread thread) {
            this.name = name;
            this.buff = buff;
            this.thread = thread;
        }

        public void run() {
            try {
                buff.get(name, thread);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }
}
