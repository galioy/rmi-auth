import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.FileOutputStream;
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
    private HashMap<String, String> session;

    private Connection connection;
    String url = "jdbc:postgresql://localhost:5432/auth?user=postgres";

    public Server() {
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(url);
        } catch (Exception e){
            System.out.println("Connection to the DB could not be established...");
            e.printStackTrace();
        }
    }

    @Override
    public String register(String username, String pswd) throws RemoteException {
        try {
            // Encrypt the password
            byte[] salt = generateSalt();
            byte[] encryptedPswd = getEncryptedPswd(pswd, salt);

            // Store encrypted pswd and salt in the DB
            String sql = "INSERT INTO users(username, password, salt) VALUES (?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, new String(encryptedPswd));
            stmt.setString(3, new String(salt));
            stmt.executeUpdate();

            // give the user an authentication session key
            SecureRandom random = new SecureRandom();
            return new BigInteger(130, random).toString(32);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | SQLException e) {
            System.out.println("Could not encrypt and store the password.");
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String authenticate(String username, String pswd) throws RemoteException {
        byte[] encryptedPswd = new byte[0], salt = new byte[0];
        try {
            String sql = "SELECT password, salt FROM users WHERE username='"+username+"'";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            if(!rs.next()) {
                System.out.println("No such user with username: " + username);
                return null;
            }

            while (rs.next()) {
                encryptedPswd = rs.getString("password").getBytes();
                salt = rs.getString("salt").getBytes();
            }
            if(!authenticatePswd(pswd, encryptedPswd, salt)){
                System.out.println("Could not authenticate user");
                return null;
            }

            // give the user an authentication session key
            SecureRandom random = new SecureRandom();
            return new BigInteger(130, random).toString(32);

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
    public String print(String filename, String printer) throws RemoteException {
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
    public String queue() throws RemoteException {
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
    public String topQueue(int jobID) throws RemoteException {
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
//        new FileOutputStream("log.txt", false).close();
        printQueue = new ArrayList<>();
        config = new HashMap<>();
        session = new HashMap<>();
        printerStatus = "ON";
        return "The print server has been started.";
    }

    /**
     * Stops the print server
     *
     * @return String
     * @throws RemoteException
     */
    @Override
    public String stop() throws RemoteException, SQLException {
        printQueue = null;
        config = null;
        session = null;
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
    public String restart() throws IOException, SQLException {
        try {
            String output = stop() + "\n";
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
    public String readConfig(String parameter) throws RemoteException {
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
    public String setConfig(String parameter, String value) throws RemoteException {
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

    private boolean authenticatePswd(String pswdInput, byte[] encryptedPswd, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException{
        byte[] encryptedPswdInput = getEncryptedPswd(pswdInput, salt);
        return Arrays.equals(encryptedPswd, encryptedPswdInput);
    }

    private byte[] getEncryptedPswd(String pswd, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String algorithm = "PBKDF2WithHmacSHA1";
        int derivedKeyLength = 160;
        int iterations = 10000;

        KeySpec spec = new PBEKeySpec(pswd.toCharArray(), salt, iterations, derivedKeyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);

        return factory.generateSecret(spec).getEncoded();
    }

    private byte[] generateSalt() throws NoSuchAlgorithmException {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

        byte[] salt = new byte[8];
        random.nextBytes(salt);

        return salt;
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