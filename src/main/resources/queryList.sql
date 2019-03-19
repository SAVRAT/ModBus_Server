CREATE TABLE WoodLog(
                      ID INTEGER PRIMARY KEY AUTO_INCREMENT,
                      dot_1 VARCHAR(50),
                      dot_2 VARCHAR(50),
                      dot_3 VARCHAR(50),
                      dot_4 VARCHAR(50),
                      dot_5 VARCHAR(50),
                      dot_6 VARCHAR(50),
                      dot_7 VARCHAR(50),
                      dot_8 VARCHAR(50),
                      dot_9 VARCHAR(50),
                      dot_10 VARCHAR(50),
                      dot_11 VARCHAR(50),
                      dot_12 VARCHAR(50),
                      dot_13 VARCHAR(50),
                      dot_14 VARCHAR(50),
                      dot_15 VARCHAR(50),
                      dot_16 VARCHAR(50)
);
drop table WoodLog;

DELETE FROM WoodLog;
ALTER TABLE WoodLog AUTO_INCREMENT=0;
ALTER TABLE WoodLog AUTO_INCREMENT=1;
INSERT INTO WoodLog (dot_1, dot_2, dot_3, dot_4, dot_5, dot_6, dot_7, dot_8, dot_9, dot_10, dot_11, dot_12, dot_13, dot_14, dot_15, dot_16)
  VALUE ('0','0','0','0','0','0','0','0','0','0','0','0','0','0','0','0');
SELECT * FROM WoodLog;

SHOW TABLES;
SELECT * FROM point_control;
DELETE FROM point_control WHERE tablename = 'gidro';

SELECT * FROM gidro;
UPDATE point_control SET ip='192.168.0.100' WHERE id=11;

DELETE FROM shlif_p1;
DELETE FROM shlif_p2;
DELETE FROM shlif_p3;
DELETE FROM shlif_p4;
DELETE FROM shlif_p5;
DELETE FROM shlif_p6;
DELETE FROM shlif_p7;
DELETE FROM shlif_p8;
DELETE FROM shlif_p9;
DELETE FROM shlif_p10;
DELETE FROM shlif_p11;
DELETE FROM shlif_p12;
DELETE FROM shlif_p13;
DELETE FROM shlif_p14;
DELETE FROM shlif_p15;
DELETE FROM shlif_p16;

DELETE FROM lu_1_p1;
DELETE FROM lu_1_p2;
DELETE FROM lu_1_p3;
DELETE FROM lu_1_p4;
DELETE FROM lu_1_p5;


SELECT * FROM shlif_p4;

UPDATE status_connection SET status = 0 WHERE ip = '192.168.49.243';

