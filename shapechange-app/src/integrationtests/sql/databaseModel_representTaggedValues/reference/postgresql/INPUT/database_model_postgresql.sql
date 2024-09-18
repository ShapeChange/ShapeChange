CREATE TABLE featuretype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   roleft1b_fk bigserial
);

CREATE TABLE featuretype1_att1 (

   featuretype1_id bigserial NOT NULL,
   att1 text NOT NULL,
   PRIMARY KEY (featuretype1_id, att1)
);

CREATE TABLE featuretype1_roleft1a (

   featuretype1_id bigserial NOT NULL,
   featuretype2_id bigserial NOT NULL,
   PRIMARY KEY (featuretype1_id, featuretype2_id)
);

CREATE TABLE featuretype2 (

   _id bigserial NOT NULL PRIMARY KEY
);


ALTER TABLE featuretype1 ADD CONSTRAINT fk_featuretype1_roleft1b_fk FOREIGN KEY (roleft1b_fk) REFERENCES featuretype2;
ALTER TABLE featuretype1_att1 ADD CONSTRAINT fk_featuretype1_att1_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;
ALTER TABLE featuretype1_roleft1a ADD CONSTRAINT fk_featuretype1_roleft1a_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;
ALTER TABLE featuretype1_roleft1a ADD CONSTRAINT fk_featuretype1_roleft1a_featuretype2_id FOREIGN KEY (featuretype2_id) REFERENCES featuretype2;
