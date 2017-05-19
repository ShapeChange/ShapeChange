CREATE TABLE T3_CodeList1 (

   CODE nvarchar(50) NOT NULL PRIMARY KEY NONCLUSTERED,
   DESCRIPTION nvarchar(255),
   NAME nvarchar(255),
   ACTIVE_INDICATOR_LF nvarchar(50),
   SOURCE_GCL nvarchar(16)
);

CREATE TABLE T3_CodeList2 (

   CODE nvarchar(50) NOT NULL PRIMARY KEY NONCLUSTERED,
   DESCRIPTION nvarchar(255),
   NAME nvarchar(255),
   ACTIVE_INDICATOR_LF nvarchar(50),
   SOURCE_GCL nvarchar(16)
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

CREATE TABLE T3_YesNoNacl (

   CODE nvarchar(50) NOT NULL PRIMARY KEY NONCLUSTERED,
   DESCRIPTION nvarchar(255),
   NAME nvarchar(255)
);


ALTER TABLE T3_CodeList1 ADD CONSTRAINT fk_T3_CodeL_T3_YesNo_ACTIVE_I FOREIGN KEY (ACTIVE_INDICATOR_LF) REFERENCES T3_YesNoNacl;
ALTER TABLE T3_CodeList2 ADD CONSTRAINT fk_T3_CodeL_T3_YesNo_ACTIVE_I0 FOREIGN KEY (ACTIVE_INDICATOR_LF) REFERENCES T3_YesNoNacl;
ALTER TABLE T3_FeatureType1 ADD CONSTRAINT fk_T3_Featu_T3_CodeL_propNumC FOREIGN KEY (propNumCode) REFERENCES T3_CodeList1;
ALTER TABLE T3_FeatureType1_propAlpCode ADD CONSTRAINT fk_T3_Featu_T3_CodeL_T3_CodeL FOREIGN KEY (T3_CodeList2_id) REFERENCES T3_CodeList2;
ALTER TABLE T3_FeatureType1_propAlpCode ADD CONSTRAINT fk_T3_Featu_T3_Featu_T3_Featu FOREIGN KEY (T3_FeatureType1_id) REFERENCES T3_FeatureType1;

INSERT INTO T3_YesNoNacl (CODE, DESCRIPTION, NAME) VALUES ('No', 'No', '');
INSERT INTO T3_YesNoNacl (CODE, DESCRIPTION, NAME) VALUES ('NotApplicable', 'NotApplicable', '');
INSERT INTO T3_YesNoNacl (CODE, DESCRIPTION, NAME) VALUES ('Yes', 'Yes', '');
INSERT INTO T3_CodeList1 (CODE, DESCRIPTION, NAME, ACTIVE_INDICATOR_LF, SOURCE_GCL) VALUES ('1', 'code1', '', 'Yes', NULL);
INSERT INTO T3_CodeList1 (CODE, DESCRIPTION, NAME, ACTIVE_INDICATOR_LF, SOURCE_GCL) VALUES ('2', 'code2', '', 'Yes', NULL);
INSERT INTO T3_CodeList1 (CODE, DESCRIPTION, NAME, ACTIVE_INDICATOR_LF, SOURCE_GCL) VALUES ('quote''me', 'code3', '', 'Yes', NULL);
INSERT INTO T3_CodeList2 (CODE, DESCRIPTION, NAME, ACTIVE_INDICATOR_LF, SOURCE_GCL) VALUES ('code''B', 'code''B', 'codeB definition', 'Yes', NULL);
INSERT INTO T3_CodeList2 (CODE, DESCRIPTION, NAME, ACTIVE_INDICATOR_LF, SOURCE_GCL) VALUES ('codeA', 'codeA', 'codeA''s definition', 'Yes', NULL);
