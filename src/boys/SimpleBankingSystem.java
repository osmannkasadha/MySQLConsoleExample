package boys;  

import java.sql.*;  
import java.util.Scanner;  

public class SimpleBankingSystem {  
    // Database connection details  
    private static final String URL = "jdbc:mysql://localhost:3306/trend"; // Database URL  
    private static final String USER = "root"; // Database username  
    private static final String PASSWORD = ""; // Database password  

    public static void main(String[] args) {  
        // Load the JDBC Driver  
        try {  
            Class.forName("com.mysql.cj.jdbc.Driver"); // Load MySQL JDBC driver  
        } catch (ClassNotFoundException e) {  
            System.err.println("Could not load JDBC driver: " + e.getMessage());  
            return;  
        }  

        // Try to establish a connection  
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {  
            System.out.println("Database connection: Connected");  
            authenticateUser(connection);  
        } catch (SQLException e) {  
            // Connection failed  
            System.err.println("Database connection: Not Connected - " + e.getMessage());  
        }  
    }  

    private static void authenticateUser(Connection connection) {  
        Scanner scanner = new Scanner(System.in);  

        try {  
            String usernameQuery = "SELECT id, username, amount, pin FROM users";  
            try (Statement stmt = connection.createStatement(); ResultSet resultSet = stmt.executeQuery(usernameQuery)) {  
                System.out.println("Welcome! Please enter your username:");  
                String inputUsername = scanner.nextLine();  
                boolean userFound = false;  

                while (resultSet.next()) {  
                    String username = resultSet.getString("username");  
                    String storedPin = resultSet.getString("pin");  
                    double amount = resultSet.getDouble("amount");  
                    int userId = resultSet.getInt("id");  

                    if (username.equalsIgnoreCase(inputUsername)) {  
                        userFound = true; // Username found  
                        System.out.println("PIN required. Please enter the 4-digit PIN:");  
                        String inputPin = scanner.nextLine();  

                        if (storedPin.equals(inputPin)) {  
                            System.out.println("Access granted! Welcome, " + username);  
                            System.out.printf("Current Balance: %.2f%n", amount);  

                            // User command loop  
                            while (true) {  
                                System.out.println("Enter 'balance', 'deposit', 'withdraw', 'statement', or 'logout':");  
                                String command = scanner.nextLine();  

                                switch (command.toLowerCase()) {  
                                    case "balance":  
                                        System.out.printf("Your balance is: %.2f%n", amount);  
                                        break;  

                                    case "deposit":  
                                        System.out.println("Enter deposit amount:");  
                                        double depositAmount = scanner.nextDouble();  
                                        if (depositAmount > 0) {  
                                            amount += depositAmount;  
                                            System.out.printf("Successfully deposited: %.2f%n", depositAmount);  
                                            insertTransaction(connection, userId, depositAmount, "Deposit");  
                                        } else {  
                                            System.out.println("Invalid amount. Please try again.");  
                                        }  
                                        scanner.nextLine(); // Consume newline  
                                        break;  

                                    case "withdraw":  
                                        System.out.println("Enter withdraw amount:");  
                                        double withdrawAmount = scanner.nextDouble();  
                                        if (withdrawAmount > amount) {  
                                            System.out.println("Insufficient funds.");  
                                        } else if (withdrawAmount > 0) {  
                                            amount -= withdrawAmount;  
                                            System.out.printf("Successfully withdrew: %.2f%n", withdrawAmount);  
                                            insertTransaction(connection, userId, withdrawAmount, "Withdraw");  
                                        } else {  
                                            System.out.println("Invalid amount. Please try again.");  
                                        }  
                                        scanner.nextLine(); // Consume newline  
                                        break;  

                                    case "statement":  
                                        generateStatement(connection, userId);  
                                        break;  

                                    case "logout":  
                                        System.out.println("You have logged out.");  
                                        return; // Exit the method and go back to the authentication prompt  

                                    default:  
                                        System.out.println("Invalid command. Please try again.");  
                                }  
                            }  
                        } else {  
                            System.out.println("Incorrect PIN. Please try again.");  
                            return; // Exit to the main method for another login attempt  
                        }  
                    }  
                }  
                if (!userFound) {  
                    System.out.println("Username not found. Please try again.");  
                }  
            }  
        } catch (SQLException e) {  
            System.err.println("Error loading user data: " + e.getMessage());  
        } finally {  
            scanner.close();  
        }  
    }  

    private static void insertTransaction(Connection connection, int userId, double amount, String transactionType) {  
        String insertSQL = "INSERT INTO transactions (username, t_amount, transaction_name, t_date, ref) VALUES (?, ?, ?, NOW(), ?)";  
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {  
            pstmt.setInt(1, userId); // Set username (ensure this is appropriate)  
            pstmt.setDouble(2, amount); // Set the transaction amount  
            pstmt.setString(3, transactionType); // Set the transaction type  
            pstmt.setString(4, "TXN" + System.currentTimeMillis()); // Example reference number  
            pstmt.executeUpdate();  
        } catch (SQLException e) {  
            System.err.println("Error inserting transaction: " + e.getMessage());  
        }  
    }  

    private static void generateStatement(Connection connection, int userId) {  
        String statementQuery = "SELECT id, username, t_amount, transaction_name, t_date, ref FROM transactions WHERE username = ?";  
        try (PreparedStatement pstmt = connection.prepareStatement(statementQuery)) {  
            pstmt.setInt(1, userId); // Specify the user ID  
            ResultSet rs = pstmt.executeQuery();  
            System.out.println("Transaction Statement:");  
            while (rs.next()) {  
                System.out.printf("ID: %d, Amount: %.2f, Type: %s, Date: %s, Reference: %s%n",  
                        rs.getInt("id"), rs.getDouble("t_amount"), rs.getString("transaction_name"),  
                        rs.getTimestamp("t_date"), rs.getString("ref"));  
            }  
        } catch (SQLException e) {  
            System.err.println("Error generating statement: " + e.getMessage());  
        }  
    }  
}  