package games;

import games.network.Client;
import games.network.Server;
import games.view.Window;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class App
{
	private static final String SERVER = "--server";
	private static final String JOIN = "--join=";
	private static final String SERVER_PORT = "--serverport=";
	private static final String CLIENT_PORT = "--clientport=";

    public static void main( String[] args ) throws UnknownHostException, InvocationTargetException, InterruptedException {
        if(args.length < 1 || args.length > 3) {
	        printHelp("Invalid number of arguments");
	        return;
        }

        boolean serverPresent = false;
	    InetAddress serverAddress = null;
        int serverPort = -1;
        int clientPort = -1;
        for(String arg : args) {
        	if(arg.equals(SERVER)) {
        		serverPresent = true;
	        } else if(arg.startsWith(JOIN)) {
		        serverAddress = InetAddress.getByName(arg.replace(JOIN, ""));
	        } else if(arg.startsWith(SERVER_PORT)) {
		        serverPort = Integer.parseInt(arg.replace(SERVER_PORT, ""));
	        } else if(arg.startsWith(CLIENT_PORT)) {
		        clientPort = Integer.parseInt(arg.replace(CLIENT_PORT, ""));
	        } else {
        		printHelp("Invalid arguments");
        		return;
	        }
        }

        if(serverPresent && serverAddress != null) {
	        printHelp("--server and --join options cannot be both present");
	        return;
        } else if(!serverPresent && serverAddress == null) {
	        printHelp("Either --server and --join option must be present");
	        return;
        } else if(serverAddress != null && Boolean.logicalXor(serverPort != -1, clientPort != -1)) {
	        printHelp("Both --serverport and --clientport or none must be present with --join");
	        return;
        }

        if(serverPresent) {
        	if(serverPort > 0) {
		        Server server = new Server(serverPort);
	        } else {
		        Server server = new Server();
	        }
        } else {
	        Client client;
	        if(serverPort != -1 && clientPort != -1) {
		        client = new Client(serverAddress, serverPort, clientPort);
	        } else {
		        client = new Client(serverAddress);
	        }
	        SwingUtilities.invokeAndWait(() -> {
		        Window window = new Window();
		        client.setWindow(window);
	        });
	        client.start();
        }
    }

    private static void printHelp(String msg) {
    	System.out.println(msg);
    	System.out.println("Usage as server or client:");
    	System.out.println("--server [--serverport=port]");
    	System.out.println("--join=IP [--serverport=port --clientport=port]");
    	System.out.println("");
    	System.out.println("Example:");
    	System.out.println("--server --listen=8085");
    	System.out.println("--join=127.0.0.1 --serverport=8085 --clientport=8088");
    	System.exit(0);
    }
}
