CREATE TABLE featuretype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute3 text NOT NULL
);

CREATE TABLE featuretype1_attribute1 (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute4 text NOT NULL,
   attribute5 integer NOT NULL,
   featuretype1_id bigserial NOT NULL
);

CREATE TABLE featuretype1_attribute2 (

   featuretype1_id bigserial NOT NULL,
   attribute2 text NOT NULL,
   PRIMARY KEY (featuretype1_id, attribute2)
);

CREATE TABLE featuretype2 (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute6 bigserial NOT NULL
);

CREATE TABLE featuretype3 (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute10 text
);

CREATE TABLE featuretype3_attribute7 (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute4 text NOT NULL,
   attribute5 integer NOT NULL,
   featuretype3_id bigserial NOT NULL
);

CREATE TABLE featuretype3_attribute8 (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute9 integer NOT NULL,
   featuretype3_id bigserial NOT NULL
);

CREATE TABLE featuretype3_attribute9 (

   featuretype3_id bigserial NOT NULL,
   attribute9 text NOT NULL,
   PRIMARY KEY (featuretype3_id, attribute9)
);

CREATE TABLE mydatatype (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute4 text NOT NULL,
   attribute5 integer NOT NULL
);


ALTER TABLE featuretype1_attribute1 ADD CONSTRAINT fk_featuretype1_attribute1_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;
ALTER TABLE featuretype1_attribute2 ADD CONSTRAINT fk_featuretype1_attribute2_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;
ALTER TABLE featuretype2 ADD CONSTRAINT fk_featuretype2_attribute6 FOREIGN KEY (attribute6) REFERENCES mydatatype;
ALTER TABLE featuretype3_attribute7 ADD CONSTRAINT fk_featuretype3_attribute7_featuretype3_id FOREIGN KEY (featuretype3_id) REFERENCES featuretype3;
ALTER TABLE featuretype3_attribute8 ADD CONSTRAINT fk_featuretype3_attribute8_featuretype3_id FOREIGN KEY (featuretype3_id) REFERENCES featuretype3;
ALTER TABLE featuretype3_attribute9 ADD CONSTRAINT fk_featuretype3_attribute9_featuretype3_id FOREIGN KEY (featuretype3_id) REFERENCES featuretype3;
