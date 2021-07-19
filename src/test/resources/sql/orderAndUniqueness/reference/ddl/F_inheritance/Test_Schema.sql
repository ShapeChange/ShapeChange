CREATE TABLE cl (

   name text NOT NULL PRIMARY KEY,
   documentation text
);

CREATE TABLE ft1 (

   _id bigserial NOT NULL PRIMARY KEY,
   attboolean boolean NOT NULL
);

CREATE TABLE ft1_attcharacterstring (

   ft1_id bigint NOT NULL,
   attcharacterstring text NOT NULL,
   seqno integer NOT NULL,
   PRIMARY KEY (ft1_id, attcharacterstring, seqno)
);

CREATE TABLE ft1_attcl (

   ft1_id bigint NOT NULL,
   cl_id text NOT NULL,
   seqno integer NOT NULL,
   UNIQUE (ft1_id, cl_id),
   PRIMARY KEY (ft1_id, cl_id, seqno)
);

CREATE TABLE ft1_attdt (

   _id bigserial NOT NULL PRIMARY KEY,
   dtatt text NOT NULL,
   ft1_id bigint NOT NULL
);

CREATE TABLE ft1_attinteger (

   ft1_id bigint NOT NULL,
   attinteger integer NOT NULL
);

CREATE TABLE ft1_attreal (

   ft1_id bigint NOT NULL,
   attreal numeric NOT NULL,
   PRIMARY KEY (ft1_id, attreal)
);

CREATE TABLE ft1_roleft1toft2 (

   ft1_id bigint NOT NULL,
   ft2_id bigint NOT NULL,
   PRIMARY KEY (ft1_id, ft2_id)
);

CREATE TABLE ft2 (

   _id bigserial NOT NULL PRIMARY KEY
);


ALTER TABLE ft1_attcharacterstring ADD CONSTRAINT fk_ft1_attcharacterstring_ft1_id FOREIGN KEY (ft1_id) REFERENCES ft1;
ALTER TABLE ft1_attcl ADD CONSTRAINT fk_ft1_attcl_cl_id FOREIGN KEY (cl_id) REFERENCES cl;
ALTER TABLE ft1_attcl ADD CONSTRAINT fk_ft1_attcl_ft1_id FOREIGN KEY (ft1_id) REFERENCES ft1;
ALTER TABLE ft1_attdt ADD CONSTRAINT fk_ft1_attdt_ft1_id FOREIGN KEY (ft1_id) REFERENCES ft1;
ALTER TABLE ft1_attinteger ADD CONSTRAINT fk_ft1_attinteger_ft1_id FOREIGN KEY (ft1_id) REFERENCES ft1;
ALTER TABLE ft1_attreal ADD CONSTRAINT fk_ft1_attreal_ft1_id FOREIGN KEY (ft1_id) REFERENCES ft1;
ALTER TABLE ft1_roleft1toft2 ADD CONSTRAINT fk_ft1_roleft1toft2_ft1_id FOREIGN KEY (ft1_id) REFERENCES ft1;
ALTER TABLE ft1_roleft1toft2 ADD CONSTRAINT fk_ft1_roleft1toft2_ft2_id FOREIGN KEY (ft2_id) REFERENCES ft2;

INSERT INTO cl (name, documentation) VALUES ('enum1', '');
INSERT INTO cl (name, documentation) VALUES ('enum2', '');

CREATE INDEX idx_ft1_attinteger ON ft1_attinteger (ft1_id, attinteger);
