CREATE TABLE t6_class1 (

   _id bigserial NOT NULL PRIMARY KEY,
   propbooleanfalse boolean NOT NULL DEFAULT FALSE,
   propbooleantrue boolean NOT NULL DEFAULT TRUE,
   propinteger integer NOT NULL DEFAULT 3,
   propnumber numeric NOT NULL DEFAULT 2.1,
   propstring text NOT NULL DEFAULT 'mydefault'
);

CREATE TABLE t6_class1_propstrings (

   t6_class1_id bigserial NOT NULL,
   propstrings text NOT NULL DEFAULT 'abc',
   PRIMARY KEY (t6_class1_id, propstrings)
);


ALTER TABLE t6_class1_propstrings ADD CONSTRAINT fk_t6_class1_propstrings_t6_class1_id FOREIGN KEY (t6_class1_id) REFERENCES t6_class1;
