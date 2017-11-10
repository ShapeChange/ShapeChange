CREATE TABLE T6_Class1 (

   _id bigint NOT NULL PRIMARY KEY,
   propBooleanFalse bit DEFAULT 0,
   propBooleanTrue bit DEFAULT 1,
   propInteger int DEFAULT 3,
   propNumber numeric DEFAULT 2.1,
   propString nvarchar(max) DEFAULT 'mydefault',
   propString2 nvarchar(20) DEFAULT 'anotherDefault'
);

CREATE TABLE T6_Class1_propStrings (

   T6_Class1_id bigint NOT NULL,
   propStrings nvarchar(50) DEFAULT 'abc' NOT NULL,
   PRIMARY KEY (T6_Class1_id, propStrings)
);


ALTER TABLE T6_Class1_propStrings ADD CONSTRAINT fk_T6_Class1_propStrings_T6_Class1_id FOREIGN KEY (T6_Class1_id) REFERENCES T6_Class1;
