package impl.services;

import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.dto.User;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.UserService;
import impl.utils.CheckedConsumer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static impl.utils.Util.*;

public class UserServiceImpl implements UserService {

    public static List<ResultSet> addUser(int userId, String firstName, String lastName, String sql, CheckedConsumer<PreparedStatement> consumer) {
        Map<String, CheckedConsumer<PreparedStatement>> queries = new HashMap<>();
        StringBuilder fullName = new StringBuilder(firstName);
        if (!firstName.matches("^[a-zA-Z ]*") && !lastName.matches("^[a-zA-Z ]*"))
            fullName.append(' ');
        fullName.append(lastName);
        queries.put(
                "INSERT INTO public.user (id, full_name) VALUES (?, ?)",
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, fullName.toString());
                });
        queries.put(sql, consumer);
        try {
            return queryAll(queries);
        } catch (SQLException e) {
            if (isInsertionFailed(e)) throw new IntegrityViolationException();
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void removeUser(int userId) {
        //TODO: remove user
        throw new UnsupportedOperationException();

    }

    @Override
    public List<User> getAllUsers() {
        //TODO: ask SA for this hacky way
        List<User> res = new ArrayList<>();
        handleResult(
                safeQuery("SELECT * FROM major INNER JOIN department"),
                (resultSet) -> {
                    User u = new User() {};
                    u.id = resultSet.getInt(1);
                    u.fullName = resultSet.getString(2);
                    res.add(u);
                });
        return res;
    }

    @Override
    public User getUser(int userId) {
        // Not used...
        throw new UnsupportedOperationException();

    }
}
