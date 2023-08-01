package minipropack;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
abstract class Courseab{
	abstract public String toString();
}
public class CourseRegistrationApp {

    private static Map<Integer, Course> courses = new HashMap<>();
    private static Map<String, Course> registrations = new HashMap<>();
    private static int courseIdCounter = 1;
    private static final String DATABASE_URL = "jdbc:mysql://localhost:3306/courses";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "2214sep03";

    public static void main(String[] args) {
        createTables();
        displayMenu();
    }

    private static void createTables() {
        String createCoursesTableSQL = "CREATE TABLE IF NOT EXISTS courses ("
                + "course_id INT PRIMARY KEY AUTO_INCREMENT, "
                + "name VARCHAR(255) NOT NULL, "
                + "instructor VARCHAR(255) NOT NULL, "
                + "available_seats INT NOT NULL)";

        String createRegistrationsTableSQL = "CREATE TABLE IF NOT EXISTS registrations ("
                + "student_name VARCHAR(255) PRIMARY KEY, "
                + "course_id INT, "
                + "FOREIGN KEY (course_id) REFERENCES courses(course_id))";

        try (Connection conn = getConnection();
             PreparedStatement createCoursesTableStmt = conn.prepareStatement(createCoursesTableSQL);
             PreparedStatement createRegistrationsTableStmt = conn.prepareStatement(createRegistrationsTableSQL)) {
            createCoursesTableStmt.execute();
            createRegistrationsTableStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Connection getConnection() throws SQLException {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return DriverManager.getConnection(DATABASE_URL, USERNAME, PASSWORD);
    }

    private static void displayMenu() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Main Menu:");
            System.out.println("1. View available courses");
            System.out.println("2. Register for a course");
            System.out.println("3. View your registrations");
            System.out.println("4. Add a course");
            System.out.println("5. Exit");

            System.out.print("Enter your choice (1-5): ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    viewCourses();
                    break;
                case "2":
                    registerCourse(scanner);
                    break;
                case "3":
                    viewRegistrations();
                    break;
                case "4":
                    addCourse(scanner);
                    break;
                case "5":
                    scanner.close();
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
                    break;
            }
        }
    }

    private static void viewCourses() {
        String selectCoursesSQL = "SELECT * FROM courses";

        try (Connection conn = getConnection();
             PreparedStatement selectCoursesStmt = conn.prepareStatement(selectCoursesSQL);
             ResultSet resultSet = selectCoursesStmt.executeQuery()) {

            System.out.println("Available Courses:");
            while (resultSet.next()) {
                int courseId = resultSet.getInt("course_id");
                String name = resultSet.getString("name");
                String instructor = resultSet.getString("instructor");
                int availableSeats = resultSet.getInt("available_seats");

                Course course = new Course(courseId, name, instructor, availableSeats);
                courses.put(courseId, course);
                System.out.println(course);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void registerCourse(Scanner scanner) {
        System.out.print("Enter your name: ");
        String studentName = scanner.nextLine();

        System.out.print("Enter the name of the course you want to register for: ");
        String courseName = scanner.nextLine();

        Course courseToRegister = findCourseByName(courseName);
        if (courseToRegister == null) {
            System.out.println("Course not found.");
            return;
        }

        if (registrations.containsKey(studentName)) {
            System.out.println("You have already registered for a course.");
            return;
        }

        if (courseToRegister.getAvailableSeats() <= 0) {
            System.out.println("No seats available for this course.");
            return;
        }

        courseToRegister.decrementSeats();
        registrations.put(studentName, courseToRegister);

        // Insert registration data into the "registrations" table
        String insertRegistrationSQL = "INSERT INTO registrations (student_name, course_id) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement insertRegistrationStmt = conn.prepareStatement(insertRegistrationSQL)) {
            insertRegistrationStmt.setString(1, studentName);
            insertRegistrationStmt.setInt(2, courseToRegister.getCourseId());
            insertRegistrationStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Registration successful!");
    }

    private static void viewRegistrations() {
        System.out.println("Your Registrations:");
        String selectRegistrationsSQL = "SELECT student_name, course_id FROM registrations";
        try (Connection conn = getConnection();
             PreparedStatement selectRegistrationsStmt = conn.prepareStatement(selectRegistrationsSQL);
             ResultSet resultSet = selectRegistrationsStmt.executeQuery()) {

            while (resultSet.next()) {
                String studentName = resultSet.getString("student_name");
                int courseId = resultSet.getInt("course_id");

                Course registeredCourse = courses.get(courseId);
                if (registeredCourse != null) {
                    registrations.put(studentName, registeredCourse);
                }
            }

            for (String studentName : registrations.keySet()) {
                Course registeredCourse = registrations.get(studentName);
                System.out.println(studentName + " is registered for " + registeredCourse);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static Course findCourseByName(String courseName) {
        for (Course course : courses.values()) {
            if (course.getName().equalsIgnoreCase(courseName)) {
                return course;
            }
        }
        return null;
    }

    private static void addCourse(Scanner scanner) {
        System.out.print("Enter the course name: ");
        String name = scanner.nextLine();

        System.out.print("Enter the instructor's name: ");
        String instructor = scanner.nextLine();

        System.out.print("Enter the number of available seats: ");
        int availableSeats = Integer.parseInt(scanner.nextLine());

        // Insert new course data into the "courses" table
        String insertCourseSQL = "INSERT INTO courses (name, instructor, available_seats) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement insertCourseStmt = conn.prepareStatement(insertCourseSQL)) {
            insertCourseStmt.setString(1, name);
            insertCourseStmt.setString(2, instructor);
            insertCourseStmt.setInt(3, availableSeats);
            insertCourseStmt.executeUpdate();

            Course newCourse = new Course(courseIdCounter, name, instructor, availableSeats);
            courses.put(courseIdCounter, newCourse);
            courseIdCounter++;

            System.out.println("Course added successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Course class with constructor and getter methods
    public static class Course extends Courseab{
        private int courseId;
        private String name;
        private String instructor;
        private int availableSeats;

        public Course(int courseId, String name, String instructor, int availableSeats) {
            this.courseId = courseId;
            this.name = name;
            this.instructor = instructor;
            this.availableSeats = availableSeats;
        }

        public int getCourseId() {
            return courseId;
        }

        public String getName() {
            return name;
        }

        public String getInstructor() {
            return instructor;
        }

        public int getAvailableSeats() {
            return availableSeats;
        }

        public void decrementSeats() {
            if (availableSeats > 0) {
                availableSeats--;
            }
        }

        @Override
        public String toString() {
            return "Course ID: " + courseId + ", Name: " + name + ", Instructor: " + instructor + ", Available Seats: " + availableSeats;
        }
    }
}
