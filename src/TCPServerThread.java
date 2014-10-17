import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;


public class TCPServerThread implements Runnable {

	int port;
	private ServerSocket serverSocket;
	
	public TCPServerThread(int port) {
		// TODO Auto-generated constructor stub
		this.port = port;
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(port);
			serverSocket.setSoTimeout(90000);
			//System.out.println(" => " + getRemoteSocketAddress());
			while(true)
			{
				try
				{
					Socket server = serverSocket.accept();
					//System.out.println(" => " + server.getRemoteSocketAddress());
					new Thread(new ReceivingServerMessage(server)).start();

				} catch(SocketTimeoutException s) {
					System.out.println("Socket timed out on server-1!");
					break;
				} catch(IOException e) {
					e.printStackTrace();
					break;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
