package com.ericsson.jic.thread;

import java.io.*;
import java.util.Random;

/**
 * Producer Consumer Pattern: Using PipedInputOutputStream
 */
public class PipedInputOutputStream {

    //    private static final int CAPACITY = 5;

    public static void main(String args[]) throws IOException {

        Buffer buff = new Buffer();
        buff.intialize();

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

        private PipedInputStream pis = new PipedInputStream(); // Internal using a 1024 bytes buffer 
        private PipedOutputStream pos = new PipedOutputStream();
        private BufferedReader br = new BufferedReader(new InputStreamReader(pis));

        public void intialize() throws IOException {
            pis.connect(pos); // directly write to the internal buffer of pis
        }

        public void put(final String name, final int e) throws IOException {
            System.out.println("[" + name + "] Producing value : +" + e);

            // using '\n' as delimiter
            pos.write(String.format("%d(%s)\n", e, name).getBytes()); // will block when internal buffer full
        }

        public String get(final String name) throws IOException {
            String e = br.readLine();
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
        }

        public void run() {

            int e = 0;
            while (true) {
                try {
                    buff.put(name, e++);
                    PipedInputOutputStream.randomSleep();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.exit(-1);
                }


            }

//            while (true) {
//                try {
//                    buff.put(name, getNextE());
//                    PipedInputOutputStream.randomSleep();
//                } catch (InterruptedException ie) {
//                    ie.printStackTrace();
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                    System.exit(-1);
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
                    PipedInputOutputStream.randomSleep();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }

    private static void randomSleep() throws InterruptedException {
        Thread.sleep(new Random().nextInt(1000));
    }
}
