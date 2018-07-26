rm -fr sharedMemory.bin

mvn exec:java -Dexec.mainClass="com.ericsson.jic.process.nioShmProducer" -Dexec.args="P-1" &
mvn exec:java -Dexec.mainClass="com.ericsson.jic.process.nioShmProducer" -Dexec.args="P-2" &
mvn exec:java -Dexec.mainClass="com.ericsson.jic.process.nioShmConsumer" -Dexec.args="C-1" &
mvn exec:java -Dexec.mainClass="com.ericsson.jic.process.nioShmConsumer" -Dexec.args="C-2" &
mvn exec:java -Dexec.mainClass="com.ericsson.jic.process.nioShmConsumer" -Dexec.args="C-3" &
