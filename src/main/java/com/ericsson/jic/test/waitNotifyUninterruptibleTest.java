package com.ericsson.jic.test;

public class waitNotifyUninterruptibleTest {

    public static void main(String args[]) {

        Buffer buff = new Buffer();

        Thread producer1 = new Producer("P-1", buff);
        Thread consumer1 = new Consumer("C-1", buff, producer1);

        producer1.start();
        consumer1.start();

    }

    public static class Buffer {

        public Buffer() {
        }

        public synchronized void put(final String name) throws InterruptedException {
            try {
                System.out.println("Producer[" + name + "] enter this.wait()!");
                this.wait(); // this.wait(0, 2000)
                System.out.println("Producer[" + name + "] exit this.wait() normally!");
            } catch (InterruptedException ie) {
                System.out.println("Producer[" + name + "] exit this.wait() by interrupt!");
                throw ie;
            }
        }

        public synchronized void get(final String name, Thread thread) throws InterruptedException {
            System.out.println("Consumer[" + name + "] notify thread!");
            this.notify();
            Thread.sleep(10000); // sleep 10s
            System.out.println("10 seconds slept, Consumer[" + name + "] interrupt thread!");
            thread.interrupt();
            Thread.sleep(10000); // sleep 10s
            System.out.println("another 10 seconds slept, Consumer[" + name + "] exit monitor lock!");
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
