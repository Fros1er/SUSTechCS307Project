package impl.services;

import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.dto.User;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.UserService;
import impl.utils.CheckedConsumer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static impl.utils.Util.*;

public class UserServiceImpl implements UserService {

    public static List<Integer> addUser(int userId, String firstName, String lastName, String sql, CheckedConsumer<PreparedStatement> consumer) {
        Map<String, CheckedConsumer<PreparedStatement>> queries = new LinkedHashMap<>();
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
            return updateAll(queries);
        } catch (SQLException e) {
            e.printStackTrace();
            if (isInsertionFailed(e)) throw new IntegrityViolationException();
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
        //TODO: need verify by SA
        List<User> res = new ArrayList<>();
        safeSelect("SELECT * FROM user",
                (resultSet) -> {
                    User u = new User() {
                    };
                    u.id = resultSet.getInt(1);
                    u.fullName = resultSet.getString(2);
                    res.add(u);
                });
        return res;
    }

    @Override
    public User getUser(int userId) {
        // TODO: verify by SA
        User res = new User() {
        };
        safeSelect("SELECT * FROM user WHERE id = ?",
                stmt -> stmt.setInt(1, userId),
                resultSet -> {
                    res.id = resultSet.getInt(1);
                    res.fullName = resultSet.getString(2);
                });
        return res;
    }
}
