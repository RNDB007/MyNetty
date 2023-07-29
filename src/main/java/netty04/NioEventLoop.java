package netty04;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.TimeUnit;

/**
 * @author Joe Lee
 * @date
 * @Description
 */
public class NioEventLoop extends SingleThreadEventLoop{

    private final SelectorProvider provider;

    private Selector selector;

    public NioEventLoop() {
        this.provider = SelectorProvider.provider();
        this.selector = openSecector();
    }

    @Override
    public void run() {
        while (true) {
            try {
                //没有事件就阻塞在这里，下面这几个方法我就不列出来了，大家知道功能即可
                select();
                //如果走到这里，就说明selector不再阻塞了
                processSelectedKeys(selector.selectedKeys());
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                //执行单线程执行器中的所有任务
                runAllTasks();
            }
        }
    }

    @Override
    public void shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        return ;
    }
}
