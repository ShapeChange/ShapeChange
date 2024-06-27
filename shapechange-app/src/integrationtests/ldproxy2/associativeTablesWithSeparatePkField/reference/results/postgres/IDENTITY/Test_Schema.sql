CREATE TABLE codelist1 (

   code text NOT NULL PRIMARY KEY,
   name text,
   documentation text,
   description text
);

CREATE TABLE featuretype (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE featuretype_pcodelist1 (

   _id bigserial NOT NULL PRIMARY KEY,
   featuretype_id bigint NOT NULL,
   codelist1_id text NOT NULL
);

CREATE TABLE featuretype_pcodelist2 (

   _id bigserial NOT NULL PRIMARY KEY,
   featuretype_id bigint NOT NULL,
   pcodelist2 text NOT NULL
);

CREATE TABLE featuretype_pstring (

   _id bigserial NOT NULL PRIMARY KEY,
   featuretype_id bigint NOT NULL,
   pstring text NOT NULL
);


ALTER TABLE featuretype_pcodelist1 ADD CONSTRAINT fk_featuretype_pcodelist1_codelist1_id FOREIGN KEY (codelist1_id) REFERENCES codelist1;
ALTER TABLE featuretype_pcodelist1 ADD CONSTRAINT fk_featuretype_pcodelist1_featuretype_id FOREIGN KEY (featuretype_id) REFERENCES featuretype;
ALTER TABLE featuretype_pcodelist2 ADD CONSTRAINT fk_featuretype_pcodelist2_featuretype_id FOREIGN KEY (featuretype_id) REFERENCES featuretype;
ALTER TABLE featuretype_pstring ADD CONSTRAINT fk_featuretype_pstring_featuretype_id FOREIGN KEY (featuretype_id) REFERENCES featuretype;

INSERT INTO codelist1 (code, name, documentation, description) VALUES ('1000', 'codeA', '', NULL);
INSERT INTO codelist1 (code, name, documentation, description) VALUES ('2000', 'codeB', '', NULL);
