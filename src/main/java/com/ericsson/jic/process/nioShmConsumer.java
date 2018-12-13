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
public class nioShmConsumer extends Thread {
    private static final int QUEUE_CAPACITY_OFFSET = 0;
    private static final int QUEUE_START_OFFSET = 16;
    private static final int QUEUE_CONTENT_LENGTH = 64;
    private static final int CAPACITY = 5;

    private String name;
    private FileChannel fileChannel;
    private MappedByteBuffer mappedByteBuffer;
    byte[] bytes = new byte[QUEUE_CONTENT_LENGTH - 4 * Integer.BYTES];

    public nioShmConsumer(String name, String fileName) {
        this.name = name;
        this.setName(name);
        
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
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    public void run() {
        boolean emptyFlag = false;
        while (true) {
            try {
                FileLock lock;  // exclusive lock on the file channel
                if((lock = fileChannel.lock()) != null) {
                    try {
                        int capacity = mappedByteBuffer.getInt(QUEUE_CAPACITY_OFFSET);
                        if(capacity > 0) {
                            emptyFlag = false;
                            System.out.println("capacity = " + capacity);
                            int offset = QUEUE_START_OFFSET + QUEUE_CONTENT_LENGTH * --capacity;
                            int length = mappedByteBuffer.getInt(offset);
                            mappedByteBuffer.position(offset + 4 * Integer.BYTES);
                            mappedByteBuffer.get(bytes);
                            mappedByteBuffer.putInt(QUEUE_CAPACITY_OFFSET, capacity);

                            System.out.println("[" + name + "] Consuming value : -" + new String(bytes));
                        } else {
                            if(emptyFlag == false) {
                                emptyFlag = true;
                                System.out.println("capacity = " + capacity);
                                System.out.println("Queue in share memory is empty, Consumer[" + name + "] is waiting for Producer to put");
                            }
                        }
                    } finally {
                        lock.release();
                    }

                    randomSleep();
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
                System.exit(-1);
            }
        }
    }

    private static void randomSleep() throws InterruptedException {
        Thread.sleep(new Random().nextInt(1000));
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
