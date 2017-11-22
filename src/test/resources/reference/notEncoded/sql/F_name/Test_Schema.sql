CREATE TABLE associationclass (

   objectidentifier bigserial NOT NULL PRIMARY KEY,
   pofassociationclass unknown NOT NULL,
   roleft4toft3_fk bigserial NOT NULL
);

CREATE TABLE featuretype1 (

   objectidentifier bigserial NOT NULL PRIMARY KEY,
   roleft1toft3_fk bigserial NOT NULL
);

CREATE TABLE featuretype3 (

   objectidentifier bigserial NOT NULL PRIMARY KEY,
   pft3 character varying(4000) NOT NULL
);

CREATE TABLE featuretype4 (

   objectidentifier bigserial NOT NULL PRIMARY KEY,
   pwithunmappedvaluetype unknown NOT NULL,
   roleft4toft3_fk bigserial NOT NULL
);


ALTER TABLE associationclass ADD CONSTRAINT fk_associationclass_roleft4toft3_fk FOREIGN KEY (roleft4toft3_fk) REFERENCES featuretype3;
ALTER TABLE featuretype1 ADD CONSTRAINT fk_featuretype1_roleft1toft3_fk FOREIGN KEY (roleft1toft3_fk) REFERENCES featuretype3;
ALTER TABLE featuretype4 ADD CONSTRAINT fk_featuretype4_roleft4toft3_fk FOREIGN KEY (roleft4toft3_fk) REFERENCES associationclass;
