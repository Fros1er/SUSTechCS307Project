package impl;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class Test {
    public static void main(String[] args) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM department");
            stmt.execute();
            ResultSet s = stmt.getResultSet();
            while (s.next()) {
                System.out.println(s.getInt(1));
                System.out.println(s.getString(2));
            }
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
