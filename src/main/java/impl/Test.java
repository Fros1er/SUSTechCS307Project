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
        String a = "egsijh";
        String b = "aaa vvvv";
        String c = "e?";
        System.out.println(a.matches("[a-zA-Z ]*"));
        System.out.println(b.matches("[a-zA-Z ]*"));
        System.out.println(c.matches("[a-zA-Z ]*"));
    }
}
