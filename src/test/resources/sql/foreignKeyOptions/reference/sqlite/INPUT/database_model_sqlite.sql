
PRAGMA foreign_keys = ON;

CREATE TABLE featuretype1 (

   _id INTEGER NOT NULL PRIMARY KEY,
   roleft1b_fk INTEGER,
   CONSTRAINT fk_featuretype1_roleft1b_fk FOREIGN KEY (roleft1b_fk) REFERENCES featuretype2 ON DELETE SET NULL ON UPDATE SET DEFAULT
);

CREATE TABLE featuretype1_roleft1a (

   featuretype1_id INTEGER NOT NULL,
   featuretype2_id INTEGER NOT NULL,
   PRIMARY KEY (featuretype1_id, featuretype2_id),
   CONSTRAINT fk_featuretype1_roleft1a_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1 ON UPDATE CASCADE,
   CONSTRAINT fk_featuretype1_roleft1a_featuretype2_id FOREIGN KEY (featuretype2_id) REFERENCES featuretype2 ON UPDATE CASCADE
);

CREATE TABLE featuretype2 (

   _id INTEGER NOT NULL PRIMARY KEY,
   roleft2b_fk INTEGER NOT NULL,
   CONSTRAINT fk_featuretype2_roleft2b_fk FOREIGN KEY (roleft2b_fk) REFERENCES featuretype1 ON UPDATE RESTRICT
);

