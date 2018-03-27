CREATE TABLE asotable (

   featuretype1_id bigserial NOT NULL,
   featuretype2_id bigserial NOT NULL,
   PRIMARY KEY (featuretype1_id, featuretype2_id)
);

CREATE TABLE featuretype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   roleft1b_fk bigserial,
   propdate date NOT NULL,
   propenum text NOT NULL,
   propunique text NOT NULL
);

CREATE TABLE featuretype2 (

   _id bigserial NOT NULL PRIMARY KEY
);


ALTER TABLE asotable ADD CONSTRAINT fk_atab_rft1a FOREIGN KEY (featuretype2_id) REFERENCES featuretype2;
ALTER TABLE asotable ADD CONSTRAINT fk_atab_rft2a FOREIGN KEY (featuretype1_id) REFERENCES featuretype1;
ALTER TABLE featuretype1 ADD CONSTRAINT fk_ft1_rft1b FOREIGN KEY (roleft1b_fk) REFERENCES featuretype2;
ALTER TABLE featuretype1 ADD CONSTRAINT ft1_pe_chk CHECK (propenum IN ('enum1', 'enum2'));
ALTER TABLE featuretype1 ADD CONSTRAINT uk_ft1_pu UNIQUE (propunique);
