package impl.services;

import cn.edu.sustech.cs307.database.SQLDataSource;
import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.prerequisite.AndPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.CoursePrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.OrPrerequisite;
import cn.edu.sustech.cs307.dto.prerequisite.Prerequisite;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.CourseService;
import impl.utils.CheckedConsumer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.*;

import static impl.utils.Util.*;

@ParametersAreNonnullByDefault
public class CourseServiceImpl implements CourseService {

    //TODO: Section left capacity
    @Override
    public void addCourse(String courseId, String courseName, int credit, int classHour, Course.CourseGrading grading, @Nullable Prerequisite prerequisite) {
        try {
            Connection conn = SQLDataSource.getInstance().getSQLConnection();
            int groupSerialStart = 1;
            try {
                PreparedStatement getSerial = conn.prepareStatement("SELECT currval('prerequisite_group_id_seq')");
                ResultSet resultSet = getSerial.executeQuery();
                resultSet.next();
                groupSerialStart = resultSet.getInt(1) + 1;
            } catch (SQLException ignored) {
            }
            conn.setAutoCommit(false);
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO public.course (id, course_name, credit, hour, grading) VALUES (?, ?, ?, ?, ?)");
            stmt.setString(1, courseId);
            stmt.setString(2, courseName);
            stmt.setInt(3, credit);
            stmt.setInt(4, classHour);
            stmt.setString(5, grading.name());
            stmt.executeUpdate();
            if (prerequisite != null) {
                List<List<String>> truthTable = prerequisite.when(new Prerequisite.Cases<>() {
                    @Override
                    public List<List<String>> match(AndPrerequisite self) {
                        List<List<String>> res = self.terms.get(0).when(this);
                        for (int i = 1; i < self.terms.size(); i++) {
                            Prerequisite term = self.terms.get(i);
                            List<List<String>> tmp = new ArrayList<>();
                            for (List<String> resList : res) {
                                for (List<String> itemList : term.when(this)) {
                                    List<String> productRes = new ArrayList<>();
                                    productRes.addAll(resList);
                                    productRes.addAll(itemList);
                                    tmp.add(productRes);
                                }
                            }
                            res = tmp;
                        }
                        return res;
                    }

                    @Override
                    public List<List<String>> match(OrPrerequisite self) {
                        ArrayList<List<String>> res = new ArrayList<>();
                        for (Prerequisite pre : self.terms) {
                            res.addAll(pre.when(this));
                        }
                        return res;
                    }

                    @Override
                    public List<List<String>> match(CoursePrerequisite self) {
                        ArrayList<List<String>> res = new ArrayList<>();
                        res.add(new ArrayList<>());
                        res.get(0).add(self.courseID);
                        return res;
                    }
                });
                stmt = conn.prepareStatement("INSERT INTO public.prerequisite_group (id, target_course_id, \"count\") VALUES (DEFAULT, '" + courseId + "', ?)");
                PreparedStatement truthTableStmt = conn.prepareStatement("INSERT INTO public.prerequisite_truth_table (id, group_id, course_id) VALUES (DEFAULT, ?, ?)");
                for (List<String> group : truthTable) {
                    stmt.setInt(1, group.size());
                    stmt.addBatch();
                    for (String preCourseId : group) {
                        truthTableStmt.setInt(1, groupSerialStart);
                        truthTableStmt.setString(2, preCourseId);
                        truthTableStmt.addBatch();
                    }
                    groupSerialStart++;
                }
                stmt.executeBatch();
                truthTableStmt.executeBatch();
            }
            conn.commit();
            conn.close();
        } catch (SQLException e) {
            if (isInsertionFailed(e)) throw new IntegrityViolationException();
            e.printStackTrace();
        }
    }

    @Override
    public int addCourseSection(String courseId, int semesterId, String sectionName, int totalCapacity) {
        return update("INSERT INTO public.section (id, course_id, semester_id, section_name, total_capacity, left_capacity) VALUES (DEFAULT, ?, ?, ?, ?, ?)",
                (stmt) -> {
                    stmt.setString(1, courseId);
                    stmt.setInt(2, semesterId);
                    stmt.setString(3, sectionName);
                    stmt.setInt(4, totalCapacity);
                    stmt.setInt(5, totalCapacity);
                }
        );
    }

    @Override
    public int addCourseSectionClass(int sectionId, int instructorId, DayOfWeek dayOfWeek, Set<Short> weekList, short classStart, short classEnd, String location) {
        if (classEnd <= classStart) throw new IntegrityViolationException();
        return update("INSERT INTO public.class (id, section_id, instructor_id, day_of_week, week_list, class_start, class_end, location) VALUES (DEFAULT, ?, ?, CAST(? AS weekday), ?, ?, ?, ?)",
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
        update("DELETE FROM course WHERE id = ?", stmt -> stmt.setString(1, courseId));
    }

    @Override
    public void removeCourseSection(int sectionId) {
        update("DELETE FROM section WHERE id = ?", stmt -> stmt.setInt(1, sectionId));
    }

    @Override
    public void removeCourseSectionClass(int classId) {
        update("DELETE FROM class WHERE id = ?", stmt -> stmt.setInt(1, classId));
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
