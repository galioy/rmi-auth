import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;

public interface RemoteInterface extends Remote{

    String register(String username, String pswd) throws RemoteException;

    String authenticate(String username, String pswd) throws RemoteException;

    /**
     * Prints file "filename" on the specified "printer"
     * @param filename String
     * @param printer String
     * @return String
     * @throws RemoteException
     */
    String print(String filename, String printer) throws RemoteException;

    /**
     * Lists the print queue on the user's display, in lines of the form <job number> <file name>
     * @return String
     * @throws RemoteException
     */
    String queue() throws RemoteException;

    /**
     * Moves "job" to the top of the queue
     * @param job int
     * @return String
     * @throws RemoteException
     */
    String topQueue(int job) throws RemoteException;

    /**
     * Starts the print server
     * @return String
     * @throws IOException
     */
    String start() throws IOException; // IOException combines RemoteException and FileNotFoundException

    /**
     * Stops the print server
     * @return String
     * @throws RemoteException
     */
    String stop() throws RemoteException, SQLException;

    /**
     * Restarts the print server, clears the print queue and starts the print server again
     * @return String
     * @throws RemoteException
     */
    String restart() throws IOException, SQLException;

    /**
     * Prints status of the printer on the user's display
     * @return String
     * @throws RemoteException
     */
    String status() throws RemoteException;

    /**
     * Prints the value of the "parameter" on the user's display
     * @param parameter String
     * @return String
     * @throws RemoteException
     */
    String readConfig(String parameter) throws RemoteException;

    /**
     * Sets the "parameter" to have "value" as value
     * @param parameter String
     * @param value String
     * @return String
     * @throws RemoteException
     */
    String setConfig(String parameter, String value) throws RemoteException;
}