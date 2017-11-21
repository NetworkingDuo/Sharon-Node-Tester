/*************************************************************
 *
 * Author: Hank Harrison, David MacDonald
 * Assignment: Program 7 EC - Compliance & Reliability Testing
 * Class: CSI 4321
 *
 *************************************************************/

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Compliance {

	private static final double SHARON_VERSION = 1.0;
	private static final int SAFE_INPUT_READ = 100;
	private static final String HANDSHAKE_INIT = "INIT SharOn/" + SHARON_VERSION
			+ "\n\n";
	private static final String handShakeFail = "WRONG HANDSHAKE" + "\n\n";

	/**
	 * Test a valid file download with the connected node.
	 * 
	 * @param currDownloadPort the port to download from
	 * @param ourAddress the address of the server
	 * @param fileName the name of the file
	 * @param fileID the ID of the file
	 * @param serverAddress the address of the server in textual format
	 * @throws IOException if error with I/O
	 * @throws NullPointerException if null
	 */
	private static void testValidDownload(int currDownloadPort,
			InetAddress ourAddress, String fileName, long fileID, int fileSize,
			String serverAddress, Logger complianceLogger)
			throws IOException, NullPointerException {

		try {

			String handShakeIn = fileID + "\n";
			byte[] toStore = new byte[SAFE_INPUT_READ];

			Socket downloadSocket = new Socket(ourAddress, currDownloadPort);
			downloadSocket.getOutputStream().write(handShakeIn.getBytes());

			byte[] reader = new byte[1];
			int bytes = 0;
			int charCounter = 0;
			int i = 0;

			downloadSocket.setSoTimeout(3000);

			while ((bytes = downloadSocket.getInputStream()
					.read(reader)) >= 0) {
				toStore[i] = reader[0];

				if (toStore[i] == '\n') {
					charCounter++;
					if (charCounter == 2) {
						break;
					}
				}

				i++;
			}

			String ourOut = new String(toStore, "UTF-8");

			File fileDL = new File(fileName);
			FileOutputStream outStream = new FileOutputStream(fileDL);

			if (fileSize > 50000) {
				System.err.println("File is too big for download.");
				return;
			}

			// Download the file, and check if it matches
			while ((bytes = downloadSocket.getInputStream()
					.read(reader)) != -1) {
				outStream.write(reader[0]);
				i++;
			}

			if (fileSize == fileDL.length()) {
				System.out.println(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currDownloadPort
								+ " passed the first file download test.");
				complianceLogger.info(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currDownloadPort
								+ " passed the first file download test.");
			} else {
				System.err.println(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currDownloadPort
								+ " failed the first file download test.");
				complianceLogger.warning(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currDownloadPort
								+ " failed the first file download test.");
			}

			// Delete the file from the directory
			fileDL.delete();

			// Flush and close the buffer
			outStream.flush();
			outStream.close();

			downloadSocket.close();

		} catch (ConnectException e) {
			System.err.println("Error with the download test connection port.");
			System.out.println();
			return;
		} catch(SocketTimeoutException e){
			System.err.println(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currDownloadPort
							+ " timed out on the first file download test.");
			complianceLogger.info(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currDownloadPort
							+ " timed out on the first file download test.");
		}

		// Connected to Downloads, run Download tests
	}

	/**
	 * Test an invalid file download through an invalid handshake.
	 * 
	 * @param currDownloadPort the port to download from
	 * @param ourAddress the address of the server
	 * @param fileID the ID of the file
	 * @param serverAddress the address of the server in textual format
	 * @throws IOException if error with I/O
	 */
	private static void testInvalidDownload(int currDownloadPort,
			InetAddress ourAddress, int fileID, int fileSize,
			String serverAddress, Logger complianceLogger) throws IOException {

		try {

			String handShakeIn = fileID + "wrong input" + "\n";

			byte[] toStore = new byte[SAFE_INPUT_READ];

			byte[] reader = new byte[1];
			int bytes = 0;
			int charCounter = 0;
			int i = 0;

			Socket downloadSocket = new Socket(ourAddress, currDownloadPort);
			downloadSocket.getOutputStream().write(handShakeIn.getBytes());
			downloadSocket.setSoTimeout(10000);

			try {
				while ((bytes = downloadSocket.getInputStream()
						.read(reader)) >= 0) {
					toStore[i] = reader[0];

					if (toStore[i] == '\n') {
						charCounter++;
						if (charCounter == 2) {
							break;
						}
					}

					i++;
				}

				String received = new String(toStore, "UTF-8");

				if (received.startsWith("ERROR")) {

					System.out.println(InetAddress.getByName(serverAddress)
							.getHostAddress() + " on port " + currDownloadPort
							+ " passed the invalid file download test.");
					complianceLogger.info(InetAddress.getByName(serverAddress)
							.getHostAddress() + " on port " + currDownloadPort
							+ " passed the invalid file download test.");

				} else {
					System.out.println(InetAddress.getByName(serverAddress)
							.getHostAddress() + " on port " + currDownloadPort
							+ " failed the invalid file download test.");
					complianceLogger.warning(InetAddress
							.getByName(serverAddress).getHostAddress()
							+ " on port " + currDownloadPort
							+ " failed the invalid file download test.");
				}
			} catch (SocketTimeoutException e) {
				System.err.println(InetAddress.getByName(serverAddress)
						.getHostAddress() + " on port " + currDownloadPort
						+ " timed out during the invalid file download test.");
				complianceLogger.warning(InetAddress.getByName(serverAddress)
						.getHostAddress() + " on port " + currDownloadPort
						+ " timed out during the invalid file download test.");
			}
		} catch (ConnectException e) {
			System.err.println("Error with the download test connection port.");
			System.out.println();
			return;
		} catch(SocketTimeoutException e){
			System.err.println(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currDownloadPort
							+ " timed out on the invalid file download test.");
			complianceLogger.info(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currDownloadPort
							+ " timed out on the invalid file download test.");
		}

	}

	/**
	 * This function will test the functionality of the given SharOn nodes.
	 * 
	 * @param ourSocket the socket to communicate over
	 * @param currNodePort the current Node to operate over
	 * @param ourAddress the address of the server
	 * @param serverAddress the address of the server in textual representation
	 * @param compliancelogger
	 * @throws IOException if error with I/O
	 * @throws NullPointerException if null variables are parsed
	 */
	static void testSharons(Socket ourSocket, int currNodePort,
			InetAddress ourAddress, int currDownloadPort, String serverAddress,
			Logger compliancelogger) throws IOException, NullPointerException {
		
		OutputStream out = ourSocket.getOutputStream();

		byte[] id = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 };
		byte[] sourceSharOnAddress = { 0, 0, 0, 0, 0 };
		byte[] destinationSharOnAddress = { 0, 0, 0, 0, 0 };
		String randString = "\n";

		// Write everything to a byte array
		try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
				DataOutputStream dout = new DataOutputStream(bout)) {
			
			InputStream in = ourSocket.getInputStream();
			DataInputStream din = new DataInputStream(in);
			
			dout.writeByte(1); // Write Search type
			dout.write(id); // Write ID
			dout.writeByte(1); // Write TTL
			dout.writeByte(0); // BFS RoutingService
			dout.write(sourceSharOnAddress);
			dout.write(destinationSharOnAddress);
			dout.writeShort(randString.length()); // Write length of string
			dout.writeBytes(randString); // Write string
			
			// Get byte array
			byte[] searchArr = bout.toByteArray();

			// Send Message to node
			out.write(searchArr);

			// Initialize a timeout for the socket in operation
			// ourSocket.setSoTimeout(5000);

			byte[] responseArray = new byte[SAFE_INPUT_READ];

			// Initialize a timeout for the socket
			//ourSocket.setSoTimeout(5000);
			
			// Try to read in the handshake response
			din.readFully(responseArray);
						
			String response = new String(responseArray,
					"UTF-8");

			if (responseArray[0] == 2) {

				System.out.println(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " passed the empty search test.");

				compliancelogger.warning(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " passed the empty search test.");
			}

			// Get the ID of the file to check
			byte[] fileID = new byte[4];

			fileID[0] = responseArray[37];
			fileID[1] = responseArray[38];
			fileID[2] = responseArray[39];
			fileID[3] = responseArray[40];

			int ourID = byte2int(fileID);

			byte[] fileSize = new byte[4];

			fileSize[0] = responseArray[41];
			fileSize[1] = responseArray[42];
			fileSize[2] = responseArray[43];
			fileSize[3] = responseArray[44];

			int ourSize = byte2int(fileSize);

			int offset = 45;

			ArrayList<Byte> ourList = new ArrayList<Byte>();

			for (int j = 0; j < SAFE_INPUT_READ; j++) {

				if (responseArray[offset] == 10) {
					break;
				}

				ourList.add(responseArray[offset]);
				offset++;
			}

			byte[] fileName = new byte[ourList.size()];

			for (int j = 0; j < fileName.length; j++) {
				fileName[j] = ourList.get(j);
			}

			String ourString = new String(fileName, "UTF-8");
			ourString += "\n";

			dout.flush();

			// Skip the available bytes left
			ourSocket.getInputStream()
					.skip(ourSocket.getInputStream().available());

			dout.writeByte(1); // Write Search type
			dout.write(id); // Write ID
			dout.writeByte(1); // Write TTL
			dout.writeByte(0); // BFS RoutingService
			dout.write(sourceSharOnAddress);
			dout.write(destinationSharOnAddress);
			dout.writeShort(ourString.length()); // Write length of string
			dout.writeBytes(ourString); // Write string
			
			// Get byte array
			byte[] newSearch = bout.toByteArray();

			// Send Message to node
			out.write(newSearch);

			// Initialize a timeout for the socket in operation
			// ourSocket.setSoTimeout(5000);

			byte[] newResponse = new byte[SAFE_INPUT_READ];

			// Initialize a timeout for the socket
			ourSocket.setSoTimeout(5000);
			
			// Try to read in the handshake response
			din.read(newResponse);
			System.out.println("Response has been read in.");

			if (newResponse[0] == 2) {

				System.out.println(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " passed the first file search test.");

				compliancelogger.warning(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " passed the first file search test.");
			}

			testValidDownload(currDownloadPort, ourAddress, ourString, ourID,
					ourSize, serverAddress, compliancelogger);

			testInvalidDownload(currDownloadPort, ourAddress, ourID, ourSize,
					serverAddress, compliancelogger);

		} catch (SocketTimeoutException e) {
			System.err.println(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the search tests.");
			compliancelogger.warning(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the search tests.");
			return;
		}

	}

	/**
	 * This function will test the connectivity of another SharOn node.
	 * 
	 * @param ourSocket the socket we communicate over
	 * @param currNodePort
	 * @param serverAddress
	 * @param compliancelogger
	 * @throws IOException if problem with I/O
	 * @throws NullPointerException if null values are processed
	 */
	static void checkConnection(Socket ourSocket, int currNodePort,
			String serverAddress, Logger compliancelogger)
			throws IOException, NullPointerException {

		try {

			// Initialize streams for reading and writing
			OutputStream out = ourSocket.getOutputStream();
			InputStream in = ourSocket.getInputStream();
			DataOutputStream dout = new DataOutputStream(out);
			DataInputStream din = new DataInputStream(in);

			// Write out handshake
			dout.write(HANDSHAKE_INIT.getBytes());

			// Where to store handshake response
			byte[] handshakeResponseStorage = new byte[SAFE_INPUT_READ];

			// Initialize a timeout for the socket
			ourSocket.setSoTimeout(5000);

			// Try to read in the handshake response
			din.read(handshakeResponseStorage);

			String response = new String(handshakeResponseStorage,
					StandardCharsets.US_ASCII);

			// Check if the connection was successfully established
			if (response.startsWith("REJECT") || !response.startsWith("OK ")) {

				System.err.println(InetAddress.getByName(serverAddress)
						.getHostAddress() + " on port " + currNodePort
						+ " rejected the handshake and failed the handshake test.");
				compliancelogger.warning(InetAddress.getByName(serverAddress)
						.getHostAddress() + " on port " + currNodePort
						+ " rejected the handshake and failed the handshake test.");

			} else {
				System.out.println(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " passed the OK handshake test.");
				compliancelogger.info(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " passed the OK handshake test.");
			}

		} catch (EOFException e) {
			System.err.println(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " reached EOF during the OK handshake test.");
			compliancelogger.warning(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " reached EOF during the OK handshake test.");
		} catch (SocketTimeoutException e) {
			System.err.println(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the OK handshake test.");
			compliancelogger.warning(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the OK handshake test.");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Sends a back handshake to the node, and tests the reject functionality
	 * 
	 * @param currNodePort the current node port to connect to
	 * @param serverAddress the address of the server
	 * @param compliancelogger
	 * @throws UnknownHostException if error with the host
	 * @throws IOException if error with I/O
	 */
	static void checkConnectionReject(Socket ourSocket, int currNodePort, String serverAddress,
			Logger compliancelogger) throws IOException {

		ourSocket.close();
		ourSocket = new Socket(InetAddress.getByName(serverAddress), currNodePort);

		OutputStream out = ourSocket.getOutputStream();
		InputStream in = ourSocket.getInputStream();
		DataOutputStream dout = new DataOutputStream(out);
		DataInputStream din = new DataInputStream(in);

		byte[] handshakeResponseStorage = new byte[SAFE_INPUT_READ];

		try {

			// Write handshake
			ourSocket.getOutputStream().write(handShakeFail.getBytes());

			// Set the timeout of the socket
			ourSocket.setSoTimeout(5000);

			// Try to read in the handshake response
			din.read(handshakeResponseStorage);

			String response = new String(handshakeResponseStorage,
					StandardCharsets.US_ASCII);

			// Check if the connection was successfully established
			if (response.startsWith("REJECT")) {

				System.out.println(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " passed the REJECT handshake test.");
				compliancelogger.info(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " passed the REJECT handshake test.");

			} else {

				System.err.println(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " failed the REJECT handshake test.");
				compliancelogger.warning(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " failed the REJECT handshake test.");
			}

		} catch (SocketTimeoutException e) {
			System.err.println(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the REJECT handshake test.");
			compliancelogger.warning(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the REJECT handshake test.");
		} catch (IOException e) {
			e.printStackTrace();
		}

		ourSocket.close();

	}

	private static int byte2int(byte[] ourBytes) {
		int toReturn = 0;

		for (int i = 0; i < ourBytes.length; i++) {
			toReturn = (toReturn << Byte.SIZE) | (ourBytes[i] & 0xFF);
		}
		return toReturn;
	}

}
