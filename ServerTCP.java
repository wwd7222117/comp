import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

class ServerTCP {
	public static void main(String[] args) throws Exception {
		// Check if port number is provided
		if (args.length != 1) {
			System.out.println("Arguments Error");
			return;
		}

		// Parse the port number
		int portNumber = Integer.parseInt(args[0]);

		// Create a socket on the given port number
		ServerSocket serverSocket = new ServerSocket(portNumber);
		while(true) {
			try (Socket clientSocket = serverSocket.accept();
					 DataInputStream inputFromClient = new DataInputStream(clientSocket.getInputStream());
					 DataOutputStream outputToClient = new DataOutputStream(clientSocket.getOutputStream())) {

				// Receive data
				int totalMessageLength = inputFromClient.readInt();
				byte[] receiveData = new byte[totalMessageLength];
				inputFromClient.readFully(receiveData);

				ByteBuffer wrapped = ByteBuffer.wrap(receiveData);
				// Parse the received data
				byte totalMessageLengthFromClient = wrapped.get();
				byte operationCode = wrapped.get();
				int operand1 = wrapped.getInt();
				int operand2 = wrapped.getInt();
				short requestId = wrapped.getShort();
				byte operationNameLength = wrapped.get();
				byte[] operationNameBytes = new byte[operationNameLength];
				wrapped.get(operationNameBytes);
				String operationName = new String(operationNameBytes, "UTF-16BE");
				System.out.println("RequestID: " + requestId);
				System.out.println("Operand1: " + operand1);
				System.out.println("Operation Name: " + operationName);
				System.out.println("Operand2: " + operand2);
				int result = performOperation(operationCode, operand1, operand2);
				// Prepare the response packet
				ByteBuffer buffer = ByteBuffer.allocate(8);
				buffer.put((byte)8);
				buffer.putInt(result);
				buffer.put((byte)0); // Error Code
				buffer.putShort(requestId);
				byte[] sendData = buffer.array();

				// Send the response packet
				outputToClient.writeInt(sendData.length);
				outputToClient.write(sendData);
			}
		}
	}
	// perform operation
	private static int performOperation(byte operationCode, int operand1, int operand2) {
		switch(operationCode) {
			case 0: return operand1 * operand2;
			case 1: return operand1 / operand2;
			case 2: return operand1 | operand2;
			case 3: return operand1 & operand2;
			case 4: return operand1 - operand2;
			case 5: return operand1 + operand2;
			default: return 127;
		}
	}
}