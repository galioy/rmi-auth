import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.util.Scanner;

public class Client {
    private String sessionKey;

    private Client() {
        this.sessionKey = null;
    }

    private static String callServer(Client client, String command, RemoteInterface stub, Scanner scanner) throws IOException{
        if(stub.status().equals("OFF")){
            switch (command){
                case "status":
                    return stub.status();
                case "start":
                    return stub.start();
                default:
                    System.out.println("> The printer server is OFF. Please enter \"start\" to start it...");
                    command = scanner.nextLine();
                    return callServer(client, command, stub, scanner);
            }
        } else {
            if(client.sessionKey == null){
                System.out.println("--> Login or register?");
                String action = scanner.nextLine();
                String username, pswd, result;
                switch(action) {
                    case "login":
                        System.out.println("> Type in username:");
                        username = scanner.nextLine();
                        System.out.println("> Type in password:");
                        pswd = scanner.nextLine();

                        result = stub.authenticate(username, pswd);
                        if (result == null) {
                            System.out.println("Could not authenticate...");
                            return callServer(client, command, stub, scanner);
                        }
                        client.sessionKey = result;
                        return "You're in!";
                    case "register":
                        System.out.println("> Type in username:");
                        username = scanner.nextLine();
                        System.out.println("> Type in password:");
                        pswd = scanner.nextLine();

                        result = stub.register(username, pswd);
                        if (result == null) {
                            System.out.println("Could not authenticate...");
                            return callServer(client, command, stub, scanner);
                        }
                        client.sessionKey = result;
                        return "You're in!";
                    default:
                        System.out.println("> Sorry, I didn't get that. Type 'login' or 'register'...");
                        command = scanner.nextLine();
                        return callServer(client, command, stub, scanner);
                }
            } else {
                switch (command) {
                    case "print":
                        System.out.println("> Please provide filename to be printed:");
                        String filename = scanner.nextLine();

                        System.out.println("> Please provide printer number:");
                        String printer = scanner.nextLine();

                        return stub.print(filename, printer);
                    case "queue":
                        return stub.queue();
                    case "topQueue":
                        System.out.println("> Please provide job ID:");
                        int jobID = scanner.nextInt();

                        return stub.topQueue(jobID);
                    case "start":
                        return "Printer server is already ON...";
                    case "stop":
                        try {
                            return stub.stop();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    case "restart":
                        try {
                            return stub.restart();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    case "status":
                        return stub.status();
                    case "readConfig":
                        System.out.println("> Please provide config parameter:");
                        String param = scanner.nextLine();

                        return stub.readConfig(param);
                    case "setConfig":
                        System.out.println("> Please provide parameter to be set:");
                        param = scanner.nextLine();

                        System.out.println("> Please provide value for the parameter:");
                        String paramValue = scanner.nextLine();

                        return stub.setConfig(param, paramValue);
                    default:
                        System.out.println("Command not recognized. Available commands are: \n" +
                                "print, queue, topQueue, start, stop, restart, status, readConfig, setConfig.\n" +
                                "> Please type in a command again:");
                        command = scanner.nextLine();
                        return callServer(client, command, stub, scanner);
                }
            }
        }
    }

    public static void main(String[] args) {
        String host = (args.length < 1) ? null : args[0];
        String command;

        try {
            Client client = new Client();

            Registry registry = LocateRegistry.getRegistry(host);
            RemoteInterface stub = (RemoteInterface) registry.lookup("RemoteInterface");

            Scanner scanner = new Scanner(System.in);
            while(true){
                System.out.println("--> Type in a command:");
                command = scanner.nextLine();

                System.out.println(callServer(client, command, stub,scanner));
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}