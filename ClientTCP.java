import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Scanner;

class ClientTCP {
	private static final String EXIT_COMMAND = "q";

	public static void main(String[] args) throws Exception {
		// Check if server name and port number are provided
		if (args.length != 2) {
			return;
		}

		String serverName = args[0];
		int serverPort = Integer.parseInt(args[1]);

		System.out.println("Client started");
		Scanner scanner = new Scanner(System.in);
		int requestCount = 1;
		System.out.println("Enter OpCode, Operand1, Operand2 or Q to exit:");
		while (scanner.hasNext()) {
			if (scanner.hasNext(EXIT_COMMAND)) {
				scanner.next();
				break;
			}
			// Create a socket and connect to the server
			try (Socket clientSocket = new Socket(serverName, serverPort)) {
				// Create input and output streams
				DataInputStream inputFromServer = new DataInputStream(clientSocket.getInputStream());
				DataOutputStream outputToServer = new DataOutputStream(clientSocket.getOutputStream());

				// Prepare the request data
				byte[] requestData = prepareRequest(scanner, requestCount);
				long startTime = System.currentTimeMillis();  // Get the current time before sending the request

				// Send the request data to the server
				outputToServer.writeInt(requestData.length); // Send the length of the request data first
				sendRequest(outputToServer, requestData);

				// Handle the response from the server
				handleResponse(inputFromServer);

				long endTime = System.currentTimeMillis();  // Get the current time after receiving the response

				// Calculate and print the return time
				long returnTime = endTime - startTime;
				System.out.println("Return Time: " + returnTime + " ms");
			}

			System.out.println("Enter OpCode, Operand1, Operand2 or Q to exit:");
			requestCount++;
		}
	}

	private static byte[] prepareRequest(Scanner scanner, int requestId) throws Exception {
		System.out.println("OpCode, Operand1, Operand2: ");
		int opCode = scanner.nextInt();
		int operand1 = scanner.nextInt();
		int operand2 = scanner.nextInt();

		// Get the operation name based on the operation code
		String operationName = getOperationName(opCode);
		byte[] operationNameBytes = operationName.getBytes("UTF-16BE");
		int operationNameLength = operationNameBytes.length;

		// Prepare the request data
		ByteBuffer buffer = ByteBuffer.allocate(13 + operationNameLength);
		buffer.put((byte) (13 + operationNameLength));
		buffer.put((byte) opCode);
		buffer.putInt(operand1);
		buffer.putInt(operand2);
		buffer.putShort((short) requestId);
		buffer.put((byte) operationNameLength);
		buffer.put(operationNameBytes);

		return buffer.array();
	}

	private static String getOperationName(int opCode) {
		// Return the operation name based on the operation code
		switch (opCode) {
			case 0: return "multiplication";
			case 1: return "division";
			case 2: return "or";
			case 3: return "and";
			case 4: return "subtraction";
			case 5: return "addition";
			default: return "";
		}
	}

	private static void sendRequest(DataOutputStream outputToServer, byte[] requestData) throws Exception {
		System.out.println("Request in HEX: " + bytesToHex(requestData));
		outputToServer.write(requestData);
	}

	private static void handleResponse(DataInputStream inputFromServer) throws Exception {
		int responseLength = inputFromServer.readInt();
		byte[] receiveData = new byte[responseLength];
		inputFromServer.readFully(receiveData);

		// Parse the response packet
		ByteBuffer wrapped = ByteBuffer.wrap(receiveData);
		byte tml = wrapped.get();
		int result = wrapped.getInt();
		byte errorCode = wrapped.get();
		short responseRequestId = wrapped.getShort();

		// Print the response
		System.out.println("Response in HEX: " + bytesToHex(receiveData));
		System.out.println("Request ID: " + responseRequestId);
		System.out.println("Result: " + result);
		System.out.println("Error Code: " + (errorCode == 0 ? "Ok" : errorCode));
	}

	private static String bytesToHex(byte[] bytes) {
		// Convert byte array to hex string
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02X ", b));
		}
		return sb.toString();
	}
}