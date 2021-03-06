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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static impl.utils.Util.*;

public class UserServiceImpl implements UserService {

    private static final Set<Integer> userSet = ConcurrentHashMap.newKeySet();
    private static final ReentrantReadWriteLock userSetLock = new ReentrantReadWriteLock(true);

    static {
        ReentrantReadWriteLock.WriteLock l = userSetLock.writeLock();
        l.lock();
        safeSelect("SELECT id FROM \"user\"", stmt -> {}, resultSet -> userSet.add(resultSet.getInt(1)));
        l.unlock();
    }

    public static String getFullName(String firstName, String lastName) {
        StringBuilder fullName = new StringBuilder(firstName);
        if (firstName.matches("^[a-zA-Z ]*") && lastName.matches("^[a-zA-Z ]*"))
            fullName.append(' ');
        fullName.append(lastName);
        return fullName.toString();
    }

    @Override
    public void removeUser(int userId) {
        if (!userSet.contains(userId)) return;
        Map<String, CheckedConsumer<PreparedStatement>> queries = new LinkedHashMap<>();
        commitAllInsertion("user");
        queries.put("ALTER TABLE student DISABLE TRIGGER delete_user_by_student", stmt -> {
        });
        queries.put("ALTER TABLE instructor DISABLE TRIGGER delete_user_by_instructor", stmt -> {
        });
        queries.put(
                "DELETE FROM \"user\" where id = ?",
                stmt -> stmt.setInt(1, userId)
        );
        queries.put("ALTER TABLE student ENABLE TRIGGER delete_user_by_student", stmt -> {
        });
        queries.put("ALTER TABLE instructor ENABLE TRIGGER delete_user_by_instructor", stmt -> {
        });
        try {
            updateAll(queries);
            ReentrantReadWriteLock.WriteLock l = userSetLock.writeLock();
            l.lock();
            userSet.remove(userId);
            l.unlock();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<User> getAllUsers() {
        //TODO: need verify by SA
        commitAllInsertion("user");
        List<User> res = new ArrayList<>();
        safeSelect("SELECT * FROM \"user\"",
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
        commitAllInsertion("user");
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

    public static boolean addUser(int id) {
        ReentrantReadWriteLock.WriteLock l = userSetLock.writeLock();
        l.lock();
        boolean res = userSet.add(id);
        l.unlock();
        return res;
    }

    public static boolean hasUser(int id) {
        ReentrantReadWriteLock.ReadLock l = userSetLock.readLock();
        l.lock();
        boolean res = userSet.contains(id);
        l.unlock();
        return res;
    }
}
