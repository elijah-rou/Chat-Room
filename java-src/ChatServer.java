

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.*;

public class ChatServer {
    //server socket
    private static ServerSocket serverSocket = null;
    //client socket
    private static Socket clientSocket = null;

    //this chat server can accept up to 5 connections
    private static final int maxNumClients = 5;
    private static final clientThread[] threads = new clientThread[maxNumClients];

    public static void main(String[] args){
        //default port number
        int portNumber = 1995;
        //open a server socket on the portnumber
        try{
            serverSocket = new ServerSocket(portNumber);
        }
        catch(IOException e)
        {
            System.out.println(e);
        }

        //create a client socket for each connection & pass it to a new client thread
        while(true){
            try{
                clientSocket = serverSocket.accept();
                int i=0;
                for(i=0;i<maxNumClients;i++){
                    if(threads[i]==null) {
                        threads[i] = new clientThread(clientSocket, threads);
                        threads[i].start();
                        break;
                    }
                }
                if(i==maxNumClients){
                    PrintStream outputStream=new PrintStream(clientSocket.getOutputStream());
                    outputStream.println("Server to busy");
                    outputStream.close();
                    clientSocket.close();
                }
            }catch(IOException e){
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
	private String clientName = null;
	private DataInputStream inputStream = null;
	private PrintStream outputStream = null;
	private Socket clientSocket = null;
	private final clientThread[] threads;
	private int maxNumClients;

	public clientThread(Socket clientSocket,clientThread[] threads){
		this.clientSocket = clientSocket;
		this.threads = threads;
		maxNumClients = threads.length;
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
			outputStream.println("Enter your name:");
			//assign the name
			name=inputStream.readLine().trim();
		
		
		//welcome chat client to the room
		outputStream.println("Welcome "+name+" to the chat room.\nTo leave type #Exit in a seperate line.");
		synchronized(this)
		{
			for(int i=0;i<maxNumClients;i++)
			{
				if(threads[i]!=null && threads[i]==this)
				{
					//name to be displayed as in room
					clientName = "@"+name;
					break;
				}
			}
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
			synchronized(this)
			{
				for(int i=0;i<maxNumClients;i++)
				{
					if(threads[i]!=null && threads[i].clientName != null)
					{
						threads[i].outputStream.println(name+": "+line);
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
