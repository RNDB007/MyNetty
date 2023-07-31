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
public class DefaultPromise<V> implements Promise<V> {
    //执行后得到的结果要赋值给该属性
    private volatile Object result;
    //用户传进来的要被执行的又返回值的任务
    private Callable<V> callable;
    //这个成员变量的作用很简单，当有一个外部线程在await方法中阻塞了，该属性就加1，每当一个外部
    //线程被唤醒了，该属性就减1.简单来说，就是用来记录阻塞的外部线程数量的
    //在我们手写的代码和源码中，这个成员变量是Short类型的，限制阻塞线程的数量，如果阻塞的
    //线程太多就报错，这里我们只做简单实现
    private int waiters;

    public DefaultPromise(Callable<V> callable) {
        this.callable = callable;
    }

    @Override
    public void run() {
        V object;
        //得到callable
        Callable<V> c = callable;
        //执行callable，得到返回值
        try {
            object = c.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //走到这就意味着任务正常结束，可以正常把执行结果赋值给成员变量outcome
        set(object);

    }

    protected void set(V v) {
        result = v;
        //唤醒被阻塞的外部线程
        checkNotifyWaiters();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException{
        //说明这时候没有结果
        if (result == null) {
            //就要阻塞等待，这个等待，指的是外部调用get方法的线程等待
            await();
        }
        return getNow();
    }

    //有限时地获取任务的返回结果
    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        //阻塞了用户设定的时间之后
        if (await(timeout, unit)) {
            //直接返回任务的执行结果
            return getNow();
        }
        return null;
    }

    //等待结果的方法
    public Promise<V> await() throws InterruptedException {
        //如果已经执行完成，直接返回即可
        if (isDone()) {
            return this;
        }
        //如果线程中断，直接抛出异常
        if (Thread.interrupted()) {
            throw new InterruptedException(toString());
        }
        //wait要和synchronized一起使用，在futurtask的源码中
        //这里使用了LockSupport.park方法
        synchronized (this) {
            //如果成功赋值则直接返回，不成功进入循环
            while (!isDone()) {
                //waiters字段加一，记录在此阻塞的线程数量
                ++waiters;
                try {
                    //释放锁并等待
                    wait();
                } finally {
                    //等待结束waiters字段减一
                    --waiters;
                }
            }
        }
        return this;
    }

    //有限时地等待结果的方法
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return await0(unit.toNanos(timeout), true);
    }

    //这个方法虽然很长，但是逻辑都很简单
    private boolean await0(long timeoutNanos, boolean interruptable) throws InterruptedException {
        //执行成功则直接返回
        if (isDone()) {
            return true;
        }
        //传入的时间小于0则直接判断是否执行完成
        if (timeoutNanos <= 0) {
            return isDone();
        }
        //interruptable为true则允许抛出中断异常，为false则不允许，判断当前线程是否被中断了
        //如果都为true则抛出中断异常
        if (interruptable && Thread.interrupted()) {
            throw new InterruptedException(toString());
        }
        //获取当前纳秒时间
        long startTime = System.nanoTime();
        //用户设置的等待时间
        long waitTime = timeoutNanos;
        for (;;) {
            synchronized (this) {
                //再次判断是否执行完成了
                if (isDone()) {
                    return true;
                }
                //如果没有执行完成，则开始阻塞等待，阻塞线程数加一
                ++waiters;
                try {
                    //阻塞在这里
                    wait(timeoutNanos);
                } finally {
                    //阻塞线程数减一
                    --waiters;
                }
            }
            //走到这里说明线程被唤醒了
            if (isDone()) {
                return true;
            } else {
                //可能是虚假唤醒。
                //System.nanoTime() - startTime得到的是经过的时间
                //得到新的等待时间，如果等待时间小于0，表示已经阻塞了用户设定的等待时间。如果waitTime大于0，则继续循环
                waitTime = timeoutNanos - (System.nanoTime() - startTime);
                if (waitTime <= 0) {
                    return isDone();
                }
            }
        }
    }

    //检查并且唤醒阻塞线程的方法
    private synchronized void checkNotifyWaiters() {
        if (waiters > 0) {
            notifyAll();
        }
    }

    //直接返回任务的执行结果，如果result未被赋值，则直接返回null
    public V getNow() {
        Object result = this.result;
        return (V) result;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    //任务是否已经执行完成，也就是判断result成员变量是否被赋值了
    @Override
    public boolean isDone() {
        return isDone0(result);
    }

    private static boolean isDone0(Object result) {
        return result != null;
    }


    //先暂且实现这几个方法，接口中的其他方法，等需要的时候再做实现
}

