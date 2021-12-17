package impl.utils;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

public class Util {
    public static List<Integer> updateAll(Map<String, CheckedConsumer<PreparedStatement>> queries) throws SQLException {
        List<Integer> res = new ArrayList<>();
        Connection conn = SQLDataSource.getInstance().getSQLConnection();
        conn.setAutoCommit(false);
        for (String sql : queries.keySet()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            queries.get(sql).accept(stmt);
            res.add(stmt.executeUpdate());
        }
        conn.commit();
        conn.close();
        return res;
    }

    public static boolean select(String sql, CheckedConsumer<PreparedStatement> consumer, CheckedConsumer<ResultSet> resHandler) throws SQLException {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            consumer.accept(stmt);
            ResultSet res = stmt.executeQuery();
            boolean flag = false;
            while (res.next()) {
                resHandler.accept(res);
                flag = true;
            }
            return flag;
        }
    }

    public static boolean safeSelect(String sql, CheckedConsumer<ResultSet> resHandler) {
        return safeSelect(sql, stmt -> {
        }, resHandler);
    }

    public static boolean safeSelect(String sql, CheckedConsumer<PreparedStatement> consumer, CheckedConsumer<ResultSet> resHandler) {
        try {
            return select(sql, consumer, resHandler);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int update(String sql, CheckedConsumer<PreparedStatement> consumer) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            consumer.accept(stmt);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            if (isInsertionFailed(e)) throw new IntegrityViolationException();
        }
        return 0;
    }

    public static int update(String sql, CheckedBiConsumer<Connection, PreparedStatement> consumer) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            consumer.accept(conn, stmt);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            if (isInsertionFailed(e)) throw new IntegrityViolationException();
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean isInsertionFailed(SQLException e) {
        for (Throwable th : e) {
            if (th instanceof SQLException && Objects.equals(((SQLException) th).getSQLState(), "23505")) {
                return true;
            }
        }
        return false;
    }

    private static final Map<String, Pair<Connection, PreparedStatement>> batchedQuery = new HashMap<>();
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();
    public static final List<Future<?>> userThreads = new ArrayList<>();

    public static int timeout = 100;

    public static void commitAllUserInsertion() {
        synchronized (Util.class) {
            userThreads.forEach(v -> {
                try {
                    v.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
            userThreads.clear();
        }
    }

    public static void updateBatch(String sql, CheckedConsumer<PreparedStatement> consumer) {
        synchronized (Util.class) {
            Connection conn;
            PreparedStatement stmt;
            try {
                if (!batchedQuery.containsKey(sql)) {
                    conn = SQLDataSource.getInstance().getSQLConnection();
                    stmt = conn.prepareStatement(sql);
                    batchedQuery.put(sql, new Pair<>(conn, stmt));
                    conn.setAutoCommit(false);
                    userThreads.add(threadPool.submit(() -> {
                        try {
                            try {
                                Thread.sleep(timeout);
                            } catch (InterruptedException ignored) {
                            } finally {
                                batchedQuery.remove(sql);
                                stmt.executeBatch();
                                conn.commit();
                                conn.close();
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }));
                } else {
                    Pair<Connection, PreparedStatement> p = batchedQuery.get(sql);
                    conn = p.first;
                    stmt = p.second;
                }
                consumer.accept(stmt);
                stmt.addBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
