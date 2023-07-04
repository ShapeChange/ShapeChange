CREATE TABLE c1py (

   _id bigserial NOT NULL PRIMARY KEY,
   t1_class1_id bigint NOT NULL,
   py text NOT NULL
);

CREATE TABLE t1_class1 (

   _id bigserial NOT NULL PRIMARY KEY,
   pa2_fk bigint NOT NULL,
   pa4_fk bigint NOT NULL,
   pa5_fk bigint NOT NULL,
   pz geometry(POINT,31467) NOT NULL
);

CREATE TABLE t1_class1_pa1 (

   _id bigserial NOT NULL PRIMARY KEY,
   t1_class1_id bigint NOT NULL,
   t1_class2_id bigint NOT NULL
);

CREATE TABLE t1_class1_pa6 (

   _id bigserial NOT NULL PRIMARY KEY,
   t1_class1_id bigint NOT NULL,
   t1_class2_id bigint NOT NULL
);

CREATE TABLE t1_class1_pa7 (

   _id bigserial NOT NULL PRIMARY KEY,
   t1_class1_id bigint NOT NULL,
   t1_class2_id bigint NOT NULL
);

CREATE TABLE t1_class1_px (

   _id bigserial NOT NULL PRIMARY KEY,
   t1_class1_id bigint NOT NULL,
   px text NOT NULL
);

CREATE TABLE t1_class2 (

   _id bigserial NOT NULL PRIMARY KEY,
   pb3_fk bigint NOT NULL,
   pb6_1_fk bigint NOT NULL,
   pb6_2_fk bigint
);

CREATE TABLE t2_class1 (

   _id bigserial NOT NULL PRIMARY KEY,
   pnormal text NOT NULL
);

CREATE TABLE t3_class1 (

   _id bigserial NOT NULL PRIMARY KEY,
   p1 integer NOT NULL,
   p5_fkdt bigint NOT NULL,
   p7_fk bigint NOT NULL
);

CREATE TABLE t3_class1_p2 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3c2p1 text NOT NULL,
   t3_class1_id bigint NOT NULL
);

CREATE TABLE t3_class1_p2_t3c2p2 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3c2subp1 text NOT NULL,
   t3_class2_id bigint NOT NULL
);

CREATE TABLE t3_class1_p2_t3c2p3 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3c2subp1 text NOT NULL,
   t3_class2_id bigint NOT NULL
);

CREATE TABLE t3_class1_p3 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3c3p2 boolean NOT NULL,
   t3_class1_id bigint NOT NULL
);

CREATE TABLE t3_class1_p3_t3c3p1 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3_class3_id bigint NOT NULL,
   t3c3p1 text NOT NULL
);

CREATE TABLE t3_class1_p4_t3_class4 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3c4p1 integer NOT NULL,
   t3c4p2 numeric NOT NULL,
   t3_class1_id bigint NOT NULL
);

CREATE TABLE t3_class1_p4_t3_class4_t3c4p3 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3_class4_id bigint NOT NULL,
   t3c4p3 integer NOT NULL
);

CREATE TABLE t3_class1_p4_t3_class4sub (

   _id bigserial NOT NULL PRIMARY KEY,
   t3c4p1 integer NOT NULL,
   t3c4p2 numeric NOT NULL,
   t3c4subp1 text NOT NULL,
   t3_class1_id bigint NOT NULL
);

CREATE TABLE t3_class1_p4_t3_class4sub_t3c4p3 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3_class4sub_id bigint NOT NULL,
   t3c4p3 integer NOT NULL
);

CREATE TABLE t3_class1_p4_t3_class4sub_t3c4subp2 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3_class4sub_id bigint NOT NULL,
   t3c4subp2 text NOT NULL
);

CREATE TABLE t3_class1_p6 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3_class1_id bigint NOT NULL,
   tb_extdtinmodel_id bigint NOT NULL
);

CREATE TABLE t3_class1_p8 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3_class1_id bigint NOT NULL,
   tb_extdtoutsidemodel_id bigint NOT NULL
);

CREATE TABLE t3_class1_p9_t3_class4 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3c4p1 integer NOT NULL,
   t3c4p2 numeric NOT NULL,
   t3_class1_id bigint NOT NULL
);

CREATE TABLE t3_class1_p9_t3_class4_t3c4p3 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3_class4_id bigint NOT NULL,
   t3c4p3 integer NOT NULL
);

CREATE TABLE t3_class1_p9_t3_class4sub (

   _id bigserial NOT NULL PRIMARY KEY,
   t3c4p1 integer NOT NULL,
   t3c4p2 numeric NOT NULL,
   t3c4subp1 text NOT NULL,
   t3_class1_id bigint NOT NULL
);

CREATE TABLE t3_class1_p9_t3_class4sub_t3c4p3 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3_class4sub_id bigint NOT NULL,
   t3c4p3 integer NOT NULL
);

CREATE TABLE t3_class1_p9_t3_class4sub_t3c4subp2 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3_class4sub_id bigint NOT NULL,
   t3c4subp2 text NOT NULL
);

CREATE TABLE t4_class1 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE t4_class1_t4c1p1 (

   _id bigserial NOT NULL PRIMARY KEY,
   t4_class1_id bigint NOT NULL,
   tb_extft_id bigint NOT NULL
);

CREATE TABLE t4c1toextft (

   _id bigserial NOT NULL PRIMARY KEY,
   t4_class1_id bigint NOT NULL,
   tb_extft_id bigint NOT NULL
);

CREATE TABLE t5_class1 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE t5_class1_t5c1p1 (

   _id bigserial NOT NULL PRIMARY KEY,
   t5_class1_id bigint NOT NULL,
   t5c1p1 text NOT NULL
);

CREATE TABLE t5_class2 (

   _id bigserial NOT NULL PRIMARY KEY,
   t5c2p1 text NOT NULL
);

CREATE TABLE t5_class2_t5c1p1 (

   _id bigserial NOT NULL PRIMARY KEY,
   t5_class2_id bigint NOT NULL,
   t5c1p1 text NOT NULL
);

CREATE TABLE t6_class1 (

   _id bigserial NOT NULL PRIMARY KEY,
   feature1a_reflexive bigint NOT NULL,
   feature2_fk bigint NOT NULL
);

CREATE TABLE t6_class1_feature1b_target (

   _id bigserial NOT NULL PRIMARY KEY,
   t6_class1_feature1b_source_id bigint NOT NULL,
   t6_class1_feature1b_target_id bigint NOT NULL
);

CREATE TABLE t6_class2 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE t7_class1 (

   _id bigserial NOT NULL PRIMARY KEY,
   att1_fkcl character varying(50) NOT NULL
);

CREATE TABLE t7_class1_att2 (

   _id bigserial NOT NULL PRIMARY KEY,
   t7_class1_id bigint NOT NULL,
   t7_codelist_id character varying(50) NOT NULL
);

CREATE TABLE t7_class1_att3 (

   _id bigserial NOT NULL PRIMARY KEY,
   attdt1_fkcl character varying(50) NOT NULL,
   t7_class1_id bigint NOT NULL
);

CREATE TABLE t7_class1_att3_attdt2 (

   _id bigserial NOT NULL PRIMARY KEY,
   t7_datatype_id bigint NOT NULL,
   t7_codelist_id character varying(50) NOT NULL
);

CREATE TABLE t7_codelist (

   myname character varying(50) NOT NULL PRIMARY KEY,
   myalias character varying(255),
   mydefinition text,
   mydescription text
);

CREATE TABLE t8_class1 (

   _id bigserial NOT NULL PRIMARY KEY,
   pmixa2 boolean NOT NULL,
   pmixa1 integer NOT NULL,
   pmixb numeric NOT NULL,
   attc1 integer NOT NULL
);

CREATE TABLE t8_class1_attc1mult (

   _id bigserial NOT NULL PRIMARY KEY,
   t8_class1_id bigint NOT NULL,
   attc1mult integer NOT NULL
);

CREATE TABLE t8_class1_pmixa2mult (

   _id bigserial NOT NULL PRIMARY KEY,
   t8_class1_id bigint NOT NULL,
   pmixa2mult integer NOT NULL
);

CREATE TABLE t8_class1_role1to2_t8_class2sub (

   _id bigserial NOT NULL PRIMARY KEY,
   t8_class1_id bigint NOT NULL,
   t8_class2sub_id bigint NOT NULL
);

CREATE TABLE t8_class1sub (

   _id bigserial NOT NULL PRIMARY KEY,
   pmixa2 boolean NOT NULL,
   pmixa1 integer NOT NULL,
   pmixb numeric NOT NULL,
   attc1 integer NOT NULL,
   pmixd integer NOT NULL,
   pmixc text NOT NULL,
   attc1sub text NOT NULL
);

CREATE TABLE t8_class1sub_attc1mult (

   _id bigserial NOT NULL PRIMARY KEY,
   t8_class1sub_id bigint NOT NULL,
   attc1mult integer NOT NULL
);

CREATE TABLE t8_class1sub_attc1submult (

   _id bigserial NOT NULL PRIMARY KEY,
   t8_class1sub_id bigint NOT NULL,
   attc1submult text NOT NULL
);

CREATE TABLE t8_class1sub_pmixa2mult (

   _id bigserial NOT NULL PRIMARY KEY,
   t8_class1sub_id bigint NOT NULL,
   pmixa2mult integer NOT NULL
);

CREATE TABLE t8_class1sub_role1to2_t8_class2sub (

   _id bigserial NOT NULL PRIMARY KEY,
   t8_class1sub_id bigint NOT NULL,
   t8_class2sub_id bigint NOT NULL
);

CREATE TABLE t8_class2sub (

   _id bigserial NOT NULL PRIMARY KEY,
   attc2 numeric NOT NULL,
   attc2sub boolean NOT NULL
);

CREATE TABLE tablepb1pa3 (

   _id bigserial NOT NULL PRIMARY KEY,
   t1_class1_id bigint NOT NULL,
   t1_class2_id bigint NOT NULL
);


ALTER TABLE c1py ADD CONSTRAINT fk_c1py_t1_class1_id FOREIGN KEY (t1_class1_id) REFERENCES t1_class1;
ALTER TABLE t1_class1 ADD CONSTRAINT fk_t1_class1_pa2_fk FOREIGN KEY (pa2_fk) REFERENCES t1_class2;
ALTER TABLE t1_class1 ADD CONSTRAINT fk_t1_class1_pa4_fk FOREIGN KEY (pa4_fk) REFERENCES t1_class2;
ALTER TABLE t1_class1 ADD CONSTRAINT fk_t1_class1_pa5_fk FOREIGN KEY (pa5_fk) REFERENCES t1_class2;
ALTER TABLE t1_class1_pa1 ADD CONSTRAINT fk_t1_class1_pa1_t1_class1_id FOREIGN KEY (t1_class1_id) REFERENCES t1_class1;
ALTER TABLE t1_class1_pa1 ADD CONSTRAINT fk_t1_class1_pa1_t1_class2_id FOREIGN KEY (t1_class2_id) REFERENCES t1_class2;
ALTER TABLE t1_class1_pa6 ADD CONSTRAINT fk_t1_class1_pa6_t1_class1_id FOREIGN KEY (t1_class1_id) REFERENCES t1_class1;
ALTER TABLE t1_class1_pa6 ADD CONSTRAINT fk_t1_class1_pa6_t1_class2_id FOREIGN KEY (t1_class2_id) REFERENCES t1_class2;
ALTER TABLE t1_class1_pa7 ADD CONSTRAINT fk_t1_class1_pa7_t1_class1_id FOREIGN KEY (t1_class1_id) REFERENCES t1_class1;
ALTER TABLE t1_class1_pa7 ADD CONSTRAINT fk_t1_class1_pa7_t1_class2_id FOREIGN KEY (t1_class2_id) REFERENCES t1_class2;
ALTER TABLE t1_class1_px ADD CONSTRAINT fk_t1_class1_px_t1_class1_id FOREIGN KEY (t1_class1_id) REFERENCES t1_class1;
ALTER TABLE t1_class2 ADD CONSTRAINT fk_t1_class2_pb3_fk FOREIGN KEY (pb3_fk) REFERENCES t1_class1;
ALTER TABLE t1_class2 ADD CONSTRAINT fk_t1_class2_pb6_1_fk FOREIGN KEY (pb6_1_fk) REFERENCES t1_class1;
ALTER TABLE t1_class2 ADD CONSTRAINT fk_t1_class2_pb6_2_fk FOREIGN KEY (pb6_2_fk) REFERENCES t1_class1;
ALTER TABLE t3_class1 ADD CONSTRAINT fk_t3_class1_p5_fkdt FOREIGN KEY (p5_fkdt) REFERENCES tb_extdtinmodel;
ALTER TABLE t3_class1 ADD CONSTRAINT fk_t3_class1_p7_fk FOREIGN KEY (p7_fk) REFERENCES tb_extdtoutsidemodel;
ALTER TABLE t3_class1_p2 ADD CONSTRAINT fk_t3_class1_p2_t3_class1_id FOREIGN KEY (t3_class1_id) REFERENCES t3_class1;
ALTER TABLE t3_class1_p2_t3c2p2 ADD CONSTRAINT fk_t3_class1_p2_t3c2p2_t3_class2_id FOREIGN KEY (t3_class2_id) REFERENCES t3_class1_p2;
ALTER TABLE t3_class1_p2_t3c2p3 ADD CONSTRAINT fk_t3_class1_p2_t3c2p3_t3_class2_id FOREIGN KEY (t3_class2_id) REFERENCES t3_class1_p2;
ALTER TABLE t3_class1_p3 ADD CONSTRAINT fk_t3_class1_p3_t3_class1_id FOREIGN KEY (t3_class1_id) REFERENCES t3_class1;
ALTER TABLE t3_class1_p3_t3c3p1 ADD CONSTRAINT fk_t3_class1_p3_t3c3p1_t3_class3_id FOREIGN KEY (t3_class3_id) REFERENCES t3_class1_p3;
ALTER TABLE t3_class1_p4_t3_class4 ADD CONSTRAINT fk_t3_class1_p4_t3_class4_t3_class1_id FOREIGN KEY (t3_class1_id) REFERENCES t3_class1;
ALTER TABLE t3_class1_p4_t3_class4_t3c4p3 ADD CONSTRAINT fk_t3_class1_p4_t3_class4_t3c4p3_t3_class4_id FOREIGN KEY (t3_class4_id) REFERENCES t3_class1_p4_t3_class4;
ALTER TABLE t3_class1_p4_t3_class4sub ADD CONSTRAINT fk_t3_class1_p4_t3_class4sub_t3_class1_id FOREIGN KEY (t3_class1_id) REFERENCES t3_class1;
ALTER TABLE t3_class1_p4_t3_class4sub_t3c4p3 ADD CONSTRAINT fk_t3_class1_p4_t3_class4sub_t3c4p3_t3_class4sub_id FOREIGN KEY (t3_class4sub_id) REFERENCES t3_class1_p4_t3_class4sub;
ALTER TABLE t3_class1_p4_t3_class4sub_t3c4subp2 ADD CONSTRAINT fk_t3_class1_p4_t3_class4sub_t3c4subp2_t3_class4sub_id FOREIGN KEY (t3_class4sub_id) REFERENCES t3_class1_p4_t3_class4sub;
ALTER TABLE t3_class1_p6 ADD CONSTRAINT fk_t3_class1_p6_t3_class1_id FOREIGN KEY (t3_class1_id) REFERENCES t3_class1;
ALTER TABLE t3_class1_p6 ADD CONSTRAINT fk_t3_class1_p6_tb_extdtinmodel_id FOREIGN KEY (tb_extdtinmodel_id) REFERENCES tb_extdtinmodel;
ALTER TABLE t3_class1_p8 ADD CONSTRAINT fk_t3_class1_p8_t3_class1_id FOREIGN KEY (t3_class1_id) REFERENCES t3_class1;
ALTER TABLE t3_class1_p8 ADD CONSTRAINT fk_t3_class1_p8_tb_extdtoutsidemodel_id FOREIGN KEY (tb_extdtoutsidemodel_id) REFERENCES tb_extdtoutsidemodel;
ALTER TABLE t3_class1_p9_t3_class4 ADD CONSTRAINT fk_t3_class1_p9_t3_class4_t3_class1_id FOREIGN KEY (t3_class1_id) REFERENCES t3_class1;
ALTER TABLE t3_class1_p9_t3_class4_t3c4p3 ADD CONSTRAINT fk_t3_class1_p9_t3_class4_t3c4p3_t3_class4_id FOREIGN KEY (t3_class4_id) REFERENCES t3_class1_p9_t3_class4;
ALTER TABLE t3_class1_p9_t3_class4sub ADD CONSTRAINT fk_t3_class1_p9_t3_class4sub_t3_class1_id FOREIGN KEY (t3_class1_id) REFERENCES t3_class1;
ALTER TABLE t3_class1_p9_t3_class4sub_t3c4p3 ADD CONSTRAINT fk_t3_class1_p9_t3_class4sub_t3c4p3_t3_class4sub_id FOREIGN KEY (t3_class4sub_id) REFERENCES t3_class1_p9_t3_class4sub;
ALTER TABLE t3_class1_p9_t3_class4sub_t3c4subp2 ADD CONSTRAINT fk_t3_class1_p9_t3_class4sub_t3c4subp2_t3_class4sub_id FOREIGN KEY (t3_class4sub_id) REFERENCES t3_class1_p9_t3_class4sub;
ALTER TABLE t4_class1_t4c1p1 ADD CONSTRAINT fk_t4_class1_t4c1p1_t4_class1_id FOREIGN KEY (t4_class1_id) REFERENCES t4_class1;
ALTER TABLE t4_class1_t4c1p1 ADD CONSTRAINT fk_t4_class1_t4c1p1_tb_extft_id FOREIGN KEY (tb_extft_id) REFERENCES tb_extft;
ALTER TABLE t4c1toextft ADD CONSTRAINT fk_t4c1toextft_t4_class1_id FOREIGN KEY (t4_class1_id) REFERENCES t4_class1;
ALTER TABLE t4c1toextft ADD CONSTRAINT fk_t4c1toextft_tb_extft_id FOREIGN KEY (tb_extft_id) REFERENCES tb_extft;
ALTER TABLE t5_class1_t5c1p1 ADD CONSTRAINT fk_t5_class1_t5c1p1_t5_class1_id FOREIGN KEY (t5_class1_id) REFERENCES t5_class1;
ALTER TABLE t5_class1_t5c1p1 ADD CONSTRAINT t5_class1_t5c1p1_t5c1p1_chk CHECK (t5c1p1 IN ('1000', '2000'));
ALTER TABLE t5_class2 ADD CONSTRAINT t5_class2_t5c2p1_chk CHECK (t5c2p1 IN ('1000', '2000'));
ALTER TABLE t5_class2_t5c1p1 ADD CONSTRAINT fk_t5_class2_t5c1p1_t5_class2_id FOREIGN KEY (t5_class2_id) REFERENCES t5_class2;
ALTER TABLE t5_class2_t5c1p1 ADD CONSTRAINT t5_class2_t5c1p1_t5c1p1_chk CHECK (t5c1p1 IN ('1000', '2000'));
ALTER TABLE t6_class1 ADD CONSTRAINT fk_t6_class1_feature1a_reflexive FOREIGN KEY (feature1a_reflexive) REFERENCES t6_class1;
ALTER TABLE t6_class1 ADD CONSTRAINT fk_t6_class1_feature2_fk FOREIGN KEY (feature2_fk) REFERENCES t6_class2;
ALTER TABLE t6_class1_feature1b_target ADD CONSTRAINT fk_t6_class1_feature1b_target_t6_class1_feature1b_source_id FOREIGN KEY (t6_class1_feature1b_source_id) REFERENCES t6_class1;
ALTER TABLE t6_class1_feature1b_target ADD CONSTRAINT fk_t6_class1_feature1b_target_t6_class1_feature1b_target_id FOREIGN KEY (t6_class1_feature1b_target_id) REFERENCES t6_class1;
ALTER TABLE t7_class1 ADD CONSTRAINT fk_t7_class1_att1_fkcl FOREIGN KEY (att1_fkcl) REFERENCES t7_codelist;
ALTER TABLE t7_class1_att2 ADD CONSTRAINT fk_t7_class1_att2_t7_class1_id FOREIGN KEY (t7_class1_id) REFERENCES t7_class1;
ALTER TABLE t7_class1_att2 ADD CONSTRAINT fk_t7_class1_att2_t7_codelist_id FOREIGN KEY (t7_codelist_id) REFERENCES t7_codelist;
ALTER TABLE t7_class1_att3 ADD CONSTRAINT fk_t7_class1_att3_attdt1_fkcl FOREIGN KEY (attdt1_fkcl) REFERENCES t7_codelist;
ALTER TABLE t7_class1_att3 ADD CONSTRAINT fk_t7_class1_att3_t7_class1_id FOREIGN KEY (t7_class1_id) REFERENCES t7_class1;
ALTER TABLE t7_class1_att3_attdt2 ADD CONSTRAINT fk_t7_class1_att3_attdt2_t7_codelist_id FOREIGN KEY (t7_codelist_id) REFERENCES t7_codelist;
ALTER TABLE t7_class1_att3_attdt2 ADD CONSTRAINT fk_t7_class1_att3_attdt2_t7_datatype_id FOREIGN KEY (t7_datatype_id) REFERENCES t7_class1_att3;
ALTER TABLE t8_class1_attc1mult ADD CONSTRAINT fk_t8_class1_attc1mult_t8_class1_id FOREIGN KEY (t8_class1_id) REFERENCES t8_class1;
ALTER TABLE t8_class1_pmixa2mult ADD CONSTRAINT fk_t8_class1_pmixa2mult_t8_class1_id FOREIGN KEY (t8_class1_id) REFERENCES t8_class1;
ALTER TABLE t8_class1_role1to2_t8_class2sub ADD CONSTRAINT fk_t8_class1_role1to2_t8_class2sub_t8_class1_id FOREIGN KEY (t8_class1_id) REFERENCES t8_class1;
ALTER TABLE t8_class1_role1to2_t8_class2sub ADD CONSTRAINT fk_t8_class1_role1to2_t8_class2sub_t8_class2sub_id FOREIGN KEY (t8_class2sub_id) REFERENCES t8_class2sub;
ALTER TABLE t8_class1sub_attc1mult ADD CONSTRAINT fk_t8_class1sub_attc1mult_t8_class1sub_id FOREIGN KEY (t8_class1sub_id) REFERENCES t8_class1sub;
ALTER TABLE t8_class1sub_attc1submult ADD CONSTRAINT fk_t8_class1sub_attc1submult_t8_class1sub_id FOREIGN KEY (t8_class1sub_id) REFERENCES t8_class1sub;
ALTER TABLE t8_class1sub_pmixa2mult ADD CONSTRAINT fk_t8_class1sub_pmixa2mult_t8_class1sub_id FOREIGN KEY (t8_class1sub_id) REFERENCES t8_class1sub;
ALTER TABLE t8_class1sub_role1to2_t8_class2sub ADD CONSTRAINT fk_t8_class1sub_role1to2_t8_class2sub_t8_class1sub_id FOREIGN KEY (t8_class1sub_id) REFERENCES t8_class1sub;
ALTER TABLE t8_class1sub_role1to2_t8_class2sub ADD CONSTRAINT fk_t8_class1sub_role1to2_t8_class2sub_t8_class2sub_id FOREIGN KEY (t8_class2sub_id) REFERENCES t8_class2sub;
ALTER TABLE tablepb1pa3 ADD CONSTRAINT fk_tablepb1pa3_t1_class1_id FOREIGN KEY (t1_class1_id) REFERENCES t1_class1;
ALTER TABLE tablepb1pa3 ADD CONSTRAINT fk_tablepb1pa3_t1_class2_id FOREIGN KEY (t1_class2_id) REFERENCES t1_class2;

CREATE INDEX idx_t1_class1_pz ON t1_class1 USING GIST (pz);
