
PRAGMA foreign_keys = ON;

CREATE TABLE featuretype1 (

   _id INTEGER NOT NULL PRIMARY KEY,
   p1 TEXT NOT NULL,
   p3 INTEGER NOT NULL,
   CONSTRAINT fk_featuretype1_p3 FOREIGN KEY (p3) REFERENCES featuretype2,
   CONSTRAINT uk_featuretype1_p1 UNIQUE (p1),
   CONSTRAINT uk_featuretype1_p3 UNIQUE (p3)
);

CREATE TABLE featuretype1_p2 (

   featuretype1_id INTEGER NOT NULL,
   p2 TEXT NOT NULL,
   PRIMARY KEY (featuretype1_id, p2),
   CONSTRAINT fk_featuretype1_p2_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1
);

CREATE TABLE featuretype2 (

   _id INTEGER NOT NULL PRIMARY KEY,
   p1 INTEGER NOT NULL,
   CONSTRAINT uk_featuretype2_p1 UNIQUE (p1)
);

