CREATE TABLE codelist1 (

   name text NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE featuretype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   att integer NOT NULL,
   cl_fk text NOT NULL
);

CREATE TABLE featuretype1_dt (

   _id bigserial NOT NULL PRIMARY KEY,
   att1 integer NOT NULL,
   att2 text NOT NULL,
   featuretype1_id bigint NOT NULL
);

CREATE TABLE featuretype1_roleft1toft2 (

   featuretype1_id bigint NOT NULL,
   featuretype2_id bigint NOT NULL,
   PRIMARY KEY (featuretype1_id, featuretype2_id)
);

CREATE TABLE featuretype2 (

   _id bigserial NOT NULL PRIMARY KEY,
   roleft2toft1b_fk bigint NOT NULL
);

CREATE TABLE featuretype2_att (

   featuretype2_id bigint NOT NULL,
   att integer NOT NULL,
   PRIMARY KEY (featuretype2_id, att)
);

CREATE TABLE featuretype2_cl (

   featuretype2_id bigint NOT NULL,
   codelist1_id text NOT NULL,
   PRIMARY KEY (featuretype2_id, codelist1_id)
);

CREATE TABLE featuretype2_dt (

   _id bigserial NOT NULL PRIMARY KEY,
   att1 integer NOT NULL,
   att2 text NOT NULL,
   featuretype2_id bigint NOT NULL
);


ALTER TABLE featuretype1 ADD CONSTRAINT fk_featuretype1_cl_fk FOREIGN KEY (cl_fk) REFERENCES codelist1 NOT DEFERRABLE;
ALTER TABLE featuretype1_dt ADD CONSTRAINT fk_featuretype1_dt_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1 NOT DEFERRABLE;
ALTER TABLE featuretype1_roleft1toft2 ADD CONSTRAINT fk_featuretype1_roleft1toft2_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1 NOT DEFERRABLE;
ALTER TABLE featuretype1_roleft1toft2 ADD CONSTRAINT fk_featuretype1_roleft1toft2_featuretype2_id FOREIGN KEY (featuretype2_id) REFERENCES featuretype2 NOT DEFERRABLE;
ALTER TABLE featuretype2 ADD CONSTRAINT fk_featuretype2_roleft2toft1b_fk FOREIGN KEY (roleft2toft1b_fk) REFERENCES featuretype1 NOT DEFERRABLE;
ALTER TABLE featuretype2_att ADD CONSTRAINT fk_featuretype2_att_featuretype2_id FOREIGN KEY (featuretype2_id) REFERENCES featuretype2 NOT DEFERRABLE;
ALTER TABLE featuretype2_cl ADD CONSTRAINT fk_featuretype2_cl_codelist1_id FOREIGN KEY (codelist1_id) REFERENCES codelist1 NOT DEFERRABLE;
ALTER TABLE featuretype2_cl ADD CONSTRAINT fk_featuretype2_cl_featuretype2_id FOREIGN KEY (featuretype2_id) REFERENCES featuretype2 NOT DEFERRABLE;
ALTER TABLE featuretype2_dt ADD CONSTRAINT fk_featuretype2_dt_featuretype2_id FOREIGN KEY (featuretype2_id) REFERENCES featuretype2 NOT DEFERRABLE;

INSERT INTO codelist1 (name, documentation) VALUES ('codeA', '');
INSERT INTO codelist1 (name, documentation) VALUES ('codeB', '');
