CREATE TABLE FeatureType1 (

   _id bigint NOT NULL PRIMARY KEY,
   roleFT1B_fk bigint,
   roleFT1C_fk bigint
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


ALTER TABLE FeatureType1 ADD CONSTRAINT fk_FeatureType1_roleFT1B_fk FOREIGN KEY (roleFT1B_fk) REFERENCES FeatureType2;
ALTER TABLE FeatureType1 ADD CONSTRAINT fk_FeatureType1_roleFT1C_fk FOREIGN KEY (roleFT1C_fk) REFERENCES FeatureType2;
ALTER TABLE FeatureType1_roleFT1A ADD CONSTRAINT fk_FeatureType1_roleFT1A_FeatureType1_id FOREIGN KEY (FeatureType1_id) REFERENCES FeatureType1;
ALTER TABLE FeatureType1_roleFT1A ADD CONSTRAINT fk_FeatureType1_roleFT1A_FeatureType2_id FOREIGN KEY (FeatureType2_id) REFERENCES FeatureType2;
ALTER TABLE FeatureType2 ADD CONSTRAINT fk_FeatureType2_roleFT2B_fk FOREIGN KEY (roleFT2B_fk) REFERENCES FeatureType1;
