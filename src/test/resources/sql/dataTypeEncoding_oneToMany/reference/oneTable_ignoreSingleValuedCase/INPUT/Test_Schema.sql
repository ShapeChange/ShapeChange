CREATE TABLE featuretype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute3 text NOT NULL
);

CREATE TABLE featuretype1_attribute2 (

   featuretype1_id bigserial NOT NULL,
   attribute2 text NOT NULL,
   PRIMARY KEY (featuretype1_id, attribute2)
);

CREATE TABLE featuretype2 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE featuretype3 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE mydatatype (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute4 text NOT NULL,
   attribute5 integer NOT NULL,
   datatypeowner_id bigserial NOT NULL
);

CREATE TABLE myotherdatatype (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute9 integer NOT NULL,
   owner_id bigserial NOT NULL
);


ALTER TABLE featuretype1_attribute2 ADD CONSTRAINT fk_featuretype1_attribute2_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;
