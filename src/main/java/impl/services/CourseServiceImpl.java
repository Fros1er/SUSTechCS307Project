package impl.services;

import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static impl.utils.Util.*;

@ParametersAreNonnullByDefault
public class CourseServiceImpl implements CourseService {

    //TODO: Section left capacity
    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {
        //TODO: Support prerequisite
        update("INSERT INTO public.course (id, course_name, credit, hour, grading) VALUES (?, ?, ?, ?, ?)",
                stmt -> {
                    stmt.setString(1, courseId);
                    stmt.setString(2, courseName);
                    stmt.setInt(3, credit);
                    stmt.setInt(4, classHour);
                    stmt.setInt(5, grading.ordinal());
                });
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        return update("INSERT INTO public.section (id, course_id, semester_id, section_name, total_capacity) VALUES (DEFAULT, ?, ?, ?, ?)",
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
        if (classEnd <= classStart) throw new IntegrityViolationException();
        return update("INSERT INTO public.class (id, section_id, instructor_id, day_of_week, week_list, class_start, class_end, location) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?)",
                (conn, stmt) -> {
                    stmt.setInt(1, sectionId);
                    stmt.setInt(2, instructorId);
                    stmt.setString(3, dayOfWeek.name());
                    stmt.setArray(4, conn.createArrayOf("smallint", weekList.toArray()));
                    stmt.setShort(5, classStart);
                    stmt.setShort(6, classEnd);
                    stmt.setString(7, location);
                }
        );
    }

    @Override
    public void removeCourse(String courseId) {
        //TODO: Support prerequisite, remove selected courses
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeCourseSection(int sectionId) {
        //TODO: Support
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeCourseSectionClass(int classId) {
        //TODO: Support
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Course> getAllCourses() {
        List<Course> res = new ArrayList<>();
        safeSelect("SELECT * FROM course",
                resultSet -> res.add(getCourseFromResultSet(resultSet)));
        return res;
    }

    @Override
    public List<CourseSection> getCourseSectionsInSemester(String courseId, int semesterId) {
        //TODO: Support
        throw new UnsupportedOperationException();

    }

    @Override
    public Course getCourseBySection(int sectionId) {
        //TODO: Support
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CourseSectionClass> getCourseSectionClasses(int sectionId) {
        //TODO: Support
        throw new UnsupportedOperationException();

    }

    @Override
    public CourseSection getCourseSectionByClass(int classId) {
        //TODO: Support
        throw new UnsupportedOperationException();

    }

    @Override
    public List<Student> getEnrolledStudentsInSemester(String courseId, int semesterId) {
        //TODO: Support
        throw new UnsupportedOperationException();
    }

    private Course getCourseFromResultSet(ResultSet resultSet) throws SQLException {
        Course c = new Course();
        c.id = resultSet.getString(1);
        c.name = resultSet.getString(2);
        c.credit = resultSet.getInt(3);
        c.classHour = resultSet.getInt(4);
        c.grading = Course.CourseGrading.values()[resultSet.getShort(5)];
        return c;
    }
}
