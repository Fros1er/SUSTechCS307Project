package impl.services;

import cn.edu.sustech.cs307.dto.*;
import cn.edu.sustech.cs307.dto.grade.Grade;
import cn.edu.sustech.cs307.dto.grade.HundredMarkGrade;
import cn.edu.sustech.cs307.dto.grade.PassOrFailGrade;
import cn.edu.sustech.cs307.exception.IntegrityViolationException;
import cn.edu.sustech.cs307.service.StudentService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.sql.Date;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static impl.services.MajorServiceImpl.hasMajor;
import static impl.services.SearchCourseQueryBuilder.buildSearchCourseSQL;
import static impl.services.UserServiceImpl.*;
import static impl.utils.Util.*;

@ParametersAreNonnullByDefault
public class StudentServiceImpl implements StudentService {

    @Override
    public void addStudent(int userId, int majorId, String firstName, String lastName, Date enrolledDate) {
        if (!hasMajor(majorId) || hasUser(userId)) throw new IntegrityViolationException();
        updateBatch("student", "SELECT insert_student(?, ?, ?, ?)",
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, getFullName(firstName, lastName));
                    stmt.setInt(3, majorId);
                    stmt.setDate(4, enrolledDate);
                });
        addUser(userId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId,
                                                @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor,
                                                @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations,
                                                CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed,
                                                boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        commitAllInsertion("student_course");
        commitAllInsertion("user");
        Map<Integer, CourseSearchEntry> resMap = new LinkedHashMap<>();
        Map<String, List<CourseSearchEntry>> courseConflictSearcherMap = new HashMap<>();
        List<CourseSearchEntry>[][][] courseTable = new List[15][7][20];
        String sql = buildSearchCourseSQL(studentId, semesterId, searchCid, searchName, searchInstructor,
                searchDayOfWeek, searchClassTime, searchClassLocations, searchCourseType, ignoreFull, ignoreMissingPrerequisites);
//        System.out.println(sql);
        if (sql.isEmpty()) return new ArrayList<>();
        safeSelect(sql, stmt -> {
        }, resultSet -> {
            int sectionId = resultSet.getInt("section_id");
            if (!resMap.containsKey(sectionId)) {
                CourseSearchEntry courseSearchEntry = new CourseSearchEntry();
                courseSearchEntry.sectionClasses = new HashSet<>();
                courseSearchEntry.conflictCourseNames = new ArrayList<>();

                Course course = new Course();
                course.id = resultSet.getString(1);
                course.classHour = resultSet.getInt(4);
                course.credit = resultSet.getInt(3);
                course.grading = Course.CourseGrading.valueOf(resultSet.getString(5));
                course.name = resultSet.getString(2);
                courseSearchEntry.course = course;

                CourseSection section = new CourseSection();
                section.id = resultSet.getInt(6);
                section.name = resultSet.getString(7);
                section.leftCapacity = resultSet.getInt(11);
                section.totalCapacity = resultSet.getInt(10);
                courseSearchEntry.section = section;

                resMap.put(sectionId, courseSearchEntry);
                if (!courseConflictSearcherMap.containsKey(course.id)) courseConflictSearcherMap.put(course.id, new LinkedList<>());
                courseConflictSearcherMap.get(course.id).add(courseSearchEntry);
            }
            CourseSectionClass cls = new CourseSectionClass();
            cls.id = resultSet.getInt(12);
            cls.instructor = InstructorServiceImpl.getInstructor(resultSet.getInt(14));
            cls.classBegin = resultSet.getShort(17);
            cls.classEnd = resultSet.getShort(18);
            cls.location = resultSet.getString(19);
            cls.dayOfWeek = DayOfWeek.valueOf(resultSet.getString(15));
            cls.weekList = Stream.of(((Short[]) resultSet.getArray(16).getArray())).collect(Collectors.toSet());
            resMap.get(sectionId).sectionClasses.add(cls);
            int dow = cls.dayOfWeek.getValue() - 1;
            for (Short i : cls.weekList) {
                for (int j = cls.classBegin; j <= cls.classEnd; j++) {
                    if (courseTable[i - 1][dow][j] == null) courseTable[i - 1][dow][j] = new LinkedList<>();
                    courseTable[i - 1][dow][j].add(resMap.get(sectionId));
                }
            }
        });



        Set<String> passedCourses = new LinkedHashSet<>();

        safeSelect("select s.id as section_id, c.course_name || '[' || s.section_name || ']' as full_name, s.course_id, s.semester_id, class.class_start, class.class_end, class.day_of_week, class.week_list, grade is not null and student_course.grade >= 60 as passed from student_course inner join section s on s.id = student_course.section_id inner join course c on c.id = s.course_id inner join class on class.section_id = student_course.section_id where student_id = ? order by c.course_name, s.section_name", stmt -> stmt.setInt(1, studentId),
                resultSet -> {
                    String course_id = resultSet.getString(3);
                    if (resultSet.getInt(4) == semesterId) {
                        String conflictString = resultSet.getString(2);
                        if (courseConflictSearcherMap.containsKey(course_id))
                            courseConflictSearcherMap.get(course_id).forEach(v -> {
                                if (!v.conflictCourseNames.contains(conflictString))
                                    v.conflictCourseNames.add(conflictString);
                            });
                        Short[] weekList = (Short[]) resultSet.getArray(8).getArray();
                        int dow = DayOfWeek.valueOf(resultSet.getString(7)).getValue() - 1;
                        int class_start = resultSet.getInt(5);
                        int class_end = resultSet.getInt(6);
                        for (Short i : weekList) {
                            for (int j = class_start; j <= class_end; j++) {
                                if (courseTable[i - 1][dow][j] != null)
                                    courseTable[i - 1][dow][j].forEach(v -> {
                                        if (!v.conflictCourseNames.contains(conflictString))
                                            v.conflictCourseNames.add(conflictString);
                                    });
                            }
                        }
                    }
                    if (resultSet.getBoolean(9)) passedCourses.add(course_id);
                }
        );

        List<CourseSearchEntry> res = new LinkedList<>(resMap.values());
        if (ignoreConflict || ignorePassed)
            res.removeIf(v -> (ignoreConflict && !v.conflictCourseNames.isEmpty()) || (ignorePassed && passedCourses.contains(v.course.id)));
//        res.sort((o1, o2) -> {
//            int idRes = o1.course.id.compareTo(o2.course.id);
//            return (idRes == 0) ? String.format("%s[%s]", o1.course.name, o1.section.name).compareTo(String.format("%s[%s]", o2.course.name, o2.section.name)) : idRes;
//        });
//        if (!ignoreConflict) {
//            res.forEach(v -> v.conflictCourseNames.sort(Comparator.naturalOrder()));
//        }

        if ((pageIndex) * pageSize >= res.size()) return new ArrayList<>();
        if ((pageIndex + 1) * pageSize > res.size()) return res.subList((pageIndex) * pageSize, res.size());
        return res.subList((pageIndex) * pageSize, (pageIndex + 1) * pageSize);
    }

    @Override
    public EnrollResult enrollCourse(int studentId, int sectionId) {
        commitAllInsertion("user");
        commitAllInsertion("student_course");
        final EnrollResult[] res = new EnrollResult[1];
        try {
            select("select enroll_course(?, ?)",
                    stmt -> {
                        stmt.setInt(1, studentId);
                        stmt.setInt(2, sectionId);
                    },
                    resultSet -> res[0] = EnrollResult.valueOf(resultSet.getString(1))
            );
        } catch (SQLException e) {
            e.printStackTrace();
            res[0] = EnrollResult.UNKNOWN_ERROR;
        }
        return res[0];
    }

    @Override
    public void dropCourse(int studentId, int sectionId) throws IllegalStateException {
        commitAllInsertion("user");
        commitAllInsertion("student_course");
        int[] res = new int[1];
        safeSelect("select drop_course(?, ?)",
                stmt -> {
                    stmt.setInt(1, studentId);
                    stmt.setInt(2, sectionId);
                }, resultSet -> res[0] = resultSet.getInt(1));
        if (res[0] == 0) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void addEnrolledCourseWithGrade(int studentId, int sectionId, @Nullable Grade grade) {
        commitAllInsertion("user");
        if (grade != null) {
            updateBatch("student_course_a", "insert into public.student_course(student_id,section_id,grade) values (?,?,?)",
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
            updateBatch("student_course_b", "insert into public.student_course(student_id,section_id) values (?,?)",
                    stmt -> {
                        stmt.setInt(1, studentId);
                        stmt.setInt(2, sectionId);
                    });
        }
    }

    @Override
    public void setEnrolledCourseGrade(int studentId, int sectionId, Grade grade) {
        commitAllInsertion("student_course");
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
        commitAllInsertion("student_course");
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
        courseTable.table = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) courseTable.table.put(day, new HashSet<>());
        safeSelect("select * from get_course_table(?, ?)",
                stmt -> {
                    stmt.setInt(1, studentId);
                    stmt.setDate(2, date);
                },
                resultSet -> {
                    CourseTable.CourseTableEntry entry = new CourseTable.CourseTableEntry();
                    entry.instructor = new Instructor();
                    entry.courseFullName = resultSet.getString(2);
                    entry.instructor.id = resultSet.getInt(3);
                    entry.instructor.fullName = resultSet.getString(4);
                    entry.classBegin = resultSet.getShort(5);
                    entry.classEnd = resultSet.getShort(6);
                    entry.location = resultSet.getString(7);
                    courseTable.table.get(DayOfWeek.valueOf(resultSet.getString(1))).add(entry);
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
        safeSelect("select major_id, major.name, department.id, department.name from major\n" +
                        "         join student on student.user_id = ? and student.major_id = major.id\n" +
                        "         join department on major.department_id = department.id;",
                stmt -> stmt.setInt(1, studentId),
                resultSet -> {
                    major.id = resultSet.getInt(1);
                    major.name = resultSet.getString(2);
                    major.department = new Department();
                    major.department.id = resultSet.getInt(3);
                    major.department.name = resultSet.getString(4);
                });
        return major;
    }
}