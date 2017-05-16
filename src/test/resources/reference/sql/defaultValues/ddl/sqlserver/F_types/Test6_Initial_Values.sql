CREATE TABLE T6_Class1 (

   _id bigint NOT NULL PRIMARY KEY,
   propBooleanFalse bit NOT NULL DEFAULT 0,
   propBooleanTrue bit NOT NULL DEFAULT 1,
   propInteger int NOT NULL DEFAULT 3,
   propNumber numeric NOT NULL DEFAULT 2.1,
   propString nvarchar(max) NOT NULL DEFAULT 'mydefault'
);

CREATE TABLE T6_Class1_propStrings (

   T6_Class1_id bigint NOT NULL,
   propStrings nvarchar(max) NOT NULL DEFAULT 'abc',
   PRIMARY KEY (T6_Class1_id, propStrings)
);


ALTER TABLE T6_Class1_propStrings ADD CONSTRAINT fk_T6_Class1_propStrings_T6_Class1_id FOREIGN KEY (T6_Class1_id) REFERENCES T6_Class1;
