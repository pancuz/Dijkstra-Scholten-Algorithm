import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;


public class ReceivingServerMessage implements Runnable {
	
	Socket sockId;
	String comMessage = "compute";
	String termMessage = "terminate";
	String ackMessage = "ACK";
	String S1,S2;
	

	public ReceivingServerMessage(Socket server) {
		// TODO Auto-generated constructor stub
		sockId = server;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		String rcvdM;
		//System.out.println("in Received MSG: ");
		InputStreamReader in;
		try {
			in = new InputStreamReader(sockId.getInputStream());
			BufferedReader reader = new BufferedReader(in);
			rcvdM = reader.readLine();
			
			TerminationDetection.ProcessData(rcvdM);

			} catch (Exception e) {
				e.printStackTrace();
		}
		

	}

}
