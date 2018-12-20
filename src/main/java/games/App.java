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
    public static void main( String[] args ) throws UnknownHostException, InvocationTargetException, InterruptedException {
        if(args.length < 1 || args.length > 2) {
	        printHelp("Invalid number of arguments");
        } else if("--server".equalsIgnoreCase(args[0])) {
            Server server = new Server();
        } else if(args.length != 2) {
	        printHelp("Invalid number of arguments");
        } else if("--join".equalsIgnoreCase(args[0])) {
	        InetAddress serverAddress = InetAddress.getByName(args[1]);
	        Client client = new Client(serverAddress);
	        SwingUtilities.invokeAndWait(() -> {
		        Window window = new Window();
		        client.setWindow(window);
	        });
	        client.start();
        } else {
	        printHelp("Invalid arguments");
        }
    }

    private static void printHelp(String msg) {
    	System.out.println(msg);
    	System.out.println("Usage as server or client:");
    	System.out.println("--server");
    	System.out.println("--join IP (e.g. --join 127.0.0.1");
    	System.exit(0);
    }
}
