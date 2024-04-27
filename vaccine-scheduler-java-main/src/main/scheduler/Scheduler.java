package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            currentPatient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
        } else {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getCaregiver = "SELECT c.Username c_name FROM Caregivers as c, " +
                    "Availabilities as a WHERE a.Username = c.Username AND time = ? ORDER BY c.username;";
            String getVaccine = "SELECT v.name v_name, v.doses doses FROM Vaccines as v";
            try {
                String date = tokens[1];
                PreparedStatement statement = con.prepareStatement(getCaregiver);
                PreparedStatement statement1 = con.prepareStatement(getVaccine);
                Date convert = Date.valueOf(date);
                statement.setDate(1, convert);

                ResultSet resultSet = statement.executeQuery();
                ResultSet resultSet1 = statement1.executeQuery();

                List<String> v_name = new ArrayList<>();
                List<String> doses = new ArrayList<>();
                List<String> c_name = new ArrayList<>();

                while (resultSet.next()) {
                    c_name.add(resultSet.getString("c_name"));
                }
                while (resultSet1.next()) {
                    v_name.add(resultSet1.getString("v_name"));
                    doses.add(resultSet1.getString("doses"));
                }
                for (String s : c_name) {
                    for (int j = 0; j < v_name.size(); j++) {
                        System.out.println(s + " " + v_name.get(j) + " " + doses.get(j));
                    }
                }
            }  catch (IllegalArgumentException e) {
                System.out.println("Please enter a valid date!");
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        }
    }

    private static void reserve(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Please try again!");
        }
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
        } else if (currentCaregiver != null) {
            System.out.println("Please login as a patient!");
        } else {
            String date = tokens[1];
            String vaccine = tokens[2];

            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getCaregiver = "SELECT c.Username c_name FROM Caregivers as c, " +
                    "Availabilities as a WHERE a.Username = c.Username AND time = ? " +
                    "AND a.Available = 1 ORDER BY c.username;";
            String getVaccine = "SELECT v.name v_name, v.doses doses FROM Vaccines as v WHERE v.name = ?;";
            String countApp = "SELECT COUNT(*) FROM Appointments";
            String setAppointment = "INSERT INTO Appointments VALUES (?, ?, ?, ?, ?);";
            String updateCaregiver = "Update Availabilities SET Available = 0 WHERE Time = ? AND Username = ?;";
            try {
                PreparedStatement statementCaregiver = con.prepareStatement(getCaregiver);
                PreparedStatement statementVaccine = con.prepareStatement(getVaccine);
                PreparedStatement statementCount = con.prepareStatement(countApp);

                Date convert = Date.valueOf(date);
                statementCaregiver.setDate(1, convert);
                statementVaccine.setString(1, vaccine);


                ResultSet resultSetC = statementCaregiver.executeQuery();
                ResultSet resultSetV = statementVaccine.executeQuery();
                ResultSet resultSetCount = statementCount.executeQuery();

                String s = "";
                if (resultSetC.next()) {
                    s = resultSetC.getString(1);
                }
                String numOfDoses = "";
                int countDoses = 0;
                if (resultSetV.next()) {
                    numOfDoses = resultSetV.getString("doses");
                    countDoses = Integer.parseInt(numOfDoses);
                }

                try {
                    if (s.equals("")) {
                        System.out.println("No Caregiver is available!");
                    } else if (numOfDoses.equals("")) {
                        System.out.println("No such vaccine exists!");
                    } else if (countDoses == 0) {
                        System.out.println("Not enough available doses!");
                    } else {
                        PreparedStatement statementApp = con.prepareStatement(setAppointment);
                        PreparedStatement statementRemove = con.prepareStatement(updateCaregiver);
                        resultSetCount.next();
                        int app_id = resultSetCount.getInt(1);

                        statementRemove.setDate(1, convert);
                        statementRemove.setString(2, s);
                        statementRemove.executeUpdate();

                        statementApp.setInt(1, app_id);
                        statementApp.setDate(2, convert);
                        statementApp.setString(3, currentPatient.getUsername());
                        statementApp.setString(4, s);
                        statementApp.setString(5, vaccine);
                        statementApp.executeUpdate();


                        System.out.println("Appointment ID: {" + app_id + "}, Caregiver username: {" + s + "}");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }

        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
        } else if (currentCaregiver != null) {
            String getForCaregiver= "SELECT Appointment_id id, V_Name vname, Time date, " +
                    "P_Username pname FROM Appointments WHERE C_Username = ? ORDER BY Appointment_id";
            try {
                PreparedStatement statementCare = con.prepareStatement(getForCaregiver);
                statementCare.setString(1, currentCaregiver.getUsername());
                ResultSet resultSet = statementCare.executeQuery();

                List<String> id = new ArrayList<>();
                List<String> vname = new ArrayList<>();
                List<String> date = new ArrayList<>();
                List<String> pname = new ArrayList<>();

                while (resultSet.next()) {
                    id.add(resultSet.getString("id"));
                    vname.add(resultSet.getString("vname"));
                    date.add(resultSet.getString("date"));
                    pname.add(resultSet.getString("pname"));
                }

                for (int i = 0; i < id.size(); i++) {
                    System.out.println("Appointment id: " + id.get(i) + ", Vaccine Name: " + vname.get(i) + ", Date: "
                            + date.get(i) + ", Patient Name: " + pname.get(i));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        } else {
            String getForPatient= "SELECT Appointment_id id, V_Name vname, Time date, " +
                    "C_Username cname FROM Appointments WHERE P_Username = ? ORDER BY Appointment_id";
            try {
                PreparedStatement statementCare = con.prepareStatement(getForPatient);
                statementCare.setString(1, currentPatient.getUsername());
                ResultSet resultSet = statementCare.executeQuery();

                List<String> id = new ArrayList<>();
                List<String> vname = new ArrayList<>();
                List<String> date = new ArrayList<>();
                List<String> cname = new ArrayList<>();

                while (resultSet.next()) {
                    id.add(resultSet.getString("id"));
                    vname.add(resultSet.getString("vname"));
                    date.add(resultSet.getString("date"));
                    cname.add(resultSet.getString("cname"));
                }

                for (int i = 0; i < id.size(); i++) {
                    System.out.println("Appointment id: " + id.get(i) + ", Vaccine Name: " + vname.get(i) + ", Date: "
                            + date.get(i) + ", Caregiver Name: " + cname.get(i));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        }
    }

    private static void logout(String[] tokens) {
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
        } else {
            currentCaregiver = null;
            currentPatient = null;
            System.out.println("Successfully logged out!");
        }
    }
}
