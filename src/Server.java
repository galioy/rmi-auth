import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Server implements RemoteInterface {

    public Server() {}

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
        return null;
    }

    /**
     * Lists the print queue on the user's display, in lines of the form <job number> <file name>
     *
     * @return String
     * @throws RemoteException
     */
    @Override
    public String queue() throws RemoteException {
        return null;
    }

    /**
     * Moves "job" to the top of the queue
     *
     * @param job int
     * @return String
     * @throws RemoteException
     */
    @Override
    public String topQueue(int job) throws RemoteException {
        return null;
    }

    /**
     * Starts the print server
     *
     * @return String
     * @throws IOException
     */
    @Override
    public String start() throws IOException {
        new FileOutputStream("log.txt", false).close();
        return "The print server has been started.";
    }

    /**
     * Stops the print server
     *
     * @return String
     * @throws RemoteException
     */
    @Override
    public String stop() throws RemoteException {
        return null;
    }

    /**
     * Restarts the print server, clears the print queue and starts the print server again
     *
     * @return String
     * @throws RemoteException
     */
    @Override
    public String restart() throws RemoteException {
        return null;
    }

    /**
     * Prints status of the printer on the user's display
     *
     * @return String
     * @throws RemoteException
     */
    @Override
    public String status() throws RemoteException {
        return "Yeah boi!";
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
        return null;
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
        return null;
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