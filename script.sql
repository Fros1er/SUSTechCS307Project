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

create unique index user_id_uindex
    on "user" (id);

create table student
(
    user_id       integer not null
        constraint student_pk
            primary key
        constraint student_user_id_fk
            references "user",
    major_id      integer not null,
    enrolled_date date    not null
);

create unique index student_user_id_uindex
    on student (user_id);

create table instructor
(
    user_id integer not null
        constraint instructor_pk
            primary key
        constraint instructor_user_id_fk
            references "user"
);

create unique index instructor_user_id_uindex
    on instructor (user_id);

create table course
(
    id          varchar     not null,
    course_name varchar     not null,
    credit      integer     not null,
    hour        integer     not null,
    grading     gradingtype not null
);

create table major_course
(
    major_id  integer         not null
        constraint major_course_major_id_fk
            references major,
    course_id varchar         not null
        constraint major_course_course_id_fk
            references course (id),
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
            references course (id),
    semester_id    integer not null
        constraint section_semester_id_fk
            references semester (id),
    total_capacity integer not null,
    left_capacity  integer not null
);

create unique index section_id_uindex
    on section (id);

create table class
(
    id            serial
        constraint class_pk
            primary key,
    section_id    integer    not null
        constraint class_section_id_fk
            references section,
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
            references course (id)
);

create unique index prerequisite_truth_table_id_uindex
    on prerequisite_truth_table (id);

create unique index prerequisite_group_id_uindex
    on prerequisite_group (id);

create table student_course
(
    student_id integer not null
        constraint student_course_student_id_fkey
            references student,
    section_id integer not null
        constraint student_course_section_id_fkey
            references section,
    grade      integer,
    constraint student_course_pkey
        primary key (student_id, section_id)
);

create function is_prerequisite_satisfied(integer, character varying) returns boolean
    language plpgsql
as
$$
begin
    create temporary table if not exists c on commit delete rows as (
        select pg.id, pg.count, ptt.course_id
        from prerequisite_group pg
                 inner join prerequisite_truth_table ptt on pg.id = ptt.group_id
        where pg.target_course_id = $2
    );
    if exists(select * from c)
    then
        return exists(
                select *
                from (
                         select c.id, c.count, count(*) over (partition by c.id) as cnt
                         from c
                                  inner join (
                             select *
                             from student_course
                                      inner join section s on student_course.section_id = s.id
                             where student_course.student_id = $1
                               and grade > 60
                         ) as sc on sc.course_id = c.course_id
                     ) cnt_table
                where count = cnt
            );
    else
        return true;
    end if;
end
$$;


