CREATE TABLE codelist (

   myname character varying(50) NOT NULL PRIMARY KEY,
   myalias character varying(255),
   mydefinition text,
   mydescription text
);

CREATE TABLE datatype (

   _id bigserial NOT NULL PRIMARY KEY,
   dtatt1 text NOT NULL,
   dtatt2 boolean NOT NULL
);

CREATE TABLE featuretype (

   _id bigserial NOT NULL PRIMARY KEY,
   ftatt2 text NOT NULL,
   ftatt3 character varying(50) NOT NULL
);

CREATE TABLE featuretype_ftatt1 (

   featuretype_id bigserial NOT NULL,
   datatype_id bigserial NOT NULL,
   PRIMARY KEY (featuretype_id, datatype_id)
);

CREATE TABLE objecttype (

   _id bigserial NOT NULL PRIMARY KEY,
   otatt1_option1 integer,
   otatt1_option2 integer
);


ALTER TABLE featuretype ADD CONSTRAINT featuretype_ftatt2_chk CHECK (ftatt2 IN ('enum1', 'enum2'));
ALTER TABLE featuretype ADD CONSTRAINT fk_featuretype_ftatt3 FOREIGN KEY (ftatt3) REFERENCES codelist;
ALTER TABLE featuretype_ftatt1 ADD CONSTRAINT fk_featuretype_ftatt1_datatype_id FOREIGN KEY (datatype_id) REFERENCES datatype;
ALTER TABLE featuretype_ftatt1 ADD CONSTRAINT fk_featuretype_ftatt1_featuretype_id FOREIGN KEY (featuretype_id) REFERENCES featuretype;

INSERT INTO codelist (myname, myalias, mydefinition, mydescription) VALUES ('code1', NULL, 'Def code1', 'Desc code1');
INSERT INTO codelist (myname, myalias, mydefinition, mydescription) VALUES ('code2', NULL, 'Def code2', 'Desc code2');

COMMENT ON TABLE codelist IS 'DEF: Def CodeList DESC: Desc CodeList';
COMMENT ON TABLE datatype IS 'DEF: Def DataType DESC: Desc DataType';
COMMENT ON COLUMN datatype.dtatt1 IS 'DEF: Def dtAtt1 DESC: Desc dtAtt1';
COMMENT ON COLUMN datatype.dtatt2 IS 'DEF: Def dtAtt2 DESC: Desc dtAtt2';
COMMENT ON TABLE featuretype IS 'DEF: Def FeatureType - there''s a quote DESC: Desc FeatureType';
COMMENT ON COLUMN featuretype.ftatt2 IS 'DEF: Def ftAtt2 DESC: Desc ftAtt2';
COMMENT ON COLUMN featuretype.ftatt3 IS 'DEF: Def ftAtt3 DESC: Desc ftAtt3';
COMMENT ON COLUMN featuretype_ftatt1.datatype_id IS 'DEF: Def ftAtt1 DESC: Desc ftAtt1';
COMMENT ON TABLE objecttype IS 'DEF: Def ObjectType DESC: Desc ObjectType';
COMMENT ON COLUMN objecttype.otatt1_option1 IS 'DEF: Def option1 DESC: Desc option1';
COMMENT ON COLUMN objecttype.otatt1_option2 IS 'DEF: Def option2 DESC: Desc option2';
