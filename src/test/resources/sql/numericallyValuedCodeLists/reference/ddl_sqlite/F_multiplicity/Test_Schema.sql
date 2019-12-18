

PRAGMA foreign_keys = ON;

CREATE TABLE featuretype (

   _id INTEGER NOT NULL PRIMARY KEY,
   att2 REAL NOT NULL,
   att3 INTEGER NOT NULL,
   att4 TEXT NOT NULL,
   att5 REAL NOT NULL,
   att6 REAL NOT NULL,
   att7 TEXT NOT NULL,
   CONSTRAINT featuretype_att6_chk CHECK (att6 IN (100.1, 100.2)),
   CONSTRAINT featuretype_att7_chk CHECK (att7 IN ('X', 'Y')),
   CONSTRAINT fk_featuretype_att2 FOREIGN KEY (att2) REFERENCES numericcodelistwithvalidprecisionandscale,
   CONSTRAINT fk_featuretype_att3 FOREIGN KEY (att3) REFERENCES integercodelist,
   CONSTRAINT fk_featuretype_att4 FOREIGN KEY (att4) REFERENCES textcodelist,
   CONSTRAINT fk_featuretype_att5 FOREIGN KEY (att5) REFERENCES numericcodelistwithinvalidprecisionandscale
);

CREATE TABLE featuretype_att1 (

   featuretype_id INTEGER NOT NULL,
   numericcodelist_id REAL NOT NULL,
   PRIMARY KEY (featuretype_id, numericcodelist_id),
   CONSTRAINT fk_featuretype_att1_featuretype_id FOREIGN KEY (featuretype_id) REFERENCES featuretype,
   CONSTRAINT fk_featuretype_att1_numericcodelist_id FOREIGN KEY (numericcodelist_id) REFERENCES numericcodelist
);

CREATE TABLE integercodelist (

   name INTEGER NOT NULL PRIMARY KEY,
   documentation TEXT
);

CREATE TABLE numericcodelist (

   name REAL NOT NULL PRIMARY KEY,
   documentation TEXT
);

CREATE TABLE numericcodelistwithinvalidprecisionandscale (

   name REAL NOT NULL PRIMARY KEY,
   documentation TEXT
);

CREATE TABLE numericcodelistwithvalidprecisionandscale (

   name REAL NOT NULL PRIMARY KEY,
   documentation TEXT
);

CREATE TABLE textcodelist (

   name TEXT NOT NULL PRIMARY KEY,
   documentation TEXT
);


INSERT INTO integercodelist (name, documentation) VALUES (10, '');
INSERT INTO integercodelist (name, documentation) VALUES (11, '');
INSERT INTO numericcodelist (name, documentation) VALUES (1.1, '');
INSERT INTO numericcodelist (name, documentation) VALUES (1.2, '');
INSERT INTO numericcodelistwithinvalidprecisionandscale (name, documentation) VALUES (3.1, '');
INSERT INTO numericcodelistwithinvalidprecisionandscale (name, documentation) VALUES (3.2, '');
INSERT INTO numericcodelistwithvalidprecisionandscale (name, documentation) VALUES (2.1, '');
INSERT INTO numericcodelistwithvalidprecisionandscale (name, documentation) VALUES (2.2, '');
INSERT INTO textcodelist (name, documentation) VALUES ('abc', '');
INSERT INTO textcodelist (name, documentation) VALUES ('def', '');
