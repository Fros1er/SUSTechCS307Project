package impl.services;

import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.CourseSearchEntry;
import cn.edu.sustech.cs307.dto.CourseTable;
import cn.edu.sustech.cs307.dto.Major;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.service.StudentService;

import javax.annotation.Nullable;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

public class StudentServiceImpl implements StudentService {

    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        throw new UnsupportedOperationException();

    }

    @Override
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {
        throw new UnsupportedOperationException();

    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {
        throw new UnsupportedOperationException();

    }

    @Override
    public Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @Nullable Integer semesterId) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public boolean passedPrerequisitesForCourse(int studentId, String courseId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Major getStudentMajor(int studentId) {
        throw new UnsupportedOperationException();
        
    }
}
