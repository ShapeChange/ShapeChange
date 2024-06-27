CREATE TABLE assoctablets1ft1attreal (

   _id bigserial NOT NULL PRIMARY KEY,
   ts1_featuretype1_id bigint NOT NULL,
   attreal numeric NOT NULL
);

CREATE TABLE assoctablets1ft1ft21 (

   _id bigserial NOT NULL PRIMARY KEY,
   ts1_featuretype1_id bigint NOT NULL,
   ts1_featuretype2_id bigint NOT NULL
);

CREATE TABLE assoctablets1ft1toextdt (

   _id bigserial NOT NULL PRIMARY KEY,
   ts1_featuretype1_id bigint NOT NULL,
   tb_extdt_id bigint NOT NULL
);

CREATE TABLE featuretype (

   _id bigserial NOT NULL PRIMARY KEY,
   title text NOT NULL,
   refrel1target_reflexive bigint NOT NULL,
   refrel4source_reflexive bigint NOT NULL,
   refrel4target_reflexive bigint NOT NULL,
   relref5target_reflexive bigint NOT NULL
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

CREATE TABLE ts1_featuretype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   attinteger integer,
   attcharacterstring text NOT NULL,
   attextdt1_fkdt bigint NOT NULL,
   roletoft2_2_fk bigint,
   roletoft2_4_fk bigint,
   roletoft2_5_fk bigint,
   rolenotencoded text NOT NULL
);

CREATE TABLE ts1_featuretype1_attlength (

   _id bigserial NOT NULL PRIMARY KEY,
   ts1_featuretype1_id bigint NOT NULL,
   attlength unknown NOT NULL
);

CREATE TABLE ts1_featuretype1_roletoft2_3 (

   _id bigserial NOT NULL PRIMARY KEY,
   ts1_featuretype1_id bigint NOT NULL,
   ts1_featuretype2_id bigint NOT NULL
);

CREATE TABLE ts1_featuretype2 (

   _id bigserial NOT NULL PRIMARY KEY,
   name text,
   roletoft1_5_fk bigint,
   roletomd1_fk bigint NOT NULL,
   roletoft3_1_ts1_featuretype3suba_fk bigint,
   roletoft3_1_ts1_featuretype3subb_fk bigint
);

CREATE TABLE ts1_featuretype2_roletoft3_2_ts1_featuretype3suba (

   _id bigserial NOT NULL PRIMARY KEY,
   ts1_featuretype2_id bigint NOT NULL,
   ts1_featuretype3suba_id bigint NOT NULL
);

CREATE TABLE ts1_featuretype2_roletoft3_2_ts1_featuretype3subb (

   _id bigserial NOT NULL PRIMARY KEY,
   ts1_featuretype2_id bigint NOT NULL,
   ts1_featuretype3subb_id bigint NOT NULL
);

CREATE TABLE ts1_featuretype2_roletomd2 (

   _id bigserial NOT NULL PRIMARY KEY,
   ts1_featuretype2_id bigint NOT NULL,
   tb_md_metadata_id bigint NOT NULL
);

CREATE TABLE ts1_featuretype3suba (

   _id bigserial NOT NULL PRIMARY KEY,
   text text NOT NULL
);

CREATE TABLE ts1_featuretype3subb (

   _id bigserial NOT NULL PRIMARY KEY,
   text text NOT NULL
);


ALTER TABLE assoctablets1ft1attreal ADD CONSTRAINT fk_assoctablets1ft1attreal_ts1_featuretype1_id FOREIGN KEY (ts1_featuretype1_id) REFERENCES ts1_featuretype1;
ALTER TABLE assoctablets1ft1ft21 ADD CONSTRAINT fk_assoctablets1ft1ft21_ts1_featuretype1_id FOREIGN KEY (ts1_featuretype1_id) REFERENCES ts1_featuretype1;
ALTER TABLE assoctablets1ft1ft21 ADD CONSTRAINT fk_assoctablets1ft1ft21_ts1_featuretype2_id FOREIGN KEY (ts1_featuretype2_id) REFERENCES ts1_featuretype2;
ALTER TABLE assoctablets1ft1toextdt ADD CONSTRAINT fk_assoctablets1ft1toextdt_tb_extdt_id FOREIGN KEY (tb_extdt_id) REFERENCES tb_extdt;
ALTER TABLE assoctablets1ft1toextdt ADD CONSTRAINT fk_assoctablets1ft1toextdt_ts1_featuretype1_id FOREIGN KEY (ts1_featuretype1_id) REFERENCES ts1_featuretype1;
ALTER TABLE featuretype ADD CONSTRAINT fk_featuretype_refrel1target_reflexive FOREIGN KEY (refrel1target_reflexive) REFERENCES featuretype;
ALTER TABLE featuretype ADD CONSTRAINT fk_featuretype_refrel4source_reflexive FOREIGN KEY (refrel4source_reflexive) REFERENCES featuretype;
ALTER TABLE featuretype ADD CONSTRAINT fk_featuretype_refrel4target_reflexive FOREIGN KEY (refrel4target_reflexive) REFERENCES featuretype;
ALTER TABLE featuretype ADD CONSTRAINT fk_featuretype_relref5target_reflexive FOREIGN KEY (relref5target_reflexive) REFERENCES featuretype;
ALTER TABLE featuretype_refrel2target ADD CONSTRAINT fk_featuretype_refrel2target_featuretype_refrel2source_id FOREIGN KEY (featuretype_refrel2source_id) REFERENCES featuretype;
ALTER TABLE featuretype_refrel2target ADD CONSTRAINT fk_featuretype_refrel2target_featuretype_refrel2target_id FOREIGN KEY (featuretype_refrel2target_id) REFERENCES featuretype;
ALTER TABLE featuretype_refrel3source ADD CONSTRAINT fk_featuretype_refrel3source_featuretype_refrel3source_id FOREIGN KEY (featuretype_refrel3source_id) REFERENCES featuretype;
ALTER TABLE featuretype_refrel3source ADD CONSTRAINT fk_featuretype_refrel3source_featuretype_refrel3target_id FOREIGN KEY (featuretype_refrel3target_id) REFERENCES featuretype;
ALTER TABLE ts1_featuretype1 ADD CONSTRAINT fk_ts1_featuretype1_attextdt1_fkdt FOREIGN KEY (attextdt1_fkdt) REFERENCES tb_extdt;
ALTER TABLE ts1_featuretype1 ADD CONSTRAINT fk_ts1_featuretype1_roletoft2_2_fk FOREIGN KEY (roletoft2_2_fk) REFERENCES ts1_featuretype2;
ALTER TABLE ts1_featuretype1 ADD CONSTRAINT fk_ts1_featuretype1_roletoft2_4_fk FOREIGN KEY (roletoft2_4_fk) REFERENCES ts1_featuretype2;
ALTER TABLE ts1_featuretype1 ADD CONSTRAINT fk_ts1_featuretype1_roletoft2_5_fk FOREIGN KEY (roletoft2_5_fk) REFERENCES ts1_featuretype2;
ALTER TABLE ts1_featuretype1_attlength ADD CONSTRAINT fk_ts1_featuretype1_attlength_ts1_featuretype1_id FOREIGN KEY (ts1_featuretype1_id) REFERENCES ts1_featuretype1;
ALTER TABLE ts1_featuretype1_roletoft2_3 ADD CONSTRAINT fk_ts1_featuretype1_roletoft2_3_ts1_featuretype1_id FOREIGN KEY (ts1_featuretype1_id) REFERENCES ts1_featuretype1;
ALTER TABLE ts1_featuretype1_roletoft2_3 ADD CONSTRAINT fk_ts1_featuretype1_roletoft2_3_ts1_featuretype2_id FOREIGN KEY (ts1_featuretype2_id) REFERENCES ts1_featuretype2;
ALTER TABLE ts1_featuretype2 ADD CONSTRAINT fk_ts1_featuretype2_roletoft1_5_fk FOREIGN KEY (roletoft1_5_fk) REFERENCES ts1_featuretype1;
ALTER TABLE ts1_featuretype2 ADD CONSTRAINT fk_ts1_featuretype2_roletoft3_1_ts1_featuretype3suba_fk FOREIGN KEY (roletoft3_1_ts1_featuretype3suba_fk) REFERENCES ts1_featuretype3suba;
ALTER TABLE ts1_featuretype2 ADD CONSTRAINT fk_ts1_featuretype2_roletoft3_1_ts1_featuretype3subb_fk FOREIGN KEY (roletoft3_1_ts1_featuretype3subb_fk) REFERENCES ts1_featuretype3subb;
ALTER TABLE ts1_featuretype2 ADD CONSTRAINT fk_ts1_featuretype2_roletomd1_fk FOREIGN KEY (roletomd1_fk) REFERENCES tb_md_metadata;
ALTER TABLE ts1_featuretype2_roletoft3_2_ts1_featuretype3suba ADD CONSTRAINT fk_ts1_featuretype2_roletoft3_2_ts1_featuretype3suba_ts1_featu0 FOREIGN KEY (ts1_featuretype3suba_id) REFERENCES ts1_featuretype3suba;
ALTER TABLE ts1_featuretype2_roletoft3_2_ts1_featuretype3suba ADD CONSTRAINT fk_ts1_featuretype2_roletoft3_2_ts1_featuretype3suba_ts1_featur FOREIGN KEY (ts1_featuretype2_id) REFERENCES ts1_featuretype2;
ALTER TABLE ts1_featuretype2_roletoft3_2_ts1_featuretype3subb ADD CONSTRAINT fk_ts1_featuretype2_roletoft3_2_ts1_featuretype3subb_ts1_featu0 FOREIGN KEY (ts1_featuretype3subb_id) REFERENCES ts1_featuretype3subb;
ALTER TABLE ts1_featuretype2_roletoft3_2_ts1_featuretype3subb ADD CONSTRAINT fk_ts1_featuretype2_roletoft3_2_ts1_featuretype3subb_ts1_featur FOREIGN KEY (ts1_featuretype2_id) REFERENCES ts1_featuretype2;
ALTER TABLE ts1_featuretype2_roletomd2 ADD CONSTRAINT fk_ts1_featuretype2_roletomd2_tb_md_metadata_id FOREIGN KEY (tb_md_metadata_id) REFERENCES tb_md_metadata;
ALTER TABLE ts1_featuretype2_roletomd2 ADD CONSTRAINT fk_ts1_featuretype2_roletomd2_ts1_featuretype2_id FOREIGN KEY (ts1_featuretype2_id) REFERENCES ts1_featuretype2;
