import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

class ServerApp {
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Usage: java Server <port number>");
			return;
		}

		int portNumber = Integer.parseInt(args[0]);
		ServerSocket serverSocket = new ServerSocket(portNumber);

		while(true) {
			try (Socket clientSocket = serverSocket.accept();
					 DataInputStream inputFromClient = new DataInputStream(clientSocket.getInputStream());
					 DataOutputStream outputToClient = new DataOutputStream(clientSocket.getOutputStream())) {

				// Receive and parse data from client
				int totalMessageLength = inputFromClient.readInt();
				byte[] receiveData = new byte[totalMessageLength];
				inputFromClient.readFully(receiveData);
				ByteBuffer wrapped = ByteBuffer.wrap(receiveData);
				byte totalMessageLengthFromClient = wrapped.get();
				byte operationCode = wrapped.get();
				int operand1 = wrapped.getInt();
				int operand2 = wrapped.getInt();
				short requestId = wrapped.getShort();
				byte operationNameLength = wrapped.get();
				byte[] operationNameBytes = new byte[operationNameLength];
				wrapped.get(operationNameBytes);
				String operationName = new String(operationNameBytes, "UTF-16BE");

				// Log received data
				System.out.printf("Request: %d, Operation: %d, Operand1: %s, Operand2: %d%n", requestId, operationName, operand1, operand2);

				// Perform operation and prepare response
				int result = performOperation(operationCode, operand1, operand2);
				ByteBuffer buffer = ByteBuffer.allocate(8);
				buffer.put((byte)8);
				if (operationCode > 5){  // Error Code
					buffer.putInt(-1);
					buffer.put((byte)1);
				}
				else {
					buffer.putInt(result);
					buffer.put((byte) 0);
				}
				buffer.putShort(requestId);
				byte[] sendData = buffer.array();

				// Send response to client
				outputToClient.writeInt(sendData.length);
				outputToClient.write(sendData);
			}
		}
	}

	private static int performOperation(byte operationCode, int operand1, int operand2) {
		switch(operationCode) {
			case 0: return operand1 * operand2;
			case 1: return operand1 / operand2;
			case 2: return operand1 | operand2;
			case 3: return operand1 & operand2;
			case 4: return operand1 - operand2;
			case 5: return operand1 + operand2;
			default: return -1;
		}
	}
}