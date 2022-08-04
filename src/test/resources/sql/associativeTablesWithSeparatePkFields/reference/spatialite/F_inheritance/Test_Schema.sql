PRAGMA foreign_keys = ON;

CREATE TABLE cl1 (

   name TEXT NOT NULL PRIMARY KEY,
   documentation TEXT
);

CREATE TABLE ft1 (

   _id INTEGER NOT NULL PRIMARY KEY
);

CREATE TABLE ft1_att1 (

   _id INTEGER NOT NULL PRIMARY KEY,
   ft1_id INTEGER NOT NULL,
   att1 REAL NOT NULL,
   CONSTRAINT fk_ft1_att1_ft1_id FOREIGN KEY (ft1_id) REFERENCES ft1
);

CREATE TABLE ft1_att2 (

   _id INTEGER NOT NULL PRIMARY KEY,
   ft1_id INTEGER NOT NULL,
   att2 TEXT NOT NULL,
   CONSTRAINT fk_ft1_att2_ft1_id FOREIGN KEY (ft1_id) REFERENCES ft1
);

CREATE TABLE ft1_codelist (

   _id INTEGER NOT NULL PRIMARY KEY,
   ft1_id INTEGER NOT NULL,
   cl1_id TEXT NOT NULL,
   CONSTRAINT fk_ft1_codelist_cl1_id FOREIGN KEY (cl1_id) REFERENCES cl1,
   CONSTRAINT fk_ft1_codelist_ft1_id FOREIGN KEY (ft1_id) REFERENCES ft1
);

CREATE TABLE ft1_roleft1toft2 (

   _id INTEGER NOT NULL PRIMARY KEY,
   ft1_id INTEGER NOT NULL,
   ft2_id INTEGER NOT NULL,
   CONSTRAINT fk_ft1_roleft1toft2_ft1_id FOREIGN KEY (ft1_id) REFERENCES ft1,
   CONSTRAINT fk_ft1_roleft1toft2_ft2_id FOREIGN KEY (ft2_id) REFERENCES ft2
);

CREATE TABLE ft2 (

   _id INTEGER NOT NULL PRIMARY KEY,
   att1 REAL NOT NULL
);

CREATE TABLE ft2_datatype (

   _id INTEGER NOT NULL PRIMARY KEY,
   att1 INTEGER NOT NULL,
   att2 TEXT NOT NULL,
   ft2_id INTEGER NOT NULL,
   CONSTRAINT fk_ft2_datatype_ft2_id FOREIGN KEY (ft2_id) REFERENCES ft2
);


SELECT AddGeometryColumn('ft1', 'geometry', 25832, 'POINT', 'XY', -1);
SELECT AddGeometryColumn('ft2', 'geometry', 25832, 'POLYGON', 'XY', -1);
SELECT CreateSpatialIndex('ft1', 'geometry');
SELECT CreateSpatialIndex('ft2', 'geometry');

INSERT INTO cl1 (name, documentation) VALUES ('codeA', '');
INSERT INTO cl1 (name, documentation) VALUES ('codeB', '');
