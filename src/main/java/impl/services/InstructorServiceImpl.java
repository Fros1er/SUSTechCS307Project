package impl.services;

import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.service.InstructorService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

import static impl.services.UserServiceImpl.addUser;
import static impl.utils.Util.safeSelect;

@ParametersAreNonnullByDefault
public class InstructorServiceImpl implements InstructorService {
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        addUser(userId, firstName, lastName,
                "INSERT INTO public.instructor (user_id) VALUES (?)",
                stmt -> stmt.setInt(1, userId));
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
