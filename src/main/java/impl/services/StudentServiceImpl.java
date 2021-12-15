package impl.services;

import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.service.StudentService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Date;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static impl.services.UserServiceImpl.addUser;
import static impl.utils.Util.*;

@ParametersAreNonnullByDefault
public class StudentServiceImpl implements StudentService {

    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        addUser(userId, firstName, lastName,
                "INSERT INTO public.student (user_id, major_id, enrolled_date) VALUES (?, ?, ?)",
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, majorId);
                    stmt.setDate(3, enrolledDate);
                });
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
        update("delete from public.student_course where student_id = ? and section_id = ?",
                stmt -> {
                    stmt.setInt(1, studentId);
                    stmt.setInt(2, sectionId);
                });
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        if (grade != null) {
            update("insert into public.student_course(student_id,section_id,grade) values (?,?,?)",
                    stmt -> {
                        stmt.setInt(1, studentId);
                        stmt.setInt(2, sectionId);
                        stmt.setString(3, grade.when(new Grade.Cases<>() {
                            @Override
                            public String match(PassOrFailGrade self) {
                                return self.name();
                            }

                            @Override
                            public String match(HundredMarkGrade self) {
                                return String.valueOf(self.mark);
                            }
                        }));
                    });
        } else {
            update("insert into public.student_course(student_id,section_id) value (?,?)",
                    stmt -> {
                        stmt.setInt(1, studentId);
                        stmt.setInt(2, sectionId);
                    });
        }
    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {
        update("update public.student_course set grade = ? where student_id = ? and section_id = ?",
                stmt -> {
                    stmt.setInt(2, studentId);
                    stmt.setInt(3, sectionId);
                    stmt.setString(1, grade.when(new Grade.Cases<>() {
                        @Override
                        public String match(PassOrFailGrade self) {
                            return self.name();
                        }

                        @Override
                        public String match(HundredMarkGrade self) {
                            return String.valueOf(self.mark);
                        }
                    }));
                });
    }

    @Override
    public Map<Course, Grade> getEnrolledCoursesAndGrades(int studentId, @Nullable Integer semesterId) {
        Map<Course, Grade> result = new HashMap<>();
        Course tem = new Course();
        StringBuilder sql = new StringBuilder("select course.id, course.course_name, course.credit, course.hour, course.grading, grade " +
                "from course join (select grade, course_id from student_course join section on ");
        if (semesterId != null) {
            sql.append("semester_id = ? and ");
        }
        sql.append("student_course.section_id = section.id) t1 on t1.course_id = course.id;");
        safeSelect(sql.toString(),
                stmt -> {
                    if (semesterId != null) {
                        stmt.setInt(1, semesterId);
                    }
                },
                resultSet -> {
                    tem.id = resultSet.getString(1);
                    tem.name = resultSet.getString(2);
                    tem.credit = resultSet.getInt(3);
                    tem.classHour = resultSet.getInt(4);
                    String t = resultSet.getString(5);
                    String t2 = resultSet.getString(6);
                    tem.grading = Course.CourseGrading.valueOf(t);
                    if (t.equals("PASS_OR_FAIL")) {
                        PassOrFailGrade grade = PassOrFailGrade.valueOf(t2);
                        result.put(tem, grade);
                    } else {
                        HundredMarkGrade grade = new HundredMarkGrade(Short.parseShort(t2));
                        result.put(tem, grade);
                    }
                }
        );
        return result;
    }

    @Override
    public CourseTable getCourseTable(int studentId, Date date) {
        CourseTable courseTable = new CourseTable();
        safeSelect("select day_of_week,section_name,instructor_id,full_name,class_start,class_end,location from class join (\n" +
                        "select * from student_course join (\n" +
                        "select section_name,section.id from section join (\n" +
                        "select id from semester where begin < ? and ? < semester.end) t1\n" +
                        "on t1.id = section.semester_id) t2\n" +
                        "on section_id = id and student_id = ?) t3\n" +
                        "on class.section_id = t3.section_id and array_position(week_list,?) IS NOT NULL\n" +
                        "join \"user\" on instructor_id = \"user\".id;",
                stmt -> {
                    stmt.setDate(1, date);
                    stmt.setDate(2, date);
                    stmt.setInt(3, studentId);
                },
                resultSet -> {
                    CourseTable.CourseTableEntry entry = new CourseTable.CourseTableEntry();
                    entry.courseFullName = resultSet.getString(2);
                    Instructor instructor = new Instructor();
                    instructor.id = resultSet.getInt(3);
                    instructor.fullName = resultSet.getString(4);
                    entry.instructor = instructor;
                    entry.classBegin = resultSet.getShort(5);
                    entry.classEnd = resultSet.getShort(6);
                    entry.location = resultSet.getString(7);
                    DayOfWeek time = DayOfWeek.valueOf(resultSet.getString(1));
                    if (courseTable.table.containsKey(time)) {
                        courseTable.table.get(time).add(entry);
                    } else {
                        courseTable.table.put(time, new HashSet<>());
                    }
                });
        return courseTable;
    }

    @Override
    public boolean passedPrerequisitesForCourse(int studentId, String courseId) {
        AtomicBoolean res = new AtomicBoolean(false);
        safeSelect("select * from is_prerequisite_satisfied(?, ?)",
                stmt -> {
                    stmt.setInt(1, studentId);
                    stmt.setString(2, courseId);
                },
                resultSet -> res.set(resultSet.getBoolean(1)));
        return res.get();
    }

    @Override
    public Major getStudentMajor(int studentId) {
        Major major = new Major();
        safeSelect("select * from major join student on user_id = ? and student.major_id = major.id",
                stmt -> stmt.setInt(1, studentId),
                resultSet -> {
                    major.id = resultSet.getInt(1);
                    major.name = resultSet.getString(2);
                    major.department.id = resultSet.getInt(3);
                });
        safeSelect("select name from department where id = ?",
                stmt -> stmt.setInt(1, major.id),
                resultSet -> major.department.name = resultSet.getString(1));
        return major;
    }
}
