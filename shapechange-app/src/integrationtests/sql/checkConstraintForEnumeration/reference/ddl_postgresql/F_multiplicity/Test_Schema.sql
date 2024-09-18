CREATE TABLE featuretype (

   _id bigserial NOT NULL PRIMARY KEY,
   att1 numeric(5,2),
   att2 text,
   att3 numeric(5,2) NOT NULL,
   att4 text NOT NULL
);


ALTER TABLE featuretype ADD CONSTRAINT featuretype_att1_chk CHECK (att1 IS NULL OR att1 IN (100.1, 100.2));
ALTER TABLE featuretype ADD CONSTRAINT featuretype_att2_chk CHECK (att2 IS NULL OR att2 IN ('X', 'Y'));
ALTER TABLE featuretype ADD CONSTRAINT featuretype_att3_chk CHECK (att3 IN (100.1, 100.2));
ALTER TABLE featuretype ADD CONSTRAINT featuretype_att4_chk CHECK (att4 IN ('X', 'Y'));
