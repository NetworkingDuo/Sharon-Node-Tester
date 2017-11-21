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
import java.util.Random;
import java.util.logging.Logger;

public class Reliability {

	private static final double SHARON_VERSION = 1.0;
	private static final int SAFE_INPUT_READ = 500;
	private static final String HANDSHAKE_INIT = "INIT SharOn/" + SHARON_VERSION
			+ "\n\n";

	static void testSharons(Socket ourSocket, int currNodePort,
			InetAddress ourAddress, int currDownloadPort, String serverAddress,
			int i2, Logger reliabilitylogger)
			throws IOException, NullPointerException {

		ourSocket.getInputStream().skip(ourSocket.getInputStream().available());

		OutputStream out = ourSocket.getOutputStream();

		Random rand = new Random();

		// Set random search string
		String randString = " ";

		byte[] id = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 };
		byte[] sourceSharOnAddress = { 0, 0, 0, 0, 0 };
		byte[] destinationSharOnAddress = { 0, 0, 0, 0, 0 };

		try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
				DataOutputStream dout = new DataOutputStream(bout)) {

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

			byte[] reader = new byte[1];
			int bytes = 0;
			byte[] toStore = new byte[SAFE_INPUT_READ];
			int charCounter = 0;
			int i = 0;

			while ((bytes = ourSocket.getInputStream().read(reader)) >= 0) {

				if (i == 100) {
					break;
				}
				toStore[i] = reader[0];

				i++;
			}

			// Get the ID of the file to check
			byte[] fileID = new byte[4];

			fileID[0] = toStore[37];
			fileID[1] = toStore[38];
			fileID[2] = toStore[39];
			fileID[3] = toStore[40];

			int ourID = byte2int(fileID);

			byte[] fileSize = new byte[4];

			fileSize[0] = toStore[41];
			fileSize[1] = toStore[42];
			fileSize[2] = toStore[43];
			fileSize[3] = toStore[44];

			int ourSize = byte2int(fileSize);

			int offset = 45;

			ArrayList<Byte> ourList = new ArrayList<Byte>();

			for (int j = 0; j < 100; j++) {

				if (toStore[offset] == 10) {
					break;
				}

				ourList.add(toStore[offset]);
				offset++;
			}

			byte[] fileName = new byte[ourList.size()];

			for (int j = 0; j < fileName.length; j++) {
				fileName[j] = ourList.get(j);
			}

			String ourString = new String(fileName, "UTF-8");

			dout.flush();
			ourSocket.getInputStream()
					.skip(ourSocket.getInputStream().available());

			testDownloadRequest(currDownloadPort, ourAddress, ourString, ourID,
					serverAddress, i2, ourSize, reliabilitylogger);

		} catch (SocketTimeoutException e) {
			System.err.println(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the download tests for test "
							+ (i2 + 1) + " of 500 tests.");
			reliabilitylogger.warning(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the download tests for test "
							+ (i2 + 1) + " of 500 tests.");
			return;

		} catch (SocketException e) {
			ourSocket = new Socket(InetAddress.getByName(serverAddress),
					currNodePort);
			System.err.println(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the download tests for test "
							+ (i2 + 1) + " of 500 tests.");
			reliabilitylogger.warning(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the download tests for test "
							+ (i2 + 1) + " of 500 tests.");
			return;
		}

	}

	private static void testDownloadRequest(int currDownloadPort,
			InetAddress ourAddress, String fileName, long fileID,
			String serverAddress, int i2, int ourSize, Logger reliabilitylogger)
			throws IOException, NullPointerException {

		try {

			String handShakeIn = fileID + "\n";

			byte[] toStore = new byte[SAFE_INPUT_READ];
			int i = 0;

			Socket downloadSocket = new Socket(ourAddress, currDownloadPort);
			downloadSocket.getOutputStream().write(handShakeIn.getBytes());

			byte[] reader = new byte[1];
			int bytes = 0;
			int charCounter = 0;

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

			if (ourSize > 50000) {
				System.err.println("File is too big for download.");
				return;
			}

			while ((bytes = downloadSocket.getInputStream()
					.read(reader)) != -1) {
				outStream.write(reader[0]);
				i++;
			}

			if (ourSize == fileDL.length()) {
				System.out.println(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currDownloadPort
								+ " passed download request for test "
								+ (i2 + 1) + " of 500.");
				reliabilitylogger.info(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currDownloadPort
								+ " passed download request for test "
								+ (i2 + 1) + " of 500.");
			} else {
				System.err.println(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currDownloadPort
								+ " failed download request for test "
								+ (i2 + 1) + " of 500.");
				reliabilitylogger.warning(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currDownloadPort
								+ " failed download request for test "
								+ (i2 + 1) + " of 500.");
			}

			// Delete the file from the directory
			fileDL.delete();

			// Flush and close the buffer
			outStream.flush();
			outStream.close();

		} catch (ConnectException e) {
			System.err.println("Error with the download test connection port.");
			System.out.println();
			return;
		}

		// Connected to Downloads, run Download tests
	}

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
	static void testBlankSearches(String serverAddress, int currNodePort,
			Socket ourSocket, int i2, Logger reliabilitylogger)
			throws UnknownHostException, IOException {

		OutputStream out = ourSocket.getOutputStream();
	
		ourSocket.getInputStream().skip(ourSocket.getInputStream().available());

		Random rand = new Random();

		// Set blank search string
		String randString = "\n";

		byte[] id = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 };
		byte[] sourceSharOnAddress = { 0, 0, 0, 0, 0 };
		byte[] destinationSharOnAddress = { 0, 0, 0, 0, 0 };

		// Write everything to a byte array
		try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
				DataOutputStream dout = new DataOutputStream(bout)) {

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

			byte[] reader = new byte[1];
			int bytes = 0;
			byte[] toStore = new byte[SAFE_INPUT_READ];
			int charCounter = 0;
			int i = 0;

			while ((bytes = ourSocket.getInputStream().read(reader)) >= 0) {

				if (i == 100) {
					break;
				}
				toStore[i] = reader[0];

				i++;
			}

			if (toStore[0] == 2) {
				System.out.println(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " passed blank search for test " + (i2 + 1)
								+ " of 500.");

				reliabilitylogger.info(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " passed blank search for test " + (i2 + 1)
								+ " of 500.");
			} else {
				System.err.println(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " failed blank search for test " + (i2 + 1)
								+ " of 500.");

				reliabilitylogger.warning(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " failed blank search for test " + (i2 + 1)
								+ " of 500.");
			}

		} catch (SocketTimeoutException e) {
			System.err.println(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the blank search for test "
							+ (i2 + 1) + " of 500 tests.");
			reliabilitylogger.warning(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the blank search for test "
							+ (i2 + 1) + " of 500 tests.");
			return;

		}

	}

	static void testWordSearches(String serverAddress, int currNodePort,
			Socket ourSocket, int i2, Logger reliabilitylogger)
			throws UnknownHostException, IOException {

		// Test 1 - Empty Search test, test if we get results back
		byte[] id = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 };
		byte[] sourceSharOnAddress = { 0, 0, 0, 0, 0 };
		byte[] destinationSharOnAddress = { 0, 0, 0, 0, 0 };
		String searchString = "";

		OutputStream out = ourSocket.getOutputStream();

		ourSocket.getInputStream().skip(ourSocket.getInputStream().available());

		// Create a random word to search for
		String ourWord = "";
		Random ourRandom = new Random();
		int rand = ourRandom.nextInt(10) + 4;

		for (int k = 0; k < rand; k++) {
			ourWord += (char) (Math.random() * 26 + 97);
		}

		searchString = ourWord;
		searchString += "\n";

		// Write everything to a byte array
		try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
				DataOutputStream dout = new DataOutputStream(bout)) {

			dout.writeByte(1); // Write Search type
			dout.write(id); // Write ID
			dout.writeByte(1); // Write TTL
			dout.writeByte(0); // BFS RoutingService
			dout.write(sourceSharOnAddress);
			dout.write(destinationSharOnAddress);
			dout.writeShort(searchString.length()); // Write length of string
			dout.writeBytes(searchString); // Write string

			// Get byte array
			byte[] searchArr = bout.toByteArray();

			// Send Message to node
			out.write(searchArr);

			// Initialize a timeout for the socket in operation
			// ourSocket.setSoTimeout(5000);

			byte[] reader = new byte[1];
			int bytes = 0;
			byte[] toStore = new byte[SAFE_INPUT_READ];
			int charCounter = 0;
			int i = 0;

			while ((bytes = ourSocket.getInputStream().read(reader)) >= 0) {

				toStore[i] = reader[0];

				if (reader[0] == 2) {
					break;
				}

				i++;
			}

			if (toStore[0] == 2) {

				System.out.println(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " passed word search for test " + (i2 + 1)
								+ " of 500 with word " + searchString);

				reliabilitylogger.info(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " passed word search for test " + (i2 + 1)
								+ " of 500 with word " + searchString);
			} else {
				System.out.println(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " failed word search for test " + (i2 + 1)
								+ " of 500 with word " + searchString);

				reliabilitylogger.warning(
						InetAddress.getByName(serverAddress).getHostAddress()
								+ " on port " + currNodePort
								+ " failed word search for test " + (i2 + 1)
								+ " of 500 with word " + searchString);
			}

		} catch (SocketTimeoutException e) {
			System.err.println(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the word search for test "
							+ (i2 + 1) + " of 500 tests with word "
							+ searchString);
			reliabilitylogger.warning(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the word search for test "
							+ (i2 + 1) + " of 500 tests with word "
							+ searchString);
			return;

		} catch (SocketException e) {
			ourSocket = new Socket(InetAddress.getByName(serverAddress),
					currNodePort);
			System.err.println(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the word search for test "
							+ (i2 + 1) + " of 500 tests with word "
							+ searchString);
			reliabilitylogger.warning(
					InetAddress.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " timed out during the word search for test "
							+ (i2 + 1) + " of 500 tests with word "
							+ searchString);
			return;
		}

	}

	private static int byte2int(byte[] ourBytes) {
		int toReturn = 0;

		for (int i = 0; i < ourBytes.length; i++) {
			toReturn = (toReturn << Byte.SIZE) | (ourBytes[i] & 0xFF);
		}
		return toReturn;
	}

}
