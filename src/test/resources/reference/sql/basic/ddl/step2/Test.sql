CREATE TABLE f1 (

   _id bigserial NOT NULL PRIMARY KEY,
   type text NOT NULL, -- Notes propertyType
   spec text NOT NULL, -- Notes propertySpecies
   pmix1 integer DEFAULT 300 NOT NULL, -- Notes propertyMix1
   pmix2_1 character varying(50) NOT NULL, -- Notes propertyMix2
   pmix2_2 character varying(50), -- Notes propertyMix2
   pmix2_3 character varying(50), -- Notes propertyMix2
   pmix2_4 character varying(50), -- Notes propertyMix2
   pmix2_5 character varying(50), -- Notes propertyMix2
   p1 text NOT NULL, -- Notes property1
   geom geometry(POINT,31467) NOT NULL, -- Notes geometry (GM_Point)
   pext_1 unknown, -- Notes propExternal
   pext_2 unknown, -- Notes propExternal
   pf2ac_1 bigserial, -- Notes propFeature2_AssocClass
   pf2ac_2 bigserial, -- Notes propFeature2_AssocClass
   pf2ac_3 bigserial, -- Notes propFeature2_AssocClass
   pf2_1 bigserial, -- Notes propFeature2
   pf2_2 bigserial, -- Notes propFeature2
   pf2_3 bigserial -- Notes propFeature2
);

CREATE TABLE f2_suffix (

   _id bigserial NOT NULL PRIMARY KEY,
   type text NOT NULL, -- Notes propertyType
   spec text NOT NULL, -- Notes propertySpecies
   pmix3 boolean, -- Notes propertyMix3
   geom geometry(MULTIPOLYGON,31467), -- Notes geometry (GM_MultiSurface)
   pf1 bigserial NOT NULL, -- Notes propFeature1
   pf1ac bigserial -- Notes propFeature1_AssocClass
);

CREATE TABLE f3_suffix (

   _id bigserial NOT NULL PRIMARY KEY,
   geom geometry(GEOMETRY,31467) NOT NULL, -- Notes geometry (GM_Object)
   type text NOT NULL, -- Notes propertyType
   p1 boolean DEFAULT TRUE NOT NULL, -- Notes property1
   p2_f2 bigserial, -- Notes property2 (MixinB)
   p2_f4 bigserial, -- Notes property2 (MixinB)
   pafb_f2 bigserial -- Notes propAFeatureB
);

CREATE TABLE f4 (

   _id bigserial NOT NULL PRIMARY KEY,
   geom geometry(GEOMETRY,31467) NOT NULL, -- Notes geometry (GM_Object)
   type text NOT NULL, -- Notes propertyType
   p1 boolean DEFAULT TRUE NOT NULL, -- Notes property1
   pmix3 boolean, -- Notes propertyMix3
   pafb_f1 bigserial, -- Notes propAFeatureB
   pafb_f2 bigserial -- Notes propAFeatureB
);


ALTER TABLE f1 ADD CONSTRAINT fk_f1_pf2_1 FOREIGN KEY (pf2_1) REFERENCES f2_suffix;
ALTER TABLE f1 ADD CONSTRAINT fk_f1_pf2_2 FOREIGN KEY (pf2_2) REFERENCES f2_suffix;
ALTER TABLE f1 ADD CONSTRAINT fk_f1_pf2_3 FOREIGN KEY (pf2_3) REFERENCES f2_suffix;
ALTER TABLE f1 ADD CONSTRAINT fk_f1_pf2ac_1 FOREIGN KEY (pf2ac_1) REFERENCES f2_suffix;
ALTER TABLE f1 ADD CONSTRAINT fk_f1_pf2ac_2 FOREIGN KEY (pf2ac_2) REFERENCES f2_suffix;
ALTER TABLE f1 ADD CONSTRAINT fk_f1_pf2ac_3 FOREIGN KEY (pf2ac_3) REFERENCES f2_suffix;
ALTER TABLE f2_suffix ADD CONSTRAINT fk_f2_suffix_pf1 FOREIGN KEY (pf1) REFERENCES f1;
ALTER TABLE f2_suffix ADD CONSTRAINT fk_f2_suffix_pf1ac FOREIGN KEY (pf1ac) REFERENCES f1;
ALTER TABLE f3_suffix ADD CONSTRAINT fk_f3_suffix_p2_f2 FOREIGN KEY (p2_f2) REFERENCES f2_suffix;
ALTER TABLE f3_suffix ADD CONSTRAINT fk_f3_suffix_p2_f4 FOREIGN KEY (p2_f4) REFERENCES f4;
ALTER TABLE f3_suffix ADD CONSTRAINT fk_f3_suffix_pafb_f2 FOREIGN KEY (pafb_f2) REFERENCES f2_suffix;
ALTER TABLE f4 ADD CONSTRAINT fk_f4_pafb_f1 FOREIGN KEY (pafb_f1) REFERENCES f1;
ALTER TABLE f4 ADD CONSTRAINT fk_f4_pafb_f2 FOREIGN KEY (pafb_f2) REFERENCES f2_suffix;

CREATE INDEX idx_f1_geom ON f1 USING GIST (geom);
CREATE INDEX idx_f2_suffix_geom ON f2_suffix USING GIST (geom);
CREATE INDEX idx_f3_suffix_geom ON f3_suffix USING GIST (geom);
CREATE INDEX idx_f4_geom ON f4 USING GIST (geom);
