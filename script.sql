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
    id          varchar  not null,
    course_name varchar  not null,
    credit      integer  not null,
    hour        integer  not null,
    grading     smallint not null
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
    total_capacity integer not null
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
    id               serial,
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
            references prerequisite_group (id),
    course_id varchar not null
        constraint prerequisite_truth_table_course_id_fk
            references course (id)
);

create unique index prerequisite_truth_table_id_uindex
    on prerequisite_truth_table (id);

create unique index prerequisite_group_id_uindex
    on prerequisite_group (id);

