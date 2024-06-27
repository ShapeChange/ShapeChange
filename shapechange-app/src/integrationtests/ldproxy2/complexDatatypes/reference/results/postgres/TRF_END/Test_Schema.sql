CREATE TABLE codelist (

   code text NOT NULL PRIMARY KEY,
   name text,
   documentation text,
   description text
);

CREATE TABLE featuretype (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE featuretype_attdt1multi (

   _id bigserial NOT NULL PRIMARY KEY,
   attcodelistsingle_fkcl text NOT NULL,
   attenumerationsingle text NOT NULL,
   attsimplesingle integer NOT NULL,
   featuretype_id bigint NOT NULL
);

CREATE TABLE featuretype_attdt1multi_attcodelistmulti (

   datatype1_id bigint NOT NULL,
   codelist_id text NOT NULL,
   PRIMARY KEY (datatype1_id, codelist_id)
);

CREATE TABLE featuretype_attdt1multi_attdatatype2multi (

   _id bigserial NOT NULL PRIMARY KEY,
   attdt2codelistsingle_fkcl text NOT NULL,
   attdt2enumerationsingle text NOT NULL,
   attdt2simplesingle integer NOT NULL,
   datatype1_id bigint NOT NULL
);

CREATE TABLE featuretype_attdt1multi_attdatatype2multi_attdt2codelistmulti (

   datatype2_id bigint NOT NULL,
   codelist_id text NOT NULL,
   PRIMARY KEY (datatype2_id, codelist_id)
);

CREATE TABLE featuretype_attdt1multi_attdatatype2multi_attdt2enumerationmult (

   datatype2_id bigint NOT NULL,
   attdt2enumerationmulti text NOT NULL,
   PRIMARY KEY (datatype2_id, attdt2enumerationmulti)
);

CREATE TABLE featuretype_attdt1multi_attdatatype2multi_attdt2simplemulti (

   datatype2_id bigint NOT NULL,
   attdt2simplemulti integer NOT NULL,
   PRIMARY KEY (datatype2_id, attdt2simplemulti)
);

CREATE TABLE featuretype_attdt1multi_attdatatype2single (

   _id bigserial NOT NULL PRIMARY KEY,
   attdt2codelistsingle_fkcl text NOT NULL,
   attdt2enumerationsingle text NOT NULL,
   attdt2simplesingle integer NOT NULL,
   datatype1_id bigint NOT NULL
);

CREATE TABLE featuretype_attdt1multi_attdatatype2single_attdt2codelistmulti (

   datatype2_id bigint NOT NULL,
   codelist_id text NOT NULL,
   PRIMARY KEY (datatype2_id, codelist_id)
);

CREATE TABLE featuretype_attdt1multi_attdatatype2single_attdt2enumerationmul (

   datatype2_id bigint NOT NULL,
   attdt2enumerationmulti text NOT NULL,
   PRIMARY KEY (datatype2_id, attdt2enumerationmulti)
);

CREATE TABLE featuretype_attdt1multi_attdatatype2single_attdt2simplemulti (

   datatype2_id bigint NOT NULL,
   attdt2simplemulti integer NOT NULL,
   PRIMARY KEY (datatype2_id, attdt2simplemulti)
);

CREATE TABLE featuretype_attdt1multi_attenumerationmulti (

   datatype1_id bigint NOT NULL,
   attenumerationmulti text NOT NULL,
   PRIMARY KEY (datatype1_id, attenumerationmulti)
);

CREATE TABLE featuretype_attdt1multi_attsimplemulti (

   datatype1_id bigint NOT NULL,
   attsimplemulti integer NOT NULL,
   PRIMARY KEY (datatype1_id, attsimplemulti)
);

CREATE TABLE featuretype_attdt1single (

   _id bigserial NOT NULL PRIMARY KEY,
   attcodelistsingle_fkcl text NOT NULL,
   attenumerationsingle text NOT NULL,
   attsimplesingle integer NOT NULL,
   featuretype_id bigint NOT NULL
);

CREATE TABLE featuretype_attdt1single_attcodelistmulti (

   datatype1_id bigint NOT NULL,
   codelist_id text NOT NULL,
   PRIMARY KEY (datatype1_id, codelist_id)
);

CREATE TABLE featuretype_attdt1single_attdatatype2multi (

   _id bigserial NOT NULL PRIMARY KEY,
   attdt2codelistsingle_fkcl text NOT NULL,
   attdt2enumerationsingle text NOT NULL,
   attdt2simplesingle integer NOT NULL,
   datatype1_id bigint NOT NULL
);

CREATE TABLE featuretype_attdt1single_attdatatype2multi_attdt2codelistmulti (

   datatype2_id bigint NOT NULL,
   codelist_id text NOT NULL,
   PRIMARY KEY (datatype2_id, codelist_id)
);

CREATE TABLE featuretype_attdt1single_attdatatype2multi_attdt2enumerationmul (

   datatype2_id bigint NOT NULL,
   attdt2enumerationmulti text NOT NULL,
   PRIMARY KEY (datatype2_id, attdt2enumerationmulti)
);

CREATE TABLE featuretype_attdt1single_attdatatype2multi_attdt2simplemulti (

   datatype2_id bigint NOT NULL,
   attdt2simplemulti integer NOT NULL,
   PRIMARY KEY (datatype2_id, attdt2simplemulti)
);

CREATE TABLE featuretype_attdt1single_attdatatype2single (

   _id bigserial NOT NULL PRIMARY KEY,
   attdt2codelistsingle_fkcl text NOT NULL,
   attdt2enumerationsingle text NOT NULL,
   attdt2simplesingle integer NOT NULL,
   datatype1_id bigint NOT NULL
);

CREATE TABLE featuretype_attdt1single_attdatatype2single_attdt2codelistmulti (

   datatype2_id bigint NOT NULL,
   codelist_id text NOT NULL,
   PRIMARY KEY (datatype2_id, codelist_id)
);

CREATE TABLE featuretype_attdt1single_attdatatype2single_attdt2enumerationmu (

   datatype2_id bigint NOT NULL,
   attdt2enumerationmulti text NOT NULL,
   PRIMARY KEY (datatype2_id, attdt2enumerationmulti)
);

CREATE TABLE featuretype_attdt1single_attdatatype2single_attdt2simplemulti (

   datatype2_id bigint NOT NULL,
   attdt2simplemulti integer NOT NULL,
   PRIMARY KEY (datatype2_id, attdt2simplemulti)
);

CREATE TABLE featuretype_attdt1single_attenumerationmulti (

   datatype1_id bigint NOT NULL,
   attenumerationmulti text NOT NULL,
   PRIMARY KEY (datatype1_id, attenumerationmulti)
);

CREATE TABLE featuretype_attdt1single_attsimplemulti (

   datatype1_id bigint NOT NULL,
   attsimplemulti integer NOT NULL,
   PRIMARY KEY (datatype1_id, attsimplemulti)
);


ALTER TABLE featuretype_attdt1multi ADD CONSTRAINT featuretype_attdt1multi_attenumerationsingle_chk CHECK (attenumerationsingle IN ('enum1', 'enum2'));
ALTER TABLE featuretype_attdt1multi ADD CONSTRAINT fk_featuretype_attdt1multi_attcodelistsingle_fkcl FOREIGN KEY (attcodelistsingle_fkcl) REFERENCES codelist DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi ADD CONSTRAINT fk_featuretype_attdt1multi_featuretype_id FOREIGN KEY (featuretype_id) REFERENCES featuretype DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attcodelistmulti ADD CONSTRAINT fk_featuretype_attdt1multi_attcodelistmulti_codelist_id FOREIGN KEY (codelist_id) REFERENCES codelist DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attcodelistmulti ADD CONSTRAINT fk_featuretype_attdt1multi_attcodelistmulti_datatype1_id FOREIGN KEY (datatype1_id) REFERENCES featuretype_attdt1multi DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attdatatype2multi ADD CONSTRAINT featuretype_attdt1multi_attdatatype2multi_attdt2enumerationsing CHECK (attdt2enumerationsingle IN ('enum1', 'enum2'));
ALTER TABLE featuretype_attdt1multi_attdatatype2multi ADD CONSTRAINT fk_featuretype_attdt1multi_attdatatype2multi_attdt2codelistsing FOREIGN KEY (attdt2codelistsingle_fkcl) REFERENCES codelist DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attdatatype2multi ADD CONSTRAINT fk_featuretype_attdt1multi_attdatatype2multi_datatype1_id FOREIGN KEY (datatype1_id) REFERENCES featuretype_attdt1multi DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attdatatype2multi_attdt2codelistmulti ADD CONSTRAINT fk_featuretype_attdt1multi_attdatatype2multi_attdt2codelistmul0 FOREIGN KEY (datatype2_id) REFERENCES featuretype_attdt1multi_attdatatype2multi DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attdatatype2multi_attdt2codelistmulti ADD CONSTRAINT fk_featuretype_attdt1multi_attdatatype2multi_attdt2codelistmult FOREIGN KEY (codelist_id) REFERENCES codelist DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attdatatype2multi_attdt2enumerationmult ADD CONSTRAINT featuretype_attdt1multi_attdatatype2multi_attdt2enumerationmult CHECK (attdt2enumerationmulti IN ('enum1', 'enum2'));
ALTER TABLE featuretype_attdt1multi_attdatatype2multi_attdt2enumerationmult ADD CONSTRAINT fk_featuretype_attdt1multi_attdatatype2multi_attdt2enumerationm FOREIGN KEY (datatype2_id) REFERENCES featuretype_attdt1multi_attdatatype2multi DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attdatatype2multi_attdt2simplemulti ADD CONSTRAINT fk_featuretype_attdt1multi_attdatatype2multi_attdt2simplemulti_ FOREIGN KEY (datatype2_id) REFERENCES featuretype_attdt1multi_attdatatype2multi DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attdatatype2single ADD CONSTRAINT featuretype_attdt1multi_attdatatype2single_attdt2enumerationsin CHECK (attdt2enumerationsingle IN ('enum1', 'enum2'));
ALTER TABLE featuretype_attdt1multi_attdatatype2single ADD CONSTRAINT fk_featuretype_attdt1multi_attdatatype2single_attdt2codelistsin FOREIGN KEY (attdt2codelistsingle_fkcl) REFERENCES codelist DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attdatatype2single ADD CONSTRAINT fk_featuretype_attdt1multi_attdatatype2single_datatype1_id FOREIGN KEY (datatype1_id) REFERENCES featuretype_attdt1multi DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attdatatype2single_attdt2codelistmulti ADD CONSTRAINT fk_featuretype_attdt1multi_attdatatype2single_attdt2codelistmu0 FOREIGN KEY (datatype2_id) REFERENCES featuretype_attdt1multi_attdatatype2single DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attdatatype2single_attdt2codelistmulti ADD CONSTRAINT fk_featuretype_attdt1multi_attdatatype2single_attdt2codelistmul FOREIGN KEY (codelist_id) REFERENCES codelist DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attdatatype2single_attdt2enumerationmul ADD CONSTRAINT featuretype_attdt1multi_attdatatype2single_attdt2enumerationmul CHECK (attdt2enumerationmulti IN ('enum1', 'enum2'));
ALTER TABLE featuretype_attdt1multi_attdatatype2single_attdt2enumerationmul ADD CONSTRAINT fk_featuretype_attdt1multi_attdatatype2single_attdt2enumeration FOREIGN KEY (datatype2_id) REFERENCES featuretype_attdt1multi_attdatatype2single DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attdatatype2single_attdt2simplemulti ADD CONSTRAINT fk_featuretype_attdt1multi_attdatatype2single_attdt2simplemulti FOREIGN KEY (datatype2_id) REFERENCES featuretype_attdt1multi_attdatatype2single DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attenumerationmulti ADD CONSTRAINT featuretype_attdt1multi_attenumerationmulti_attenumerationmulti CHECK (attenumerationmulti IN ('enum1', 'enum2'));
ALTER TABLE featuretype_attdt1multi_attenumerationmulti ADD CONSTRAINT fk_featuretype_attdt1multi_attenumerationmulti_datatype1_id FOREIGN KEY (datatype1_id) REFERENCES featuretype_attdt1multi DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1multi_attsimplemulti ADD CONSTRAINT fk_featuretype_attdt1multi_attsimplemulti_datatype1_id FOREIGN KEY (datatype1_id) REFERENCES featuretype_attdt1multi DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single ADD CONSTRAINT featuretype_attdt1single_attenumerationsingle_chk CHECK (attenumerationsingle IN ('enum1', 'enum2'));
ALTER TABLE featuretype_attdt1single ADD CONSTRAINT fk_featuretype_attdt1single_attcodelistsingle_fkcl FOREIGN KEY (attcodelistsingle_fkcl) REFERENCES codelist DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single ADD CONSTRAINT fk_featuretype_attdt1single_featuretype_id FOREIGN KEY (featuretype_id) REFERENCES featuretype DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attcodelistmulti ADD CONSTRAINT fk_featuretype_attdt1single_attcodelistmulti_codelist_id FOREIGN KEY (codelist_id) REFERENCES codelist DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attcodelistmulti ADD CONSTRAINT fk_featuretype_attdt1single_attcodelistmulti_datatype1_id FOREIGN KEY (datatype1_id) REFERENCES featuretype_attdt1single DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attdatatype2multi ADD CONSTRAINT featuretype_attdt1single_attdatatype2multi_attdt2enumerationsin CHECK (attdt2enumerationsingle IN ('enum1', 'enum2'));
ALTER TABLE featuretype_attdt1single_attdatatype2multi ADD CONSTRAINT fk_featuretype_attdt1single_attdatatype2multi_attdt2codelistsin FOREIGN KEY (attdt2codelistsingle_fkcl) REFERENCES codelist DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attdatatype2multi ADD CONSTRAINT fk_featuretype_attdt1single_attdatatype2multi_datatype1_id FOREIGN KEY (datatype1_id) REFERENCES featuretype_attdt1single DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attdatatype2multi_attdt2codelistmulti ADD CONSTRAINT fk_featuretype_attdt1single_attdatatype2multi_attdt2codelistmu0 FOREIGN KEY (datatype2_id) REFERENCES featuretype_attdt1single_attdatatype2multi DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attdatatype2multi_attdt2codelistmulti ADD CONSTRAINT fk_featuretype_attdt1single_attdatatype2multi_attdt2codelistmul FOREIGN KEY (codelist_id) REFERENCES codelist DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attdatatype2multi_attdt2enumerationmul ADD CONSTRAINT featuretype_attdt1single_attdatatype2multi_attdt2enumerationmul CHECK (attdt2enumerationmulti IN ('enum1', 'enum2'));
ALTER TABLE featuretype_attdt1single_attdatatype2multi_attdt2enumerationmul ADD CONSTRAINT fk_featuretype_attdt1single_attdatatype2multi_attdt2enumeration FOREIGN KEY (datatype2_id) REFERENCES featuretype_attdt1single_attdatatype2multi DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attdatatype2multi_attdt2simplemulti ADD CONSTRAINT fk_featuretype_attdt1single_attdatatype2multi_attdt2simplemulti FOREIGN KEY (datatype2_id) REFERENCES featuretype_attdt1single_attdatatype2multi DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attdatatype2single ADD CONSTRAINT featuretype_attdt1single_attdatatype2single_attdt2enumerationsi CHECK (attdt2enumerationsingle IN ('enum1', 'enum2'));
ALTER TABLE featuretype_attdt1single_attdatatype2single ADD CONSTRAINT fk_featuretype_attdt1single_attdatatype2single_attdt2codelistsi FOREIGN KEY (attdt2codelistsingle_fkcl) REFERENCES codelist DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attdatatype2single ADD CONSTRAINT fk_featuretype_attdt1single_attdatatype2single_datatype1_id FOREIGN KEY (datatype1_id) REFERENCES featuretype_attdt1single DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attdatatype2single_attdt2codelistmulti ADD CONSTRAINT fk_featuretype_attdt1single_attdatatype2single_attdt2codelistm0 FOREIGN KEY (datatype2_id) REFERENCES featuretype_attdt1single_attdatatype2single DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attdatatype2single_attdt2codelistmulti ADD CONSTRAINT fk_featuretype_attdt1single_attdatatype2single_attdt2codelistmu FOREIGN KEY (codelist_id) REFERENCES codelist DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attdatatype2single_attdt2enumerationmu ADD CONSTRAINT featuretype_attdt1single_attdatatype2single_attdt2enumerationmu CHECK (attdt2enumerationmulti IN ('enum1', 'enum2'));
ALTER TABLE featuretype_attdt1single_attdatatype2single_attdt2enumerationmu ADD CONSTRAINT fk_featuretype_attdt1single_attdatatype2single_attdt2enumeratio FOREIGN KEY (datatype2_id) REFERENCES featuretype_attdt1single_attdatatype2single DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attdatatype2single_attdt2simplemulti ADD CONSTRAINT fk_featuretype_attdt1single_attdatatype2single_attdt2simplemult FOREIGN KEY (datatype2_id) REFERENCES featuretype_attdt1single_attdatatype2single DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attenumerationmulti ADD CONSTRAINT featuretype_attdt1single_attenumerationmulti_attenumerationmult CHECK (attenumerationmulti IN ('enum1', 'enum2'));
ALTER TABLE featuretype_attdt1single_attenumerationmulti ADD CONSTRAINT fk_featuretype_attdt1single_attenumerationmulti_datatype1_id FOREIGN KEY (datatype1_id) REFERENCES featuretype_attdt1single DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE featuretype_attdt1single_attsimplemulti ADD CONSTRAINT fk_featuretype_attdt1single_attsimplemulti_datatype1_id FOREIGN KEY (datatype1_id) REFERENCES featuretype_attdt1single DEFERRABLE INITIALLY DEFERRED;
