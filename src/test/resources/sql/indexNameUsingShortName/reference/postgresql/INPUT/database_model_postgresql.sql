CREATE TABLE featuretype (

   _id bigserial NOT NULL PRIMARY KEY,
   proppoint geometry(POINT,4979) NOT NULL,
   propcurve geometry(LINESTRING,4979) NOT NULL
);


CREATE INDEX idx_ft_c ON featuretype USING GIST (propcurve);
CREATE INDEX idx_ft_p ON featuretype USING GIST (proppoint);
