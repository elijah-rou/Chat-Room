

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.*;

public class ChatClient implements Runnable {
    //client socket
    private static Socket clientSocket = null;
    //output stream
    private static PrintStream textOutStream=null;
    //input stream
    private static DataInputStream inputStream = null;

    private static BufferedReader inputLine = null;
    private static boolean closed = false;

    public static void main(String[] args){
        //default port
        int portNumber = 5000;
        //default host
        String hostname="localhost";

        //if port and hostname supplied
        if(args.length == 2)
        {
            hostname = args[0];
            portNumber = Integer.parseInt(args[1]);
        }

        //open a socket on the host and port
        //open input and output stream
        try{
            //creating socket...
            clientSocket = new Socket(hostname, portNumber);

            inputLine = new BufferedReader(new InputStreamReader(System.in));
            //opening output stream...
            /*
            ELI
            Seperated output and input streams into seperate
            objects order to be able to send pictures
            */
            outputStream = clientSocket.getOutputStream()
            textOutStream = new PrintStream(outputStream);
            //opening input stream...
            inputStream = new DataInputStream(clientSocket.getInputStream());
        }
        catch(IOException e)
        {
            System.out.println(e);
        }

        if(clientSocket !=null && textOutStream != null && inputStream !=null) //to ensure everything has been initialized
        {
            try{
                //create thread that will read from the server
                new Thread(new ChatClient()).start();
                while(!closed){
                    textOutStream.println(inputLine.readLine().trim());
                }
                //when closed == true
                textOutStream.close();//close output stream
                inputStream.close();//close input stream
                clientSocket.close();//close the socket
            }
            catch(IOException e)
            {
                System.out.println(e);
            }
        }
    }

    //creating thread that will read from the server
    public void run()
    {
        //want to keep reading from the socket until the greeting message from the server is received
        //once the greeting message received, want to end.
        String reply;
        try{
            while((reply=inputStream.readLine()) != null){
                System.out.println(reply);
                if(reply.contains("# Goodbye"))
                {
                    break;
                }
            }
            closed = true;
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
    }
}

