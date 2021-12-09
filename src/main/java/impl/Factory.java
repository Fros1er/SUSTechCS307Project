package impl;

import cn.edu.sustech.cs307.factory.ServiceFactory;
import cn.edu.sustech.cs307.service.*;
import impl.services.*;

import java.util.List;

public class Factory extends ServiceFactory {
    public Factory() {
        super();
        registerService(DepartmentService.class, new DepartmentServiceImpl());
        registerService(CourseService.class, new CourseServiceImpl());
        registerService(InstructorService.class, new InstructorServiceImpl());
        registerService(MajorService.class, new MajorServiceImpl());
        registerService(SemesterService.class, new SemesterServiceImpl());
        registerService(StudentService.class, new StudentServiceImpl());
        registerService(UserService.class, new UserServiceImpl());
    }
    @Override
    public List<String> getUIDs() {
        return null;
    }
}
