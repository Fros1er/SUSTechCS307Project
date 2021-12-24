package impl.utils;

import cn.edu.sustech.cs307.database.SQLDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchedStatement {
    public static int COUNTDOWN_TIME = 50;
    public static int BATCH_SIZE = 114514;

    private final AtomicInteger countdown = new AtomicInteger();
    private int count = 0;

    private final Connection conn;
    private PreparedStatement stmt;
    private final Thread thread;
    private final Object lock = new Object();
    private final AtomicBoolean submitting = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final long start;
    private final String sql;

    @SuppressWarnings("all")
    public BatchedStatement(String sql, String name) throws SQLException {
        this.sql = sql;
        start = System.nanoTime();
        countdown.set(COUNTDOWN_TIME);
        conn = SQLDataSource.getInstance().getSQLConnection();
        conn.setAutoCommit(false);
        stmt = conn.prepareStatement(sql);
        thread = new Thread(() -> {
            while (true) {
                try {
                    if (!submitting.get()) countdown.decrementAndGet();
                    if (countdown.get() <= 0) break;
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            synchronized (lock) {
                try {
                    closed.set(true);
                    stmt.executeBatch();
                    conn.commit();
                    conn.close();
                    finished.set(true);
                    System.out.printf("Batch %s finished, used %.2fms\n", name, (System.nanoTime() - start) / 1000000.0);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public boolean addBatch(CheckedConsumer<PreparedStatement> consumer) throws SQLException {
        synchronized (lock) {
            if (closed.get()) return false;
            consumer.accept(stmt);
            stmt.addBatch();
            if (++count >= BATCH_SIZE) {
                submitting.set(true);
                stmt.executeBatch();
                stmt = conn.prepareStatement(sql);
                count = 0;
                submitting.set(false);
            }
            countdown.set(COUNTDOWN_TIME);
            return true;
        }
    }

    public void join() {
        if (finished.get()) return;
        try {
            synchronized (lock) {
                closed.set(true);
                countdown.set(0);
            }
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean hasFinished() {
        return finished.get();
    }
}
