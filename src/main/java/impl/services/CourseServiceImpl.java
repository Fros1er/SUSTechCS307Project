package impl.services;

import cn.edu.sustech.cs307.dto.Course;
import cn.edu.sustech.cs307.dto.CourseSection;
import cn.edu.sustech.cs307.dto.CourseSectionClass;
import cn.edu.sustech.cs307.dto.Student;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Set;

import static impl.utils.Util.*;

@ParametersAreNonnullByDefault
public class CourseServiceImpl implements CourseService {
    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {
        //TODO: Support prerequisite
        try {
            query("INSERT INTO public.course (course_id, course_name, credit, hour, grading) VALUES (?, ?, ?, ?, ?)",
                    stmt -> {
                        stmt.setString(1, courseId);
                        stmt.setString(2, courseName);
                        stmt.setInt(3, credit);
                        stmt.setInt(4, classHour);
                        stmt.setInt(5, grading.ordinal());
                    });
        } catch (SQLException e) {
            if (isInsertionFailed(e)) throw new IntegrityViolationException();
            e.printStackTrace();
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        return insert("INSERT INTO public.section (id, courseId, semesterId, sectionName, totalCapacity) VALUES (DEFAULT, ?, ?, ?, ?)",
                (stmt) -> {
                    stmt.setString(1, courseId);
                    stmt.setInt(2, semesterId);
                    stmt.setString(3, sectionName);
                    stmt.setInt(4, totalCapacity);
                }
        );
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        //TODO: section
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeCourse(String courseId) {
        //TODO: Support prerequisite, remove selected courses
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
