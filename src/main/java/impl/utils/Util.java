package impl.utils;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.Department;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Util {
    public static ResultSet query(CheckedFunction<Connection, PreparedStatement> func) throws SQLException {
        Connection conn = SQLDataSource.getInstance().getSQLConnection();
        PreparedStatement stmt = func.apply(conn);
        stmt.execute();
        return stmt.getResultSet();
    }

    public static ResultSet safeQuery(CheckedFunction<Connection, PreparedStatement> func) {
        try {
            return query(func);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void handleResult(ResultSet res, CheckedConsumer<ResultSet> consumer) {
        if (res == null) return;
        try {
            while (res.next()) {
                consumer.accept(res);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
