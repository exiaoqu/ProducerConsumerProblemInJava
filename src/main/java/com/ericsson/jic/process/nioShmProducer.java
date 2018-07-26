package com.ericsson.jic.process;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.Random;

/**
 * Producer Consumer Pattern: Using NIO and Shared Memory(Direct Byte Buffer)
 * CLI running:
 * mvn exec:java -Dexec.mainClass="com.ericsson.jic.process.nioShmProducer" -Dexec.args="P-1"
 */
public class nioShmProducer extends Thread {
    private static final int QUEUE_CAPACITY_OFFSET = 0;
    private static final int QUEUE_START_OFFSET = 16;
    private static final int QUEUE_CONTENT_LENGTH = 64;
    private static final int CAPACITY = 5;

    private String name;
    private FileChannel fileChannel;
    private MappedByteBuffer mappedByteBuffer;

    public nioShmProducer(String name, String fileName) {
        this.name = name;
        
        try {
            // acquire an random access file object
            RandomAccessFile raf = new RandomAccessFile(fileName, "rw");

            // set file size
            raf.setLength(4096);

            // acquire corresponding file channel
            fileChannel = raf.getChannel();

            // acquire file size, for mapping share memory
            int size = (int) fileChannel.size();

            // acquire the read-write share memory
            mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, size).load();
        } catch (IOException ex) {
            System.out.println(ex);
            System.exit(0);
        }

    }

    public void run() {
        int e = 0;
        
        while (true) {

            try {
                FileLock lock = fileChannel.tryLock(); // exclusive lock on the file channel
                if (lock != null) {

                    int capacity = mappedByteBuffer.getInt(QUEUE_CAPACITY_OFFSET);
                    System.out.println("capacity = " + capacity);
                    
                    if (capacity >= CAPACITY) {
                        System.out.println("Queue is full, Producer[" + name + "] process waiting for "
                                + "consumer to take something from share memory.");
                    } else {
                        System.out.println("[" + name + "] Producing value : +" + e);
                        
                        byte[] bytes = String.format("%d(%s)", e++, name).getBytes();
                        
                        for(int index = 0; index < bytes.length; index++) {
                            mappedByteBuffer.put(capacity * QUEUE_CONTENT_LENGTH + QUEUE_START_OFFSET + index, bytes[index]);
                        }
                        
                        capacity++;
                        mappedByteBuffer.putInt(QUEUE_CAPACITY_OFFSET, capacity);
                    }

                    lock.release();

                } else {
                    // System.err.println("Producer: lock failed");
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

        nioShmProducer producer = new nioShmProducer(args[0], "sharedMemory.bin");
        producer.start();
    }

}
