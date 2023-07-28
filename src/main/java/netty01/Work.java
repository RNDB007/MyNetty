package netty01;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * @author Joe Lee
 * @date 2023/7/27
 * @Description ：用于多线程控制Selector
 * 新的线程不需要持有服务端的channel
 * 只启动一个客户端去连接服务端，而服务端有两个线程，每个线程都持有一个 selector ，所以客户端连接进来的时候
 * 每个 selector 都可以接收到连接事件， 但是真正接收到的客户端连接只有一个，被其中一个线程处理并接收了，
 * 另一个线程得到的当然为 null。
 * 因此，我们新创建的线程，只能够处理 read 和 write 事件。
 * 当有新的客户端连接被接收后，该客户端的 channel 将被注册到新线程持有的 selector 上，每当 selector 有读事件到来，就由新的线程来处理。
 */
public class Work implements Runnable{

    private static final Logger logger = LoggerFactory.getLogger(Work.class);

    private boolean flags;

    private Selector selector = Selector.open();;

    private Thread thread;

    private SelectionKey selectionKey;

    public Work() throws IOException {
        thread = new Thread(this);
    }

    public Selector getSelector() {
        return selector;
    }

    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    public void start() {
        //保证只启动一次
        //单一个main线程，这样写没问题
        if (flags) {
            return;
        }
        flags = true;
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            logger.info("新线程阻塞在这里吧。。。。。。。");
            try {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    iterator.remove();
                    if (selectionKey.isReadable()) {
                        SocketChannel channel = (SocketChannel)selectionKey.channel();
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        int len = channel.read(byteBuffer);
                        if (len == -1) {
                            logger.info("客户端通道要关闭！");
                            channel.close();
                            break;
                        }
                        byte[] bytes = new byte[len];
                        byteBuffer.flip();
                        byteBuffer.get(bytes);
                        logger.info("新线程收到客户端发送的数据:{}",new String(bytes));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

