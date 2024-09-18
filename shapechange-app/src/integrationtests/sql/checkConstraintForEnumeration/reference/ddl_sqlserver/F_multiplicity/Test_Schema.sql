CREATE TABLE FeatureType (

   _id bigint NOT NULL PRIMARY KEY,
   att1 numeric(5,2),
   att2 nvarchar(max),
   att3 numeric(5,2) NOT NULL,
   att4 nvarchar(max) NOT NULL
);


ALTER TABLE FeatureType ADD CONSTRAINT FeatureType_att1_CK CHECK (att1 IS NULL OR att1 IN (100.1, 100.2));
ALTER TABLE FeatureType ADD CONSTRAINT FeatureType_att2_CK CHECK (att2 IS NULL OR att2 IN ('X', 'Y'));
ALTER TABLE FeatureType ADD CONSTRAINT FeatureType_att3_CK CHECK (att3 IN (100.1, 100.2));
ALTER TABLE FeatureType ADD CONSTRAINT FeatureType_att4_CK CHECK (att4 IN ('X', 'Y'));
