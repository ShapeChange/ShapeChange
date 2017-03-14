CREATE TABLE c1py (

   t1_class1_id bigserial NOT NULL,
   py text NOT NULL,
   PRIMARY KEY (t1_class1_id, py)
);

CREATE TABLE t1_class1 (

   _id bigserial NOT NULL PRIMARY KEY,
   pa2_fk bigserial NOT NULL,
   pa4_fk bigserial NOT NULL,
   pa5_fk bigserial NOT NULL
);

CREATE TABLE t1_class1_pa1 (

   t1_class1_id bigserial NOT NULL,
   t1_class2_id bigserial NOT NULL,
   PRIMARY KEY (t1_class1_id, t1_class2_id)
);

CREATE TABLE t1_class1_pa6 (

   t1_class1_id bigserial NOT NULL,
   t1_class2_id bigserial NOT NULL,
   PRIMARY KEY (t1_class1_id, t1_class2_id)
);

CREATE TABLE t1_class1_pa7 (

   t1_class1_id bigserial NOT NULL,
   t1_class2_id bigserial NOT NULL,
   PRIMARY KEY (t1_class1_id, t1_class2_id)
);

CREATE TABLE t1_class1_px (

   t1_class1_id bigserial NOT NULL,
   px text NOT NULL,
   PRIMARY KEY (t1_class1_id, px)
);

CREATE TABLE t1_class2 (

   _id bigserial NOT NULL PRIMARY KEY,
   pb3_fk bigserial NOT NULL,
   pb6_1_fk bigserial NOT NULL,
   pb6_2_fk bigserial,
   pb6_3_fk bigserial
);

CREATE TABLE t2_class1 (

   _id bigserial NOT NULL PRIMARY KEY,
   pnormal text NOT NULL
);

CREATE TABLE t3_class1 (

   _id bigserial NOT NULL PRIMARY KEY,
   p1 integer NOT NULL,
   p2_t3c2p1 text NOT NULL,
   p3_fkdt bigserial NOT NULL,
   p5_fkdt bigserial NOT NULL,
   p7_fk bigserial NOT NULL
);

CREATE TABLE t3_class1_p4 (

   t3_class1_id bigserial NOT NULL,
   t3_class4_id bigserial NOT NULL,
   PRIMARY KEY (t3_class1_id, t3_class4_id)
);

CREATE TABLE t3_class1_p6 (

   t3_class1_id bigserial NOT NULL,
   tb_extdtinmodel_id bigserial NOT NULL,
   PRIMARY KEY (t3_class1_id, tb_extdtinmodel_id)
);

CREATE TABLE t3_class1_p8 (

   t3_class1_id bigserial NOT NULL,
   tb_extdtoutsidemodel_id bigserial NOT NULL,
   PRIMARY KEY (t3_class1_id, tb_extdtoutsidemodel_id)
);

CREATE TABLE t3_class3 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3c3p1 text NOT NULL,
   t3c3p2 boolean NOT NULL
);

CREATE TABLE t3_class4 (

   _id bigserial NOT NULL PRIMARY KEY,
   t3c4p1 integer NOT NULL,
   t3c4p2 numeric NOT NULL
);

CREATE TABLE t4_class1 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE t4_class1_t4c1p1 (

   t4_class1_id bigserial NOT NULL,
   tb_extft_id bigserial NOT NULL,
   PRIMARY KEY (t4_class1_id, tb_extft_id)
);

CREATE TABLE t4c1toextft (

   t4_class1_id bigserial NOT NULL,
   tb_extft_id bigserial NOT NULL,
   PRIMARY KEY (t4_class1_id, tb_extft_id)
);

CREATE TABLE t5_class1 (

   _id bigserial NOT NULL PRIMARY KEY
);

CREATE TABLE t5_class1_t5c1p1 (

   t5_class1_id bigserial NOT NULL,
   t5c1p1 text NOT NULL,
   PRIMARY KEY (t5_class1_id, t5c1p1)
);

CREATE TABLE t5_class2 (

   _id bigserial NOT NULL PRIMARY KEY,
   t5c2p1 text NOT NULL
);

CREATE TABLE t5_class2_t5c1p1 (

   t5_class2_id bigserial NOT NULL,
   t5c1p1 text NOT NULL,
   PRIMARY KEY (t5_class2_id, t5c1p1)
);

CREATE TABLE tablepb1pa3 (

   t1_class1_id bigserial NOT NULL,
   t1_class2_id bigserial NOT NULL,
   PRIMARY KEY (t1_class1_id, t1_class2_id)
);


ALTER TABLE c1py ADD CONSTRAINT fk_c1py_t1_class1_id FOREIGN KEY (t1_class1_id) REFERENCES t1_class1;
ALTER TABLE t1_class1 ADD CONSTRAINT fk_t1_class1_pa2 FOREIGN KEY (pa2_fk) REFERENCES t1_class2;
ALTER TABLE t1_class1 ADD CONSTRAINT fk_t1_class1_pa4 FOREIGN KEY (pa4_fk) REFERENCES t1_class2;
ALTER TABLE t1_class1 ADD CONSTRAINT fk_t1_class1_pa5 FOREIGN KEY (pa5_fk) REFERENCES t1_class2;
ALTER TABLE t1_class1_pa1 ADD CONSTRAINT fk_t1_class1_pa1_t1_class1_id FOREIGN KEY (t1_class1_id) REFERENCES t1_class1;
ALTER TABLE t1_class1_pa1 ADD CONSTRAINT fk_t1_class1_pa1_t1_class2_id FOREIGN KEY (t1_class2_id) REFERENCES t1_class2;
ALTER TABLE t1_class1_pa6 ADD CONSTRAINT fk_t1_class1_pa6_t1_class1_id FOREIGN KEY (t1_class1_id) REFERENCES t1_class1;
ALTER TABLE t1_class1_pa6 ADD CONSTRAINT fk_t1_class1_pa6_t1_class2_id FOREIGN KEY (t1_class2_id) REFERENCES t1_class2;
ALTER TABLE t1_class1_pa7 ADD CONSTRAINT fk_t1_class1_pa7_t1_class1_id FOREIGN KEY (t1_class1_id) REFERENCES t1_class1;
ALTER TABLE t1_class1_pa7 ADD CONSTRAINT fk_t1_class1_pa7_t1_class2_id FOREIGN KEY (t1_class2_id) REFERENCES t1_class2;
ALTER TABLE t1_class1_px ADD CONSTRAINT fk_t1_class1_px_t1_class1_id FOREIGN KEY (t1_class1_id) REFERENCES t1_class1;
ALTER TABLE t1_class2 ADD CONSTRAINT fk_t1_class2_pb3 FOREIGN KEY (pb3_fk) REFERENCES t1_class1;
ALTER TABLE t1_class2 ADD CONSTRAINT fk_t1_class2_pb6_1 FOREIGN KEY (pb6_1_fk) REFERENCES t1_class1;
ALTER TABLE t1_class2 ADD CONSTRAINT fk_t1_class2_pb6_2 FOREIGN KEY (pb6_2_fk) REFERENCES t1_class1;
ALTER TABLE t1_class2 ADD CONSTRAINT fk_t1_class2_pb6_3 FOREIGN KEY (pb6_3_fk) REFERENCES t1_class1;
ALTER TABLE t3_class1 ADD CONSTRAINT fk_t3_class1_p3 FOREIGN KEY (p3_fkdt) REFERENCES t3_class3;
ALTER TABLE t3_class1 ADD CONSTRAINT fk_t3_class1_p5 FOREIGN KEY (p5_fkdt) REFERENCES tb_extdtinmodel;
ALTER TABLE t3_class1 ADD CONSTRAINT fk_t3_class1_p7 FOREIGN KEY (p7_fk) REFERENCES tb_extdtoutsidemodel;
ALTER TABLE t3_class1_p4 ADD CONSTRAINT fk_t3_class1_p4_t3_class1_id FOREIGN KEY (t3_class1_id) REFERENCES t3_class1;
ALTER TABLE t3_class1_p4 ADD CONSTRAINT fk_t3_class1_p4_t3_class4_id FOREIGN KEY (t3_class4_id) REFERENCES t3_class4;
ALTER TABLE t3_class1_p6 ADD CONSTRAINT fk_t3_class1_p6_t3_class1_id FOREIGN KEY (t3_class1_id) REFERENCES t3_class1;
ALTER TABLE t3_class1_p6 ADD CONSTRAINT fk_t3_class1_p6_tb_extdtinmodel_id FOREIGN KEY (tb_extdtinmodel_id) REFERENCES tb_extdtinmodel;
ALTER TABLE t3_class1_p8 ADD CONSTRAINT fk_t3_class1_p8_t3_class1_id FOREIGN KEY (t3_class1_id) REFERENCES t3_class1;
ALTER TABLE t3_class1_p8 ADD CONSTRAINT fk_t3_class1_p8_tb_extdtoutsidemodel_id FOREIGN KEY (tb_extdtoutsidemodel_id) REFERENCES tb_extdtoutsidemodel;
ALTER TABLE t4_class1_t4c1p1 ADD CONSTRAINT fk_t4_class1_t4c1p1_t4_class1_id FOREIGN KEY (t4_class1_id) REFERENCES t4_class1;
ALTER TABLE t4_class1_t4c1p1 ADD CONSTRAINT fk_t4_class1_t4c1p1_tb_extft_id FOREIGN KEY (tb_extft_id) REFERENCES tb_extft;
ALTER TABLE t4c1toextft ADD CONSTRAINT fk_t4c1toextft_t4_class1_id FOREIGN KEY (t4_class1_id) REFERENCES t4_class1;
ALTER TABLE t4c1toextft ADD CONSTRAINT fk_t4c1toextft_tb_extft_id FOREIGN KEY (tb_extft_id) REFERENCES tb_extft;
ALTER TABLE t5_class1_t5c1p1 ADD CONSTRAINT fk_t5_class1_t5c1p1_t5_class1_id FOREIGN KEY (t5_class1_id) REFERENCES t5_class1;
ALTER TABLE t5_class1_t5c1p1 ADD CONSTRAINT t5_class1_t5c1p1_t5c1p1_chk CHECK (t5c1p1 IN ('e1val1', 'e1val2'));
ALTER TABLE t5_class2 ADD CONSTRAINT t5_class2_t5c2p1_chk CHECK (t5c2p1 IN ('e2val1', 'e2val2'));
ALTER TABLE t5_class2_t5c1p1 ADD CONSTRAINT fk_t5_class2_t5c1p1_t5_class2_id FOREIGN KEY (t5_class2_id) REFERENCES t5_class2;
ALTER TABLE t5_class2_t5c1p1 ADD CONSTRAINT t5_class2_t5c1p1_t5c1p1_chk CHECK (t5c1p1 IN ('e1val1', 'e1val2'));
ALTER TABLE tablepb1pa3 ADD CONSTRAINT fk_tablepb1pa3_t1_class1_id FOREIGN KEY (t1_class1_id) REFERENCES t1_class1;
ALTER TABLE tablepb1pa3 ADD CONSTRAINT fk_tablepb1pa3_t1_class2_id FOREIGN KEY (t1_class2_id) REFERENCES t1_class2;
