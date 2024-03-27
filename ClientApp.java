import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;

class ClientApp {
	private static final String EXIT_COMMAND = "=";

	private static byte[] prepareRequest(Scanner scanner, int requestId) throws Exception {
		// Prompt the user for input
		System.out.println("OpCode, Operand1, Operand2: ");

		// Read operation code and operands from user input
		int opCode = scanner.nextInt();
		int operand1 = scanner.nextInt();
		int operand2 = scanner.nextInt();

		// Get the operation name based on the operation code
		String operationName = getOperationName(opCode);

		// Convert operation name to byte array
		byte[] operationNameBytes = operationName.getBytes("UTF-16BE");

		// Get the length of operation name byte array
		int operationNameLength = operationNameBytes.length;

		// Prepare the request data
		ByteBuffer buffer = ByteBuffer.allocate(13 + operationNameLength);

		// Add length of the request, operation code, operands, request ID and operation name length to the buffer
		buffer.put((byte) (13 + operationNameLength));
		buffer.put((byte) opCode);
		buffer.putInt(operand1);
		buffer.putInt(operand2);
		buffer.putShort((short) requestId);
		buffer.put((byte) operationNameLength);

		// Add operation name bytes to the buffer
		buffer.put(operationNameBytes);

		// Return the buffer as byte array
		return buffer.array();
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: java ClientTCP <server host name> <server port number>");
			return;
		}

		String serverName = args[0];
		int serverPort = Integer.parseInt(args[1]);

		Scanner scanner = new Scanner(System.in);
		int requestCount = 1;
		System.out.println("Enter OpCode, Operand1 and Operand2 (or enter '=' to exit):");
		while (scanner.hasNext()) {
			if (scanner.hasNext(EXIT_COMMAND)) {
				break;
			}

			try (Socket clientSocket = new Socket(serverName, serverPort);
					 DataInputStream inputFromServer = new DataInputStream(clientSocket.getInputStream());
					 DataOutputStream outputToServer = new DataOutputStream(clientSocket.getOutputStream())) {

				byte[] requestData = prepareRequest(scanner, requestCount);
				long startTime = System.currentTimeMillis();

				outputToServer.writeInt(requestData.length);
				sendRequest(outputToServer, requestData);
				handleResponse(inputFromServer);

				long returnTime = System.currentTimeMillis() - startTime;
				System.out.println("TTL: " + returnTime + " ms");
			} catch (IOException e) {
				System.err.println("Connection error: " + e.getMessage());
			}

			System.out.println("\nEnter OpCode, Operand1 and Operand2 (or enter '=' to exit):");
			requestCount++;
		}
	}


	private static void sendRequest(DataOutputStream outputToServer, byte[] requestData) throws Exception {
		System.out.println("Request in HEX format: " + bytesToHex(requestData));
		outputToServer.write(requestData);
	}

	// Method to handle the response from the server
	private static void handleResponse(DataInputStream inputFromServer) throws IOException {
		// Read the length of the response from the server
		int responseLength = inputFromServer.readInt();

		// Create a byte array to hold the response data
		byte[] receiveData = new byte[responseLength];

		// Read the response data from the server into the byte array
		inputFromServer.readFully(receiveData);

		// Wrap the byte array in a ByteBuffer for easier data extraction
		ByteBuffer wrapped = ByteBuffer.wrap(receiveData);

		// Extract the total message length from the ByteBuffer
		byte tml = wrapped.get();

		// Extract the result from the ByteBuffer
		int result = wrapped.getInt();

		// Extract the error code from the ByteBuffer
		byte errorCode = wrapped.get();

		// Extract the request ID from the ByteBuffer
		short responseRequestId = wrapped.getShort();

		// Print the request ID to the console
		System.out.println("Request ID: " + responseRequestId);

		// Print the result to the console
		System.out.println("Result: " + result);

		// Print the error code to the console. If the error code is 0, print "Ok", otherwise print the error code.
		System.out.println("Error Code: " + (errorCode == 0 ? "Ok" : errorCode));
	}
	private static String getOperationName(int opCode) {
		switch (opCode) {
			case 0: return "multiplication";
			case 1: return "division";
			case 2: return "or";
			case 3: return "and";
			case 4: return "subtraction";
			case 5: return "addition";
			default: return "error";
		}
	}

	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02X ", b));
		}
		return sb.toString();
	}
}