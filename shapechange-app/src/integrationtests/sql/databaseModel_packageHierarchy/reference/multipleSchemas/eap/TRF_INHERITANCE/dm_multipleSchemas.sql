CREATE TABLE as1_cl1 (

   code text NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE as1_ft1 (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute1_fk text NOT NULL
);

CREATE TABLE as1_ft2 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE as1_ft3 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE as1_ft3_roleas1ft3toas1ft4 (

   as1_ft3_id bigserial NOT NULL,
   as1_ft4_id bigserial NOT NULL,
   PRIMARY KEY (as1_ft3_id, as1_ft4_id)
);

CREATE TABLE as1_ft4 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE as2_ft1 (

   _id bigserial NOT NULL PRIMARY KEY,
   attribute1_fk text NOT NULL
);

CREATE TABLE as2_ft1_roleas2ft1toas2ft2 (

   as2_ft1_id bigserial NOT NULL,
   as2_ft2_id bigserial NOT NULL,
   PRIMARY KEY (as2_ft1_id, as2_ft2_id)
);

CREATE TABLE as2_ft2 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE as2_ft3 (

   _id bigserial NOT NULL PRIMARY KEY,
   roleas2ft3toas1ft2_fk bigserial NOT NULL
);


ALTER TABLE as1_ft1 ADD CONSTRAINT fk_as1_ft1_attribute1_fk FOREIGN KEY (attribute1_fk) REFERENCES as1_cl1;
ALTER TABLE as1_ft3_roleas1ft3toas1ft4 ADD CONSTRAINT fk_as1_ft3_roleas1ft3toas1ft4_as1_ft3_id FOREIGN KEY (as1_ft3_id) REFERENCES as1_ft3;
ALTER TABLE as1_ft3_roleas1ft3toas1ft4 ADD CONSTRAINT fk_as1_ft3_roleas1ft3toas1ft4_as1_ft4_id FOREIGN KEY (as1_ft4_id) REFERENCES as1_ft4;
ALTER TABLE as2_ft1 ADD CONSTRAINT fk_as2_ft1_attribute1_fk FOREIGN KEY (attribute1_fk) REFERENCES as1_cl1;
ALTER TABLE as2_ft1_roleas2ft1toas2ft2 ADD CONSTRAINT fk_as2_ft1_roleas2ft1toas2ft2_as2_ft1_id FOREIGN KEY (as2_ft1_id) REFERENCES as2_ft1;
ALTER TABLE as2_ft1_roleas2ft1toas2ft2 ADD CONSTRAINT fk_as2_ft1_roleas2ft1toas2ft2_as2_ft2_id FOREIGN KEY (as2_ft2_id) REFERENCES as2_ft2;
ALTER TABLE as2_ft3 ADD CONSTRAINT fk_as2_ft3_roleas2ft3toas1ft2_fk FOREIGN KEY (roleas2ft3toas1ft2_fk) REFERENCES as1_ft2;

INSERT INTO as1_cl1 (code, documentation) VALUES ('code1', '');
INSERT INTO as1_cl1 (code, documentation) VALUES ('code2', '');
