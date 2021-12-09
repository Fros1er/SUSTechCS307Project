package impl.services;

import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.dto.CourseSectionClass;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.service.CourseService;

import javax.annotation.Nullable;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

public class CourseServiceImpl implements CourseService {

    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeCourse(String courseId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeCourseSection(int sectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeCourseSectionClass(int classId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Course> getAllCourses() {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public Course getCourseBySection(int sectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        throw new UnsupportedOperationException();
    }
}
