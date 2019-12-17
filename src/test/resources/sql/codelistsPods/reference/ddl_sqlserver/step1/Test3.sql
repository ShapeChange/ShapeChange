CREATE TABLE T3_CodeList1 (

   CODE nvarchar(50) NOT NULL PRIMARY KEY NONCLUSTERED,
   DESCRIPTION nvarchar(255),
   CODE_STATUS_CL nvarchar(50),
   CODE_STATUS_NOTES nvarchar(255),
   CODE_SUPERCEDES nvarchar(50)
);

CREATE TABLE T3_CodeList2 (

   CODE nvarchar(50) NOT NULL PRIMARY KEY NONCLUSTERED,
   DESCRIPTION nvarchar(255),
   CODE_STATUS_CL nvarchar(50),
   CODE_STATUS_NOTES nvarchar(255),
   CODE_SUPERCEDES nvarchar(50)
);

CREATE TABLE T3_CodeStatusCl (

   CODE nvarchar(50) NOT NULL PRIMARY KEY NONCLUSTERED,
   DESCRIPTION nvarchar(255)
);

CREATE TABLE T3_FeatureType1 (

   _id bigint NOT NULL PRIMARY KEY,
   propNumCode nvarchar(50) -- DEF: propNumCode definition; DESC: propNumCode description; EX: propNumCode example 1 propNumCode example 2; LB: propNumCode legal basis; DCS: propNumCode data capture statement 1 propNumCode data capture statement 2; PC: propNumCode primary code
);

CREATE TABLE T3_FeatureType1_propAlpCode (

   T3_FeatureType1_id bigint NOT NULL,
   T3_CodeList2_id nvarchar(50) NOT NULL, -- DEF: ; DESC: ; EX: ; LB: ; DCS: ; PC:
   PRIMARY KEY (T3_FeatureType1_id, T3_CodeList2_id)
);


ALTER TABLE T3_CodeList1 ADD CONSTRAINT fk_T3_CodeL_T3_CodeS_CODE_STA FOREIGN KEY (CODE_STATUS_CL) REFERENCES T3_CodeStatusCl;
ALTER TABLE T3_CodeList2 ADD CONSTRAINT fk_T3_CodeL_T3_CodeS_CODE_STA0 FOREIGN KEY (CODE_STATUS_CL) REFERENCES T3_CodeStatusCl;
ALTER TABLE T3_FeatureType1 ADD CONSTRAINT fk_T3_Featu_T3_CodeL_propNumC FOREIGN KEY (propNumCode) REFERENCES T3_CodeList1;
ALTER TABLE T3_FeatureType1_propAlpCode ADD CONSTRAINT fk_T3_Featu_T3_CodeL_T3_CodeL FOREIGN KEY (T3_CodeList2_id) REFERENCES T3_CodeList2;
ALTER TABLE T3_FeatureType1_propAlpCode ADD CONSTRAINT fk_T3_Featu_T3_Featu_T3_Featu FOREIGN KEY (T3_FeatureType1_id) REFERENCES T3_FeatureType1;

INSERT INTO T3_CodeStatusCl (CODE, DESCRIPTION) VALUES ('valid', '');
INSERT INTO T3_CodeStatusCl (CODE, DESCRIPTION) VALUES ('unknown', '');
INSERT INTO T3_CodeStatusCl (CODE, DESCRIPTION) VALUES ('invalid', '');
INSERT INTO T3_CodeList1 (CODE, DESCRIPTION, CODE_STATUS_CL, CODE_STATUS_NOTES, CODE_SUPERCEDES) VALUES ('1', '', 'valid', NULL, NULL);
INSERT INTO T3_CodeList1 (CODE, DESCRIPTION, CODE_STATUS_CL, CODE_STATUS_NOTES, CODE_SUPERCEDES) VALUES ('2', '', 'valid', NULL, NULL);
INSERT INTO T3_CodeList1 (CODE, DESCRIPTION, CODE_STATUS_CL, CODE_STATUS_NOTES, CODE_SUPERCEDES) VALUES ('quote''me', '', 'valid', NULL, NULL);
INSERT INTO T3_CodeList2 (CODE, DESCRIPTION, CODE_STATUS_CL, CODE_STATUS_NOTES, CODE_SUPERCEDES) VALUES ('code''B', 'codeB definition', 'valid', NULL, NULL);
INSERT INTO T3_CodeList2 (CODE, DESCRIPTION, CODE_STATUS_CL, CODE_STATUS_NOTES, CODE_SUPERCEDES) VALUES ('codeA', 'codeA''s definition', 'valid', NULL, NULL);
