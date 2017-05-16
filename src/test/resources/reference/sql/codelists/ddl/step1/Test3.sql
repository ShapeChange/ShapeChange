CREATE TABLE t3_codelist1 (

   myname character varying(50) NOT NULL PRIMARY KEY,
   myalias character varying(255),
   mydatacapturestatement text,
   mydefinition text,
   mydescription text,
   mydocumentation text,
   myexample text,
   mylegalbasis text,
   myprimarycode text
);

CREATE TABLE t3_codelist2 (

   myname character varying(50) NOT NULL PRIMARY KEY,
   myalias character varying(255),
   mydatacapturestatement text,
   mydefinition text,
   mydescription text,
   mydocumentation text,
   myexample text,
   mylegalbasis text,
   myprimarycode text
);

CREATE TABLE t3_featuretype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   propnumcode character varying(50) -- DEF: propNumCode definition; DESC: propNumCode description; EX: propNumCode example 1 propNumCode example 2; LB: propNumCode legal basis; DCS: propNumCode data capture statement 1 propNumCode data capture statement 2; PC: propNumCode primary code
);

CREATE TABLE t3_featuretype1_propalpcode (

   t3_featuretype1_id bigserial NOT NULL,
   t3_codelist2_id character varying(50) NOT NULL, -- DEF: ; DESC: ; EX: ; LB: ; DCS: ; PC:
   PRIMARY KEY (t3_featuretype1_id, t3_codelist2_id)
);


ALTER TABLE t3_featuretype1 ADD CONSTRAINT fk_t3_featuretype1_propnumcode FOREIGN KEY (propnumcode) REFERENCES t3_codelist1;
ALTER TABLE t3_featuretype1_propalpcode ADD CONSTRAINT fk_t3_featuretype1_propalpcode_t3_codelist2_id FOREIGN KEY (t3_codelist2_id) REFERENCES t3_codelist2;
ALTER TABLE t3_featuretype1_propalpcode ADD CONSTRAINT fk_t3_featuretype1_propalpcode_t3_featuretype1_id FOREIGN KEY (t3_featuretype1_id) REFERENCES t3_featuretype1;

INSERT INTO t3_codelist1 (myname, myalias, mydatacapturestatement, mydefinition, mydescription, mydocumentation, myexample, mylegalbasis, myprimarycode) VALUES ('1', NULL, NULL, '', NULL, 'DEF: ; DESC: ; EX: ; LB: ; DCS: ; PC: ', NULL, NULL, NULL);
INSERT INTO t3_codelist1 (myname, myalias, mydatacapturestatement, mydefinition, mydescription, mydocumentation, myexample, mylegalbasis, myprimarycode) VALUES ('2', NULL, NULL, '', NULL, 'DEF: ; DESC: ; EX: ; LB: ; DCS: ; PC: ', NULL, NULL, NULL);
INSERT INTO t3_codelist1 (myname, myalias, mydatacapturestatement, mydefinition, mydescription, mydocumentation, myexample, mylegalbasis, myprimarycode) VALUES ('quote''me', NULL, NULL, '', NULL, 'DEF: ; DESC: ; EX: ; LB: ; DCS: ; PC: ', NULL, NULL, NULL);
INSERT INTO t3_codelist2 (myname, myalias, mydatacapturestatement, mydefinition, mydescription, mydocumentation, myexample, mylegalbasis, myprimarycode) VALUES ('code''B', 'codeB alias', NULL, 'codeB definition', 'codeB description', 'DEF: codeB definition; DESC: codeB description; EX: codeB example 1
codeB example 2; LB: codeB legal basis; DCS: ; PC: codeB primary code', 'codeB example 1
codeB example 2', 'codeB legal basis', 'codeB primary code');
INSERT INTO t3_codelist2 (myname, myalias, mydatacapturestatement, mydefinition, mydescription, mydocumentation, myexample, mylegalbasis, myprimarycode) VALUES ('codeA', 'codeA''s alias', 'codeA data capture statement codeA data capture statement 2', 'codeA''s definition', 'codeA''s description', 'DEF: codeA''s definition; DESC: codeA''s description; EX: codeA''s example 1
codeA''s example 2; LB: codeA''s legal basis; DCS: codeA data capture statement

codeA data capture statement 2; PC: codeA''s primary code', 'codeA''s example 1
codeA''s example 2', 'codeA''s legal basis', 'codeA''s primary code');
