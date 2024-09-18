CREATE TABLE FeatureType1 (

   _id bigint NOT NULL PRIMARY KEY,
   roleFT1B_fk bigint
);

CREATE TABLE FeatureType1_roleFT1A (

   FeatureType1_id bigint NOT NULL,
   FeatureType2_id bigint NOT NULL,
   PRIMARY KEY (FeatureType1_id, FeatureType2_id)
);

CREATE TABLE FeatureType2 (

   _id bigint NOT NULL PRIMARY KEY,
   roleFT2B_fk bigint NOT NULL
);


CREATE TABLE FeatureType3 (

   _id bigint NOT NULL PRIMARY KEY
);

CREATE TABLE FeatureType3_attDataType (

   _id bigint NOT NULL PRIMARY KEY,
   att1 int NOT NULL,
   att2 nvarchar(max) NOT NULL,
   FeatureType3_id bigint NOT NULL
);


ALTER TABLE FeatureType1 ADD CONSTRAINT fk_FeatureType1_roleFT1B_fk FOREIGN KEY (roleFT1B_fk) REFERENCES FeatureType2 ON DELETE SET NULL ON UPDATE SET DEFAULT;
ALTER TABLE FeatureType1_roleFT1A ADD CONSTRAINT fk_FeatureType1_roleFT1A_FeatureType1_id FOREIGN KEY (FeatureType1_id) REFERENCES FeatureType1 ON UPDATE CASCADE;
ALTER TABLE FeatureType1_roleFT1A ADD CONSTRAINT fk_FeatureType1_roleFT1A_FeatureType2_id FOREIGN KEY (FeatureType2_id) REFERENCES FeatureType2 ON UPDATE CASCADE;
ALTER TABLE FeatureType2 ADD CONSTRAINT fk_FeatureType2_roleFT2B_fk FOREIGN KEY (roleFT2B_fk) REFERENCES FeatureType1;
ALTER TABLE FeatureType3_attDataType ADD CONSTRAINT fk_FeatureType3_attDataType_FeatureType3_id FOREIGN KEY (FeatureType3_id) REFERENCES FeatureType3 ON DELETE CASCADE ON UPDATE CASCADE;
