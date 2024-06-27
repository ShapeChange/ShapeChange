CREATE TABLE featuretype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   roleft1b_fk bigserial
);

CREATE TABLE featuretype1_roleft1a (

   featuretype1_id bigserial NOT NULL,
   featuretype2_id bigserial NOT NULL,
   PRIMARY KEY (featuretype1_id, featuretype2_id)
);

CREATE TABLE featuretype2 (

   _id bigserial NOT NULL PRIMARY KEY,
   roleft2b_fk bigserial NOT NULL
);


CREATE TABLE featuretype3 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE featuretype3_attdatatype (

   _id bigserial NOT NULL PRIMARY KEY,
   att1 integer NOT NULL,
   att2 text NOT NULL,
   featuretype3_id bigserial NOT NULL
);


ALTER TABLE featuretype1 ADD CONSTRAINT fk_featuretype1_roleft1b_fk FOREIGN KEY (roleft1b_fk) REFERENCES featuretype2 ON DELETE SET NULL ON UPDATE SET DEFAULT;
ALTER TABLE featuretype1_roleft1a ADD CONSTRAINT fk_featuretype1_roleft1a_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1 ON UPDATE CASCADE;
ALTER TABLE featuretype1_roleft1a ADD CONSTRAINT fk_featuretype1_roleft1a_featuretype2_id FOREIGN KEY (featuretype2_id) REFERENCES featuretype2 ON UPDATE CASCADE;
ALTER TABLE featuretype2 ADD CONSTRAINT fk_featuretype2_roleft2b_fk FOREIGN KEY (roleft2b_fk) REFERENCES featuretype1 ON UPDATE RESTRICT;
ALTER TABLE featuretype3_attdatatype ADD CONSTRAINT fk_featuretype3_attdatatype_featuretype3_id FOREIGN KEY (featuretype3_id) REFERENCES featuretype3 ON DELETE CASCADE ON UPDATE CASCADE;
