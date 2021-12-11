package impl.services;

import cn.edu.sustech.cs307.dto.Department;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.exception.EntityNotFoundException;
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
        return update("INSERT INTO public.major (id, name, department_id) VALUES (DEFAULT, ?, ?)", stmt -> {
            stmt.setString(1, name);
            stmt.setInt(2, departmentId);
        });
    }

    @Override
    public void removeMajor(int majorId) {
        //TODO: remove students
        update("DELETE FROM public.major WHERE id = ?",
                stmt -> stmt.setInt(1, majorId));
    }

    @Override
    public List<Major> getAllMajors() {
        List<Major> res = new ArrayList<>();
        safeSelect("SELECT * FROM major INNER JOIN department",
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
        if (safeSelect("SELECT * FROM major INNER JOIN department where major.id = ?",
                stmt -> stmt.setInt(1, majorId),
                (resultSet) -> getMajorValuesFromResultSet(res, resultSet))) {
            throw new EntityNotFoundException();
        }
        return res;
    }

    private void getMajorValuesFromResultSet(Major res, ResultSet resultSet) throws SQLException {
        Department d = new Department();
        res.id = resultSet.getInt(1);
        res.name = resultSet.getString(2);
        d.id = resultSet.getInt(3);
        d.name = resultSet.getString(4);
        res.department = d;
    }

    @Override
    public void addMajorCompulsoryCourse(int majorId, String courseId) {
        update("INSERT INTO public.major_course (major_id, course_id, type) VALUES (?, ?, 'Compulsory')",
                stmt -> {
                    stmt.setInt(1, majorId);
                    stmt.setString(2, courseId);
                });
    }

    @Override
    public void addMajorElectiveCourse(int majorId, String courseId) {
        update("INSERT INTO public.major_course (major_id, course_id, type) VALUES (?, ?, 'Elective')",
                stmt -> {
                    stmt.setInt(1, majorId);
                    stmt.setString(2, courseId);
                });
    }
}
