CREATE TABLE FeatureType (

   _id bigint NOT NULL PRIMARY KEY,
   attFull numeric,
   attFullNN numeric NOT NULL,
   attInt int,
   attIntNN int NOT NULL
);

CREATE TABLE FeatureType_attSingle (

   FeatureType_id bigint NOT NULL,
   attSingle numeric NOT NULL,
   PRIMARY KEY (FeatureType_id, attSingle)
);

CREATE TABLE FeatureType_attSingleNN (

   FeatureType_id bigint NOT NULL,
   attSingleNN numeric NOT NULL,
   PRIMARY KEY (FeatureType_id, attSingleNN)
);


ALTER TABLE FeatureType ADD CONSTRAINT FeatureType_attFullNN_CK CHECK (attFullNN BETWEEN -5.5 AND 5.5);
ALTER TABLE FeatureType ADD CONSTRAINT FeatureType_attFull_CK CHECK (attFull IS NULL OR attFull BETWEEN -5.5 AND 5.5);
ALTER TABLE FeatureType ADD CONSTRAINT FeatureType_attIntNN_CK CHECK (attIntNN BETWEEN -1000000000 AND 2);
ALTER TABLE FeatureType ADD CONSTRAINT FeatureType_attInt_CK CHECK (attInt IS NULL OR attInt BETWEEN -2 AND 1000000000);
ALTER TABLE FeatureType_attSingle ADD CONSTRAINT FeatureType_attSingle_attSingle_CK CHECK (attSingle BETWEEN -3.3 AND 1000000000);
ALTER TABLE FeatureType_attSingle ADD CONSTRAINT fk_FeatureType_attSingle_FeatureType_id FOREIGN KEY (FeatureType_id) REFERENCES FeatureType;
ALTER TABLE FeatureType_attSingleNN ADD CONSTRAINT FeatureType_attSingleNN_attSingleNN_CK CHECK (attSingleNN BETWEEN -1000000000 AND 3.3);
ALTER TABLE FeatureType_attSingleNN ADD CONSTRAINT fk_FeatureType_attSingleNN_FeatureType_id FOREIGN KEY (FeatureType_id) REFERENCES FeatureType;
