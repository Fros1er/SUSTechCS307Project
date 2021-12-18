package impl.utils;

import cn.edu.sustech.cs307.database.SQLDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchedStatement {
    public static int COUNTDOWN_TIME = 50;

    private final AtomicInteger countdown = new AtomicInteger();

    private final Connection conn;
    private final PreparedStatement stmt;
    private final Thread thread;
    private final Object lock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean finished = new AtomicBoolean(false);

    public BatchedStatement(String sql) throws SQLException {
        countdown.set(COUNTDOWN_TIME);
        conn = SQLDataSource.getInstance().getSQLConnection();
        conn.setAutoCommit(false);
        stmt = conn.prepareStatement(sql);
        thread = new Thread(() -> {
            while (countdown.getAndDecrement() > 0) {
                try {
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
                    System.out.println("User load finished");
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
            countdown.set(COUNTDOWN_TIME);
            return true;
        }
    }

    public void join() {
        if (finished.get()) return;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean hasFinished() {
        return finished.get();
    }
}
