package netty02;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Joe Lee
 * @date 2023/7/27
 * @Description 将线程包装在一个线程池类中，引入 Executor 接口。
 * 补全线程池必须具备的最基本属性：任务队列、拒绝策略、添加任务的方法和执行方法
 * 在这个单线程线程池中，实际上，如果是main线程调用该线程池的register方法，则把任务放入队列；若是线程池中的线程调用，则负责取出运行，或执行IO。
 */
public class SingleThreadEventExecutor implements Executor {

    private static final Logger logger = LoggerFactory.getLogger(SingleThreadEventExecutor.class);

    //任务队列的容量，默认是Integer的最大值
    protected static final int DEFAULT_MAX_PENDING_TASKS = Integer.MAX_VALUE;

    private final Queue<Runnable> taskQueue;

    //拒绝策略
    private final RejectedExecutionHandler rejectedExecutionHandler;

    private volatile boolean start = false;

    //用于获取Socket
    private final SelectorProvider provider;

    private Selector selector;

    private Thread thread;

    public SingleThreadEventExecutor() {
        //java中的方法，通过provider不仅可以得到selector，还可以得到ServerSocketChannel和SocketChannel
        this.provider = SelectorProvider.provider();
        this.taskQueue = newTaskQueue(DEFAULT_MAX_PENDING_TASKS);
        this.rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();
        this.selector = openSecector();
    }

    protected Queue<Runnable> newTaskQueue(int maxPendingTasks) {
        return new LinkedBlockingQueue<Runnable>(maxPendingTasks);
    }

    @Override
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        //把任务提交到任务队列中
        addTask(task);
        //启动单线程执行器中的线程
        startThread();
    }

    private void addTask(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        //如果添加失败，执行拒绝策略
        if (!offerTask(task)) {
            reject(task);
        }
    }

    private void startThread() {
        if (start) {
            return;
        }
        start = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                //这里是得到了新创建的线程
                thread = Thread.currentThread();
                //执行run方法，在run方法中，就是对io事件的处理
                //因为写在run中，这个方法，其实是由这个新的线程来运行的
                SingleThreadEventExecutor.this.run();
            }
        }).start();
        logger.info("新线程创建了！");
    }

    final boolean offerTask(Runnable task) {
        return taskQueue.offer(task);
    }

    /**
     * @Author:
     * @Description:判断任务队列中是否有任务
     */
    protected boolean hasTasks() {
        System.out.println("我没任务了！");
        return !taskQueue.isEmpty();
    }

    /**
     * @Author:
     * @Description:执行任务队列中的所有任务
     */
    protected void runAllTasks() {
        runAllTasksFrom(taskQueue);
    }

    protected void runAllTasksFrom(Queue<Runnable> taskQueue) {
        //从任务队列中拉取任务,如果第一次拉取就为null，说明任务队列中没有任务，直接返回即可
        Runnable task = pollTaskFrom(taskQueue);
        if (task == null) {
            return;
        }
        for (;;) {
            //执行任务队列中的任务
            safeExecute(task);
            //执行完毕之后，拉取下一个任务，如果为null就直接返回
            task = pollTaskFrom(taskQueue);
            if (task == null) {
                return;
            }
        }
    }

    private void safeExecute(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            logger.warn("A task raised an exception. Task: {}", task, t);
        }
    }

    protected static Runnable pollTaskFrom(Queue<Runnable> taskQueue) {
        return taskQueue.poll();
    }

    /**
     * @Author:
     * @Description: 判断当前执行任务的线程是否是执行器的线程
     * 如果是true，表明当前线程是执行器的线程（注意不是管理该线程池的线程）
     */
    public boolean inEventLoop(Thread thread) {
        return thread == this.thread;
    }

    protected final void reject(Runnable task) {
        //rejectedExecutionHandler.rejectedExecution(task, this);
    }
    //该方法中的 this.thread 属性就是该单线程执行器管理的线程，而该线程只有在被创建的时候才会赋值，并且是由新创建的线程赋值给该属性。
    // 而作为方法传递进来的 thread 参数，则是正在执行 register 方法的线程。 所以当执行inEventLoop(Thread.currentThread())
    // 这个方法的时候，执行当前方法的线程是主线程，而单线程执行器管理的线程还未创建，结果肯定会返回 false。所以，代码就会执行到下面的分支，
    // 把 register0 方法封装成异步任务，提交给单线程执行器去执行.
    public void register(SocketChannel socketChannel) {
        //如果执行该方法的线程就是执行器中的线程，直接执行方法即可
        if (inEventLoop(Thread.currentThread())) {
            register0(socketChannel);
        }else {
            //在这里，第一次向单线程执行器中提交任务的时候，执行器终于开始执行了,新的线程也开始创建
            this.execute(new Runnable() {
                @Override
                public void run() {
                    register0(socketChannel);
                    logger.info("客户端的channel已注册到新线程的多路复用器上了！");
                }
            });
        }
    }

    private void register0(SocketChannel channel) {
        try {
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * @Author:
     * @Description:得到用于多路复用的selector
     */
    private Selector openSecector() {
        try {
            selector = provider.openSelector();
            return selector;
        } catch (IOException e) {
            throw new RuntimeException("failed to open a new selector", e);
        }
    }

    /**
     * @Author:
     * @Description: 判断线程是否需要在selector阻塞，或者继续运行的方法，为什么现在需要这个方法了？
     * 因为现在我们新创建的线程不仅要处理io事件，还要处理用户提交过来的任务，如果一直在selector上阻塞着，
     * 显然用户提交的任务也就无法执行了。所以要有限时的阻塞，并且只要用户提交了任务，就要去执行那些任务。
     * 在这里，用户提交的任务就是把客户端channel注册到selector上。
     */
    private void select() throws IOException {
        Selector selector = this.selector;
        //这里是一个死循环
        for (;;){
            //如果没有就绪事件，就在这里阻塞3秒，有限时的阻塞
            logger.info("新线程阻塞在这里3秒吧。。。。。。。");
            int selectedKeys = selector.select(3000);
            //如果有事件或者单线程执行器中有任务待执行，或者有IO事件要执行，就退出循环
            if (selectedKeys != 0 || hasTasks()) {
                break;
            }
        }
    }

    private void processSelectedKeys(Set<SelectionKey> selectedKeys) throws IOException {
        if (selectedKeys.isEmpty()) {
            return;
        }
        Iterator<SelectionKey> i = selectedKeys.iterator();
        for (;;) {
            final SelectionKey k = i.next();
            i.remove();
            //处理就绪事件
            processSelectedKey(k);
            if (!i.hasNext()) {
                break;
            }
        }
    }

    private void processSelectedKey(SelectionKey k) throws IOException {
        //如果是读事件
        if (k.isReadable()) {
            SocketChannel channel = (SocketChannel)k.channel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            int len = channel.read(byteBuffer);
            if (len == -1) {
                logger.info("客户端通道要关闭！");
                channel.close();
                return;
            }
            byte[] bytes = new byte[len];
            byteBuffer.flip();
            byteBuffer.get(bytes);
            logger.info("新线程收到客户端发送的数据:{}",new String(bytes));
        }
    }

    public void run() {
        while (true) {
            try {
                //没有事件就阻塞在这里
                select();
                //如果走到这里，就说明selector没有阻塞了
                processSelectedKeys(selector.selectedKeys());
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                //执行单线程执行器中的所有任务
                runAllTasks();
            }
        }
    }
}
