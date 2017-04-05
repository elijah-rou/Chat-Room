

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.PrintStream;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

public class ChatClient implements Runnable {
    //client socket
    private static Socket serverSocket = null;
    private static Socket pictureSocket = null;
    //output stream
    private static OutputStream outputStream = null;
    private static OutputStream picOutStream = null;
    private static ByteArrayOutputStream bstream = null;
    private static PrintStream textOutStream = null;
    //input stream
    private static InputStream picInStream = null;
    private static DataInputStream inputStream = null;

    private static BufferedReader inputLine = null;
    private static boolean closed = false;

    public static void main(String[] args){

        // clear whole screen
        clear();

        //default port
        int port = 5000;
        int picPort = 5001;
        //default host
        String hostname="localhost";

        //if port and hostname supplied
        if(args.length == 2)
        {
            hostname = args[0];
            port = Integer.parseInt(args[1]);
        }

        //open a socket on the host and port
        //open input and output stream
        try{
            //creating socket...
            serverSocket = new Socket(hostname, port);
            pictureSocket = new Socket(hostname, picPort);

            inputLine = new BufferedReader(new InputStreamReader(System.in));
            //opening output stream...
            /*
            ELI
            Seperated output and input streams into seperate
            objects order to be able to send pictures
            */
            outputStream = serverSocket.getOutputStream();
            textOutStream = new PrintStream(outputStream);

            picOutStream = pictureSocket.getOutputStream();
            bstream = new ByteArrayOutputStream();
            //opening input stream...
            inputStream = new DataInputStream(serverSocket.getInputStream());
            picInStream = new DataInputStream(pictureSocket.getInputStream());

        }
        catch(IOException e)
        {
            System.out.println(e);
            System.exit(0);
        }

        if(serverSocket !=null && textOutStream != null && inputStream !=null) //to ensure everything has been initialized
        {
            try{
                //create thread that will read from the server
                System.out.println("Enter your name");
                String s = inputLine.readLine().trim();
                clear();
                System.out.println("\nConnected to " + hostname + ":" + port + " as " + s + "\n");
                new Thread(new ChatClient()).start();
                // sepearate thread for receiving images
                //new Thread(new ImageReceiver()).start();

                /*
                ELI
                Added a shutdown hook to send server notice of termination
                if forced closed
                */
                Runtime.getRuntime().addShutdownHook(new Thread(){
                            public void run(){
                                textOutStream.println("#Exit");
                            }
                });
                // end

                /*
                ELI
                Made more neat by clearing screen 
                after user enters name
                */
                textOutStream.println(s);
                while(!closed){
                    /*
                    ELI
                    Added check here to remove reliance on server
                    to quit
                    */
                    //query();
                    s = inputLine.readLine().trim();

                    // want to clear screen here
                    //System.out.print("\033[H\033[2J");  
                    //System.out.flush();  
                    
                    if (s.equals("#Exit")) {
                        clear();
                        textOutStream.println(s);
                        System.exit(0);
                    }
                    // user wants to send a picture
                    else if (s.equals("#P")){
                        System.out.println("Please specify a file to send (in the img/ folder, #q to cancel)");
                        s = inputLine.readLine().trim();
                        if(s.equals("#q")){
                            System.out.println("Image transfer cancelled");
                            continue;
                        }
                        s = "img/" + s;
                        System.out.println("Reading " + s);
                        try{
                            final String path = s;
                            new Thread(){
                                public void run(){
                                    sendImage(path);
                                }
                            }.start();
                        }
                        catch(Exception e){
                            System.out.println(e);
                        }

                    }
                    textOutStream.println(s);
                    //end
                }
                // close pic streams too
                //when closed == true
                textOutStream.close();//close output stream
                inputStream.close();//close input stream
                serverSocket.close();//close the socket
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
        /* REDONE
        //want to keep reading from the socket until the greeting message from the server is received
        //once the greeting message received, want to end.
           REDONE
        */
        String reply;
        try{
            while((reply=inputStream.readLine()) != null){
                System.out.println(reply);
                /*
                ELI
                Unadvised to do this, need to remove reliance on server for 
                termination
                
                if(reply.contains("# Goodbye"))
                {
                    break;
                }
                */
            }
            closed = true;
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
    }

    /*
    Eli Methods
    */

    private static void sendImage(String path){
        try{
            // send an image to the server
            BufferedImage pic = ImageIO.read(new File(path));
            ImageIO.write(pic, "jpg", bstream);
            byte[] length = ByteBuffer.allocate(4).putInt(bstream.size()).array();
            picOutStream.write(length);
            picOutStream.write(bstream.toByteArray());
            picOutStream.flush();
        }
        catch(Exception e){

        }
    }

    private static void clear(){
        // clears the whole screen
        System.out.print("\033[H\033[2J");  
        System.out.flush();  
    }

    private static void query(){
        // query client for message
        System.out.print("\n|YOU> ");
        System.out.flush();
    }
}

class ImageReceiver extends Thread{

}

