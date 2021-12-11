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

create table major_course
(
    major_id integer not null
        constraint major_course_pk
            primary key
        constraint major_course_major_id_fk
            references major
);

create table semester
(
    id    serial,
    name  integer not null,
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
    credit      integer not null,
    course_name varchar not null,
    course_id   varchar not null,
    hour        integer not null,
    grading     integer not null
);

create unique index course_course_id_uindex
    on course (course_id);

create table section
(
    id             serial
        constraint section_pk
            primary key,
    section_name   varchar not null,
    course_id      varchar not null
        constraint section_course_course_id_fk
            references course (course_id),
    semester_id    integer not null
        constraint section_semester_id_fk
            references semester (id),
    total_capacity integer not null
);

create unique index section_id_uindex
    on section (id);


