CREATE TABLE AssociativeTableOT1AndFT1 (

   FeatureType1_id bigint NOT NULL,
   ObjectType1_id bigint NOT NULL,
   PRIMARY KEY (FeatureType1_id, ObjectType1_id)
);

CREATE TABLE CodeList (

   code nvarchar(max) NOT NULL PRIMARY KEY,
   documentation nvarchar(max)
);

CREATE TABLE FeatureType1 (

   _id bigint NOT NULL PRIMARY KEY,
   attBooleanDefaultValue bit DEFAULT 1 NOT NULL,
   attCharacterStringDefaultValue nvarchar(max) DEFAULT 'My default value' NOT NULL,
   attCharacterStringLimitedLength nvarchar(200) NOT NULL,
   attCodeList_fk nvarchar(max) NOT NULL,
   attIntegerDefaultValue int DEFAULT 5 NOT NULL,
   attNumericEnumeration numeric(5,2),
   attOptionalInteger int,
   attRealPrecision numeric(8) NOT NULL,
   attRealPrecisionScale numeric(5,2) NOT NULL,
   attTextEnumeration nvarchar(max) NOT NULL,
   attGuid nvarchar(16) NOT NULL,
   attMyReal1 numeric(5) NOT NULL,
   attMyReal2 numeric(5,2) NOT NULL,
   attPoint geometry NOT NULL
);

CREATE TABLE FeatureType2 (

   _id bigint NOT NULL PRIMARY KEY,
   roleFT2ToOT1_fk bigint NOT NULL,
   roleFT2ToFT1_fk bigint NOT NULL
);

CREATE TABLE ObjectType1 (

   _id bigint NOT NULL PRIMARY KEY,
   roleOT1ToFT2_fk bigint NOT NULL
);


ALTER TABLE AssociativeTableOT1AndFT1 ADD CONSTRAINT fk_AssociativeTableOT1AndFT1_FeatureType1_id FOREIGN KEY (FeatureType1_id) REFERENCES FeatureType1;
ALTER TABLE AssociativeTableOT1AndFT1 ADD CONSTRAINT fk_AssociativeTableOT1AndFT1_ObjectType1_id FOREIGN KEY (ObjectType1_id) REFERENCES ObjectType1;
ALTER TABLE FeatureType1 ADD CONSTRAINT FeatureType1_attNumericEnumeration_CK CHECK (attNumericEnumeration IS NULL OR attNumericEnumeration IN (100.1, 100.2));
ALTER TABLE FeatureType1 ADD CONSTRAINT FeatureType1_attTextEnumeration_CK CHECK (attTextEnumeration IN ('X', 'Y'));
ALTER TABLE FeatureType1 ADD CONSTRAINT fk_FeatureType1_attCodeList_fk FOREIGN KEY (attCodeList_fk) REFERENCES CodeList;
ALTER TABLE FeatureType2 ADD CONSTRAINT fk_FeatureType2_roleFT2ToFT1_fk FOREIGN KEY (roleFT2ToFT1_fk) REFERENCES FeatureType1;
ALTER TABLE FeatureType2 ADD CONSTRAINT fk_FeatureType2_roleFT2ToOT1_fk FOREIGN KEY (roleFT2ToOT1_fk) REFERENCES ObjectType1;
ALTER TABLE ObjectType1 ADD CONSTRAINT fk_ObjectType1_roleOT1ToFT2_fk FOREIGN KEY (roleOT1ToFT2_fk) REFERENCES FeatureType2;

INSERT INTO CodeList (code, documentation) VALUES ('code1', '');
INSERT INTO CodeList (code, documentation) VALUES ('code2', '');

CREATE SPATIAL INDEX idx_FeatureType1_attPoint ON FeatureType1 (attPoint) USING GEOMETRY_AUTO_GRID WITH (BOUNDING_BOX = (-1000,-1000,1000,1000));
