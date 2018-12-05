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

            System.out.println("fileChannel.size() = " + size);

            // acquire the read-write share memory
            mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, size).load();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(-1);
        }

    }

    public void run() {
        int e = 0;
        boolean fullFlag = false;
        while (true) {
            try {
                FileLock lock;  // exclusive lock on the file channel
                if((lock = fileChannel.tryLock()) != null) {
                   try {
                       int capacity = mappedByteBuffer.getInt(QUEUE_CAPACITY_OFFSET);
                       if(capacity < CAPACITY) {
                           fullFlag = false;
                           System.out.println("capacity = " + capacity);
                           System.out.println("[" + name + "] Producing value : +" + e);
                           byte[] bytes = String.format("%d(%s)", e++, name).getBytes();
                           int offset = QUEUE_START_OFFSET + QUEUE_CONTENT_LENGTH * capacity++;
                           mappedByteBuffer.putInt(offset, bytes.length);
                           mappedByteBuffer.position(offset + 4 * Integer.BYTES);
                           mappedByteBuffer.put(bytes);
                           mappedByteBuffer.putInt(QUEUE_CAPACITY_OFFSET, capacity);

                       } else {
                           if(fullFlag == false) {
                               fullFlag = true;
                               System.out.println("capacity = " + capacity);
                               System.out.println("Queue in share memory is full, Producer[" + name
                                       + "] is waiting for consumer to take!");
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

        nioShmProducer producer = new nioShmProducer(args[0], "sharedMemory.bin");
        producer.start();
    }

}
