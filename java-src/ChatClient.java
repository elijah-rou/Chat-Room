

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.PrintStream;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import javax.imageio.ImageWriter;
import javax.imageio.ImageWriteParam;
import javax.imageio.IIOImage;
import java.io.IOException;
import javax.imageio.stream.FileCacheImageInputStream;
import java.net.*;
import javax.imageio.stream.ImageOutputStream;
import java.util.Iterator;
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
    private static ImageReceiver ir = null;

    static void sendExitCode(){
        textOutStream.println("#Exit");
    }

    public static void main(String[] args){

        // clear whole screen
        clear();

        //default port
        int port = 5000;
        int picPort = 5001;
        //default host
        String hostname = "localhost";
        System.out.println("Please specify a host name... (blank for localhost)");
        try{
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            hostname = inputLine.readLine().trim();
            if(hostname.equals("")){
                hostname = "localhost";
            }
        }
        catch(Exception e){
            System.out.println(e);
            System.exit(1);
        }
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
            System.out.println("Connecting to " + hostname);
            serverSocket = new Socket(hostname, port);
            pictureSocket = new Socket(hostname, picPort);
            //opening output stream...
            outputStream = serverSocket.getOutputStream();
            textOutStream = new PrintStream(outputStream);
            picOutStream = pictureSocket.getOutputStream();
            //opening input stream...
            inputStream = new DataInputStream(serverSocket.getInputStream());
            picInStream = new DataInputStream(pictureSocket.getInputStream());

            // start thread to recieve images
            ir = new ImageReceiver(picInStream);
            ir.start();

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
                
                // thread to recieve text
                new Thread(new ChatClient()).start();
                /*
                Added a shutdown hook to send server notice of termination
                if forced closed
                */
                Runtime.getRuntime().addShutdownHook(new Thread(){
                            public void run(){
                                ChatClient.sendExitCode();
                            }
                });
                
                textOutStream.println(s);
                while(!closed){
                    s = inputLine.readLine().trim();

                    if (s.equals("#Exit")) {
                        clear();
                        textOutStream.println(s);
                        System.exit(0);
                    }
                    // user wants to send a picture
                    else if (s.equals("#P")){
                        System.out.println("\033[36mPlease specify a file to send (in the img/ folder, #q to cancel)\033[0m");
                        s = inputLine.readLine().trim();
                        if(s.equals("#q")){
                            System.out.println("\033[36mImage transfer cancelled\033[0m");
                            continue;
                        }
                        s = "img/" + s;
                        System.out.println("\033[36mReading " + s + "\033[0m");
                        try{
                            final File path = new File(s);
                            textOutStream.println(s);
                            if(path.exists()){
                                new Thread(){
                                    public void run(){
                                        sendImage(path);
                                    }
                                }.start();
                            }
                            else{
                                System.out.println("\033[36m" + s + " does not exist\033[0m");
                            }
                        }
                        catch(Exception e){
                            System.out.println(e);
                        }
                        continue;

                    }
                    // user wants to download picture
                    else if(s.equals("#D")){
                        System.out.println("\033[35mPlease specify a file to download from the server (#q to cancel)\033[0m");
                        s = inputLine.readLine().trim();
                        if(s.equals("#q")){
                            System.out.println("\033[35mImage download cancelled\033[0m");
                            continue;
                        } //Download code\
                        ImageReceiver.imgName = s;
                        textOutStream.println("D> " + s);
                        continue;
                    }
                    textOutStream.println(s);
                }
                // close pic streams too
                // unexpected shutdown from server
                ir.turnOff();
                textOutStream.close();//close output stream
                picOutStream.close();
                inputStream.close();//close input stream
                picInStream.close();
                serverSocket.close();//close the socket
                pictureSocket.close();
                System.out.println("DISCONNECTED");
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
        String reply;
        try{
            while((reply=inputStream.readLine()) != null){
                System.out.println(reply);
            }
            closed = true;
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
    }

// function that sends an image to the connected server
// also compresses the image
    private static void sendImage(File path){
        try {
            System.out.println("\033[36mCompressing...\033[0m");
            BufferedImage pic = ImageIO.read(path);
            bstream = new ByteArrayOutputStream();
            ImageIO.write(pic, "jpg", bstream);
            byte[] length = ByteBuffer.allocate(4).putInt(bstream.size()).array();
            picOutStream.write(length);
            picOutStream.write(bstream.toByteArray());
            picOutStream.flush();
            System.out.println("\033[36m" + path + " sent!\033[0m");

        }
        catch(Exception e){

        }
    }

    private static void clear(){
        // clears the whole screen
        System.out.print("\033[H\033[2J");  
        System.out.flush();  
    }
}

// thread for listening for incoming image uploads
class ImageReceiver extends Thread{
	private InputStream inStream = null;
	static volatile String imgName = "default.jpg";
    private boolean on = true;

	public ImageReceiver(InputStream stream){
		inStream = stream;
	}

    void turnOff(){
        on = false;
    }
	
	
	public void run(){
		while (on) {
            try {
                if (inStream.available() != 0) {
                    System.out.println("\033[35mReceiving " + imgName + " \033[0m");
                    FileCacheImageInputStream in = new FileCacheImageInputStream(inStream, new File("cache/"));
                    byte[] sizeAr = new byte[4];
                    in.read(sizeAr);
                    int size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
                    byte[] imageAr = new byte[size];
                    in.read(imageAr);
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageAr));

                    System.out.println("\033[35mReceived: " + imgName + " " + image.getWidth() + "x" + image.getHeight()
                            + "@ " + System.currentTimeMillis() + "\033[0m");
                    ImageIO.write(image, "jpg", new File("img/" + imgName));
                    in.flush();
                    in.close();
                }
            } catch (Exception e) {
            }
        }
	}
}