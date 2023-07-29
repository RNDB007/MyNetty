package netty04;

import netty03.NioEventLoop;

/**
 * @author Joe Lee
 * @date
 * @Description
 */
public class NioEventLoopGroup extends MultithreadEventLoopGroup{

    public NioEventLoopGroup(int threads) {
        super(threads);
    }

    //在这里，如果返回的这个EventLoop接口继承了EventExecutor接口，就可以调用EventExecutor接口中的
    //方法了。
    @Override
    protected EventLoop newChild(){
        return new NioEventLoop();
    }

}
