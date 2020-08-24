CREATE TABLE featuretype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   p3 text NOT NULL
);

CREATE TABLE featuretype1_p1 (

   p1 text NOT NULL,
   p2 integer NOT NULL PRIMARY KEY,
   featuretype1_id bigint NOT NULL
);

CREATE TABLE featuretype1_p2 (

   featuretype1_id bigint NOT NULL,
   p2 text NOT NULL,
   PRIMARY KEY (featuretype1_id, p2)
);

CREATE TABLE featuretype1_roleft1toft2 (

   featuretype1_id bigint NOT NULL,
   featuretype2_id numeric NOT NULL,
   PRIMARY KEY (featuretype1_id, featuretype2_id)
);

CREATE TABLE featuretype2 (

   p1 text NOT NULL,
   p2 numeric NOT NULL PRIMARY KEY,
   ignoredidentifier text NOT NULL
);

ALTER TABLE featuretype1_p1 ADD CONSTRAINT fk_featuretype1_p1_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;
ALTER TABLE featuretype1_p2 ADD CONSTRAINT fk_featuretype1_p2_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;
ALTER TABLE featuretype1_roleft1toft2 ADD CONSTRAINT fk_featuretype1_roleft1toft2_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;
ALTER TABLE featuretype1_roleft1toft2 ADD CONSTRAINT fk_featuretype1_roleft1toft2_featuretype2_id FOREIGN KEY (featuretype2_id) REFERENCES featuretype2;
