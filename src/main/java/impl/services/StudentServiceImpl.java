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

import static impl.services.UserServiceImpl.getFullName;
import static impl.utils.Util.*;

@ParametersAreNonnullByDefault
public class StudentServiceImpl implements StudentService {

    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        updateBatch("user", "SELECT insert_student(?, ?, ?, ?)",
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, getFullName(firstName, lastName));
                    stmt.setInt(3, majorId);
                    stmt.setDate(4, enrolledDate);
                });
    }

    @Override
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        commitAllEnrolledCourseInsertion();
        commitAllInsertion("user");
        StringBuilder sql1 = new StringBuilder("select * from section join class on semester_id = ? and section.id = class.section_id");
        StringBuilder sql2 = new StringBuilder(" join course on section.course_id = course.id");
        StringBuilder sql3 = new StringBuilder(" join \"user\" on \"user\".id = instructor_id");
        StringBuilder sql4 = new StringBuilder(" join student on user_id = ? join major_course");
        String sql5 = "";
        String sql6 = " order by course.id,course_name limit ? offset ?;";
        if (searchCid != null) {
            sql2.append(" and course.id = '").append(searchCid).append("'");
        }
        if (searchName != null) {
            sql2.append(" and course_name = '").append(searchName).append("'");
        }
        if (searchInstructor != null) {
            sql3.append(" and full_name = '").append(searchInstructor).append("'");
        }
        if (searchDayOfWeek != null) {
            sql1.append(" and day_of_week = '").append(searchDayOfWeek.name()).append("'");
        }
        if (searchClassTime != null) {
            sql1.append(" and class_start <= ").append(searchClassTime).append(" and class_end >= ").append(searchClassTime);
        }
        if (searchClassLocations != null && !searchClassLocations.isEmpty()) {
            sql1.append(" and location in (");
            for (int i = 0; i < searchClassLocations.size(); i++) {
                if (i == searchClassLocations.size() - 1) {
                    sql1.append("'").append(searchClassLocations.get(i)).append("')");
                } else {
                    sql1.append("'").append(searchClassLocations.get(i)).append("',");
                }
            }
        }
        boolean useType = false;
        boolean situation = false;
        switch (searchCourseType) {
            case ALL: {
                situation=true;
                break;
            }
            case MAJOR_COMPULSORY:
            case MAJOR_ELECTIVE: {
                sql4.append(" on student.major_id = major_course.major_id and type = CAST(? AS majorcoursetype)");
                useType = true;
                break;
            }
            case CROSS_MAJOR: {
                sql4.append(" on student.major_id != major_course.major_id");
                break;
            }
            case PUBLIC: {
                sql4.append(" on course.id != major_course.course_id");
                break;
            }
        }
        if (ignoreFull) {
            sql1.append(" and left_capacity > 0");
        }
        boolean igp = true;
        if (!ignoreMissingPrerequisites) {
            igp=false;
            if (!situation) {
                sql4.append(" and is_prerequisite_satisfied(student.user_id, course.id) = true");
            } else  {
                sql4.append(" on is_prerequisite_satisfied(student.user_id, course.id) = true");
            }
        }
        if (igp && situation)
            sql4.append(" on false");
        if (ignorePassed) {
            sql5 = " join student_course on section.id = student_course.section_id and student_id = student.user_id and grade >= 60";
        }
        HashMap<String, String[]> enrolledCourse = new HashMap<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            enrolledCourse.put(d.name(), new String[10]);
        }
        safeSelect("select day_of_week,section_name,course_name,class_start from student_course\n" +
                        "    join class on class.section_id = student_course.section_id and student_id = ?\n" +
                        "    join section on class.section_id = section.id\n" +
                        "    join course on section.course_id = course.id;",
                stmt -> stmt.setInt(1, studentId),
                resultSet -> {
                    String weekday = resultSet.getString(1);
                    enrolledCourse.get(weekday)[resultSet.getInt(4)] = String.format("%s[%s]", resultSet.getString(3), resultSet.getString(2));
                });
        StringBuilder sql0 = new StringBuilder();
        sql0.append(sql1).append(sql2).append(sql3).append(sql4).append(sql5).append(sql6);
        List<CourseSearchEntry> list = new ArrayList<>();
        HashMap<Integer, CourseSearchEntry> buffer = new HashMap<>();
        boolean finalUseType = useType;
        safeSelect(sql0.toString(),
                stmt -> {
                    stmt.setInt(1, semesterId);
                    stmt.setInt(2, studentId);
                    if (finalUseType) {
                        if (searchCourseType == CourseType.MAJOR_COMPULSORY) {
                            stmt.setString(3, "Compulsory");
                        } else {
                            stmt.setString(3, "Elective");
                        }
                        stmt.setInt(4, pageSize);
                        stmt.setInt(5, pageIndex * (pageSize - 1));
                    } else {
                        stmt.setInt(3, pageSize);
                        stmt.setInt(4, pageIndex * (pageSize - 1));
                    }

                },
                resultSet -> {
                    Course course = new Course();
                    CourseSection section = new CourseSection();
                    CourseSectionClass tem = new CourseSectionClass();
                    tem.instructor = new Instructor();
                    course.id = resultSet.getString(3);
                    course.name = resultSet.getString(16);
                    course.classHour = resultSet.getInt(18);
                    course.credit = resultSet.getInt(17);
                    course.grading = Course.CourseGrading.valueOf(resultSet.getString(19));
                    section.id = resultSet.getInt(1);
                    section.name = resultSet.getString(2);
                    section.totalCapacity = resultSet.getInt(5);
                    section.leftCapacity = resultSet.getInt(6);
                    tem.id = resultSet.getInt(7);
                    tem.instructor.id = resultSet.getInt(20);
                    tem.instructor.fullName = resultSet.getString(21);
                    tem.classBegin = resultSet.getShort(12);
                    tem.classEnd = resultSet.getShort(13);
                    tem.dayOfWeek = DayOfWeek.valueOf(resultSet.getString(10));
                    tem.weekList = new HashSet<>(Arrays.asList((Short[]) resultSet.getArray(11).getArray()));
                    tem.location = resultSet.getString(14);
                    boolean sw = true;
                    if (!ignoreConflict) {
                        if (enrolledCourse.get(tem.dayOfWeek.name())[tem.classBegin] != null) {
                            sw = false;
                        }
                    }
                    if (buffer.containsKey(section.id) && sw) {
                        buffer.get(section.id).sectionClasses.add(tem);
                    } else if (sw) {
                        CourseSearchEntry cse = new CourseSearchEntry();
                        cse.sectionClasses = new HashSet<>();
                        cse.conflictCourseNames = new ArrayList<>();
                        cse.course = course;
                        cse.section = section;
                        if (ignoreConflict) {
                            cse.conflictCourseNames.add(enrolledCourse.get(tem.dayOfWeek.name())[tem.classBegin]);
                        }
                        buffer.put(section.id, cse);
                    }
                }
        );
        return list;
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {
        commitAllInsertion("user");
        commitAllInsertion("student_course_add");
        updateBatch("student_course_remove", "delete from public.student_course where student_id = ? and section_id = ?",
                stmt -> {
                    stmt.setInt(1, studentId);
                    stmt.setInt(2, sectionId);
                });
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        commitAllInsertion("user");
        commitAllInsertion("student_course_remove");
        if (grade != null) {
            updateBatch("student_course_add", "insert into public.student_course(student_id,section_id,grade) values (?,?,?)",
                    stmt -> {
                        stmt.setInt(1, studentId);
                        stmt.setInt(2, sectionId);
                        stmt.setShort(3, grade.when(new Grade.Cases<>() {
                            @Override
                            public Short match(PassOrFailGrade self) {
                                return (short) ((self == PassOrFailGrade.PASS) ? 101 : -1);
                            }

                            @Override
                            public Short match(HundredMarkGrade self) {
                                return self.mark;
                            }
                        }));
                    });
        } else {
            updateBatch("student_course_add", "insert into public.student_course(student_id,section_id) values (?,?)",
                    stmt -> {
                        stmt.setInt(1, studentId);
                        stmt.setInt(2, sectionId);
                    });
        }
    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {
        commitAllEnrolledCourseInsertion();
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
        commitAllEnrolledCourseInsertion();
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
                        "select section_name,section.id,begin from section join (\n" +
                        "select id,begin from semester where ? between begin and \"end\") t1\n" +
                        "on t1.id = section.semester_id) t2\n" +
                        "on section_id = id and student_id = ?) t3\n" +
                        "on class.section_id = t3.section_id and findWeek(begin,?) = ANY (week_list)\n" +
                        "join \"user\" on instructor_id = \"user\".id\n" +
                        "order by day_of_week;",
                stmt -> {
                    stmt.setDate(1, date);
                    stmt.setInt(2, studentId);
                    stmt.setDate(3, date);
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
