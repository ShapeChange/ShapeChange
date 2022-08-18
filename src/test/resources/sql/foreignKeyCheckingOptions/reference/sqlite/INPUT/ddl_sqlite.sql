PRAGMA foreign_keys = ON;

CREATE TABLE codelist1 (

   name TEXT NOT NULL PRIMARY KEY,
   documentation TEXT
);

CREATE TABLE featuretype1 (

   _id INTEGER NOT NULL PRIMARY KEY,
   att INTEGER NOT NULL,
   cl_fk TEXT NOT NULL,
   CONSTRAINT fk_featuretype1_cl_fk FOREIGN KEY (cl_fk) REFERENCES codelist1 DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE featuretype1_dt (

   _id INTEGER NOT NULL PRIMARY KEY,
   att1 INTEGER NOT NULL,
   att2 TEXT NOT NULL,
   featuretype1_id INTEGER NOT NULL,
   CONSTRAINT fk_featuretype1_dt_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1 DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE featuretype1_roleft1toft2 (

   featuretype1_id INTEGER NOT NULL,
   featuretype2_id INTEGER NOT NULL,
   PRIMARY KEY (featuretype1_id, featuretype2_id),
   CONSTRAINT fk_featuretype1_roleft1toft2_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1 DEFERRABLE INITIALLY DEFERRED,
   CONSTRAINT fk_featuretype1_roleft1toft2_featuretype2_id FOREIGN KEY (featuretype2_id) REFERENCES featuretype2 DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE featuretype2 (

   _id INTEGER NOT NULL PRIMARY KEY,
   roleft2toft1b_fk INTEGER NOT NULL,
   CONSTRAINT fk_featuretype2_roleft2toft1b_fk FOREIGN KEY (roleft2toft1b_fk) REFERENCES featuretype1 DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE featuretype2_att (

   featuretype2_id INTEGER NOT NULL,
   att INTEGER NOT NULL,
   PRIMARY KEY (featuretype2_id, att),
   CONSTRAINT fk_featuretype2_att_featuretype2_id FOREIGN KEY (featuretype2_id) REFERENCES featuretype2 DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE featuretype2_cl (

   featuretype2_id INTEGER NOT NULL,
   codelist1_id TEXT NOT NULL,
   PRIMARY KEY (featuretype2_id, codelist1_id),
   CONSTRAINT fk_featuretype2_cl_codelist1_id FOREIGN KEY (codelist1_id) REFERENCES codelist1 DEFERRABLE INITIALLY DEFERRED,
   CONSTRAINT fk_featuretype2_cl_featuretype2_id FOREIGN KEY (featuretype2_id) REFERENCES featuretype2 DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE featuretype2_dt (

   _id INTEGER NOT NULL PRIMARY KEY,
   att1 INTEGER NOT NULL,
   att2 TEXT NOT NULL,
   featuretype2_id INTEGER NOT NULL,
   CONSTRAINT fk_featuretype2_dt_featuretype2_id FOREIGN KEY (featuretype2_id) REFERENCES featuretype2 DEFERRABLE INITIALLY DEFERRED
);


INSERT INTO codelist1 (name, documentation) VALUES ('codeA', '');
INSERT INTO codelist1 (name, documentation) VALUES ('codeB', '');
