CREATE TABLE t3_codelist1 (

	myname text NOT NULL PRIMARY KEY,
	myalias text,
	mydatacapturestatement text,
	mydefinition text,
	mydescription text,
	mydocumentation text,
	myexample text,
	mylegalbasis text,
	myprimarycode text
);

CREATE TABLE t3_codelist2 (

	myname text NOT NULL PRIMARY KEY,
	myalias text,
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
	propnumcode text   -- DEF: propNumCode definition; DESC: propNumCode description; EX: propNumCode example 1 propNumCode example 2; LB: propNumCode legal basis; DCS: propNumCode data capture statement 1 propNumCode data capture statement 2; PC: propNumCode primary code
);


CREATE TABLE t3_featuretype1_propalpcode (

	t3_featuretype1_id bigserial NOT NULL,
	t3_codelist2_id text NOT NULL,
	PRIMARY KEY (t3_featuretype1_id,t3_codelist2_id)
);




ALTER TABLE t3_featuretype1 ADD CONSTRAINT fk_t3_featuretype1_propnumcode FOREIGN KEY (propnumcode) REFERENCES t3_codelist1;
ALTER TABLE t3_featuretype1_propalpcode ADD CONSTRAINT fk_t3_featuretype1_propalpcode_t3_featuretype1_id FOREIGN KEY (t3_featuretype1_id) REFERENCES t3_featuretype1;
ALTER TABLE t3_featuretype1_propalpcode ADD CONSTRAINT fk_t3_featuretype1_propalpcode_t3_codelist2_id FOREIGN KEY (t3_codelist2_id) REFERENCES t3_codelist2;



INSERT INTO t3_codelist1
(myname, myalias, mydatacapturestatement, mydefinition, mydescription, mydocumentation, myexample, mylegalbasis, myprimarycode)
VALUES
('1', NULL, NULL, NULL, NULL, 'DEF: ; DESC: ; EX: ; LB: ; DCS: ; PC: ', NULL, NULL, NULL);

INSERT INTO t3_codelist1
(myname, myalias, mydatacapturestatement, mydefinition, mydescription, mydocumentation, myexample, mylegalbasis, myprimarycode)
VALUES
('2', NULL, NULL, NULL, NULL, 'DEF: ; DESC: ; EX: ; LB: ; DCS: ; PC: ', NULL, NULL, NULL);

INSERT INTO t3_codelist2
(myname, myalias, mydatacapturestatement, mydefinition, mydescription, mydocumentation, myexample, mylegalbasis, myprimarycode)
VALUES
('codeA', 'codeA alias', 'codeA data capture statement 2 codeA data capture statement', 'codeA definition', 'codeA description', 'DEF: codeA definition; DESC: codeA description; EX: codeA example 1
codeA example 2; LB: codeA legal basis; DCS: codeA data capture statement 2

codeA data capture statement; PC: codeA primary code', 'codeA example 1
codeA example 2', 'codeA legal basis', 'codeA primary code');

INSERT INTO t3_codelist2
(myname, myalias, mydatacapturestatement, mydefinition, mydescription, mydocumentation, myexample, mylegalbasis, myprimarycode)
VALUES
('codeB', 'codeB alias', NULL, 'codeB definition', 'codeB description', 'DEF: codeB definition; DESC: codeB description; EX: codeB example 1
codeB example 2; LB: codeB legal basis; DCS: ; PC: codeB primary code', 'codeB example 1
codeB example 2', 'codeB legal basis', 'codeB primary code');

