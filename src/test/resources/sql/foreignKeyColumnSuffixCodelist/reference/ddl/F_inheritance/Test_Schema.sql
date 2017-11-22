CREATE TABLE codelist (

   name text NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE datatype (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute integer NOT NULL
);

CREATE TABLE featuretype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   propcl text NOT NULL,
   feature2_id bigserial NOT NULL
);

CREATE TABLE featuretype1_propenum (

   featuretype1_id bigserial NOT NULL,
   propenum text NOT NULL,
   PRIMARY KEY (featuretype1_id, propenum)
);

CREATE TABLE featuretype2 (

   _id bigserial NOT NULL PRIMARY KEY,
   propdt_iddt bigserial NOT NULL
);


ALTER TABLE featuretype1 ADD CONSTRAINT fk_featuretype1_feature2_id FOREIGN KEY (feature2_id) REFERENCES featuretype2;
ALTER TABLE featuretype1 ADD CONSTRAINT fk_featuretype1_propcl FOREIGN KEY (propcl) REFERENCES codelist;
ALTER TABLE featuretype1_propenum ADD CONSTRAINT fk_featuretype1_propenum_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;
ALTER TABLE featuretype2 ADD CONSTRAINT fk_featuretype2_propdt_iddt FOREIGN KEY (propdt_iddt) REFERENCES datatype;

INSERT INTO codelist (name, documentation) VALUES ('code1', '');
INSERT INTO codelist (name, documentation) VALUES ('code2', '');
