package com.ericsson.jic.thread;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Producer Consumer Pattern: Using BlockingQueueDemo
 */
public class BlockingQueueDemo {

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

        BlockingQueue<String> blockingQueue;

        public Buffer(int maxSize) {
            blockingQueue = new LinkedBlockingDeque<String>(maxSize);
        }

        public void put(final String name, final int e) throws InterruptedException {
            System.out.println("[" + name + "] Producing value : +" + e);
            blockingQueue.put(String.format("%d(%s)", e, name));
        }

        public String get(final String name) throws InterruptedException {
            String e = blockingQueue.take();
            System.out.println("[" + name + "] Consuming value : -" + e);
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
                    BlockingQueueDemo.randomSleep();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }

//            while (true) {
//                try {
//                    buff.put(name, getNextE());
//                    BlockingQueueDemo.randomSleep();
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
            this.setName(name);
        }

        public void run() {
            while (true) {
                try {
                    buff.get(name);
                    BlockingQueueDemo.randomSleep();
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
