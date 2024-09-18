CREATE TABLE codelist (

   name text NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE featuretypea (

   _id bigserial NOT NULL PRIMARY KEY,
   attclsingle_fkcl text NOT NULL,
   attesingle text NOT NULL,
   ftatoftc_fk bigint NOT NULL
);

CREATE TABLE featuretypea_attclmulti (

   featuretypea_fk bigint NOT NULL,
   codelist_fkcl text NOT NULL,
   PRIMARY KEY (featuretypea_fk, codelist_fkcl)
);

CREATE TABLE featuretypea_attdtmulti (

   _id bigserial NOT NULL PRIMARY KEY,
   atta integer NOT NULL,
   attb text NOT NULL,
   featuretypea_fk bigint NOT NULL
);

CREATE TABLE featuretypea_attdtmulti_attdtbmulti (

   _id bigserial NOT NULL PRIMARY KEY,
   att1 numeric NOT NULL,
   datatypea_fkdt bigint NOT NULL
);

CREATE TABLE featuretypea_attdtmulti_attdtbsingle (

   _id bigserial NOT NULL PRIMARY KEY,
   att1 numeric NOT NULL,
   datatypea_fkdt bigint NOT NULL
);

CREATE TABLE featuretypea_attdtsingle (

   _id bigserial NOT NULL PRIMARY KEY,
   atta integer NOT NULL,
   attb text NOT NULL,
   featuretypea_fk bigint NOT NULL
);

CREATE TABLE featuretypea_attdtsingle_attdtbmulti (

   _id bigserial NOT NULL PRIMARY KEY,
   att1 numeric NOT NULL,
   datatypea_fkdt bigint NOT NULL
);

CREATE TABLE featuretypea_attdtsingle_attdtbsingle (

   _id bigserial NOT NULL PRIMARY KEY,
   att1 numeric NOT NULL,
   datatypea_fkdt bigint NOT NULL
);

CREATE TABLE featuretypea_attemulti (

   featuretypea_fk bigint NOT NULL,
   attemulti text NOT NULL,
   PRIMARY KEY (featuretypea_fk, attemulti)
);

CREATE TABLE featuretypea_ftatoftb (

   featuretypea_fk bigint NOT NULL,
   featuretypeb_fk bigint NOT NULL,
   PRIMARY KEY (featuretypea_fk, featuretypeb_fk)
);

CREATE TABLE featuretypeb (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE featuretypec (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE featuretyped (

   _id bigserial NOT NULL PRIMARY KEY,
   ftdsingletarget_reflexive bigint NOT NULL
);

CREATE TABLE featuretyped_ftdmultisource (

   featuretyped_ftdmultisource_reflexive bigint NOT NULL,
   featuretyped_ftdmultitarget_reflexive bigint NOT NULL,
   PRIMARY KEY (featuretyped_ftdmultisource_reflexive, featuretyped_ftdmultitarget_reflexive)
);


ALTER TABLE featuretypea ADD CONSTRAINT featuretypea_attesingle_chk CHECK (attesingle IN ('enum1', 'enum2'));
ALTER TABLE featuretypea ADD CONSTRAINT fk_featuretypea_attclsingle_fkcl FOREIGN KEY (attclsingle_fkcl) REFERENCES codelist;
ALTER TABLE featuretypea ADD CONSTRAINT fk_featuretypea_ftatoftc_fk FOREIGN KEY (ftatoftc_fk) REFERENCES featuretypec;
ALTER TABLE featuretypea_attclmulti ADD CONSTRAINT fk_featuretypea_attclmulti_codelist_fkcl FOREIGN KEY (codelist_fkcl) REFERENCES codelist;
ALTER TABLE featuretypea_attclmulti ADD CONSTRAINT fk_featuretypea_attclmulti_featuretypea_fk FOREIGN KEY (featuretypea_fk) REFERENCES featuretypea;
ALTER TABLE featuretypea_attdtmulti ADD CONSTRAINT fk_featuretypea_attdtmulti_featuretypea_fk FOREIGN KEY (featuretypea_fk) REFERENCES featuretypea;
ALTER TABLE featuretypea_attdtmulti_attdtbmulti ADD CONSTRAINT fk_featuretypea_attdtmulti_attdtbmulti_datatypea_fkdt FOREIGN KEY (datatypea_fkdt) REFERENCES featuretypea_attdtmulti;
ALTER TABLE featuretypea_attdtmulti_attdtbsingle ADD CONSTRAINT fk_featuretypea_attdtmulti_attdtbsingle_datatypea_fkdt FOREIGN KEY (datatypea_fkdt) REFERENCES featuretypea_attdtmulti;
ALTER TABLE featuretypea_attdtsingle ADD CONSTRAINT fk_featuretypea_attdtsingle_featuretypea_fk FOREIGN KEY (featuretypea_fk) REFERENCES featuretypea;
ALTER TABLE featuretypea_attdtsingle_attdtbmulti ADD CONSTRAINT fk_featuretypea_attdtsingle_attdtbmulti_datatypea_fkdt FOREIGN KEY (datatypea_fkdt) REFERENCES featuretypea_attdtsingle;
ALTER TABLE featuretypea_attdtsingle_attdtbsingle ADD CONSTRAINT fk_featuretypea_attdtsingle_attdtbsingle_datatypea_fkdt FOREIGN KEY (datatypea_fkdt) REFERENCES featuretypea_attdtsingle;
ALTER TABLE featuretypea_attemulti ADD CONSTRAINT featuretypea_attemulti_attemulti_chk CHECK (attemulti IN ('enum1', 'enum2'));
ALTER TABLE featuretypea_attemulti ADD CONSTRAINT fk_featuretypea_attemulti_featuretypea_fk FOREIGN KEY (featuretypea_fk) REFERENCES featuretypea;
ALTER TABLE featuretypea_ftatoftb ADD CONSTRAINT fk_featuretypea_ftatoftb_featuretypea_fk FOREIGN KEY (featuretypea_fk) REFERENCES featuretypea;
ALTER TABLE featuretypea_ftatoftb ADD CONSTRAINT fk_featuretypea_ftatoftb_featuretypeb_fk FOREIGN KEY (featuretypeb_fk) REFERENCES featuretypeb;
ALTER TABLE featuretyped ADD CONSTRAINT fk_featuretyped_ftdsingletarget_reflexive FOREIGN KEY (ftdsingletarget_reflexive) REFERENCES featuretyped;
ALTER TABLE featuretyped_ftdmultisource ADD CONSTRAINT fk_featuretyped_ftdmultisource_featuretyped_ftdmultisource_refl FOREIGN KEY (featuretyped_ftdmultisource_reflexive) REFERENCES featuretyped;
ALTER TABLE featuretyped_ftdmultisource ADD CONSTRAINT fk_featuretyped_ftdmultisource_featuretyped_ftdmultitarget_refl FOREIGN KEY (featuretyped_ftdmultitarget_reflexive) REFERENCES featuretyped;

INSERT INTO codelist (name, documentation) VALUES ('codeA', '');
INSERT INTO codelist (name, documentation) VALUES ('codeB', '');
