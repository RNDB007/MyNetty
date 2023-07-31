package netty05;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Joe Lee
 * @date
 * @Description
 */
public class DefaultPromiseTest {
    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        //创建一个Callable
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                //睡眠一会
                Thread.sleep(1);
                return 1314;
            }
        };
        //创建一个DefaultPromise，把任务传进DefaultPromise中
        Promise<Integer> promise = new DefaultPromise<Integer>(callable);
        //创建一个线程
        Thread t = new Thread(promise);
        t.start();
        Thread.sleep(1000);
        //无超时获取结果
        System.out.println(promise.get());

        //有超时获取结果
        System.out.println(promise.get(500, TimeUnit.MILLISECONDS));
    }
}
