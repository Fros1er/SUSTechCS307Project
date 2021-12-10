package impl.utils;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
            if (isInsertionFailed(e)) throw new IntegrityViolationException();
            e.printStackTrace();
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
}
