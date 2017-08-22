CREATE TABLE datatype (

   _id bigserial NOT NULL PRIMARY KEY,
   dtatt1 text NOT NULL,
   dtatt2 boolean NOT NULL,
   dtatt3 numeric(3,2) NOT NULL
);

CREATE TABLE featuretype (

   _id bigserial NOT NULL PRIMARY KEY,
   ftatt2 numeric(3,2) NOT NULL,
   ftatt3 numeric NOT NULL
);

CREATE TABLE featuretype_ftatt1 (

   featuretype_id bigserial NOT NULL,
   datatype_id bigserial NOT NULL,
   PRIMARY KEY (featuretype_id, datatype_id)
);

CREATE TABLE objecttype (

   _id bigserial NOT NULL PRIMARY KEY,
   otatt1_option1 numeric(5),
   otatt1_option2 integer
);


ALTER TABLE featuretype_ftatt1 ADD CONSTRAINT fk_featuretype_ftatt1_datatype_id FOREIGN KEY (datatype_id) REFERENCES datatype;
ALTER TABLE featuretype_ftatt1 ADD CONSTRAINT fk_featuretype_ftatt1_featuretype_id FOREIGN KEY (featuretype_id) REFERENCES featuretype;
