CREATE TABLE codelist (

   name character varying(50) NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE datatype (

   _id bigserial NOT NULL PRIMARY KEY,
   dtprop text NOT NULL,
   dtprop2 character varying(50) NOT NULL,
   dtprop3 text NOT NULL
);

CREATE TABLE featuretype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   p1 character varying(30) NOT NULL,
   p4 integer NOT NULL,
   p5 text NOT NULL,
   p6 text NOT NULL,
   p7 text NOT NULL
);

CREATE TABLE featuretype1_p2 (

   featuretype1_id bigserial NOT NULL,
   p2 text NOT NULL,
   PRIMARY KEY (featuretype1_id, p2)
);

CREATE TABLE featuretype1_p3 (

   featuretype1_id bigserial NOT NULL,
   datatype_id bigserial NOT NULL,
   PRIMARY KEY (featuretype1_id, datatype_id)
);


ALTER TABLE featuretype1_p2 ADD CONSTRAINT fk_featuretype1_p2_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;
ALTER TABLE featuretype1_p3 ADD CONSTRAINT fk_featuretype1_p3_datatype_id FOREIGN KEY (datatype_id) REFERENCES datatype;
ALTER TABLE featuretype1_p3 ADD CONSTRAINT fk_featuretype1_p3_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;

INSERT INTO codelist (name, documentation) VALUES ('code1', '');
INSERT INTO codelist (name, documentation) VALUES ('code2', '');
