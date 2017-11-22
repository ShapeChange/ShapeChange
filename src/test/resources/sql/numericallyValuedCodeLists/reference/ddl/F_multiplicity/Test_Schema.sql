CREATE TABLE featuretype (

   _id bigserial NOT NULL PRIMARY KEY,
   att2 numeric(10,2) NOT NULL,
   att3 integer NOT NULL,
   att4 text NOT NULL,
   att5 numeric NOT NULL,
   att6 numeric(5,2) NOT NULL,
   att7 text NOT NULL
);

CREATE TABLE featuretype_att1 (

   featuretype_id bigserial NOT NULL,
   numericcodelist_id numeric NOT NULL,
   PRIMARY KEY (featuretype_id, numericcodelist_id)
);

CREATE TABLE integercodelist (

   name integer NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE numericcodelist (

   name numeric NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE numericcodelistwithinvalidprecisionandscale (

   name numeric NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE numericcodelistwithvalidprecisionandscale (

   name numeric(10,2) NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE textcodelist (

   name text NOT NULL PRIMARY KEY,
   documentation text
);


ALTER TABLE featuretype ADD CONSTRAINT featuretype_att6_chk CHECK (att6 IN (100.1, 100.2));
ALTER TABLE featuretype ADD CONSTRAINT featuretype_att7_chk CHECK (att7 IN ('X', 'Y'));
ALTER TABLE featuretype ADD CONSTRAINT fk_featuretype_att2 FOREIGN KEY (att2) REFERENCES numericcodelistwithvalidprecisionandscale;
ALTER TABLE featuretype ADD CONSTRAINT fk_featuretype_att3 FOREIGN KEY (att3) REFERENCES integercodelist;
ALTER TABLE featuretype ADD CONSTRAINT fk_featuretype_att4 FOREIGN KEY (att4) REFERENCES textcodelist;
ALTER TABLE featuretype ADD CONSTRAINT fk_featuretype_att5 FOREIGN KEY (att5) REFERENCES numericcodelistwithinvalidprecisionandscale;
ALTER TABLE featuretype_att1 ADD CONSTRAINT fk_featuretype_att1_featuretype_id FOREIGN KEY (featuretype_id) REFERENCES featuretype;
ALTER TABLE featuretype_att1 ADD CONSTRAINT fk_featuretype_att1_numericcodelist_id FOREIGN KEY (numericcodelist_id) REFERENCES numericcodelist;

INSERT INTO integercodelist (name, documentation) VALUES (10, '');
INSERT INTO integercodelist (name, documentation) VALUES (11, '');
INSERT INTO numericcodelist (name, documentation) VALUES (1.1, '');
INSERT INTO numericcodelist (name, documentation) VALUES (1.2, '');
INSERT INTO numericcodelistwithinvalidprecisionandscale (name, documentation) VALUES (3.1, '');
INSERT INTO numericcodelistwithinvalidprecisionandscale (name, documentation) VALUES (3.2, '');
INSERT INTO numericcodelistwithvalidprecisionandscale (name, documentation) VALUES (2.1, '');
INSERT INTO numericcodelistwithvalidprecisionandscale (name, documentation) VALUES (2.2, '');
INSERT INTO textcodelist (name, documentation) VALUES ('abc', '');
INSERT INTO textcodelist (name, documentation) VALUES ('def', '');
