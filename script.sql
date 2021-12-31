create type majorcoursetype as enum ('Compulsory', 'Elective');

create type weekday as enum ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY');

create type gradingtype as enum ('PASS_OR_FAIL', 'HUNDRED_MARK_SCORE');

create table department
(
    id   serial
        constraint department_pk
            primary key,
    name varchar not null
);

create unique index department_id_uindex
    on department (id);

create unique index department_name_uindex
    on department (name);

create table major
(
    id            serial
        constraint major_pk
            primary key,
    name          varchar not null,
    department_id integer not null
        constraint major_department_id_fk
            references department
            on delete cascade
);

create unique index major_id_uindex
    on major (id);

create unique index major_name_uindex
    on major (name);

create table semester
(
    id    serial,
    name  varchar not null,
    begin date    not null,
    "end" date    not null
);

create unique index semester_id_uindex
    on semester (id);

create table "user"
(
    id        integer not null
        constraint user_pk
            primary key,
    full_name varchar not null
);

create table student
(
    user_id       integer not null
        constraint student_pk
            primary key
        constraint student_user_id_fk
            references "user"
            on delete cascade,
    major_id      integer not null
        constraint student_major_id_fk
            references major
            on delete cascade,
    enrolled_date date    not null
);

create table instructor
(
    user_id integer not null
        constraint instructor_pk
            primary key
        constraint instructor_user_id_fk
            references "user"
            on delete cascade
);

create table course
(
    id          varchar     not null
        constraint course_pk
            primary key,
    course_name varchar     not null,
    credit      integer     not null,
    hour        integer     not null,
    grading     gradingtype not null
);

create table major_course
(
    major_id  integer         not null
        constraint major_course_major_id_fk
            references major
            on delete cascade,
    course_id varchar         not null
        constraint major_course_course_id_fk
            references course
            on delete cascade,
    type      majorcoursetype not null,
    constraint major_course_pk
        primary key (major_id, course_id)
);

create unique index course_course_id_uindex
    on course (id);

create table section
(
    id             serial
        constraint section_pk
            primary key,
    section_name   varchar not null,
    course_id      varchar not null
        constraint section_course_course_id_fk
            references course
            on delete cascade,
    semester_id    integer not null
        constraint section_semester_id_fk
            references semester (id)
            on delete cascade,
    total_capacity integer not null,
    left_capacity  integer not null
);

create unique index section_id_uindex
    on section (id);

create index hash_idx_section_semester
    on section using hash (semester_id);

create table class
(
    id            serial
        constraint class_pk
            primary key,
    section_id    integer    not null
        constraint class_section_id_fk
            references section
            on delete cascade,
    instructor_id integer    not null
        constraint class_instructor_user_id_fk
            references instructor,
    day_of_week   weekday    not null,
    week_list     smallint[] not null,
    class_start   smallint   not null,
    class_end     smallint   not null,
    location      varchar    not null
);

create unique index class_id_uindex
    on class (id);

create index class_start_btree_idx
    on class (class_start);

create index class_end_btree_idx
    on class (class_end);

create index hash_idx_class_dow
    on class using hash (day_of_week);

create index btree_idx_class_instructor
    on class (instructor_id);

create table prerequisite_group
(
    id               serial
        constraint prerequisite_group_pk
            primary key,
    target_course_id varchar not null,
    count            integer not null
);

create table prerequisite_truth_table
(
    id        serial
        constraint prerequisite_truth_table_pk
            primary key,
    group_id  integer not null
        constraint prerequisite_truth_table_prerequisite_group_id_fk
            references prerequisite_group,
    course_id varchar not null
        constraint prerequisite_truth_table_course_id_fk
            references course
            on delete cascade
);

create unique index prerequisite_truth_table_id_uindex
    on prerequisite_truth_table (id);

create unique index prerequisite_group_id_uindex
    on prerequisite_group (id);

create table student_course
(
    student_id integer not null
        references student
            on delete cascade,
    section_id integer not null
        references section
            on delete cascade,
    grade      integer,
    primary key (student_id, section_id)
);

create table res
(
    exists boolean
);

create function is_prerequisite_satisfied(integer, character varying) returns boolean
    language plpgsql
as
$$
declare
    res boolean;
begin
    create temporary table if not exists c on commit drop as (
        select pg.id, pg.count, ptt.course_id
        from prerequisite_group pg
               inner join prerequisite_truth_table ptt on pg.id = ptt.group_id
        where pg.target_course_id = $2
    );
    if exists(select * from c)
    then
        select exists(
            select *
            from (
                select c.id, c.count, count(*) over (partition by c.id) as cnt
                from c
                inner join (
                    select distinct course_id
                    from section s
                    inner join student_course on student_course.section_id = s.id
                    where student_course.student_id = $1
                    and grade > 60
                ) as sc on sc.course_id = c.course_id
            ) cnt_table
            where count = cnt
        ) into res;
    else
        select true into res;
    end if;
    drop table c;
    return res;
end
$$;

create function user_student_delete_trigger() returns trigger
    language plpgsql
as
$$
BEGIN
        delete from "user" where "user".id not in (select user_id from student);
        return null;
    end;
$$;

create function user_instructor_delete_trigger() returns trigger
    language plpgsql
as
$$
BEGIN
        delete from "user" where "user".id not in (select user_id from instructor);
        return null;
    end;
$$;

create function user_delete_trigger() returns trigger
    language plpgsql
as
$$
BEGIN
        delete from "user" where "user".id = old.user_id;
        return null;
    end;
$$;

create trigger delete_user_by_student
    after delete
    on student
    for each row
execute procedure user_delete_trigger();

create trigger delete_user_by_instructor
    after delete
    on instructor
    for each row
execute procedure user_delete_trigger();

create function prerequisite_group_update_count_trigger() returns trigger
    language plpgsql
as
$$
begin
        update prerequisite_group set count = count - 1 where id = old.group_id;
        return old;
    end
$$;

create trigger delete_prerequisite_course
    after delete
    on prerequisite_truth_table
    for each row
execute procedure prerequisite_group_update_count_trigger();

create function insert_instructor(integer, character varying) returns void
    language plpgsql
as
$$
BEGIN
    insert into "user" (id, full_name) VALUES ($1, $2);
    insert into instructor (user_id) values ($1);
end;
$$;

create function insert_student(integer, character varying, integer, date) returns void
    language plpgsql
as
$$
BEGIN
    insert into "user" (id, full_name) VALUES ($1, $2);
    insert into student (user_id, major_id, enrolled_date) VALUES ($1, $3, $4);
end;
$$;

create function findweek(start date, target date) returns integer
    language plpgsql
as
$$
declare
    out int;
begin
    out = 0;
    while (start < target)
        loop
            start = start + integer'7';
            out = out + 1;
        end loop;
    return out;
end
$$;

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

create function isnullsection(sectionid integer) returns boolean
    language plpgsql
as
$$
declare sw bool;
    begin
        create temporary table if not exists b on commit drop as (
        select id from class where class.section_id = sectionId);
        if (exists(select * from b))
            then sw = true;
        else sw = false;
        end if;
        return sw;
    end
$$;

create function drop_course(_student_id integer, _section_id integer) returns integer
    language plpgsql
as
$$
declare
    flag int;
begin
    if not exists(select * from student_course where student_id = _student_id and section_id = _section_id) then
        return -1;
    end if;
    with deleted as (delete from student_course where student_id = _student_id and section_id = _section_id and grade is null returning *) select count(*) as cnt into flag from deleted;
    if flag != 0 then
        update section set left_capacity = left_capacity + 1 where _section_id = id and left_capacity != section.total_capacity;
        return 1;
    end if;
    return 0;
end
$$;

create function ignorepassed(stid integer)
    returns TABLE(courseid character varying)
    language plpgsql
as
$$
begin
      return query select c.id from student_course join section s on s.id = student_course.section_id
         join course c on s.course_id = c.id and student_course.student_id = stid and grade >= 60;
    end
$$;

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
                               where student_id = $1)
        select c.day_of_week                                  as day,
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
          and week_list @> array[cast(week_num as smallint)];
end;
$$;


