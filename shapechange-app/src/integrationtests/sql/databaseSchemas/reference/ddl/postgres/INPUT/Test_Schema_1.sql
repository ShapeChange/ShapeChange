DROP SCHEMA IF EXISTS s1, s2 CASCADE;

CREATE SCHEMA s1;
CREATE SCHEMA s2;

ALTER ROLE xyz SET search_path TO s1,s2,public;

CREATE TABLE s1.featuretypes1 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE s1.featuretypes1_rolefts1tots1 (

   featuretypes1_id bigint NOT NULL,
   types1_id bigint NOT NULL,
   PRIMARY KEY (featuretypes1_id, types1_id)
);

CREATE TABLE s1.types1 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE s2.datatypes2 (

   _id bigserial NOT NULL PRIMARY KEY,
   att integer NOT NULL
);

CREATE TABLE s2.featuretypes2 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE s2.featuretypes2_attdts2 (

   featuretypes2_id bigint NOT NULL,
   datatypes2_id bigint NOT NULL,
   PRIMARY KEY (featuretypes2_id, datatypes2_id)
);

CREATE TABLE s2.tablefts1fts2 (

   featuretypes1_id bigint NOT NULL,
   featuretypes2_id bigint NOT NULL,
   PRIMARY KEY (featuretypes1_id, featuretypes2_id)
);


ALTER TABLE s1.featuretypes1_rolefts1tots1 ADD CONSTRAINT fk_s1_featuretypes1_rolefts1tots1_featuretypes1_id FOREIGN KEY (featuretypes1_id) REFERENCES s1.featuretypes1;
ALTER TABLE s1.featuretypes1_rolefts1tots1 ADD CONSTRAINT fk_s1_featuretypes1_rolefts1tots1_types1_id FOREIGN KEY (types1_id) REFERENCES s1.types1;
ALTER TABLE s2.featuretypes2_attdts2 ADD CONSTRAINT fk_s2_featuretypes2_attdts2_datatypes2_id FOREIGN KEY (datatypes2_id) REFERENCES s2.datatypes2;
ALTER TABLE s2.featuretypes2_attdts2 ADD CONSTRAINT fk_s2_featuretypes2_attdts2_featuretypes2_id FOREIGN KEY (featuretypes2_id) REFERENCES s2.featuretypes2;
ALTER TABLE s2.tablefts1fts2 ADD CONSTRAINT fk_s2_tablefts1fts2_featuretypes1_id FOREIGN KEY (featuretypes1_id) REFERENCES s1.featuretypes1;
ALTER TABLE s2.tablefts1fts2 ADD CONSTRAINT fk_s2_tablefts1fts2_featuretypes2_id FOREIGN KEY (featuretypes2_id) REFERENCES s2.featuretypes2;
