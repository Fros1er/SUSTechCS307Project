package impl.services;

import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.service.InstructorService;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static impl.services.UserServiceImpl.addUser;

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
        //TODO: section not implemented
        throw new UnsupportedOperationException();
    }
}
