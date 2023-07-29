package netty04;

import java.util.concurrent.TimeUnit;

/**
 * @author Joe Lee
 * @date
 * @Description
 */
public interface EventExecutorGroup {

    EventExecutor next();

    void shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit);
}
