CREATE TABLE codelist (

   name text NOT NULL PRIMARY KEY,
   documentation text
);

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

   _id bigserial NOT NULL PRIMARY KEY,
   attribute10 text
);

CREATE TABLE featuretype3_attribute9 (

   featuretype3_id bigserial NOT NULL,
   attribute9 text NOT NULL,
   PRIMARY KEY (featuretype3_id, attribute9)
);

CREATE TABLE featuretype4 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE mydatatype (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute4 text NOT NULL,
   attribute5 integer NOT NULL,
   attribute15 text NOT NULL,
   attribute17 text NOT NULL,
   datatypeowner_id bigserial NOT NULL
);

CREATE TABLE mydatatype_attribute14 (

   mydatatype_id bigserial NOT NULL,
   attribute14 text NOT NULL,
   PRIMARY KEY (mydatatype_id, attribute14)
);

CREATE TABLE mydatatype_attribute16 (

   mydatatype_id bigserial NOT NULL,
   codelist_id text NOT NULL,
   PRIMARY KEY (mydatatype_id, codelist_id)
);

CREATE TABLE myotherdatatype (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute9 integer NOT NULL,
   rolemodttoft4 bigserial NOT NULL,
   owner_id bigserial NOT NULL
);


ALTER TABLE featuretype1_attribute2 ADD CONSTRAINT fk_featuretype1_attribute2_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;
ALTER TABLE featuretype3_attribute9 ADD CONSTRAINT fk_featuretype3_attribute9_featuretype3_id FOREIGN KEY (featuretype3_id) REFERENCES featuretype3;
ALTER TABLE mydatatype ADD CONSTRAINT fk_mydatatype_attribute17 FOREIGN KEY (attribute17) REFERENCES codelist;
ALTER TABLE mydatatype ADD CONSTRAINT mydatatype_attribute15_chk CHECK (attribute15 IN ('enum1', 'enum2'));
ALTER TABLE mydatatype_attribute14 ADD CONSTRAINT fk_mydatatype_attribute14_mydatatype_id FOREIGN KEY (mydatatype_id) REFERENCES mydatatype;
ALTER TABLE mydatatype_attribute14 ADD CONSTRAINT mydatatype_attribute14_attribute14_chk CHECK (attribute14 IN ('enum1', 'enum2'));
ALTER TABLE mydatatype_attribute16 ADD CONSTRAINT fk_mydatatype_attribute16_codelist_id FOREIGN KEY (codelist_id) REFERENCES codelist;
ALTER TABLE mydatatype_attribute16 ADD CONSTRAINT fk_mydatatype_attribute16_mydatatype_id FOREIGN KEY (mydatatype_id) REFERENCES mydatatype;
ALTER TABLE myotherdatatype ADD CONSTRAINT fk_myotherdatatype_rolemodttoft4 FOREIGN KEY (rolemodttoft4) REFERENCES featuretype4;

INSERT INTO codelist (name, documentation) VALUES ('code1', '');
INSERT INTO codelist (name, documentation) VALUES ('code2', '');
