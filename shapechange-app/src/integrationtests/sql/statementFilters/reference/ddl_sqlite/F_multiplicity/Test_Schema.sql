CREATE TABLE codelistcat1 (

   name TEXT NOT NULL PRIMARY KEY,
   documentation TEXT
);

CREATE TABLE codelistcat2 (

   name TEXT NOT NULL PRIMARY KEY,
   documentation TEXT
);

CREATE TABLE codelistcat3 (

   name TEXT NOT NULL PRIMARY KEY,
   documentation TEXT
);

CREATE TABLE codelistcat4 (

   name TEXT NOT NULL PRIMARY KEY,
   documentation TEXT
);

CREATE TABLE featuretype (

   _id INTEGER NOT NULL PRIMARY KEY,
   pcode1 TEXT NOT NULL,
   pcode2 TEXT NOT NULL,
   pcode3 TEXT NOT NULL,
   pcode4 TEXT NOT NULL
);


SELECT AddGeometryColumn('featuretype', 'pgeom1', 4326, 'POINT', -1);
SELECT AddGeometryColumn('featuretype', 'pgeom2', 4326, 'LINESTRING', -1);

INSERT INTO codelistcat1 (name, documentation) VALUES ('code1A', '');
INSERT INTO codelistcat1 (name, documentation) VALUES ('code1B', '');
INSERT INTO codelistcat4 (name, documentation) VALUES ('code4A', '');
INSERT INTO codelistcat4 (name, documentation) VALUES ('code4B', '');
