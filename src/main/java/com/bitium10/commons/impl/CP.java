package com.bitium10.commons.impl;

import com.bitium10.commons.CPConfig;
import com.bitium10.commons.ConnectPool;
import com.bitium10.commons.PooledConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <b>项目名</b>： db-pool <br>
 * <b>包名称</b>： com.bitium10.commons.impl <br>
 * <b>类名称</b>： CP <br>
 * <b>类描述</b>： <br>
 * <b>创建人</b>： <a href="mailto:wylipengming@chinabank.com.cn">李朋明</a> <br>
 * <b>修改人</b>： <br>
 * <b>创建时间</b>：2014/10/2 9:21
 * <b>修改时间</b>： <br>
 * <b>修改备注</b>： <br>
 *
 * @version 1.0.0 <br>
 */
public class CP implements ConnectPool {
    private static final Logger log = LoggerFactory.getLogger(CP.class);

    private static final String[] classPaths = System.getProperty("java.class.path", "classes").split(System.getProperty("path.separator", ";"));

    private static final AtomicInteger POOL_ID = new AtomicInteger(0);
    private final int poolId = 0;
    private String poolName;
    private final CPConfig config = null;
    private final AtomicInteger connectionNo = new AtomicInteger(0);

    private final AtomicInteger validConnectionNum = new AtomicInteger(0);

    private final Map<Integer, PooledConnection> validConnectionsPool = new ConcurrentHashMap();

    private final LinkedStack<Integer> idleConnectionsId = new LinkedStack();

    private AtomicBoolean shutdown = new AtomicBoolean(false);

    private AtomicBoolean inited = new AtomicBoolean(false);
    private Thread monitor;
    private boolean configFromProperties = false;

    private BlockingQueue<NamedConnection> unclosedConnections = new LinkedBlockingQueue();

    @Override
    public String getPoolName() {
        return null;
    }

    @Override
    public void setPoolName(String paramString) {

    }

    @Override
    public int getActiveConnectionsCount() {
        return 0;
    }

    @Override
    public int getIdleConnectionsCount() {
        return 0;
    }

    @Override
    public void reloadProperties() throws SQLException {

    }

    @Override
    public void shutdown() {

    }

    private static class LinkedStack<E> {
        private LinkedList<E> stack;
        private final ReentrantLock operLock = new ReentrantLock();
        private final Condition notEmpty = this.operLock.newCondition();
        private final Condition requireMore = this.operLock.newCondition();

        public LinkedStack() {
            this.stack = new LinkedList();
        }

        public int size() {
            return this.stack.size();
        }

        public boolean removeStackBottom(E e) {
            this.operLock.lock();
            try {
                if (this.stack.size() == 0) {
                    return false;
                }
                Object x = this.stack.getFirst();
                boolean bool2;
                if (x.equals(e)) {
                    this.stack.removeFirst();
                    return true;
                }
                return false;
            } finally {
                this.operLock.unlock();
            }
        }

        public void push(E e) {
            int c = -1;
            this.operLock.lock();
            try {
                c = this.stack.size();
                this.stack.addLast(e);
                if (c == 0)
                    this.notEmpty.signal();
            } finally {
                this.operLock.unlock();
            }
        }

        public E pop() {
            this.operLock.lock();
            try {
                Object localObject1;
                if (0 == this.stack.size()) {
                    return null;
                }
                return this.stack.removeLast();
            } finally {
                this.operLock.unlock();
            }
        }

        public long awaitRequireMore(long timeout, TimeUnit unit)
                throws InterruptedException {
            this.operLock.lockInterruptibly();
            try {
                return this.requireMore.awaitNanos(unit.toNanos(timeout));
            } catch (InterruptedException e) {
                this.requireMore.signal();
                throw e;
            } finally {
                this.operLock.unlock();
            }
        }

        public boolean awaitNotEmpty(long timeout, TimeUnit unit) {
            try {
                this.operLock.lockInterruptibly();
            } catch (InterruptedException e) {
                return true;
            }
            try {
                return this.notEmpty.await(timeout, unit);
            } catch (InterruptedException e) {
                return true;
            } finally {
                this.operLock.unlock();
            }
        }

        public E pop(long timeout, TimeUnit unit) throws InterruptedException, SQLException {
            long nanos = unit.toNanos(timeout);
            this.operLock.lockInterruptibly();
            try {
                while (true) {
                    Object x;
                    if (this.stack.size() > 0) {
                        x = this.stack.removeLast();
                        if (this.stack.size() > 0) {
                            this.notEmpty.signal();
                        }
                        return (E) x;
                    }

                    this.requireMore.signal();

                    if (nanos <= 0L)
                        return null;
                    try {
                        nanos = this.notEmpty.awaitNanos(nanos);
                    } catch (InterruptedException ie) {
                        this.notEmpty.signal();
                        throw ie;
                    }
                }
            } finally {
                this.operLock.unlock();
            }
        }

        public Integer[] toArray() {
            this.operLock.lock();
            try {
                Integer[] ret = new Integer[this.stack.size()];
                return (Integer[]) this.stack.toArray(ret);
            } finally {
                this.operLock.unlock();
            }
        }
    }


    private class CPMonitor extends Thread
    {
        private ExecutorService executorService = Executors.newSingleThreadExecutor();

        private CPMonitor()
        {
        }

        private long idleConnectionCheckOrClose()
                throws InterruptedException
        {
            long timeToNextCheck = CP.this.config.getCheckoutTimeoutMilliSec();
            Integer[] connIds = CP.this.idleConnectionsId.toArray();
            for (Integer connId : connIds) {
                PooledConnection pc = (PooledConnection)CP.this.validConnectionsPool.get(connId);
                pc.lock();
                try {
                    if (pc.isCheckOut())
                    {
                        pc.unlock(); break;
                    }
                    timeToNextCheck = pc.getTimeCheckIn() + WangyinCP.this.config.getIdleTimeoutMilliSec() - System.currentTimeMillis();
                    if (timeToNextCheck <= 0L)
                    {
                        timeToNextCheck = WangyinCP.this.config.getIdleTimeoutMilliSec();
                        if (this.validConnectionNum.get() > WangyinCP.this.config.getMinConnections())
                        {
                            if (!this.closeConnection(pc))
                            {
                                pc.unlock(); break;
                            }
                        }
                        else {
                            asyncCheckConnection(pc);
                        }

                    }
                    else
                    {
                        pc.unlock(); break; }  } finally { pc.unlock(); }

            }
            return timeToNextCheck;
        }

        private void asyncCheckConnection(final PooledConnection pooledConnection) {
            Future future = this.executorService.submit(new Runnable()
            {
                public void run() {
                    pooledConnection.doCheck();
                }
            });
            try {
                future.get(WangyinCP.this.config.getIdleTimeoutMilliSec(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                WangyinCP.log.warn(new Object[] { "get connection: ", pooledConnection.getConnectionName(), " check result error: ", e });
                pooledConnection.close();
            }
        }

        private void newMoreConnections(long waitTime)
                throws InterruptedException
        {
            try
            {
                while (WangyinCP.this.validConnectionNum.get() < WangyinCP.this.config.getMinConnections())
                    WangyinCP.this.newConnection(false);
            }
            catch (SQLException e) {
                WangyinCP.log.warn(e);
            }
            long nanos = TimeUnit.MILLISECONDS.toNanos(waitTime);
            long timeout = System.nanoTime() + nanos;
            while (timeout - System.nanoTime() > 0L) {
                nanos = WangyinCP.this.idleConnectionsId.awaitRequireMore(timeout - System.nanoTime(), TimeUnit.NANOSECONDS);
                if (nanos <= 0L) break;
                try {
                    WangyinCP.this.newConnection(false);
                } catch (SQLException e) {
                    WangyinCP.log.warn(e);
                }
            }
        }

        public void run()
        {
            WangyinCP.log.info(new Object[] { getName(), " start!" });
            long idleTimeout = WangyinCP.this.config.getIdleTimeoutMilliSec();
            while (!WangyinCP.this.shutdown.get()) {
                try
                {
                    newMoreConnections(idleTimeout);

                    WangyinCP.this.closeUnclosedConnection();

                    idleTimeout = idleConnectionCheckOrClose();

                    WangyinCP.this.logVerboseInfo(WangyinCP.this.config.isVerbose());
                } catch (InterruptedException e) {
                    if (WangyinCP.this.shutdown.get())
                        break;
                }
                catch (Exception e) {
                    idleTimeout = WangyinCP.this.config.getIdleTimeoutMilliSec();
                    WangyinCP.log.warn(e);
                } catch (Throwable t) {
                    idleTimeout = WangyinCP.this.config.getIdleTimeoutMilliSec();
                    WangyinCP.log.error(t);
                }
            }
            this.executorService.shutdown();
            try {
                if (!this.executorService.awaitTermination(1L, TimeUnit.SECONDS)) {
                    this.executorService.shutdownNow();
                    this.executorService.awaitTermination(1L, TimeUnit.SECONDS);
                }
            } catch (InterruptedException ignr) {  }

            WangyinCP.log.info(new Object[] { getName(), " quit!" });
        }
    }


}
