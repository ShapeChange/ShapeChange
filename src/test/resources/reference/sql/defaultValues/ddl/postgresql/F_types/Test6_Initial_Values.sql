CREATE TABLE t6_class1 (

   _id bigserial NOT NULL PRIMARY KEY,
   propbooleanfalse boolean DEFAULT FALSE,
   propbooleantrue boolean DEFAULT TRUE,
   propinteger integer DEFAULT 3,
   propnumber numeric DEFAULT 2.1,
   propstring text DEFAULT 'mydefault'
);

CREATE TABLE t6_class1_propstrings (

   t6_class1_id bigserial NOT NULL,
   propstrings character varying(50) DEFAULT 'abc' NOT NULL,
   PRIMARY KEY (t6_class1_id, propstrings)
);


ALTER TABLE t6_class1_propstrings ADD CONSTRAINT fk_t6_class1_propstrings_t6_class1_id FOREIGN KEY (t6_class1_id) REFERENCES t6_class1;
