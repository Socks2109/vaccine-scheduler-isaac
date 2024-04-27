CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username),
);

CREATE TABLE Patients (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    Available int,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Appointments (
    Appointment_id int,
    Time date,
    P_Username varchar(255),
    C_Username varchar(255),
	V_Name varchar(255),
    PRIMARY KEY (Appointment_id),
    FOREIGN KEY (Time, C_Username) REFERENCES Availabilities,
    FOREIGN KEY (V_Name) REFERENCES Vaccines,
    FOREIGN KEY (P_Username) REFERENCES Patients
);