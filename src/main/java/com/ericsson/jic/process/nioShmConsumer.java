package com.ericsson.jic.process;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Random;

/**
 * Producer Consumer Pattern: Using NIO and Shared Memory(Direct Byte Buffer)
 * CLI running:
 * mvn exec:java -Dexec.mainClass="com.ericsson.jic.process.nioShmConsumer" -Dexec.args="C-1"
 */
public class mnioShmConsumer extends Thread {
    private static final int QUEUE_CAPACITY_OFFSET = 0;
    private static final int QUEUE_START_OFFSET = 16;
    private static final int QUEUE_CONTENT_LENGTH = 64;
    private static final int CAPACITY = 5;

    private String name;
    private FileChannel fileChannel;
    private MappedByteBuffer mappedByteBuffer;
    byte[] bytes = new byte[QUEUE_CONTENT_LENGTH];

    public nioShmConsumer(String name, String fileName) {
        this.name = name;
        
        try {
            // acquire an random access file object
            RandomAccessFile raf = new RandomAccessFile(fileName, "rw");

            // set file size
            raf.setLength(4096);

            // acquire corresponding file channel
            fileChannel = raf.getChannel();

            // acquire file size, for mapping the shared memory
            int size = (int) fileChannel.size();

            // acquire the read-write shared memory
            mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, size).load();

        } catch (IOException ex) {
            System.out.println(ex);
            System.exit(0);
        }
    }

    public void run() {
        while (true) {

            try {
                FileLock lock = fileChannel.tryLock(); // exclusive lock on the file channel
                if (lock != null) {

                    int capacity = mappedByteBuffer.getInt(QUEUE_CAPACITY_OFFSET);
                    System.out.println("capacity = " + capacity);

                    if (capacity == 0) {
                        System.out.println("Queue is empty, Consumer[" + name + "] thread is waiting for Producer");
                    } else {
                        capacity--;
                        
                        for(int index = 0; index < QUEUE_CONTENT_LENGTH; index++) {
                            bytes[index] = mappedByteBuffer.get(capacity * QUEUE_CONTENT_LENGTH + QUEUE_START_OFFSET + index);
                        }
                        
                        mappedByteBuffer.putInt(QUEUE_CAPACITY_OFFSET, capacity);
                        
                        System.out.println("[" + name + "] Consuming value : -" + new String(bytes));
                    }

                    lock.release();

                } else {
                    // System.err.println("Consumer: lock failed");
                    continue;
                }

            } catch (IOException ex) {
                System.out.print(ex);
                break;
            }

            randomSleep();
        }
    }

    private static void randomSleep() {
        try {
            Thread.sleep(new Random().nextInt(1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        System.out.println("main...");
        for (String arg : args) {
            System.out.println(arg);
        }

        nioShmConsumer consumer = new nioShmConsumer(args[0], "sharedMemory.bin");
        consumer.start();
    }
}
