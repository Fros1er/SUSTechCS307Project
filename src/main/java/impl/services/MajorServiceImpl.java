package impl.services;

import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.MajorService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static impl.utils.Util.*;

@ParametersAreNonnullByDefault
public class MajorServiceImpl implements MajorService {
    @Override
    public int addMajor(String name, int departmentId) {
        return insert("INSERT INTO public.major (id, name, department_id) VALUES (DEFAULT, ?, ?)", stmt -> {
            stmt.setString(1, name);
            stmt.setInt(2, departmentId);
        });
    }

    @Override
    public void removeMajor(int majorId) {
        //TODO: remove students
        safeQuery("DELETE FROM public.major WHERE id = ?",
                stmt -> stmt.setInt(1, majorId));
    }

    @Override
    public List<Major> getAllMajors() {
        List<Major> res = new ArrayList<>();
        handleResult(
                safeQuery("SELECT * FROM major INNER JOIN department"),
                (resultSet) -> {
                    Major m = new Major();
                    getMajorValuesFromResultSet(m, resultSet);
                    res.add(m);
                });
        return res;
    }

    @Override
    public Major getMajor(int majorId) {
        Major res = new Major();
        if (handleResult(
                safeQuery("SELECT * FROM major INNER JOIN department where major.id = ?",
                        stmt -> stmt.setInt(1, majorId)),
                (resultSet) -> getMajorValuesFromResultSet(res, resultSet))) {
            return res;
        }
        return null;
    }

    private void getMajorValuesFromResultSet(Major res, ResultSet resultSet) throws SQLException {
        Department d = new Department();
        res.id = resultSet.getInt(1);
        res.name = resultSet.getString(2);
        d.id = resultSet.getInt(3);
        d.name = resultSet.getString(4);
        res.department = d;
    }

    //TODO: IF COURSE_ID UNIQUE

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {
        throw new UnsupportedOperationException();

    }
}
