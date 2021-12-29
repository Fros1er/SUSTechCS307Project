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

import static impl.services.MajorServiceImpl.hasMajor;
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
    public List<CourseSearchEntry> searchCourse(int studentId, int semesterId, @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor, @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, CourseType searchCourseType, boolean ignoreFull, boolean ignoreConflict, boolean ignorePassed, boolean ignoreMissingPrerequisites, int pageSize, int pageIndex) {
        commitAllInsertion("student_course");
        commitAllInsertion("user");
        String sql7 = "select class.id,\n" +
                "       class.section_id,\n" +
                "       class.instructor_id,\n" +
                "       class.day_of_week,\n" +
                "       class.week_list,\n" +
                "       class.class_start,\n" +
                "       class.class_end,\n" +
                "       class.location,\n" +
                "       \"user\".full_name,\n" +
                "       class.instructor_id,\n" +
                "       section_name,\n" +
                "       Cid,\n" +
                "       total_capacity,\n" +
                "       left_capacity,\n" +
                "       course_name,\n" +
                "       credit,\n" +
                "       hour,\n" +
                "       grading from class\n" +
                "           join \"user\" on class.instructor_id = \"user\".id\n" +
                "           join (";
        StringBuilder sql1_1 = new StringBuilder("select *,t.id as sid,t.course_id as Cid");
        StringBuilder sql1_2 = new StringBuilder(" from (select * from section where exists(select * from class where section_id = section.id)) t\n" +
                "         join course on t.course_id = course.id and semester_id = ?");
        StringBuilder sql2 = new StringBuilder(" join student on student.user_id = ?");
        StringBuilder sql3 = new StringBuilder();
        String sql4 = "";
        String sql5 = "";
        StringBuilder sql6 = new StringBuilder(" order by t.course_id, course_name || '[' || section_name || ']') t2 on t2.sid = class.section_id ");
        String sort = " order by Cid, course_name || '[' || section_name || ']';";
        //先找出section
        if (searchCid != null) {//course
            sql1_2.append(" and course.id like '%").append(searchCid).append("%'");
        }
        if (searchName != null) {//course
            sql1_2.append(" and course_name || '[' || t.section_name || ']' like '%").append(searchName).append("%'");
        }
        if (searchInstructor == null && (searchDayOfWeek != null || searchClassTime != null || searchClassLocations != null)){
            sql2.append(" and exists(select * from class where section_id = t.id");
        }
        if (searchInstructor != null) {//class
            sql2.append(" and exists(select * from class join \"user\" on instructor_id = \"user\".id and section_id = t.id where replace(full_name,' ','') like '%").append(searchInstructor.replace(" ","")).append("%'");
        }
        if (searchDayOfWeek != null) {//class
            sql2.append(" and day_of_week = '").append(searchDayOfWeek.name()).append("'");
        }
        if (searchClassTime != null) {//class
            sql2.append(" and ").append(searchClassTime).append(" between class_start and class_end and section_id = t.id");
        }
        if (searchClassLocations != null && !searchClassLocations.isEmpty()) {
            sql2.append(" and location like any (array[");//class
            for (int i = 0; i < searchClassLocations.size(); i++) {
                if (i == searchClassLocations.size() - 1) {
                    sql2.append("'%").append(searchClassLocations.get(i)).append("%'])");
                } else {
                    sql2.append("'%").append(searchClassLocations.get(i)).append("%',");
                }
            }
        }
        if (searchDayOfWeek != null || searchClassTime != null || searchClassLocations != null || searchInstructor != null){
            sql2.append(")");
        }
        boolean useType = false;
        switch (searchCourseType) {//course
            case ALL: {
                break;
            }
            case MAJOR_COMPULSORY:
            case MAJOR_ELECTIVE: {
                sql3.append(" join major_course on student.major_id = major_course.major_id and course.id = major_course.course_id and type = CAST(? AS majorcoursetype)");
                useType = true;
                break;
            }
            case CROSS_MAJOR: {
                sql3.append(" join major_course on student.major_id != major_course.major_id and course.id = major_course.course_id");
                break;
            }
            case PUBLIC: {
                sql3.append(" and exists(select * from major_course where t.course_id = major_course.course_id) = false");
                break;
            }
        }
        if (ignoreFull) {
            sql1_2.append(" and left_capacity > 0");
        }
        if (ignoreMissingPrerequisites) {
            sql2.append(" join (select course.id as coid from course left join prerequisite_group pg on pg.target_course_id = course.id where pg.id is null or is_prerequisite_satisfied(").append(studentId).append(", course.id) = true) t3 on t3.coid = t.course_id");
        }
        ArrayList<String> courseId = new ArrayList<>();
        if (ignorePassed) {//course
            StringBuilder queryConflict = new StringBuilder("select * from ignorePassed(?)");
            safeSelect(queryConflict.toString(),
                    stmt -> stmt.setInt(1,studentId),
                    resultSet -> {
                        courseId.add(resultSet.getString(1));
                    });
        }
        LinkedHashMap<Integer, node> enrolledCourse = new LinkedHashMap<>();
        int[] count = {0};
        safeSelect("select day_of_week,section_name,course_name,class_start,class_end,week_list,course.id from student_course\n" +
                        "    join class on class.section_id = student_course.section_id and student_id = ?\n" +
                        "    join section on class.section_id = section.id and semester_id = ?\n" +
                        "    join course on section.course_id = course.id order by course_name,section_name;",
                stmt -> {
                    stmt.setInt(1, studentId);
                    stmt.setInt(2,semesterId);
//                    System.out.println(stmt);
                    },
                resultSet -> {
                    count[0]++;
                    CourseSectionClass sectionClass = new CourseSectionClass();
                    sectionClass.dayOfWeek = DayOfWeek.valueOf(resultSet.getString(1));
                    sectionClass.weekList = new HashSet<>(Arrays.asList((Short[]) resultSet.getArray(6).getArray()));
                    sectionClass.classBegin = resultSet.getShort(4);
                    sectionClass.classEnd = resultSet.getShort(5);
                    node node = new node();
                    node.index = count[0];
                    node.courseId = resultSet.getString(7);
                    node.name = String.format("%s[%s]",resultSet.getString(3),resultSet.getString(2));
                    node.sectionClass = sectionClass;
                    enrolledCourse.put(count[0],node);
                });
        StringBuilder sql0 = new StringBuilder();
        sql0.append(sql7).append(sql1_1).append(sql1_2).append(sql2).append(sql3).append(sql4).append(sql5).append(sql6).append(sort);
        LinkedHashMap<Integer,CourseSearchEntry> buffer = new LinkedHashMap<>();
        HashMap<Integer,ArrayList<node>> nodes = new HashMap<>();
        boolean finalUseType = useType;
        safeSelect(sql0.toString(),
                    stmt->{
                        stmt.setInt(1, semesterId);
                        stmt.setInt(2, studentId);
                        if (finalUseType) {
                            if (searchCourseType == CourseType.MAJOR_COMPULSORY) {
                                stmt.setString(3, "Compulsory");
                            } else {
                                stmt.setString(3, "Elective");
                            }
//                            stmt.setInt(4, pageSize);
//                            stmt.setInt(5, pageSize * pageIndex);
                        }
//                            stmt.setInt(3, pageSize);
//                            stmt.setInt(4, pageSize * pageIndex);
//                        System.out.println(stmt);
                    },
                    resultSet -> {
                        CourseSearchEntry entry = new CourseSearchEntry();
                        entry.sectionClasses = new HashSet<>();
                        entry.conflictCourseNames = new ArrayList<>();
                        Course course = new Course();
                        CourseSection section = new CourseSection();
                        section.id = resultSet.getInt(2);
                        section.totalCapacity = resultSet.getInt(13);
                        section.leftCapacity = resultSet.getInt(14);
                        section.name = resultSet.getString(11);
                        course.id = resultSet.getString(12);
                        course.grading = Course.CourseGrading.valueOf(resultSet.getString(18));
                        course.credit = resultSet.getInt(16);
                        course.name = resultSet.getString(15);
                        course.classHour = resultSet.getInt(17);
                        entry.section = section;
                        entry.course = course;
                        //------------------------------------------------------------class
                        CourseSectionClass sectionClass = new CourseSectionClass();
                        Instructor instructor = new Instructor();
                        instructor.id = resultSet.getInt(3);
                        instructor.fullName = resultSet.getString(9);
                        sectionClass.instructor = instructor;
                        sectionClass.id = resultSet.getInt(1);
                        sectionClass.classBegin = resultSet.getShort(6);
                        sectionClass.classEnd = resultSet.getShort(7);
                        sectionClass.location = resultSet.getString(8);
                        sectionClass.dayOfWeek = DayOfWeek.valueOf(resultSet.getString(4));
                        sectionClass.weekList = new HashSet<>(Arrays.asList((Short[]) resultSet.getArray(5).getArray()));
                        boolean sw = true;
                        if (ignorePassed){
                            for (String courseid: courseId){
                                if (courseid.equals(course.id)){
                                    sw = false;
                                    break;
                                }
                            }
                        }
                        if (!buffer.containsKey(section.id) && sw){
                            buffer.put(section.id, entry);
                        }
                        if (ignoreConflict) {
                            for (int i = 1; i <= enrolledCourse.size(); i++){
                                CourseSectionClass tem = enrolledCourse.get(i).sectionClass;
                                boolean conflict = false;
                                for (int num = tem.classBegin; num <= tem.classEnd; num++){
                                    if (num >= sectionClass.classBegin && num <= sectionClass.classEnd){
                                        conflict = true;
                                        break;
                                    }
                                }
                                if (sectionClass.weekList.stream().anyMatch(v->tem.weekList.contains(v)) && sectionClass.dayOfWeek.equals(tem.dayOfWeek) &&conflict){
                                    sw = false;
                                }
                            }
                        }
                        if (sw){
                            buffer.get(section.id).sectionClasses.add(sectionClass);
                            if (!ignoreConflict){
                                for (int i = 1; i <= enrolledCourse.size(); i++){
                                    CourseSectionClass tem = enrolledCourse.get(i).sectionClass;
                                    if (!nodes.containsKey(section.id)){
                                        nodes.put(section.id,new ArrayList<>());
                                    }
                                    boolean conflict = false;
                                    for (int num = tem.classBegin; num <= tem.classEnd; num++){
                                        if (num >= sectionClass.classBegin && num <= sectionClass.classEnd){
                                            conflict = true;
                                            break;
                                        }
                                    }
                                    if (sectionClass.weekList.stream().anyMatch(v->tem.weekList.contains(v)) && sectionClass.dayOfWeek.equals(tem.dayOfWeek) && conflict){
                                        nodes.get(section.id).add(enrolledCourse.get(i));
                                    }
                                    if (course.id.equals(enrolledCourse.get(i).courseId)){
                                        nodes.get(section.id).add(enrolledCourse.get(i));
                                    }
                                }
                            }
                        }
                    }
                );
        int begin = pageSize * pageIndex; // offset
        int end = begin + pageSize; // limit
        int index = 0;
        List<CourseSearchEntry> list = new ArrayList<>();
        for (CourseSearchEntry entry: buffer.values()){
            if (entry.sectionClasses.isEmpty()){
                buffer.remove(entry.section.id);
            } else {
                index++;
                if (index > end) {
                    break;
                }
                if (index > begin){
                    list.add(entry);
                }
            }
        }
        for (CourseSearchEntry entry: list){
            ArrayList<node> tem = nodes.get(entry.section.id);
            if (tem != null) {
                tem.sort(Comparator.comparingInt(o -> o.index));
                for (node node : tem) {
                    if (!entry.conflictCourseNames.contains(node.name)) {
                        entry.conflictCourseNames.add(node.name);
                    }
                }
            }
        }
        return list;
    }

    static class node{
        int index;
        String courseId;
        CourseSectionClass sectionClass;
        String name;
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
        delete("truncate table student_course", resultSet -> {});
//        if (delete("delete from student_course where student_id = ? and section_id = ? and grade is null",
//                stmt -> {
//                    stmt.setInt(1, studentId);
//                    stmt.setInt(2, sectionId);
//                }) == 0) {
//            throw new IllegalStateException();
//        }
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