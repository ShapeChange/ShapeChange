CREATE TABLE CodeListCat1 (

   name nvarchar(max) NOT NULL PRIMARY KEY,
   documentation nvarchar(max)
);

CREATE TABLE CodeListCat2 (

   name nvarchar(max) NOT NULL PRIMARY KEY,
   documentation nvarchar(max)
);

CREATE TABLE CodeListCat3 (

   name nvarchar(max) NOT NULL PRIMARY KEY,
   documentation nvarchar(max)
);

CREATE TABLE CodeListCat4 (

   name nvarchar(max) NOT NULL PRIMARY KEY,
   documentation nvarchar(max)
);

CREATE TABLE FeatureType (

   _id bigint NOT NULL PRIMARY KEY,
   pCode1 nvarchar(max) NOT NULL,
   pCode2 nvarchar(max) NOT NULL,
   pCode3 nvarchar(max) NOT NULL,
   pCode4 nvarchar(max) NOT NULL,
   pGeom1 geometry NOT NULL,
   pGeom2 geometry NOT NULL
);


INSERT INTO CodeListCat1 (name, documentation) VALUES ('code1A', '');
INSERT INTO CodeListCat1 (name, documentation) VALUES ('code1B', '');
INSERT INTO CodeListCat4 (name, documentation) VALUES ('code4A', '');
INSERT INTO CodeListCat4 (name, documentation) VALUES ('code4B', '');
