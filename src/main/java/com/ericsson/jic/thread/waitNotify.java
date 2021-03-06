package com.ericsson.jic.thread;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class waitNotify {

    private static final int CAPACITY = 5;

    public static void main(String args[]) {

        Buffer buff = new Buffer(CAPACITY);

        Thread producer1 = new Producer("P-1", buff);
        Thread producer2 = new Producer("P-2", buff);

        Thread consumer1 = new Consumer("C-1", buff);
        Thread consumer2 = new Consumer("C-2", buff);
        Thread consumer3 = new Consumer("C-3", buff);
        Thread consumer4 = new Consumer("C-4", buff);

        producer1.start();
        producer2.start();

        consumer1.start();
        consumer2.start();
        consumer3.start();
        consumer4.start();
    }

    public static class Buffer {

        private Queue<String> queue = new LinkedList<String>();
        private int maxSize;

        public Buffer(int maxSize) {
            this.maxSize = maxSize;
        }

        public synchronized void put(final String name, final int e) throws InterruptedException {
            while (queue.size() >= maxSize) {
                System.out.println("Queue is full, Producer[" + name + "] thread waiting for "
                        + "consumer to take something from queue.");
                this.wait(); // this.wait(0, 2000)
            }

            System.out.println("[" + name + "] Producing value : +" + e);
            queue.offer(String.format("%d(%s)", e, name));
            this.notify(); // this.notifyAll()
        }

        public synchronized String get(final String name) throws InterruptedException {
            while (queue.isEmpty()) {
                System.out.println("Queue is empty, Consumer[" + name + "] thread is waiting for Producer");
                this.wait(); // this.wait(0, 2000)
            }

            String e = queue.poll();
            System.out.println("[" + name + "] Consuming value : -" + e);
            this.notify(); // this.notifyAll()
            return e;
        }
    }

    public static class Producer extends Thread {

        private String name;
        private Buffer buff;

        public Producer(final String name, final Buffer buff) {
            this.name = name;
            this.buff = buff;
            this.setName(name);
        }

        public void run() {

            int e = 0;
            while (true) {
                try {
                    buff.put(name, e++);
                    waitNotify.randomSleep();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                // exception handling in thread run method
            }

//            while (true) {
//                try {
//                    buff.put(name, getNextE());
//                    waitNotify.randomSleep();
//                } catch (InterruptedException ie) {
//                    ie.printStackTrace();
//                }
//
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
            this.setName(name);
        }

        public void run() {
            while (true) {
                try {
                    buff.get(name);
                    waitNotify.randomSleep();
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
