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
    public static List<ResultSet> queryAll(Map<String, CheckedConsumer<PreparedStatement>> queries) throws SQLException {
        List<ResultSet> res = new ArrayList<>();
        Connection conn = SQLDataSource.getInstance().getSQLConnection();
        for (String sql : queries.keySet()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            queries.get(sql).accept(stmt);
            stmt.execute();
            res.add(stmt.getResultSet());
        }
        conn.commit();
        return res;
    }

    public static ResultSet query(String sql, CheckedConsumer<PreparedStatement> consumer) throws SQLException {
        Connection conn = SQLDataSource.getInstance().getSQLConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        consumer.accept(stmt);
        stmt.execute();
        conn.commit();
        return stmt.getResultSet();
    }

    public static ResultSet safeQuery(String sql) {
        return safeQuery(sql, stmt -> {});
    }

    public static ResultSet safeQuery(String sql, CheckedConsumer<PreparedStatement> consumer) {
        try {
            return query(sql, consumer);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int insert(String sql, CheckedConsumer<PreparedStatement> consumer) {
        try {
            ResultSet rs = query(sql, consumer);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            if (isInsertionFailed(e)) throw new IntegrityViolationException();
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean handleResult(ResultSet res, CheckedConsumer<ResultSet> consumer) {
        return handleResult(res, consumer, false);
    }

    public static boolean handleResult(ResultSet res, CheckedConsumer<ResultSet> consumer, boolean isUnique) {
        if (res == null) return false;
        boolean flag = false;
        try {
            while (res.next()) {
                consumer.accept(res);
                flag = true;
                if (isUnique) return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flag;
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
