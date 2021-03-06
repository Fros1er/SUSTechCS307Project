package impl.services;

import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
import cn.edu.sustech.cs307.service.DepartmentService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

import static impl.utils.Util.*;

@ParametersAreNonnullByDefault
public class DepartmentServiceImpl implements DepartmentService {
    @Override
    public int addDepartment(String name) {
        return update(
                "INSERT INTO public.department (id, name) VALUES (DEFAULT, ?)",
                stmt -> stmt.setString(1, name)
        );
    }

    @Override
    public void removeDepartment(int departmentId) {
        update(
                "DELETE FROM department WHERE id = ?",
                stmt -> stmt.setInt(1, departmentId)
        );
    }

    @Override
    public List<Department> getAllDepartments() {
        List<Department> res = new ArrayList<>();
        safeSelect("SELECT * FROM department",
                resultSet -> {
                    Department d = new Department();
                    d.id = resultSet.getInt(1);
                    d.name = resultSet.getString(2);
                    res.add(d);
                });
        return res;
    }

    @Override
    public Department getDepartment(int departmentId) {
        Department d = new Department();
        if(!safeSelect("SELECT * FROM department where id = ?",
                stmt -> stmt.setInt(1, departmentId),
                resultSet -> {
                    d.id = resultSet.getInt(1);
                    d.name = resultSet.getString(2);
                })) throw new EntityNotFoundException();
        return d;
    }
}
