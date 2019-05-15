CREATE TABLE CodeList (

   name nvarchar(50) NOT NULL PRIMARY KEY,
   documentation nvarchar(max)
);

CREATE TABLE DataType (

   _id bigint NOT NULL PRIMARY KEY,
   dtProp nvarchar(max) NOT NULL,
   dtProp2 nvarchar(50) NOT NULL,
   dtProp3 nvarchar(max) NOT NULL
);

CREATE TABLE FeatureType1 (

   _id bigint NOT NULL PRIMARY KEY,
   p1 nvarchar(30) NOT NULL,
   p4 int NOT NULL,
   p5 nvarchar(max) NOT NULL,
   p6 nvarchar(max) NOT NULL
);

CREATE TABLE FeatureType1_p2 (

   FeatureType1_id bigint NOT NULL,
   p2 nvarchar(max) NOT NULL,
   PRIMARY KEY (FeatureType1_id, p2)
);

CREATE TABLE FeatureType1_p3 (

   FeatureType1_id bigint NOT NULL,
   DataType_id bigint NOT NULL,
   PRIMARY KEY (FeatureType1_id, DataType_id)
);


ALTER TABLE FeatureType1_p2 ADD CONSTRAINT fk_FeatureType1_p2_FeatureType1_id FOREIGN KEY (FeatureType1_id) REFERENCES FeatureType1;
ALTER TABLE FeatureType1_p3 ADD CONSTRAINT fk_FeatureType1_p3_DataType_id FOREIGN KEY (DataType_id) REFERENCES DataType;
ALTER TABLE FeatureType1_p3 ADD CONSTRAINT fk_FeatureType1_p3_FeatureType1_id FOREIGN KEY (FeatureType1_id) REFERENCES FeatureType1;

INSERT INTO CodeList (name, documentation) VALUES ('code1', '');
INSERT INTO CodeList (name, documentation) VALUES ('code2', '');
