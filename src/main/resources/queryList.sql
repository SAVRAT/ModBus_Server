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

SELECT tablename, address, ip, length, type, id FROM oborudovanie;


CREATE TABLE woodParams (
    id INT AUTO_INCREMENT,
    dataId VARCHAR(50),
    inputRad DOUBLE,
    outputRad DOUBLE,
    avrRad DOUBLE,
    volume DOUBLE,
    usefulVolume DOUBLE,
    PRIMARY KEY (id)
);

CREATE TABLE woodData_test (
    id INT AUTO_INCREMENT,
    xData DOUBLE,
    yDAta DOUBLE,
    PRIMARY KEY (id)
);

INSERT INTO woodData (xData, yData, stringKey) VALUES ('12,43,54,23', '54,23,42,65', '3qwghds5ds');
INSERT INTO woodParams (dataId, inputRad, outputRad, avrRad, volume, usefulVolume, timeStamp) VALUES (?, ?, ?, ?, ?, ?,
                                                                                                      UNIX_TIMESTAMP());
CREATE TABLE vibroIndication (
    hl_1 BOOLEAN,
    hl_2 BOOLEAN,
    hl_3 BOOLEAN,
    hl_4 BOOLEAN,
    hl_5 BOOLEAN,
    hl_6 BOOLEAN,
    hl_7 BOOLEAN,
    hl_8 BOOLEAN,
    hl_9 BOOLEAN,
    hl_10 BOOLEAN,
    hl_11 BOOLEAN,
    hl_12 BOOLEAN,
    hl_13 BOOLEAN,
    hl_14 BOOLEAN,
    hl_15 BOOLEAN,
    hl_16 BOOLEAN,
    hl_17 BOOLEAN,
    hl_18 BOOLEAN,
    hl_19 BOOLEAN,
    hl_20 BOOLEAN,
    hl_21 BOOLEAN,
    hl_22 BOOLEAN,
    hl_23 BOOLEAN,
    hl_24 BOOLEAN,
    hl_25 BOOLEAN,
    hl_26 BOOLEAN,
    hl_27 BOOLEAN
);

INSERT INTO vibroIndication VALUES (false, false, true, false, false, true, false, false, true, false, false, true,
                                    false, false, true, false, false, true, false, false, true, false, false, true,
                                    false, false, true);

SELECT * FROM err_message;

CREATE TABLE woodData_3 (
    id INT,
    xCentre REAL,
    yCentre REAL,
    radius REAL,
    PRIMARY KEY(id)
);

DROP TABLE woodData_3;

CREATE TABLE SystemLog (
    logMessage TEXT,
    timeStamp INT
);

INSERT INTO SystemLog (logMessage, timeStamp) VALUES ('qrtfjnals', UNIX_TIMESTAMP());
