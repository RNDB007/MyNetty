package netty04;

import netty03.NioEventLoop;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author Joe Lee
 * @date
 * @Description
 */
public abstract class SingleThreadEventLoop extends SingleThreadEventExecutor implements EventLoop{


    @Override
    public EventLoop next() {
        return this;
    }

    @Override
    public void register(SocketChannel channel, NioEventLoop nioEventLoop) {
        nioEventLoop.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    channel.configureBlocking(false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    channel.register(nioEventLoop.selector(), SelectionKey.OP_READ);
                } catch (ClosedChannelException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
