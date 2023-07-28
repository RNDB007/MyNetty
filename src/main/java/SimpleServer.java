import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class SimpleServer {

    public static void main(String[] args) throws IOException {
        Logger logger = LoggerFactory.getLogger(SimpleServer.class);

        //创建服务端channel
        //实际调用的是SelectorProvider.provider().openServerSocketChannel();方法。
        //我们可以获得provider来得到socketchannel和selector。
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        //设置channel非阻塞
        //意味着当进行网络 I/O 操作时，程序会立即返回，可以同时处理多个连接而不会阻塞主线程。
        serverSocketChannel.configureBlocking(false);
        //获得selector
        Selector selector = Selector.open();
        //把channel注册到selector上,现在还没有给key设置感兴趣的事件
        //selector: 一个已经打开的选择器对象，用于监控通道的I/O事件。当服务器套接字通道上发生感兴趣的I/O事件时，
        // 选择器会通知 程序进行相应的处理。这个参数指定了将通道注册到哪个选择器上。
        //0: 这是一个标志位，表示感兴趣的事件集合。在这里，0 表示不对任何事件感兴趣。这样注
        //册后，这个通道不会对任何I/O事件产生通知。通常，这个参数是一个表示感兴趣事件的整数，
        // 可以通过 SelectionKey.OP_XXX 常量来指定，例如 SelectionKey.OP_READ 表示对读事件感兴趣，
        // SelectionKey.OP_WRITE 表示对写事件感兴趣，等等。如果不对任何事件感兴趣，可以用 0 来表示。
        //serverSocketChannel: 这是要注册的服务器套接字通道本身。通常情况下，这个参数是要注册的通道。
        SelectionKey selectionKey = serverSocketChannel.register(selector, 0, serverSocketChannel);
        //给key设置感兴趣的事件
        selectionKey.interestOps(SelectionKey.OP_ACCEPT);
        //绑定端口号
        serverSocketChannel.bind(new InetSocketAddress(8080));
        //然后开始接受连接,处理事件,整个处理都在一个死循环之中
        while (true) {
            //当没有事件到来的时候，这里是阻塞的,有事件的时候会自动运行
            selector.select();
            //如果有事件到来，这里可以得到注册到该selector上的所有的key，每一个key上都有一个channel
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            //得到集合的迭代器
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
            while (keyIterator.hasNext()) {
                //得到每一个key
                SelectionKey key = keyIterator.next();
                //首先要从集合中把key删除，否则会一直报告该key
                //我们也可以在最后clear
                keyIterator.remove();
                //接下来就要处理事件，判断selector轮询到的是什么事件，并根据事件作出回应
                //如果是连接事件
                if (key.isAcceptable()) {
                    //得到服务端的channel,这里有两种方式获得服务端的channel，一种是直接获得,一种是通过attachment获得
                    //在注册通道时，可以使用 SelectableChannel 的 register() 方法中的第三个参数来指定附件对象。
                    // 注册后，可以通过 SelectionKey 对象的 attachment() 方法获取与通道关联的附件对象。
                    ServerSocketChannel channel = (ServerSocketChannel)key.channel();
                    //ServerSocketChannel attachment = (ServerSocketChannel)key.attachment();
                    //得到客户端的channel
                    //因为之前把服务端channel注册到selector上时，同时把serverSocketChannel放进去了
                    //channel.accept() 方法是一个阻塞方法，直到有客户端连接到达，它才会返回客户端的套接字通道
                    //没有NIO，这个就是阻塞的方法了。
                    SocketChannel socketChannel = channel.accept();
                    socketChannel.configureBlocking(false);
                    //接下来就要管理客户端的channel了，和服务端的channel的做法相同，客户端的channel也应该被注册到selector上
                    //通过一次次的轮询来接受并处理channel上的相关事件
                    //把客户端的channel注册到之前已经创建好的selector上
                    SelectionKey socketChannelKey = socketChannel.register(selector, 0, socketChannel);
                    //给客户端的channel设置可读事件
                    socketChannelKey.interestOps(SelectionKey.OP_READ);
                    logger.info("客户端连接成功！");
                    //连接成功之后，用客户端的channel写回一条消息
                    socketChannel.write(ByteBuffer.wrap("我发送成功了".getBytes()));
                    logger.info("向客户端发送数据成功！");
                }
                //如果接受到的为可读事件，说明要用客户端的channel来处理
                if (key.isReadable()) {
                    SocketChannel channel = (SocketChannel)key.channel();
                    //分配字节缓冲区来接受客户端传过来的数据
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    //向buffer写入客户端传来的数据
                    int len = channel.read(buffer);
                    logger.info("读到的字节数：" + len);
                    if (len == -1) {
                        channel.close();
                        break;
                    }else{
                        //切换buffer的读模式
                        buffer.flip();
                        logger.info(Charset.defaultCharset().decode(buffer).toString());
                    }
                }
            }
        }
    }
}
