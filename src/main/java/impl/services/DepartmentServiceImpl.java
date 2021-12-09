package impl.services;

import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.DepartmentService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static impl.utils.Util.*;

public class DepartmentServiceImpl implements DepartmentService {
    @Override
    public int addDepartment(String name) {
        try {
            ResultSet rs = query((conn -> {
                PreparedStatement stmt = null;
                stmt = conn.prepareStatement("INSERT INTO public.department (id, name) VALUES (DEFAULT, ?)");
                stmt.setString(1, name);
                return stmt;
            }));
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            for (Throwable th : e) {
                if (th instanceof SQLException && Objects.equals(((SQLException) th).getSQLState(), "23505")) {
                    throw new IntegrityViolationException();
                }
            }
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void removeDepartment(int departmentId) {
        safeQuery(conn -> {
            PreparedStatement stmt = conn.prepareStatement("DELETE FROM department WHERE id = ?");
            stmt.setInt(1, departmentId);
            return stmt;
        });
    }

    @Override
    public List<Department> getAllDepartments() {
        List<Department> res = new ArrayList<>();
            handleResult(
                safeQuery(conn -> conn.prepareStatement("SELECT * FROM department")),
                (resultSet) -> {
                    Department d = new Department();
                    d.id = resultSet.getInt(1);
                    d.name = resultSet.getString(2);
                    res.add(d);
            });
        return res;
    }

    @Override
    public Department getDepartment(int departmentId) {
        try {
            ResultSet s = query(conn -> {
                PreparedStatement stmt = conn.prepareStatement("SELECT * FROM department where id = ?");
                stmt.setInt(1, departmentId);
                return stmt;
            });
            if (s.next()) {
                Department d = new Department();
                d.id = s.getInt(1);
                d.name = s.getString(2);
                return d;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
