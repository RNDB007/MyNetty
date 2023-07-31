package netty05;

import java.util.concurrent.Future;

/**
 * @author Joe Lee
 * @date
 * @Description
 */
public interface Promise<V> extends Runnable, Future<V> {

    @Override
    void run();

}
