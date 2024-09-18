
PRAGMA foreign_keys = ON;

CREATE TABLE featuretype (

   _id INTEGER NOT NULL PRIMARY KEY,
   attfull REAL,
   attfullnn REAL NOT NULL,
   attint INTEGER,
   attintnn INTEGER NOT NULL,
   CONSTRAINT featuretype_attfull_chk CHECK (attfull IS NULL OR attfull BETWEEN -5.5 AND 5.5),
   CONSTRAINT featuretype_attfullnn_chk CHECK (attfullnn BETWEEN -5.5 AND 5.5),
   CONSTRAINT featuretype_attint_chk CHECK (attint IS NULL OR attint BETWEEN -2 AND 1000000000),
   CONSTRAINT featuretype_attintnn_chk CHECK (attintnn BETWEEN -1000000000 AND 2)
);

CREATE TABLE featuretype_attsingle (

   featuretype_id INTEGER NOT NULL,
   attsingle REAL NOT NULL,
   PRIMARY KEY (featuretype_id, attsingle),
   CONSTRAINT featuretype_attsingle_attsingle_chk CHECK (attsingle BETWEEN -3.3 AND 1000000000),
   CONSTRAINT fk_featuretype_attsingle_featuretype_id FOREIGN KEY (featuretype_id) REFERENCES featuretype
);

CREATE TABLE featuretype_attsinglenn (

   featuretype_id INTEGER NOT NULL,
   attsinglenn REAL NOT NULL,
   PRIMARY KEY (featuretype_id, attsinglenn),
   CONSTRAINT featuretype_attsinglenn_attsinglenn_chk CHECK (attsinglenn BETWEEN -1000000000 AND 3.3),
   CONSTRAINT fk_featuretype_attsinglenn_featuretype_id FOREIGN KEY (featuretype_id) REFERENCES featuretype
);

