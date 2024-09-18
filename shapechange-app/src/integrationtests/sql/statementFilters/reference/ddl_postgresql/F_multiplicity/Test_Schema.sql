CREATE TABLE codelistcat1 (

   name text NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE codelistcat2 (

   name text NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE codelistcat3 (

   name text NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE codelistcat4 (

   name text NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE featuretype (

   _id bigserial NOT NULL PRIMARY KEY,
   pcode1 text NOT NULL,
   pcode2 text NOT NULL,
   pcode3 text NOT NULL,
   pcode4 text NOT NULL,
   pgeom1 geometry(POINT,4326) NOT NULL,
   pgeom2 geometry(LINESTRING,4326) NOT NULL
);


INSERT INTO codelistcat1 (name, documentation) VALUES ('code1A', '');
INSERT INTO codelistcat1 (name, documentation) VALUES ('code1B', '');
INSERT INTO codelistcat4 (name, documentation) VALUES ('code4A', '');
INSERT INTO codelistcat4 (name, documentation) VALUES ('code4B', '');
