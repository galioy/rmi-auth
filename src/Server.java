import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Server implements RemoteInterface {
    private ArrayList<String> printQueue;
    private String printerStatus = "OFF";
    private HashMap<String, String> config;
    private HashMap<String, String> sessions;

    private Connection connection;
    private String url = "jdbc:postgresql://localhost:5432/auth?user=postgres";

    private Server() {
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(url);
        } catch (Exception e){
            System.out.println("Connection to the DB could not be established...");
            e.printStackTrace();
        }
    }

    /**
     * Stores a new user with password and salt in the DB
     * @param username String
     * @param pswd String
     * @return String
     * @throws RemoteException
     */
    @Override
    public String register(String username, String pswd) throws RemoteException {
        try {
            // Ensure there's no user already registered with this username
            String selectSQL = "SELECT password, salt FROM users WHERE username=?";
            PreparedStatement selectStmt = connection.prepareStatement(selectSQL);
            selectStmt.setString(1, username);
            ResultSet rs = selectStmt.executeQuery();
            if(rs.next()) {
                // User with this username exists, so return NULL and do not proceed with registration
                System.out.println("User with username \"" + username + "\" already registered!");
                return null;
            }

            // Encrypt the password
            byte[] salt = generateSalt();
            byte[] encryptedPswd = getEncryptedPswd(pswd, salt);

            // Store encrypted pswd and salt in the DB
            String insertSQL = "INSERT INTO users(username, password, salt) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = connection.prepareStatement(insertSQL);
            insertStmt.setString(1, username);
            insertStmt.setBytes(2, encryptedPswd);
            insertStmt.setBytes(3, salt);
            insertStmt.executeUpdate();

            // give the user an authentication sessions key
            SecureRandom random = new SecureRandom();
            String sessionKey = new BigInteger(130, random).toString(32);
            sessions.put(username, sessionKey);
            return sessionKey;

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | SQLException e) {
            System.out.println("Could not encrypt and store the password.");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets the bytes-encrypted password for a given username from the DB and checks it against a given string password
     * @param username String
     * @param pswd String
     * @return String
     * @throws RemoteException
     */
    @Override
    public String authenticate(String username, String pswd) throws RemoteException {
        byte[] encryptedPswd = new byte[0], salt = new byte[0];
        try {
            String sql = "SELECT password, salt FROM users WHERE username=?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                encryptedPswd = rs.getBytes("password");
                salt = rs.getBytes("salt");
            }

            if(encryptedPswd.length == 0 && salt.length == 0) {
                System.out.println("No such user with username: " + username);
                return null;
            }

            if(!authenticatePswd(pswd, encryptedPswd, salt)){
                System.out.println("Wrong username or password!");
                return null;
            }

            // give the user an authentication sessions key
            SecureRandom random = new SecureRandom();
            String sessionKey = new BigInteger(130, random).toString(32);
            sessions.put(username, sessionKey);
            return sessionKey;

        } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.out.println("Could not fetch user or user does not exist");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Prints file "filename" on the specified "printer"
     *
     * @param filename String
     * @param printer  String
     * @return String
     * @throws RemoteException
     */
    @Override
    public String print(String filename, String printer, String sessionKey) throws RemoteException {
        if(!verifyUserSessionAuthenticated(sessionKey)){
            return "Not authenticated!";
        }

        printQueue.add(filename);
        return "\"" + filename + "\" was added to the print queue.";
    }

    /**
     * Lists the print printQueue on the user's display, in lines of the form <job number> <file name>
     *
     * @return String
     * @throws RemoteException
     */
    @Override
    public String queue(String sessionKey) throws RemoteException {
        if(!verifyUserSessionAuthenticated(sessionKey)){
            return "Not authenticated!";
        }

        String topLine = "List of files on the queue:";
        if(printQueue.size() == 0){
            return topLine + "\n -- empty --";
        }
        return constructListOfFiles(topLine);
    }

    /**
     * Moves "job" to the top of the printQueue
     *
     * @param jobID int
     * @return String
     * @throws RemoteException
     */
    @Override
    public String topQueue(int jobID, String sessionKey) throws RemoteException {
        if(!verifyUserSessionAuthenticated(sessionKey)){
            return "Not authenticated!";
        }

        if (jobID >= printQueue.size()){
            return "Print job with ID " + jobID + " does not exist.";
        }

        String fileToMove = printQueue.get(jobID);
        printQueue.remove(jobID);
        printQueue.add(0, fileToMove);

        return constructListOfFiles("The file \""+fileToMove+"\" has been moved to top of the queue. Now the queue is:");
    }

    /**
     * Starts the print server
     *
     * @return String
     * @throws IOException
     */
    @Override
    public String start() throws IOException {
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(url);

            printQueue = new ArrayList<>();
            config = new HashMap<>();
            sessions = new HashMap<>();
            printerStatus = "ON";

            return "The print server has been started.";
        } catch (Exception e){
            System.out.println("Connection to the DB could not be established...");
            e.printStackTrace();
            return "The print server could not be started because DB connection problems...";
        }
    }

    /**
     * Stops the print server
     *
     * @return String
     * @throws RemoteException
     */
    @Override
    public String stop(String sessionKey) throws RemoteException, SQLException {
        if(!verifyUserSessionAuthenticated(sessionKey)){
            return "Not authenticated!";
        }

        printQueue = null;
        config = null;
        sessions = null;
        printerStatus = "OFF";
        try {
            connection.close();
        } catch (SQLException e){
            e.printStackTrace();
            return "An error occurred while stopping the server...";
        }
        return "The print server has been stopped.";
    }

    /**
     * Restarts the print server, clears the print printQueue and starts the print server again
     *
     * @return String
     * @throws IOException
     */
    @Override
    public String restart(String sessionKey) throws IOException, SQLException {
        if(!verifyUserSessionAuthenticated(sessionKey)){
            return "Not authenticated!";
        }

        try {
            String output = stop(sessionKey) + "\n";
            output += start();
            return output;
        } catch (SQLException e){
            e.printStackTrace();
            return "An error occurred while stopping the server...";
        }
    }

    /**
     * Prints printerStatus of the printer on the user's display
     *
     * @return String
     * @throws RemoteException
     */
    @Override
    public String status() throws RemoteException {
        return printerStatus;
    }

    /**
     * Prints the value of the "parameter" on the user's display
     *
     * @param parameter String
     * @return String
     * @throws RemoteException
     */
    @Override
    public String readConfig(String parameter, String sessionKey) throws RemoteException {
        if(!verifyUserSessionAuthenticated(sessionKey)){
            return "Not authenticated!";
        }

        if(config.get(parameter) == null){
            return "No configuration with key \"" + parameter + "\"";
        }
        return parameter + ": " + config.get(parameter);
    }

    /**
     * Sets the "parameter" to have "value" as value
     *
     * @param parameter String
     * @param value     String
     * @return String
     * @throws RemoteException
     */
    @Override
    public String setConfig(String parameter, String value, String sessionKey) throws RemoteException {
        if(!verifyUserSessionAuthenticated(sessionKey)){
            return "Not authenticated!";
        }

        config.put(parameter, value);
        return "Done! :)";
    }

    /**
     * Iterates through the printer queue and constructs a user-friendly list of files on the queue, ordered from top
     * to bottom of the queue.
     * @param topLine String The text to be printed before the list of files.
     * @return String
     */
    private String constructListOfFiles(String topLine){
        String listOfFiles = topLine + "\n";
        for (int i=1; i<=printQueue.size(); i++) {
            listOfFiles += i + ". " + printQueue.get(i-1) + "\n";
        }

        return listOfFiles;
    }

    /**
     * Checks for equality a string password against an encrypted bytes array password
     * @param pswdInput String
     * @param encryptedPswd byte[]
     * @param salt byte[]
     * @return boolean
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private boolean authenticatePswd(String pswdInput, byte[] encryptedPswd, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException{
        byte[] encryptedPswdInput = getEncryptedPswd(pswdInput, salt);
        return Arrays.equals(encryptedPswd, encryptedPswdInput);
    }

    /**
     * Encrypts a string password into bytes
     * @param pswd String
     * @param salt byte[]
     * @return byte[]
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private byte[] getEncryptedPswd(String pswd, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String algorithm = "PBKDF2WithHmacSHA1";
        int derivedKeyLength = 160;
        int iterations = 10000;

        KeySpec spec = new PBEKeySpec(pswd.toCharArray(), salt, iterations, derivedKeyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);

        return factory.generateSecret(spec).getEncoded();
    }

    /**
     * Generates a random byte array
     * @return byte[]
     * @throws NoSuchAlgorithmException
     */
    private byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

        byte[] salt = new byte[8];
        random.nextBytes(salt);

        return salt;
    }

    /**
     * Checks if the provided session key is set in the sessions list.
     * @param sessionKey String
     * @return True/False True if the session key is set, False otherwise
     */
    private boolean verifyUserSessionAuthenticated(String sessionKey){
        return sessions.containsValue(sessionKey);
    }

    public static void main(String args[]) {

        try {
            Server obj = new Server();
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("RemoteInterface", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}