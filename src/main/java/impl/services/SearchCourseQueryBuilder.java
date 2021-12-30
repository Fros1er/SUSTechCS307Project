package impl.services;

import cn.edu.sustech.cs307.dto.Instructor;
import cn.edu.sustech.cs307.service.StudentService;

import javax.annotation.Nullable;
import java.time.DayOfWeek;
import java.util.List;

import static impl.utils.Util.safeSelect;

public class SearchCourseQueryBuilder {

    public static String buildSearchCourseSQL(int studentId, int semesterId,
        @Nullable String searchCid, @Nullable String searchName, @Nullable String searchInstructor,
        @Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations,
        StudentService.CourseType searchCourseType, boolean ignoreFull, boolean ignoreMissingPrerequisites) {

        if (!UserServiceImpl.hasUser(studentId)) return "";
        List<Instructor> instructors = null;
        if (searchInstructor != null) {
            instructors = InstructorServiceImpl.searchInstructor(searchInstructor);
            if (instructors.isEmpty()) return "";
        }

        StringBuilder classSubQuery = buildClassSubQuery(searchDayOfWeek, searchClassTime, searchClassLocations, instructors);
        StringBuilder courseSubQuery = buildCourseSubQuery(searchCourseType, studentId);
        StringBuilder all = new StringBuilder("select * from ");
        all.append(courseSubQuery).append("inner join section on crs.course_id = section.course_id ");
        all.append("inner join class on class.section_id = section.id where section.semester_id = ").append(semesterId).append(' ');
        if (classSubQuery.length() != 0)
            all.append("and section.id in ").append(classSubQuery);
        if (searchCid != null)
            all.append("and crs.course_id like '%").append(searchCid).append("%' ");
        if (searchName != null)
            all.append("and crs.course_name || '[' || section.section_name || ']' like '%").append(searchName).append("%' ");
        if (ignoreFull)
            all.append("and section.left_capacity > 0 ");
        if (ignoreMissingPrerequisites)
            all.append(String.format("and is_prerequisite_satisfied(%d, crs.course_id) ", studentId));
        return all.toString();
    }

    public static StringBuilder buildClassSubQuery(@Nullable DayOfWeek searchDayOfWeek, @Nullable Short searchClassTime, @Nullable List<String> searchClassLocations, List<Instructor> instructors) {
        StringBuilder classSubQuery = new StringBuilder();
        if (searchDayOfWeek != null || searchClassTime != null || searchClassLocations != null) {
            classSubQuery.append("(select section_id from class where ");
            StringBuilder where = new StringBuilder();
            if (searchDayOfWeek != null) {
                if (where.length() != 0) where.append(" and ");
                where.append("class.day_of_week = '").append(searchDayOfWeek.name()).append('\'');
            }
            if (searchClassTime != null) {
                if (where.length() != 0) where.append(" and ");
                where.append(String.format("class.class_start <= %d and class.class_end >= %d", searchClassTime, searchClassTime));
            }
            if (searchClassLocations != null && !searchClassLocations.isEmpty()) {
                if (where.length() != 0) where.append(" and ");
                where.append('(');
                boolean flag = false;
                for (String s : searchClassLocations) {
                    if (flag) where.append(" or ");
                    where.append(String.format("class.location like '%%%s%%'", s));
                    flag = true;
                }
                where.append(')');
            }
            if (instructors != null) {
                if (where.length() != 0) where.append(" and ");
                where.append("class.instructor_id in (");
                boolean flag = false;
                for (Instructor v : instructors) {
                    if (flag) where.append(", ");
                    where.append(v.id);
                    flag = true;
                }
                where.append(')');
            }
            classSubQuery.append(where).append(" group by section_id) ");
        }
        return classSubQuery;
    }

    public static StringBuilder buildCourseSubQuery(StudentService.CourseType searchCourseType, int studentId) {
        StringBuilder courseSubQuery = new StringBuilder("(select id as course_id, course_name, credit, hour, grading from course");
        if (searchCourseType == StudentService.CourseType.ALL) {
            courseSubQuery.append(") as crs ");
            return courseSubQuery;
        }
        int[] majorIdTemp = new int[1];
        safeSelect("select * from student where user_id = ?", stmt -> stmt.setInt(1, studentId), resultSet -> majorIdTemp[0] = resultSet.getInt(2));
        int majorId = majorIdTemp[0];
        courseSubQuery.append(" left join major_course mc on course.id = mc.course_id where mc.major_id");
        switch (searchCourseType) {
            case MAJOR_COMPULSORY: {
                courseSubQuery.append(" = ").append(majorId).append(" and mc.type = 'Compulsory'");
                break;
            }
            case MAJOR_ELECTIVE: {
                courseSubQuery.append(" = ").append(majorId).append(" and mc.type = 'Elective'");
                break;
            }
            case CROSS_MAJOR: {
                courseSubQuery.append(" != ").append(majorId).append(" and mc.major_id is not null");
                break;
            }
            case PUBLIC: {
                courseSubQuery.append(" is null");
                break;
            }
        }
        courseSubQuery.append(") as crs ");
        return courseSubQuery;
    }
}
