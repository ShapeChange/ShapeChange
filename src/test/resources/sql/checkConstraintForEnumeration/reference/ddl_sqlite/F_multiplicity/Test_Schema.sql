
CREATE TABLE featuretype (

   _id INTEGER NOT NULL PRIMARY KEY,
   att1 REAL,
   att2 TEXT,
   att3 REAL NOT NULL,
   att4 TEXT NOT NULL,
   CONSTRAINT featuretype_att1_chk CHECK (att1 IS NULL OR att1 IN (100.1, 100.2)),
   CONSTRAINT featuretype_att2_chk CHECK (att2 IS NULL OR att2 IN ('X', 'Y')),
   CONSTRAINT featuretype_att3_chk CHECK (att3 IN (100.1, 100.2)),
   CONSTRAINT featuretype_att4_chk CHECK (att4 IN ('X', 'Y'))
);

