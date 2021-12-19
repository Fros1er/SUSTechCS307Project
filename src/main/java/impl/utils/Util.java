package impl.utils;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class Util {
    public static List<Integer> updateAll(Map<String, CheckedConsumer<PreparedStatement>> queries) throws SQLException {
        List<Integer> res = new ArrayList<>();
        Connection conn = SQLDataSource.getInstance().getSQLConnection();
        conn.setAutoCommit(false);
        conn.prepareStatement("BEGIN TRANSACTION").execute();
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
            System.out.println(sql);
        }
        return false;
    }

    public static int update(String sql, CheckedConsumer<PreparedStatement> consumer) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            consumer.accept(stmt);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            int res = 0;
            if (rs.next()) {
                res = rs.getInt(1);
            }
            return res;
        } catch (SQLException e) {
            if (isInsertionFailed(e)) throw new IntegrityViolationException();
            e.printStackTrace();
        }
        return 0;
    }

    public static int update(String sql, CheckedBiConsumer<Connection, PreparedStatement> consumer) {
        try (Connection conn = SQLDataSource.getInstance().getSQLConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            consumer.accept(conn, stmt);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            int res = 0;
            if (rs.next()) {
                res = rs.getInt(1);
            }
            return res;
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

    private static final Map<String, List<BatchedStatement>> batchedStatements = new ConcurrentHashMap<>();

    public static void commitAllInsertion(String name) {
        if (Objects.equals(name, "user")) {
            commitAllUpdates("instructor");
            commitAllUpdates("student");
        } else if (Objects.equals(name, "student_course")) {
            commitAllUpdates("student_course_a");
            commitAllUpdates("student_course_b");
        }
    }

    public static void updateBatch(String name, String sql, CheckedConsumer<PreparedStatement> consumer) {
        try {
            synchronized (Util.class) {
                if (!batchedStatements.containsKey(name)) batchedStatements.put(name, new LinkedList<>());
                boolean flag = true;
                for (Iterator<BatchedStatement> it = batchedStatements.get(name).iterator(); it.hasNext(); ) {
                    BatchedStatement batchedStatement = it.next();
                    if (batchedStatement.hasFinished()) it.remove();
                    else if (batchedStatement.addBatch(consumer)) {
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    BatchedStatement batchedStatement = new BatchedStatement(sql, name);
                    batchedStatement.addBatch(consumer);
                    batchedStatements.get(name).add(batchedStatement);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void commitAllUpdates(String name) {
        synchronized (Util.class) {
            if (batchedStatements.containsKey(name)) {
                if (batchedStatements.get(name).isEmpty()) return;
                for (BatchedStatement batchedStatement : batchedStatements.get(name)) {
                    System.out.println("Joining: " + name);
                    batchedStatement.join();
                }
                batchedStatements.get(name).clear();
            }
        }
    }
}
