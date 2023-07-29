package netty04;
import netty03.NioEventLoop;
import java.nio.channels.SocketChannel;

/**
 * 这个类在这里，继承了MultithreadEventExecutorGroup，其作用跟
 * SingleThreadEventLoop继承了SingleThreadEventExecutor的作用是一样的。
 */
public abstract class MultithreadEventLoopGroup extends MultithreadEventExecutorGroup implements EventLoopGroup{

    public MultithreadEventLoopGroup(int threads) {
        super(threads);
    }

    @Override
    protected abstract EventLoop newChild();

    @Override
    public EventLoop next() {
        return (EventLoop) super.next();
    }

    //这个方法是实现了EventLoopGroup接口中的同名方法
    @Override
    public void register(SocketChannel channel, NioEventLoop nioEventLoop){
        next().register(channel,nioEventLoop);
    }
}
