import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {

    private Client() {}

    private static String callServer(String command, RemoteInterface stub, Scanner scanner) throws IOException{
        String response;

        return stub.start();
    }

    public static void main(String[] args) {

        String host = (args.length < 1) ? null : args[0];
        String command;

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            RemoteInterface stub = (RemoteInterface) registry.lookup("RemoteInterface");

            Scanner scanner = new Scanner(System.in);

            System.out.println("Type in a command.");
            while(true){
                command = scanner.nextLine();

                System.out.println(callServer(command, stub,scanner));
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}