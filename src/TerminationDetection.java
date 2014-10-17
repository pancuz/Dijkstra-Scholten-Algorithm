import java.io.IOException;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.io.BufferedWriter;
import java.io.PrintWriter;


/* This program is an implementation of the Dijkstra-Scholten algorithm for termination detection.
 * There are fifteen processes, P1; : : : ; P15, of which process P1 is the initiator. Each process, Pi, 
 * once activated goes through the following sequence of actions:
 * 1. Lets a period of time, t, elapse where t is randomly selected from the interval [0:25 second; 1 second],
 * 2. Generates a random value, v, in the range [0; 1]. If 0  v < 0:1, Pi goes from active state to idle state. 
 * If 0.1 <= v <= 1, Pi generates a computation message for a process Pj selected randomly, where j != i.
 * 3. If Pi is idle, it exits this sequence of steps.

 * If an idle process receives a computation message, it becomes activated and resumes executing the sequence of steps
 * listed above.
 */


/* Three java files have been used here. 
 * 
 * "ReceivingServerMessage.java" is for listening to all incoming requests and creating a new thread to process it.
 * "TCPServerThread.java" is to process incoming message by checking if it is computation/ACK message.
 */



public class TerminationDetection{

	public static volatile int ackFlag=0;
	public static volatile int ackCount=0;
	public static String whoIsParent="";
	public static volatile String recordLogForFile = "\n";
	public static String localHost;
	public static ArrayList<String> listOfMachines;
	public static int nodeId = -1;
	public static volatile boolean isIdle;
	public static boolean isPendingAck;
	public static int computeCount=0;
	
	public TerminationDetection() {
		// TODO Auto-generated constructor stub
		listOfMachines = new ArrayList<>();
		
	}

	
	/* Method to return physical time  - returned time will have milli-second reading also
	 * Returned time format - 11:27:24:733
	 * */
	public static String getPhysicalTime(){
		final Date mydt = new Date();
		String date = mydt.toString();
		String [] alldate = date.split("\\s");
		long millis = System.currentTimeMillis() % 1000;
		String myTime = alldate[3]+":"+millis;
		return myTime;
	}

	
	/* To perform a write to log file
	 * This method is called regularly to put log into the file
	 */
	public static synchronized void writeTofile(String fileName,String stringToWrite){

		String givenFilename= "./"+fileName+nodeId;
		//System.out.println("File Path " + givenFilename);
		File file = new File(givenFilename);

		try{
			// if file doesn't exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(stringToWrite);
			bw.close();                                                                                                                                                             //System.out.println("Done");
		} catch (IOException e) {
			e.printStackTrace();
		}
		//After writing to file, clean string buffer for new data
		recordLogForFile= " ";


	}

	/* To return random value between given range (min and max)	 */
	
	public static int getRandomValue(int min,int max){
		/* Generate a random number between min and max range */
		Random  random=new Random(); 
		int d1 = random.nextInt(max - min + 1) + min;
		return d1;
	}

	/* Method used to send either compute or ACK message to selected host
	 * It creates a new client socket on port 8025 and uses printWriter to write the the message to stream
	 */
	public static synchronized void sendMessageToHost(String serverName,String message) {
		
		String [] msgArray = new String[2];
		String comMatch = "compute";
		msgArray = message.split("\\s+");
		String msg = msgArray[0];
		if(msg.equals(comMatch)){
			ackCount++;
			recordLogForFile+=getPhysicalTime()+"\treceiver is " + serverName +"\tSending COMPUTE  message " + "\tNo. of  ACKs yet to be received:  " + ackCount+"\n";
			writeTofile("output.txt",recordLogForFile);
		}

		try{
			Socket client = new Socket(serverName, 8025);                                                                                                                          //System.out.println("Just connected to " + client.getRemoteSocketAddress());
			PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			out.println(message);
		} catch(IOException e){
			e.printStackTrace();
		}
		writeTofile("output.txt",recordLogForFile);
	}

	/* To terminate the execution of the program
	 * Simply exit if termination message is received
	 */
	public static void terminateAll(){
		recordLogForFile+=getPhysicalTime()+"\t### \"Computation Terminated\" ### " + "\n";
		writeTofile("output.txt",recordLogForFile);

		System.out.println(" ======== Terminating ========");
		System.exit(0);
	}
	
	public static void changeToIdle(){
		System.out.println("Changed to idle");
		if(ackCount==0 && isPendingAck){
			ackFlag=0;
			recordLogForFile+=getPhysicalTime()+"\t###  Sending ACK to parent and detaching from tree:  "+whoIsParent+"   ###\n";
			writeTofile("output.txt",recordLogForFile);
			isPendingAck = false;
			sendMessageToHost(whoIsParent,("ACK "+localHost));
			
		}
		else if(ackCount==0) {
			recordLogForFile+=getPhysicalTime()+"\t ### TERMINATION ### \n";

			for(int i=1;i<listOfMachines.size();i++){
				String hostMachine = listOfMachines.get(i);
				sendMessageToHost(hostMachine, ("terminate "+localHost));
			}
			String hostMachine = listOfMachines.get(0);
			sendMessageToHost(hostMachine, ("terminate "+localHost));
		}

	}

	public static synchronized void doComputation(){
	
		int childHost;
		int n;
		int sleepTime = getRandomValue(250,1000);
		System.out.println("Sleeping for" + sleepTime);
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException ie) {
			System.out.println(ie);
		}

		Double randomForAction=Math.random();
		while(randomForAction>0.1 && computeCount<25){
			
			while(nodeId == (n = getRandomValue(1, listOfMachines.size()))  );
			
			
			String hostMachine = listOfMachines.get(n-1);			
			computeCount++;
			sendMessageToHost(hostMachine, ("compute "+localHost));
			randomForAction=Math.random();
		}

		recordLogForFile+=getPhysicalTime()+"\t###  From Active to Idle  ### \n";
		isIdle = true;
		changeToIdle();
	}

	/*method to process incoming request - verify if it is a Compute or ACK message
	 * If it is a compute message, verify if it is first message - if so, declare sender as Parent and start computation;  else -
	 * immediately send an ACK message and strat computation
	 */
	
	public static synchronized void ProcessData(String message) {

		String[] S3 = message.split("\\s+");
		String S1 = S3[0];
		String S2 = S3[1];
		String comMessage = "compute";
		String termMessage = "terminate";
		String ackMessage = "ACK";

		if(comMessage.equals(S1)) {
			System.out.println("***** Received compute message *****  " + TerminationDetection.ackFlag);
			if(TerminationDetection.nodeId == 1){
				TerminationDetection.ackFlag=1;
			}
			if(TerminationDetection.ackFlag==0){
				//this is first computation message, so hold ACK and join tree
				System.out.println("First Message");
				TerminationDetection.isIdle = false;
				TerminationDetection.isPendingAck = true;
				TerminationDetection.ackFlag=1;
				TerminationDetection.recordLogForFile += TerminationDetection.getPhysicalTime() + "\t<<<- received FIRST compute message from sender: " + S2 + "\t";
				TerminationDetection.recordLogForFile += "\n\n\t \t ###\t \t  joined the tree to  parent:  " + S2 + "\t \t ###\n\t\n";
				TerminationDetection.whoIsParent=S2;
				TerminationDetection.writeTofile("output.txt",TerminationDetection.recordLogForFile);
				TerminationDetection.doComputation();

			} else if(TerminationDetection.ackFlag==1) {
					//If system is idle and received compute message, it will start computations again - calloing doComputation()
				if(TerminationDetection.isIdle ==true && TerminationDetection.nodeId != 1){
					TerminationDetection.isIdle = false;
					TerminationDetection.recordLogForFile += TerminationDetection.getPhysicalTime()+ "\t<<<- received compute message from sender: " + S2 + "\t ---> ACK is being sent back\n";
					TerminationDetection.writeTofile("output.txt",TerminationDetection.recordLogForFile);
					TerminationDetection.sendMessageToHost(S2,("ACK "+TerminationDetection.localHost));
					TerminationDetection.doComputation();

				}
				else {
				TerminationDetection.recordLogForFile += TerminationDetection.getPhysicalTime() + 
						"\t<<<- received compute message from sender: " + S2 + "\t ---> ACK is being sent back\n";
				TerminationDetection.sendMessageToHost(S2,("ACK "+TerminationDetection.localHost));
				TerminationDetection.writeTofile("output.txt",TerminationDetection.recordLogForFile);
				}

			}
		} else if(termMessage.equals(S1)) {
			System.out.println(" === Received Terminate message from : " + S2 );
			TerminationDetection.terminateAll();

		} else if(ackMessage.equals(S1)) {
			--TerminationDetection.ackCount;
			System.out.println("\\### Received ACK message ### " + TerminationDetection.ackCount);
			TerminationDetection.recordLogForFile += TerminationDetection.getPhysicalTime() +"\t<<<- received ACK message from server: " + S2 + "\tNo. of ACKs yet to be received: "+TerminationDetection.ackCount+"\n";
			TerminationDetection.writeTofile("output.txt",TerminationDetection.recordLogForFile);
			if(TerminationDetection.ackCount == 0) {
				System.out.println("All Acknowledgments have been received");
				if(isIdle == true) {
					changeToIdle();
				} 
			}
			
		}
	}

	/* To start listening for all incoming requests on port 8025*/
	private static void StartServerThread(int port) {
		try{
			TCPServerThread tst = new TCPServerThread(port);
			Thread t = new Thread(tst);
			t.start();
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		
		TerminationDetection t = new TerminationDetection();
		
		int lineCount = 0;

		if(args.length == 0) {
			System.out.println("Usage : java TerminationDetection nodeId");
		} else {
			nodeId = Integer.parseInt(args[0]);
			
			String fileName = new File("configuration.txt").getAbsolutePath();
			try {
				FileReader configFile = new FileReader(fileName);
				BufferedReader bufferReader = new BufferedReader(configFile);
				String line;

				while ((line = bufferReader.readLine()) != null) {
					lineCount++;
					if(lineCount == nodeId) {
						localHost = line;
					}
					listOfMachines.add(line);
				}
			} catch(FileNotFoundException ex) {
				System.out.println("Configuration file not found! " + ex);
			} catch(Exception ex) {
				ex.printStackTrace();
			}


// Start server thread on all machines
			StartServerThread(8025);
// If it is first node - make it the initiator node 			
			if(nodeId ==1){
			System.out.println("For 1 -start compute");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			doComputation();
			}
		}

	}
}
