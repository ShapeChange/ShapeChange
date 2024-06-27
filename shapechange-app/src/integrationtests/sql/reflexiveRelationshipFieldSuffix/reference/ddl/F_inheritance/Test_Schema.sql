CREATE TABLE featuretype1 (

   _id bigserial NOT NULL PRIMARY KEY,
   feature1a_reflexive bigserial NOT NULL,
   feature2_id bigserial NOT NULL
);

CREATE TABLE featuretype1_feature1b_target (

   featuretype1_feature1b_source_id bigserial NOT NULL,
   featuretype1_feature1b_target_id bigserial NOT NULL,
   PRIMARY KEY (featuretype1_feature1b_source_id, featuretype1_feature1b_target_id)
);

CREATE TABLE featuretype2 (

   _id bigserial NOT NULL PRIMARY KEY
);


ALTER TABLE featuretype1 ADD CONSTRAINT fk_featuretype1_feature1a_reflexive FOREIGN KEY (feature1a_reflexive) REFERENCES featuretype1;
ALTER TABLE featuretype1 ADD CONSTRAINT fk_featuretype1_feature2_id FOREIGN KEY (feature2_id) REFERENCES featuretype2;
ALTER TABLE featuretype1_feature1b_target ADD CONSTRAINT fk_featuretype1_feature1b_target_featuretype1_feature1b_source_ FOREIGN KEY (featuretype1_feature1b_source_id) REFERENCES featuretype1;
ALTER TABLE featuretype1_feature1b_target ADD CONSTRAINT fk_featuretype1_feature1b_target_featuretype1_feature1b_target_ FOREIGN KEY (featuretype1_feature1b_target_id) REFERENCES featuretype1;
