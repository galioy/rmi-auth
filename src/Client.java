import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class Client {

    private Client() {}

    private static String callServer(String command, RemoteInterface stub, Scanner scanner) throws IOException{
        switch (command){
            case "print":
                System.out.println("-> Please provide filename to be printed:");
                String filename = scanner.nextLine();

                System.out.println("-> Please provide printer name:");
                String printer = scanner.nextLine();

                return stub.print(filename, printer);
            case "queue":
                return stub.queue();
            case "topQueue":
                System.out.println("-> Please provide job ID:");
                int jobID = scanner.nextInt();

                return stub.topQueue(jobID);
            case "start":
                return stub.start();
            case "stop":
                return stub.stop();
            case "restart":
                return stub.restart();
            case "status":
                return stub.status();
            case "readConfig":
                System.out.println("-> Please provide config parameter:");
                String param = scanner.nextLine();

                return stub.readConfig(param);
            case "setConfig":
                System.out.println("-> Please provide parameter to be set:");
                param = scanner.nextLine();

                System.out.println("-> Please provide value for the parameter:");
                String paramValue = scanner.nextLine();

                return stub.print(param, paramValue);
            default:
                System.out.println("Command not recognized. Available commands are: \n" +
                        "print, queue, topQueue, start, stop, restart, status, readConfig, setConfig.\n" +
                        "-> Please type in a command again:");
                command = scanner.nextLine();
                return callServer(command, stub, scanner);
        }
    }

    public static void main(String[] args) {

        String host = (args.length < 1) ? null : args[0];
        String command;

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            RemoteInterface stub = (RemoteInterface) registry.lookup("RemoteInterface");

            Scanner scanner = new Scanner(System.in);

            while(true){
                System.out.println("--> Type in a command:");
                command = scanner.nextLine();

                System.out.println(callServer(command, stub,scanner));
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}