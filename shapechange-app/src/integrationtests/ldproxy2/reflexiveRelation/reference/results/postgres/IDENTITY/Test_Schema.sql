CREATE TABLE featuretype (

   _id bigserial NOT NULL PRIMARY KEY,
   refrel1target_fk bigint NOT NULL,
   refrel4source_fk bigint NOT NULL,
   refrel4target_fk bigint NOT NULL,
   relref5target_fk bigint NOT NULL
);

CREATE TABLE featuretype_refrel2target (

   _id bigserial NOT NULL PRIMARY KEY,
   featuretype_refrel2source_id bigint NOT NULL,
   featuretype_refrel2target_id bigint NOT NULL
);

CREATE TABLE featuretype_refrel3source (

   _id bigserial NOT NULL PRIMARY KEY,
   featuretype_refrel3source_id bigint NOT NULL,
   featuretype_refrel3target_id bigint NOT NULL
);


ALTER TABLE featuretype ADD CONSTRAINT fk_featuretype_refrel1target_fk FOREIGN KEY (refrel1target_fk) REFERENCES featuretype;
ALTER TABLE featuretype ADD CONSTRAINT fk_featuretype_refrel4source_fk FOREIGN KEY (refrel4source_fk) REFERENCES featuretype;
ALTER TABLE featuretype ADD CONSTRAINT fk_featuretype_refrel4target_fk FOREIGN KEY (refrel4target_fk) REFERENCES featuretype;
ALTER TABLE featuretype ADD CONSTRAINT fk_featuretype_relref5target_fk FOREIGN KEY (relref5target_fk) REFERENCES featuretype;
ALTER TABLE featuretype_refrel2target ADD CONSTRAINT fk_featuretype_refrel2target_featuretype_refrel2source_id FOREIGN KEY (featuretype_refrel2source_id) REFERENCES featuretype;
ALTER TABLE featuretype_refrel2target ADD CONSTRAINT fk_featuretype_refrel2target_featuretype_refrel2target_id FOREIGN KEY (featuretype_refrel2target_id) REFERENCES featuretype;
ALTER TABLE featuretype_refrel3source ADD CONSTRAINT fk_featuretype_refrel3source_featuretype_refrel3source_id FOREIGN KEY (featuretype_refrel3source_id) REFERENCES featuretype;
ALTER TABLE featuretype_refrel3source ADD CONSTRAINT fk_featuretype_refrel3source_featuretype_refrel3target_id FOREIGN KEY (featuretype_refrel3target_id) REFERENCES featuretype;
