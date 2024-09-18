CREATE TABLE featuretype (

   _id bigserial NOT NULL PRIMARY KEY,
   attfull numeric,
   attfullnn numeric NOT NULL,
   attint integer,
   attintnn integer NOT NULL
);

CREATE TABLE featuretype_attsingle (

   featuretype_id bigserial NOT NULL,
   attsingle numeric NOT NULL,
   PRIMARY KEY (featuretype_id, attsingle)
);

CREATE TABLE featuretype_attsinglenn (

   featuretype_id bigserial NOT NULL,
   attsinglenn numeric NOT NULL,
   PRIMARY KEY (featuretype_id, attsinglenn)
);


ALTER TABLE featuretype ADD CONSTRAINT featuretype_attfull_chk CHECK (attfull IS NULL OR attfull BETWEEN -5.5 AND 5.5);
ALTER TABLE featuretype ADD CONSTRAINT featuretype_attfullnn_chk CHECK (attfullnn BETWEEN -5.5 AND 5.5);
ALTER TABLE featuretype ADD CONSTRAINT featuretype_attint_chk CHECK (attint IS NULL OR attint BETWEEN -2 AND 1000000000);
ALTER TABLE featuretype ADD CONSTRAINT featuretype_attintnn_chk CHECK (attintnn BETWEEN -1000000000 AND 2);
ALTER TABLE featuretype_attsingle ADD CONSTRAINT featuretype_attsingle_attsingle_chk CHECK (attsingle BETWEEN -3.3 AND 1000000000);
ALTER TABLE featuretype_attsingle ADD CONSTRAINT fk_featuretype_attsingle_featuretype_id FOREIGN KEY (featuretype_id) REFERENCES featuretype;
ALTER TABLE featuretype_attsinglenn ADD CONSTRAINT featuretype_attsinglenn_attsinglenn_chk CHECK (attsinglenn BETWEEN -1000000000 AND 3.3);
ALTER TABLE featuretype_attsinglenn ADD CONSTRAINT fk_featuretype_attsinglenn_featuretype_id FOREIGN KEY (featuretype_id) REFERENCES featuretype;
