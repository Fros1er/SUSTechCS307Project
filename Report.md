# CS307 Database Project 2 Report





#### enroll course:

To implement the enrollCourse() function, we declared a SQL function enroll_course with two parameters, student ID and section ID, as the process contains multiple searching quires. 

According to the requirement, the function should return **SUCCESS** as the enroll result or other 7 different types of enroll failure by a certain return priority. 

​	**COURSE_NOT_FOUND** failure can be determined by searching the corresponding course ID in table section.

​	**ALREADY_ENROLLED** failure can be determined by searching the section ID in student_course table.

​	**ALREADY_PASSED** is similar. By searching the section IDs related to the course ID in student_course table, where grade is over 60.

​	**PREREQUISITES_NOT_FULFILLED** failure can be easily solved by the previous declared function is_prerequisite_satisfied().

​	**COURSE_CONFLICT_FOUND** happens when there exists time conflicts or course conflicts, so the first judgement is whether the student has picked a course section having the same course ID with the target section, then the second is whether the classes of the section have time conflicts with other sections the student picked. Using `unnest` function can process the array week_list into separated numbers.

Code:

```sql
create function enroll_course(integer, integer) returns character varying
    language plpgsql
as
$$
declare
    target_course_id     varchar;
    target_left_capacity integer;

begin
    select course_id from section where id = $2 into target_course_id;
    select left_capacity from section where id = $2 into target_left_capacity;

    raise notice 'sid: %', $1;
    raise notice 'id: %', target_course_id;

    if (target_course_id is null)
    then
        return 'COURSE_NOT_FOUND';
    end if;

    if (exists(select from student_course where student_id = $1 and section_id = $2))
    then
        return 'ALREADY_ENROLLED';
    end if;

    if (exists(select
               from student_course
                        join section s2 on student_course.section_id = s2.id
               where course_id = target_course_id
                 and student_id = $1
                 and grade >= 60)) then
        return 'ALREADY_PASSED';
    end if;
    raise notice 'prereq: %', is_prerequisite_satisfied($1, target_course_id);

    if not (is_prerequisite_satisfied($1, target_course_id))
    then
        return 'PREREQUISITES_NOT_FULFILLED';
    end if;

    if (exists(select course_id, semester_id
               from section
                        join course c on c.id = section.course_id
               where section.id = $2
               intersect
               select course_id, semester_id
               from section
                        join course c on c.id = section.course_id
                        join student_course sc on section.id = sc.section_id
               where student_id = $1)) then
        return 'COURSE_CONFLICT_FOUND';
    end if;

    if (exists(with target_section as (select semester_id       as semester,
                                              day_of_week       as day,
                                              unnest(week_list) as week,
                                              class_start       as start_time,
                                              class_end         as end_time
                                       from class
                                                join section on class.section_id = section.id
                                       where section_id = $2),
                    enrolled_section as (select semester_id       as semester,
                                                day_of_week       as day,
                                                unnest(week_list) as week,
                                                class_start       as start_time,
                                                class_end         as end_time
                                         from (select section_id from student_course where student_id = $1) as t
                                                  join section
                                                       on t.section_id = section.id
                                                  join class
                                                       on t.section_id = class.section_id)
               select
               from enrolled_section,
                    target_section
               where enrolled_section.semester = target_section.semester
                 and enrolled_section.day = target_section.day
                 and enrolled_section.week = target_section.week
                 and ((target_section.start_time between enrolled_section.start_time and enrolled_section.end_time)
                   or (target_section.end_time between enrolled_section.start_time and enrolled_section.end_time))
        )) then
        return 'COURSE_CONFLICT_FOUND';
    end if;

    if (target_left_capacity = 0)
    then
        return 'COURSE_IS_FULL';
    end if;

    insert into student_course(student_id, section_id, grade) VALUES ($1, $2, null);
    update section set left_capacity = left_capacity - 1 where id = $2;
    return 'SUCCESS';
end;
$$;
```



#### get course table:

We designed a SQL function with parameter of student ID and the date to implement this function. 

The main problem is to find the target week number by the given date. Through taking the difference between the target date and the date when semester begin, the problem can be solved. Using `ceil` to get the target week.

Code:

```sql
create function get_course_table(integer, date)
    returns TABLE(day_of_week weekday, course_name text, instructor_id integer, instructor_name character varying, class_begin smallint, class_end smallint, location character varying)
    language plpgsql
as
$$
declare
    current_semester_id integer;
    diff                integer;
    week_num            integer;
BEGIN
    select id,
           $2 - begin
    into current_semester_id, diff
    from semester
    where begin <= $2
      and $2 <= semester.end;

    select ceil((diff + 1) / 7.0) into week_num;
    return query
        with temp_sections as (select section_id
                               from student_course
                               where student_id = $1),
             temp_week as (select unnest(week_list)                              as week,
                                  c.day_of_week                                  as day,
                                  c2.course_name || '[' || s.section_name || ']' as class_name,
                                  c.instructor_id                                as instructor_id,
                                  u.full_name                                    as instructor_full_name,
                                  c.class_start                                  as class_begin,
                                  c.class_end                                    as class_end,
                                  c.location                                     as location
                           from temp_sections
                                    join section s on section_id = s.id
                                    join class c on s.id = c.section_id
                                    join instructor i on i.user_id = c.instructor_id
                                    join "user" u on u.id = i.user_id
                                    join course c2 on s.course_id = c2.id
                           where semester_id = current_semester_id
             )
        select temp_week.day,
               temp_week.class_name,
               temp_week.instructor_id,
               temp_week.instructor_full_name,
               temp_week.class_begin,
               temp_week.class_end,
               temp_week.location
        from temp_week
        where week = week_num;
end;
$$;
```

