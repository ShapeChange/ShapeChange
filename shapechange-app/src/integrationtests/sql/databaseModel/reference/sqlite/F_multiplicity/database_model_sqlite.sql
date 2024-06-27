PRAGMA foreign_keys = ON;

CREATE TABLE associativetableot1andft1 (

   featuretype1_id INTEGER NOT NULL,
   objecttype1_id INTEGER NOT NULL,
   PRIMARY KEY (featuretype1_id, objecttype1_id),
   CONSTRAINT fk_associativetableot1andft1_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1,
   CONSTRAINT fk_associativetableot1andft1_objecttype1_id FOREIGN KEY (objecttype1_id) REFERENCES objecttype1
);

CREATE TABLE codelist (

   code TEXT NOT NULL PRIMARY KEY,
   documentation TEXT
);

CREATE TABLE featuretype1 (

   _id INTEGER NOT NULL PRIMARY KEY,
   attbooleandefaultvalue INTEGER DEFAULT 1 NOT NULL,
   attcharacterstringdefaultvalue TEXT DEFAULT 'My default value' NOT NULL,
   attcharacterstringlimitedlength TEXT NOT NULL,
   attcodelist_fk TEXT NOT NULL,
   attintegerdefaultvalue INTEGER DEFAULT 5 NOT NULL,
   attnumericenumeration REAL,
   attoptionalinteger INTEGER,
   attrealprecision REAL NOT NULL,
   attrealprecisionscale REAL NOT NULL,
   atttextenumeration TEXT NOT NULL,
   attguid TEXT NOT NULL,
   attmyreal1 REAL NOT NULL,
   attmyreal2 REAL NOT NULL,
   CONSTRAINT featuretype1_attnumericenumeration_chk CHECK (attnumericenumeration IS NULL OR attnumericenumeration IN (100.1, 100.2)),
   CONSTRAINT featuretype1_atttextenumeration_chk CHECK (atttextenumeration IN ('X', 'Y')),
   CONSTRAINT fk_featuretype1_attcodelist_fk FOREIGN KEY (attcodelist_fk) REFERENCES codelist
);

CREATE TABLE featuretype2 (

   _id INTEGER NOT NULL PRIMARY KEY,
   roleft2toot1_fk INTEGER NOT NULL,
   roleft2toft1_fk INTEGER NOT NULL,
   CONSTRAINT fk_featuretype2_roleft2toft1_fk FOREIGN KEY (roleft2toft1_fk) REFERENCES featuretype1,
   CONSTRAINT fk_featuretype2_roleft2toot1_fk FOREIGN KEY (roleft2toot1_fk) REFERENCES objecttype1
);

CREATE TABLE objecttype1 (

   _id INTEGER NOT NULL PRIMARY KEY,
   roleot1toft2_fk INTEGER NOT NULL,
   CONSTRAINT fk_objecttype1_roleot1toft2_fk FOREIGN KEY (roleot1toft2_fk) REFERENCES featuretype2
);


SELECT AddGeometryColumn('featuretype1', 'attpoint', 4326, 'POINT', -1);
SELECT CreateSpatialIndex('featuretype1', 'attpoint');

INSERT INTO codelist (code, documentation) VALUES ('code1', '');
INSERT INTO codelist (code, documentation) VALUES ('code2', '');
