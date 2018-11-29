package com.ericsson.jic.thread;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Producer Consumer Pattern: Using Lock and Condition
 */
public class LockCondition {

    private static final int CAPACITY = 5;

    public static void main(String args[]) {

        Buffer buff = new Buffer(CAPACITY);

        Thread producer1 = new Producer("P-1", buff);
        Thread producer2 = new Producer("P-2", buff);

        Thread consumer1 = new Consumer("C-1", buff);
        Thread consumer2 = new Consumer("C-2", buff);
        Thread consumer3 = new Consumer("C-3", buff);

        producer1.start();
        producer2.start();

        consumer1.start();
        consumer2.start();
        consumer3.start();
    }

    public static class Buffer {

        private static final Lock lock = new ReentrantLock(); // * fair vs. non-fair detail
        private static final Condition fullCondition = lock.newCondition();  // full condition
        private static final Condition emptyCondition = lock.newCondition(); // empty condition

        private Queue<String> queue = new LinkedList<String>();
        private int maxSize;

        public Buffer(int maxSize) {
            this.maxSize = maxSize;
        }

        public void put(final String name, final int e) throws InterruptedException {
            // acquire the lock - dead wait problem
            lock.lock(); // trylock(), trylock(10, TimeUnit.MILLISECONDS), lockInterruptibly()
            try {
                while (queue.size() >= maxSize) {
                    System.out.println("Queue is full, Producer[" + name + "] thread waiting for "
                            + "consumer to take something from queue.");

                    // conidtion not fulfil, release lock and waiting for being waken, once waken acquire the lock again
                    fullCondition.await(); // await(10, TimeUnit.MILLISECONDS), awaitNanos(1000), awaitUninterruptibly(), awaitUntil(Date deadline)
                }

                System.out.println("[" + name + "] Producing value : +" + e);
                queue.offer(String.format("%d(%s)", e, name));

                // awake other producers and consumers
                fullCondition.signalAll();  // signal()
                emptyCondition.signalAll();
            } finally {
                // release the lock
                lock.unlock();
            }
        }

        public String get(final String name) throws InterruptedException {
            // acquire the lock  - dead wait problem
            lock.lock(); // trylock(), trylock(10, TimeUnit.MILLISECONDS), lockInterruptibly()
            try {
                while (queue.isEmpty()) {
                    System.out.println("Queue is empty, Consumer[" + name + "] thread is waiting for Producer");

                    // conidtion not fulfil, release lock and waiting for being waken, once waken acquire the lock again
                    emptyCondition.await(); // await(10, TimeUnit.MILLISECONDS), awaitNanos(1000), awaitUntil(Date deadline), awaitUninterruptibly()
                }
                String e = queue.poll();
                System.out.println("[" + name + "] Consuming value : -" + e);

                // awake other producers and consumers
                fullCondition.signalAll();  // signal()
                emptyCondition.signalAll();

                return e;
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

            int e = 0;
            while (true) {
                try {
                    buff.put(name, e++);
                    LockCondition.randomSleep();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }

//            while (true) {
//                try {
//                    buff.put(name, getNextE());
//                    LockCondition.randomSleep();
//                } catch (InterruptedException ie) {
//                    ie.printStackTrace();
//                }
//            }

        }

        private static int e = 0;

        private static synchronized int getNextE() {
            return e++;
        }
    }

    public static class Consumer extends Thread {

        private String name;
        private Buffer buff;

        public Consumer(final String name, final Buffer buff) {
            this.name = name;
            this.buff = buff;
        }

        public void run() {
            while (true) {
                try {
                    buff.get(name);
                    LockCondition.randomSleep();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    private static void randomSleep() throws InterruptedException {
        Thread.sleep(new Random().nextInt(1000));
    }
}
