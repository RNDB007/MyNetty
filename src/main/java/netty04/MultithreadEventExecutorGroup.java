package netty04;

import java.util.concurrent.TimeUnit;

/**
 * @author Joe Lee
 * @date
 * @Description
 */
public abstract class MultithreadEventExecutorGroup implements EventExecutorGroup{

    private EventExecutor[] eventExecutor;

    private int index = 0;

    public MultithreadEventExecutorGroup(int threads) {
        eventExecutor = new EventExecutor[threads];
        for (int i = 0; i < threads; i ++){
            eventExecutor[i] =  newChild();
        }
    }

    /**
     * 这里定义了一个抽象方法。是给子类实现的。因为你不知道要返回的是EventExecutor的哪个实现类。
     */
    protected abstract EventExecutor newChild();

    @Override
    public EventExecutor next() {
        int id = index % eventExecutor.length;
        index++;
        return eventExecutor[id];
    }

    @Override
    public void shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        next().shutdownGracefully(quietPeriod, timeout, unit);
    }
}
