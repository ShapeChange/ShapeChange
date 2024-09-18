CREATE TABLE CodeList1 (

   name nvarchar(max) NOT NULL PRIMARY KEY,
   documentation nvarchar(max)
);

CREATE TABLE FeatureType1 (

   _id bigint NOT NULL PRIMARY KEY,
   att int NOT NULL,
   cl_fk nvarchar(max) NOT NULL
);

CREATE TABLE FeatureType1_dt (

   _id bigint NOT NULL PRIMARY KEY,
   att1 int NOT NULL,
   att2 nvarchar(max) NOT NULL,
   FeatureType1_id bigint NOT NULL
);

CREATE TABLE FeatureType1_roleFt1ToFt2 (

   FeatureType1_id bigint NOT NULL,
   FeatureType2_id bigint NOT NULL,
   PRIMARY KEY (FeatureType1_id, FeatureType2_id)
);

CREATE TABLE FeatureType2 (

   _id bigint NOT NULL PRIMARY KEY,
   roleFt2ToFt1B_fk bigint NOT NULL
);

CREATE TABLE FeatureType2_att (

   FeatureType2_id bigint NOT NULL,
   att int NOT NULL,
   PRIMARY KEY (FeatureType2_id, att)
);

CREATE TABLE FeatureType2_cl (

   FeatureType2_id bigint NOT NULL,
   CodeList1_id nvarchar(max) NOT NULL,
   PRIMARY KEY (FeatureType2_id, CodeList1_id)
);

CREATE TABLE FeatureType2_dt (

   _id bigint NOT NULL PRIMARY KEY,
   att1 int NOT NULL,
   att2 nvarchar(max) NOT NULL,
   FeatureType2_id bigint NOT NULL
);


ALTER TABLE FeatureType1 ADD CONSTRAINT fk_FeatureType1_cl_fk FOREIGN KEY (cl_fk) REFERENCES CodeList1;
ALTER TABLE FeatureType1_dt ADD CONSTRAINT fk_FeatureType1_dt_FeatureType1_id FOREIGN KEY (FeatureType1_id) REFERENCES FeatureType1;
ALTER TABLE FeatureType1_roleFt1ToFt2 ADD CONSTRAINT fk_FeatureType1_roleFt1ToFt2_FeatureType1_id FOREIGN KEY (FeatureType1_id) REFERENCES FeatureType1;
ALTER TABLE FeatureType1_roleFt1ToFt2 ADD CONSTRAINT fk_FeatureType1_roleFt1ToFt2_FeatureType2_id FOREIGN KEY (FeatureType2_id) REFERENCES FeatureType2;
ALTER TABLE FeatureType2 ADD CONSTRAINT fk_FeatureType2_roleFt2ToFt1B_fk FOREIGN KEY (roleFt2ToFt1B_fk) REFERENCES FeatureType1;
ALTER TABLE FeatureType2_att ADD CONSTRAINT fk_FeatureType2_att_FeatureType2_id FOREIGN KEY (FeatureType2_id) REFERENCES FeatureType2;
ALTER TABLE FeatureType2_cl ADD CONSTRAINT fk_FeatureType2_cl_CodeList1_id FOREIGN KEY (CodeList1_id) REFERENCES CodeList1;
ALTER TABLE FeatureType2_cl ADD CONSTRAINT fk_FeatureType2_cl_FeatureType2_id FOREIGN KEY (FeatureType2_id) REFERENCES FeatureType2;
ALTER TABLE FeatureType2_dt ADD CONSTRAINT fk_FeatureType2_dt_FeatureType2_id FOREIGN KEY (FeatureType2_id) REFERENCES FeatureType2;

INSERT INTO CodeList1 (name, documentation) VALUES ('codeA', '');
INSERT INTO CodeList1 (name, documentation) VALUES ('codeB', '');
