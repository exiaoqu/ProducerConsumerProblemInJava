package com.ericsson.jic.process;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

/**
 * Producer Consumer Pattern: Using Posix Message Queue through JNA
 * CLI running: 
 * mvn exec:java -Dexec.mainClass="com.ericsson.jic.process.JnaMqProducer" -Dexec.args="P-1"
 */
public class JnaMqProducer extends Thread {

    public interface IPCLibrary extends Library {

        IPCLibrary INSTANCE = (IPCLibrary) Native.loadLibrary("c", IPCLibrary.class);

        class MsgBuf extends Structure {

            public static class ByReference extends MsgBuf implements Structure.ByReference {}

            // public static class ByValue extends MsgBuf implements Structure.ByValue {}

            public NativeLong mtype; /* type of message */
            public byte[] mtext = new byte[1024];

            @Override
            protected List<?> getFieldOrder() {
                return Arrays.asList("mtype", "mtext");
            }
        }

        // Initialize queue, or if it exists, get it            
        int msgget(NativeLong key, int msgflg);

        // Send messages to queue
        int msgsnd(int msqid, MsgBuf.ByReference msgptr, int msgsz, int msgflg);

        // Receive messages from queue
        int msgrcv(int msqid, MsgBuf.ByReference msgptr, int msgsz, long msgtype, int flag);
    }

    // class definition begin...

    private static final int MSG_QUEUE_KEY = 12500;
    private static final int MSG_TYPE = 1; // just needs to be a positive number
    private static final int IPC_CREAT = 01000; // starts with 0 so its octal or 512 

    private String name;
    private int msqid;
    private IPCLibrary.MsgBuf.ByReference message = new IPCLibrary.MsgBuf.ByReference();

    public JnaMqProducer(String name) {
        this.name = name;
        this.setName(name);

        // create Sys-V message queue
        NativeLong msgkey = new NativeLong(MSG_QUEUE_KEY);
        msqid = IPCLibrary.INSTANCE.msgget(msgkey, 0666 | IPC_CREAT);
        if (msqid < 0) {
            System.out.println("msgget() failed! return:" + msqid + "  errno:" + Native.getLastError());
            System.exit(-1);
        }

        System.out.println("message queue(id:" + msqid + ") has been open");

        // prepare sending buffer
        message.mtype = new NativeLong(MSG_TYPE);
    }

    public void run() {
        int e = 0;

        while (true) {

            // Sending message
            byte[] bytes = String.format("%d(%s)", e, name).getBytes();

            System.arraycopy(bytes, 0, message.mtext, 0, bytes.length);

            int ret = IPCLibrary.INSTANCE.msgsnd(msqid, message, message.mtext.length * Native.getNativeSize(Byte.TYPE),
                    0);

            if (ret != 0) {
                System.out.println("msgsnd() failed! return:" + ret + "  errno:" + Native.getLastError());
                System.exit(0);
            }

            System.out.println("[" + name + "] Producing value : +" + e++);

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

    public static void main(String[] args) {

        System.out.println("main...");
        for (String arg : args) {
            System.out.println(arg);
        }

        JnaMqProducer producer = new JnaMqProducer(args[0]);
        producer.start();
    }

}
