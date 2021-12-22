package impl.services;

import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.InstructorService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

import static impl.services.UserServiceImpl.*;
import static impl.utils.Util.safeSelect;
import static impl.utils.Util.updateBatch;

@ParametersAreNonnullByDefault
public class InstructorServiceImpl implements InstructorService {
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        if (hasUser(userId)) throw new IntegrityViolationException();
        updateBatch("instructor", "SELECT insert_instructor(?, ?)",
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, getFullName(firstName, lastName));
                });
        addUser(userId);
    }

    @Override
    public List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId) {
        List<CourseSection> res = new ArrayList<>();
        safeSelect("SELECT section_id, section_name, total_capacity, left_capacity FROM class INNER JOIN section s ON s.id = class.section_id WHERE instructor_id = ? and semester_id = ?",
                stmt -> {
                    stmt.setInt(1, instructorId);
                    stmt.setInt(2, semesterId);
                },
                resultSet -> {
                    CourseSection s = new CourseSection();
                    s.id = resultSet.getInt(1);
                    s.name = resultSet.getString(2);
                    s.totalCapacity = resultSet.getInt(3);
                    s.leftCapacity = resultSet.getInt(4);
                    res.add(s);
                });
        return res;
    }
}
