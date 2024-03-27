import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;

class Client {
	private static final String EXIT_COMMAND = "p";

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: java ClientTCP <server host name> <server port number>");
			return;
		}

		String serverName = args[0];
		int serverPort = Integer.parseInt(args[1]);

		Scanner scanner = new Scanner(System.in);
		int requestCount = 1;
		System.out.println("Enter OpCode, Operand1 and Operand2 (or enter 'P' to exit):");
		while (scanner.hasNext()) {
			if (scanner.hasNext(EXIT_COMMAND)) {
				break;
			}

			// Connect to the server and handle request
			try (Socket clientSocket = new Socket(serverName, serverPort);
					 DataInputStream inputFromServer = new DataInputStream(clientSocket.getInputStream());
					 DataOutputStream outputToServer = new DataOutputStream(clientSocket.getOutputStream())) {

				byte[] requestData = prepareRequest(scanner, requestCount);
				long startTime = System.currentTimeMillis();

				outputToServer.writeInt(requestData.length);
				sendRequest(outputToServer, requestData);
				handleResponse(inputFromServer);

				long returnTime = System.currentTimeMillis() - startTime;
				System.out.println("Return Time: " + returnTime + " ms");
			} catch (IOException e) {
				System.err.println("Connection error: " + e.getMessage());
			}

			System.out.println("\nEnter OpCode, Operand1 and Operand2 (or enter 'P' to exit):");
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
		switch (opCode) {
			case 0: return "multiplication";
			case 1: return "division";
			case 2: return "or";
			case 3: return "and";
			case 4: return "subtraction";
			case 5: return "addition";
			default: return "unknown";
		}
	}

	private static void sendRequest(DataOutputStream outputToServer, byte[] requestData) throws Exception {
		System.out.println("Request[HEX]: " + bytesToHex(requestData));
		outputToServer.write(requestData);
	}

	private static void handleResponse(DataInputStream inputFromServer) throws IOException {
		int responseLength = inputFromServer.readInt();
		byte[] receiveData = new byte[responseLength];
		inputFromServer.readFully(receiveData);

		ByteBuffer wrapped = ByteBuffer.wrap(receiveData);
		byte tml = wrapped.get();  // Total Message Length
		int result = wrapped.getInt();
		byte errorCode = wrapped.get();
		short responseRequestId = wrapped.getShort();

		System.out.println("Request ID: " + responseRequestId);
		System.out.println("Result: " + result);
		System.out.println("Error Code: " + (errorCode == 0 ? "Ok" : errorCode));
	}

	private static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02X ", b));
		}
		return sb.toString();
	}
}