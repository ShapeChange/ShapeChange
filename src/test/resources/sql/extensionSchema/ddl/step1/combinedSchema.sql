CREATE TABLE feature1 (

   _id bigserial NOT NULL PRIMARY KEY,
   propa text NOT NULL,
   propb integer NOT NULL
);

CREATE TABLE feature1subtype (

   _id bigserial NOT NULL PRIMARY KEY,
   propa text NOT NULL,
   propb integer NOT NULL,
   propc boolean NOT NULL
);

CREATE TABLE feature2 (

   _id bigserial NOT NULL PRIMARY KEY,
   propf2tof1_feature1 bigserial,
   propf2tof1_feature1subtype bigserial
);


ALTER TABLE feature2 ADD CONSTRAINT fk_feature2_propf2tof1_feature1 FOREIGN KEY (propf2tof1_feature1) REFERENCES feature1;
ALTER TABLE feature2 ADD CONSTRAINT fk_feature2_propf2tof1_feature1subtype FOREIGN KEY (propf2tof1_feature1subtype) REFERENCES feature1subtype;
