CREATE TABLE F1 (

   _id bigint NOT NULL PRIMARY KEY,
   type nvarchar(max) NOT NULL, -- Notes propertyType
   spec nvarchar(max) NOT NULL, -- Notes propertySpecies
   pMix1 int DEFAULT 300 NOT NULL, -- Notes propertyMix1
   pMix2_1 nvarchar(50) NOT NULL, -- Notes propertyMix2
   pMix2_2 nvarchar(50), -- Notes propertyMix2
   pMix2_3 nvarchar(50), -- Notes propertyMix2
   pMix2_4 nvarchar(50), -- Notes propertyMix2
   pMix2_5 nvarchar(50), -- Notes propertyMix2
   p1 nvarchar(max) NOT NULL, -- Notes property1
   geom geometry NOT NULL, -- Notes geometry (GM_Point)
   pExt_1 nvarchar(max), -- Notes propExternal
   pExt_2 nvarchar(max), -- Notes propExternal
   pF2AC_1 bigint, -- Notes propFeature2_AssocClass
   pF2AC_2 bigint, -- Notes propFeature2_AssocClass
   pF2AC_3 bigint, -- Notes propFeature2_AssocClass
   pF2_1 bigint, -- Notes propFeature2
   pF2_2 bigint, -- Notes propFeature2
   pF2_3 bigint -- Notes propFeature2
);

CREATE TABLE F2_SUFFIX (

   _id bigint NOT NULL PRIMARY KEY,
   type nvarchar(max) NOT NULL, -- Notes propertyType
   spec nvarchar(max) NOT NULL, -- Notes propertySpecies
   pMix3 bit, -- Notes propertyMix3
   geom geometry, -- Notes geometry (GM_MultiSurface)
   pF1 bigint NOT NULL, -- Notes propFeature1
   pF1AC bigint -- Notes propFeature1_AssocClass
);

CREATE TABLE F3_SUFFIX (

   _id bigint NOT NULL PRIMARY KEY,
   geom geometry NOT NULL, -- Notes geometry (GM_Object)
   type nvarchar(max) NOT NULL, -- Notes propertyType
   p1 bit DEFAULT 1 NOT NULL, -- Notes property1
   pAFB_F1 bigint, -- Notes propAFeatureB
   p2_f2 bigint, -- Notes property2 (MixinB)
   p2_f4 bigint, -- Notes property2 (MixinB)
   pAFB_F2 bigint -- Notes propAFeatureB
);

CREATE TABLE F4 (

   _id bigint NOT NULL PRIMARY KEY,
   geom geometry NOT NULL, -- Notes geometry (GM_Object)
   type nvarchar(max) NOT NULL, -- Notes propertyType
   p1 bit DEFAULT 1 NOT NULL, -- Notes property1
   pMix3 bit, -- Notes propertyMix3
   pAFB_F1 bigint, -- Notes propAFeatureB
   pAFB_F2 bigint -- Notes propAFeatureB
);


ALTER TABLE F1 ADD CONSTRAINT fk_F1_pF2AC_1 FOREIGN KEY (pF2AC_1) REFERENCES F2_SUFFIX;
ALTER TABLE F1 ADD CONSTRAINT fk_F1_pF2AC_2 FOREIGN KEY (pF2AC_2) REFERENCES F2_SUFFIX;
ALTER TABLE F1 ADD CONSTRAINT fk_F1_pF2AC_3 FOREIGN KEY (pF2AC_3) REFERENCES F2_SUFFIX;
ALTER TABLE F1 ADD CONSTRAINT fk_F1_pF2_1 FOREIGN KEY (pF2_1) REFERENCES F2_SUFFIX;
ALTER TABLE F1 ADD CONSTRAINT fk_F1_pF2_2 FOREIGN KEY (pF2_2) REFERENCES F2_SUFFIX;
ALTER TABLE F1 ADD CONSTRAINT fk_F1_pF2_3 FOREIGN KEY (pF2_3) REFERENCES F2_SUFFIX;
ALTER TABLE F2_SUFFIX ADD CONSTRAINT fk_F2_SUFFIX_pF1 FOREIGN KEY (pF1) REFERENCES F1;
ALTER TABLE F2_SUFFIX ADD CONSTRAINT fk_F2_SUFFIX_pF1AC FOREIGN KEY (pF1AC) REFERENCES F1;
ALTER TABLE F3_SUFFIX ADD CONSTRAINT fk_F3_SUFFIX_p2_f2 FOREIGN KEY (p2_f2) REFERENCES F2_SUFFIX;
ALTER TABLE F3_SUFFIX ADD CONSTRAINT fk_F3_SUFFIX_p2_f4 FOREIGN KEY (p2_f4) REFERENCES F4;
ALTER TABLE F3_SUFFIX ADD CONSTRAINT fk_F3_SUFFIX_pAFB_F1 FOREIGN KEY (pAFB_F1) REFERENCES F1;
ALTER TABLE F3_SUFFIX ADD CONSTRAINT fk_F3_SUFFIX_pAFB_F2 FOREIGN KEY (pAFB_F2) REFERENCES F2_SUFFIX;
ALTER TABLE F4 ADD CONSTRAINT fk_F4_pAFB_F1 FOREIGN KEY (pAFB_F1) REFERENCES F1;
ALTER TABLE F4 ADD CONSTRAINT fk_F4_pAFB_F2 FOREIGN KEY (pAFB_F2) REFERENCES F2_SUFFIX;

CREATE SPATIAL INDEX idx_F1_geom ON F1 (geom) USING GEOMETRY_AUTO_GRID WITH (BOUNDING_BOX = (-1000,-1000,1000,1000));
CREATE SPATIAL INDEX idx_F2_SUFFIX_geom ON F2_SUFFIX (geom);
CREATE SPATIAL INDEX idx_F3_SUFFIX_geom ON F3_SUFFIX (geom);
CREATE SPATIAL INDEX idx_F4_geom ON F4 (geom);
