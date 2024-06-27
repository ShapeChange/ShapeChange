CREATE TABLE featuretype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   p1 text NOT NULL,
   p3 bigserial NOT NULL
);

CREATE TABLE featuretype1_p2 (

   featuretype1_id bigserial NOT NULL,
   p2 text NOT NULL,
   PRIMARY KEY (featuretype1_id, p2)
);

CREATE TABLE featuretype2 (

   _id bigserial NOT NULL PRIMARY KEY,
   p1 integer NOT NULL
);


ALTER TABLE featuretype1 ADD CONSTRAINT fk_featuretype1_p3 FOREIGN KEY (p3) REFERENCES featuretype2;
ALTER TABLE featuretype1 ADD CONSTRAINT uk_featuretype1_p1 UNIQUE (p1);
ALTER TABLE featuretype1 ADD CONSTRAINT uk_featuretype1_p3 UNIQUE (p3);
ALTER TABLE featuretype1_p2 ADD CONSTRAINT fk_featuretype1_p2_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;
ALTER TABLE featuretype2 ADD CONSTRAINT uk_featuretype2_p1 UNIQUE (p1);
