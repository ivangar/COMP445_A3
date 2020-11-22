import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;
import java.io.*;

/**
 * httpfs is a file server application using Java Socket standar API libraries
 * It is responsible for creating a server instance and wait for client to communicate through the socket
 * @authors
 * Ivan Garzon 27006284
 * Ki Ho Lee 40073402
 */

public class httpfs {

    private static int serverPort;

    public static void main(String[] args){

        setServerPort(args);
        printHelp(args);
        new httpfs().runServer(args);

    }

    /**
     * Generates a ServerSocket with try-with-resources, waits for client Socket and uses httpfsLibrary to
     * process and handle client requests and return a response back. It listens infinitely in a loop until
     * process is killed
     * @param args arguments passed from terminal
     */
    public void runServer(String[] args){

        try (ServerSocket server = new ServerSocket(serverPort)){

            while (true) {
                System.out.println("Server is connected at port " + serverPort + " waiting for the Client to connect.");
                Socket client = server.accept();

                System.out.println();
                System.out.println("Client and Server are connected from httpfsLibrary.");
                System.out.println("---------------------- Http Client Request ----------------------------");

                httpfsLibrary httpfsLib = new httpfsLibrary(args, client);
                httpfsLib.parseClientRequest();

                client.close();
            }

        }catch (IOException e){
            System.out.println("Connection Problem with connection or port " + serverPort);
            System.out.println(e.getMessage());
        }
    }

    /**
     * Sets the server port
     * @param args arguments passed from terminal
     */
    private static void setServerPort(String[] args){
        int findP = Arrays.asList(args).indexOf("-p");
        if(findP == -1){
            serverPort = 8080;
        }
        else
            serverPort = Integer.parseInt(args[findP+1]);
    }

    /**
     * Prints help commands to user
     * @param args arguments passed from terminal
     */
    private static void printHelp(String[] args){
        if(args[0].equalsIgnoreCase("help")){
            System.out.println("httpfs is a simple file server.");
            System.out.println("usage: httpfs [-v] [-p PORT] [-d PATH-TO-DIR]");
            System.out.println("  -v Prints debugging messages.");
            System.out.println("  -p Specifies the port number that the server will listen and serve at.");
            System.out.println("     Default is 8080.");
            System.out.println("  -d Specifies the directory that the server will use to read/write\r\nrequested files. Default is the current directory when launching the\r\napplication.");
            System.exit(0);
        }
    }
}
