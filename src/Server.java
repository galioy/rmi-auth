import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class Server implements RemoteInterface {
    private ArrayList<String> printQueue;
    private String printerStatus = "OFF";


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
        new FileOutputStream("log.txt", false).close();
        printQueue = new ArrayList<>();
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
    public String stop() throws RemoteException {
        printQueue = null;
        printerStatus = "OFF";
        return "The print server has been stopped.";
    }

    /**
     * Restarts the print server, clears the print printQueue and starts the print server again
     *
     * @return String
     * @throws IOException
     */
    @Override
    public String restart() throws IOException {
        String output = stop() + "\n";
        output += start();
        return output;
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

    private String constructListOfFiles(String topLine){
        String listOfFiles = topLine + "\n";
        for (String fileName: printQueue) {
            listOfFiles += "* " + fileName + "\n";
        }

        return listOfFiles;
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