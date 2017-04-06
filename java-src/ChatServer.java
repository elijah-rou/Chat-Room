

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.File;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.stream.FileCacheImageInputStream;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import java.net.*;
import java.nio.ByteBuffer;
import java.awt.image.BufferedImage;

public class ChatServer {
    //server socket for text
    private static ServerSocket serverSocket = null;
	//server socket for pictures
	private static ServerSocket pictureSocket =  null;
    //client socket
    private static Socket clientSocket = null;
	private static Socket clientPicSocket = null;

    //this chat server can accept up to 5 connections
    static final int maxNumClients = 5;
    private static final clientThread[] threads = new clientThread[maxNumClients];

	static ImageHandler ih = null;



    public static void main(String[] args){
        //default port number

		/*
		ELI
		changed port number to accomodate server
		*/
        int port = 5000;
		int picPort = 5001;

        //open a server socket on the port
        try{
            serverSocket = new ServerSocket(port);
			pictureSocket = new ServerSocket(picPort);
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
		System.out.print("\033[H\033[2J");  
        System.out.flush();
		System.out.println("Server started on " + serverSocket.getLocalSocketAddress().toString());
        //create a client socket for each connection & pass it to a new client thread
		ih = new ImageHandler();
		ih.start();
        while(true){
            try{
                clientSocket = serverSocket.accept();
				clientPicSocket = pictureSocket.accept();
                int i=0;
                for(i=0;i<maxNumClients;i++){
                    if(threads[i]==null) {
						System.out.println("\nConnecting Client " + i + "...");
                        threads[i] = new clientThread(clientSocket, threads, i);
						System.out.println("Text Socket: " + clientSocket.getRemoteSocketAddress());
                        threads[i].start();
						ih.addSocket(new Tuple<InputStream, Socket>(clientPicSocket.getInputStream(), clientPicSocket), i);
                        break;
                    }
                }
                if(i==maxNumClients){
                    PrintStream outputStream=new PrintStream(clientSocket.getOutputStream());
                    outputStream.println("Server too busy");
                    outputStream.close();
                    clientSocket.close();
                }
            }catch(IOException e){
                System.out.println(e);
            }
			try{
				
			}
			catch(Exception e){
				System.out.println(e);
			}
        }
    }
}

//thread for each chat client
//thread opens the input and output streams for a particular client
//it gets the name and informs all in the chatroom that new client has joined
//while it recieves data, it sends it back to all chat clients in the chat room
//it also broadcast the incoming messages to all clients
//will inform chat room when the particular client leaves
class clientThread extends Thread{
	private int number = 0;
	private String clientName = null;
	private String address = null;
	private DataInputStream inputStream = null;
	private PrintStream outputStream = null;
	private Socket clientSocket = null;
	private final clientThread[] threads;
	private int maxNumClients;

	public clientThread(Socket clientSocket,clientThread[] threads, int num){
		this.clientSocket = clientSocket;
		this.threads = threads;
		number = num;
		maxNumClients = threads.length;
	}
	public int getNumber(){
		return number;
	}

	public void run()
	{
		
		int maxNumClients = this.maxNumClients;
		clientThread[] threads = this.threads;

		try{

			//create input stream for this client
			inputStream = new DataInputStream(clientSocket.getInputStream());
			//create ouput stream for this client
			outputStream = new PrintStream(clientSocket.getOutputStream());
			String name;
			//promt chat client for his/her name
			//REMOVED //outputStream.println("Enter your name:");
			//assign the name
			name = inputStream.readLine().trim();
			address = clientSocket.getRemoteSocketAddress().toString();

			// name is given an identifier based on the 
			// last 2 numbers of the port it's connected to
			Integer num = clientSocket.getPort();
			name = name + "<" + num.toString().substring(3) + ">";
			// added client name here

			clientName = "@"+name;
			System.out.print(address);
			System.out.println(" connected as: " + name);
			//welcome chat client to the room
			outputStream.println("Welcome "+name+" to the chat room.\nTo leave type #Exit in a seperate line.\n");
			outputStream.println("Commands: \n#P - Send pictue to server\n#D - Download picture from server\n");

			synchronized(this)
			{
				// not needed
				/*
				for(int i=0;i<maxNumClients;i++)
				{
					if(threads[i]!=null && threads[i]==this)
					{
						//name to be displayed as in room
						clientName = "@"+name;
						break;
					}
				}
				*/
				for(int i=0;i<maxNumClients;i++)
				{
					if(threads[i]!=null && threads[i]!=this && threads[i].clientName !=null)
					{
						threads[i].outputStream.println("--- "+name+" entered the chat room. ---");
					}
				}
			}//sync
			
			//start the conversation
			while(true)
			{
				String line=inputStream.readLine();
				if(line.startsWith("#Exit"))
				{
					break;
				}
				//broadcast message to the chatroom
				if (line.startsWith("@")) {
					//split the persons name from the actual msg
					String[] msg = line.split(">", 2);
					msg[0] += ">";
					if (msg.length >= 1 && msg[1] != null)//check that it actually contains a msg
					{
						msg[1] = msg[1].trim();
						if (!msg[1].isEmpty()) {
							synchronized (this) {
								for (int i = 0; i < maxNumClients; i++) {
									if (threads[i] != null && threads[i] != this && threads[i].clientName != null
											&& threads[i].clientName.equals(msg[0])) {
										System.out.println(name + " to " + msg[0] + ": " + msg[1]);
										threads[i].outputStream.println("\033[1mFrom " + name + ":\033[0;0m " + msg[1]);
										this.outputStream.println("\033[1mTo " + msg[0].substring(1) + ":\033[0;0m " + msg[1]);
										break;
									}
									if(i==maxNumClients-1){
										this.outputStream.println("User \033[1m" + msg[0].substring(1) + "\033[0;0m doesn't exist");
									}
								}
							}
						}
					}
					else{
						this.outputStream.println("\033[1mNo message!\033[0;0m");
					}
				}
				else if (line.startsWith("D>")){
					synchronized (this){
						String[] msg = line.split(" ", 2);
						String path = "server-img/" + msg[1];
						try{
							File img = new File(path);
							if(img.exists()){
								//System.out.println(ChatServer.ih);
								ChatServer.ih.sendImage(img, this.getNumber());
								System.out.println(path + " sent to: " + ChatServer.ih.getAddress(this.getNumber()));
								continue;
							}
							else{
								this.outputStream.println("Image doesn't exist on server!");
							}
						}
						catch(Exception e){
							this.outputStream.println(e);
						}
					}
				}
				else {
					synchronized (this) {
						//System.out.println(line.substring(4));
						//System.out.println(line.substring(line.length()-4));
						// Check is it an image?
						if(line.length() > 7 && line.substring(0, 4).equals("img/") && 
						(line.substring(line.length()-4).equals(".jpg") || line.substring(line.length()-4).equals(".png"))){
							line = line.substring(4);
							ImageHandler.imgName = line;
							line = "uploaded \033[3m" + line + " \033[0m" ;
						}
						// entension is jpeg?
						else if(line.length() > 8 && line.substring(0, 4).equals("img/") && 
								line.substring(line.length()-5).equals(".jpeg")){
							line = line.substring(4);
							ImageHandler.imgName = line;
							line = "uploaded \033[3m" + line + " \033[0m" ;
						}
						//System.out.println(ImageHandler.imgName);
						System.out.println(
								"Broadcast: " + line + ", From: " + name + "@" + clientSocket.getRemoteSocketAddress());
						for (int i = 0; i < maxNumClients; i++) {
							if (threads[i] != null && threads[i].clientName != null
									&& !(threads[i].clientName.equals("@" + name))) {
								threads[i].outputStream.println(name + ": " + line);
							}
						}
					}
				}
			}
			//when a chat client leaves
			synchronized(this)
			{
				for(int i=0;i<maxNumClients;i++)
				{
					if(threads[i]!=null && threads[i]!= this && threads[i].clientName !=null)
					{
						threads[i].outputStream.println("--- "+name+" has left the chat room.---");
					}
				}
			}
			outputStream.println("--- Goodbye "+name+" ---");

			//set current thread to null so new client can be accepted by server
			synchronized(this)
			{
				for(int i=0;i<maxNumClients;i++)
				{
					if(threads[i]==this)
					{
						ChatServer.ih.removeSocket(i);
						threads[i]=null;
					}
				}
			}
			
			//close input stream
			inputStream.close();
			//close ouput stream
			outputStream.close();
			//close the socket
			clientSocket.close();
		}catch(IOException e)
		{
		System.out.println(e);
		}
	}
}

class ImageHandler extends Thread{
	private Tuple<InputStream, Socket>[] clients = null;
	static volatile String imgName = "default.jpg";

	public ImageHandler(){
		clients = new Tuple[ChatServer.maxNumClients];
		System.out.println(clients.length + " clients permitted.");
	}

	public void addSocket(Tuple<InputStream, Socket> s, int arrayPos){
		System.out.println("Media Socket: " + s.getY().getRemoteSocketAddress() + "\n");
		clients[arrayPos] = s;
	}

	public void removeSocket(int arrayPos){
		clients[arrayPos] = null;
	}

	public Socket getSocket(int index) throws IOException{
		return clients[index].y;
	}

	public String getAddress(int index) throws IOException{
		return clients[index].y.getRemoteSocketAddress().toString();
	}

	public void sendImage(File img, int threadIndex) {
		try {
			// send an image to the server
			System.out.print("Attempting to send image: " + img.getName());
			Socket s = getSocket(threadIndex);
			System.out.println(" to " + s.getRemoteSocketAddress());
			OutputStream outStream = s.getOutputStream();
			BufferedImage pic = ImageIO.read(img);
			ByteArrayOutputStream bstream = new ByteArrayOutputStream();
			ImageIO.write(pic, "jpg", bstream);
			byte[] length = ByteBuffer.allocate(4).putInt(bstream.size()).array();
			outStream.write(length);
			outStream.write(bstream.toByteArray());
			outStream.flush();
		} 
		catch (Exception e) {
			System.out.println("Send Image Error: " + e);
		}
	}
	
	
	public void run(){
		System.out.println("Image Handler Started");
		while(true){
			//Socket socket = pictureSocket.accept();
			//inputStream = socket.getInputStream();

			for(Tuple<InputStream, Socket> k : clients){
				if(k != null){
					//System.out.println(k.getY().getRemoteSocketAddress().toString());
					try{
						//System.out.println("Attempting to write");
						synchronized(this){
							if(k.getX().available() != 0){
								FileCacheImageInputStream in = new FileCacheImageInputStream(k.getX(), new File("cache/"));
								
								System.out.println("Reading: " + System.currentTimeMillis());
								byte[] sizeAr = new byte[4];
								in.read(sizeAr);
								int size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
								System.out.println("Reading: " + System.currentTimeMillis());
								byte[] imageAr = new byte[size];
								in.read(imageAr);
								BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageAr));

								System.out.println("Received " + image.getHeight() + "x" + image.getWidth() + ": " + System.currentTimeMillis());
								ImageIO.write(image, "jpg", new File("server-img/" + imgName));
								in.flush();
								in.close();
							}
						}
					}
					catch(Exception e){
						//System.out.println(e);
					}
				}
			}
		}
	}
	
}

