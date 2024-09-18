CREATE TABLE associativetableot1andft1 (

   featuretype1_id bigserial NOT NULL,
   objecttype1_id bigserial NOT NULL,
   PRIMARY KEY (featuretype1_id, objecttype1_id)
);

CREATE TABLE codelist (

   code text NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE featuretype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   attbooleandefaultvalue boolean DEFAULT TRUE NOT NULL,
   attcharacterstringdefaultvalue text DEFAULT 'My default value' NOT NULL,
   attcharacterstringlimitedlength character varying(200) NOT NULL,
   attcodelist_fk text NOT NULL,
   attintegerdefaultvalue integer DEFAULT 5 NOT NULL,
   attnumericenumeration numeric(5,2),
   attoptionalinteger integer,
   attrealprecision numeric(8) NOT NULL,
   attrealprecisionscale numeric(5,2) NOT NULL,
   atttextenumeration text NOT NULL,
   attguid varchar(16) NOT NULL,
   attmyreal1 numeric(5) NOT NULL,
   attmyreal2 numeric(5,2) NOT NULL,
   attpoint geometry(POINT,4979) NOT NULL
);

CREATE TABLE featuretype2 (

   _id bigserial NOT NULL PRIMARY KEY,
   roleft2toot1_fk bigserial NOT NULL,
   roleft2toft1_fk bigserial NOT NULL
);

CREATE TABLE objecttype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   roleot1toft2_fk bigserial NOT NULL
);


ALTER TABLE associativetableot1andft1 ADD CONSTRAINT fk_associativetableot1andft1_featuretype1_id FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;
ALTER TABLE associativetableot1andft1 ADD CONSTRAINT fk_associativetableot1andft1_objecttype1_id FOREIGN KEY (objecttype1_id) REFERENCES objecttype1;
ALTER TABLE featuretype1 ADD CONSTRAINT featuretype1_attnumericenumeration_chk CHECK (attnumericenumeration IS NULL OR attnumericenumeration IN (100.1, 100.2));
ALTER TABLE featuretype1 ADD CONSTRAINT featuretype1_atttextenumeration_chk CHECK (atttextenumeration IN ('X', 'Y'));
ALTER TABLE featuretype1 ADD CONSTRAINT fk_featuretype1_attcodelist_fk FOREIGN KEY (attcodelist_fk) REFERENCES codelist;
ALTER TABLE featuretype2 ADD CONSTRAINT fk_featuretype2_roleft2toft1_fk FOREIGN KEY (roleft2toft1_fk) REFERENCES featuretype1;
ALTER TABLE featuretype2 ADD CONSTRAINT fk_featuretype2_roleft2toot1_fk FOREIGN KEY (roleft2toot1_fk) REFERENCES objecttype1;
ALTER TABLE objecttype1 ADD CONSTRAINT fk_objecttype1_roleot1toft2_fk FOREIGN KEY (roleot1toft2_fk) REFERENCES featuretype2;

INSERT INTO codelist (code, documentation) VALUES ('code1', '');
INSERT INTO codelist (code, documentation) VALUES ('code2', '');

CREATE INDEX idx_featuretype1_attpoint ON featuretype1 USING GIST (attpoint);
