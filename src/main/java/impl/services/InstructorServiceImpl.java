package impl.services;

import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.service.InstructorService;

import java.util.List;

public class InstructorServiceImpl implements InstructorService {
    @Override
    public void addInstructor(int userId, String firstName, String lastName) {
        throw new UnsupportedOperationException();

    }

    @Override
    public List<CourseSection> getInstructedCourseSections(int instructorId, int semesterId) {
        throw new UnsupportedOperationException();
    }
}
