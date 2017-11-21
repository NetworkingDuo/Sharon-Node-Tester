/*************************************************************
 *
 * Author: Hank Harrison, David MacDonald
 * Assignment: Program 7 EC - Compliance & Reliability Testing
 * Class: CSI 4321
 * Version: 1.0
 *
 *************************************************************/

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * SharOn Protocol Fuzzer
 */
public class SharOnFuzzer {

	private Socket nodeSocket; // Socket connected to student's node
	private Socket downloadSocket; // Socket connected to student's download
									// port

	private static final int TIMEOUT = 2000; // Timout value in milliseconds
	private static final int ITERATIONS = 1000; // Amount of tests to run

	// Randomizer for Fuzzer class
	private static final Random rand = new Random(System.currentTimeMillis());

	// File with student's ID
	private static final String PORT_FILE_NAME = "ports1.txt";
	private static final String HANDSHAKE_START = "INIT SharOn/1.0\n\n";
	private static final String HANDSHAKE_RESPONSE = "OK SharOn\n\n";

	private static final int ID_SIZE = 15; // Size of a message ID
	private static final int BYTE_MAX = 256; // Max byte value plus 1
	private static final int SHAR_ADDR_SIZE = 5; // Size of a SharOn address
	private static final int FILE_ID_SIZE = 4; // Size of file ID
	private static final int MAX_STR_LEN = 200; // Max size of a search string
	private static final int MAX_CHANGES = 10; // Max number of random changes
	// Number of bytes until file name in Result
	private static final int FILENAME_OFFSET = 8;

	// Number of ascii characters minus first 32 in the ascii table
	private static final int NUM_NORMAL_ASCII = 223;
	// Offset to normal characters (No \n, \r, etc) in ascii table
	private static final int ASCII_OFFSET = 32;

	// Allowed bits for an unsigned int in a long
	private static final long UNSIGNED_INT_MASK = 0xFFFFFFFFL;

	// Number of bytes that can be randomized in SharOn header
	private static final int RAND_HEADER = 27;

	public static void main(String args[]) {
		// Get file with port numbers
		File file = new File(PORT_FILE_NAME);
		try (FileReader reader = new FileReader(file)) {
			BufferedReader bufferedReader = new BufferedReader(reader);

			// Set logger
			Logger stuLogger = Logger.getLogger("fuzzer_results.txt");
			FileHandler fh = new FileHandler("fuzzer_results.txt");
			fh.setFormatter(new SimpleFormatter());
			stuLogger.setUseParentHandlers(false);
			stuLogger.addHandler(fh);

			// Perform tests on each student in the file
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				String[] stuInfo = line.split("[\\s\\t]+");

				// Get Student name, Node port and download port
				String name = stuInfo[0];
				int sharPort = Integer.parseInt(stuInfo[1]);
				int downloadPort = Integer.parseInt(stuInfo[2]);
				String server = stuInfo[4];
				stuLogger.info("Testing: " + name);

				// Connect a socket to their Node and Download Port
				InetAddress IPAddr = InetAddress.getByName(server);
				try (Socket sock = new Socket();
						Socket downSock = new Socket()) {

					// Connect to node and specify amount of time to wait to
					// connect
					try {
						sock.connect(new InetSocketAddress(IPAddr, sharPort),
								TIMEOUT);
						downSock.connect(
								new InetSocketAddress(IPAddr, downloadPort),
								TIMEOUT);

						// Specify read timeout for sockets
						sock.setSoTimeout(TIMEOUT);
						downSock.setSoTimeout(TIMEOUT);

						// Send Handshake
						DataOutputStream dout = new DataOutputStream(
								sock.getOutputStream());
						dout.writeBytes(HANDSHAKE_START);

						// Receive Handshake
						DataInputStream din = new DataInputStream(
								sock.getInputStream());
						byte[] handshake = new byte[HANDSHAKE_RESPONSE
								.length()];
						din.read(handshake);

						// Make sure handshake is correct
						if (!new String(handshake, StandardCharsets.US_ASCII)
								.equals(HANDSHAKE_RESPONSE)) {
							throw new IOException(
									"Incorrect Handshake " + "response");
						}

						SharOnFuzzer sof = new SharOnFuzzer(sock, downSock);

						sof.fuzzNodeSearch(stuLogger, ITERATIONS);
						System.out.println("Done with SearchFuzz");
						sof.fuzzNodeResponse(stuLogger, ITERATIONS);
						System.out.println("Done with ResponseFuzz");
						sof.fuzzDownloads(stuLogger, ITERATIONS);
						System.out.println("Done with DownloadFuzz");
					} catch (SocketTimeoutException e) {
						stuLogger.warning("Cannot connect to " + name + "'s "
								+ "Node for Fuzzing" + "\n***TESTING "
								+ "ABORTED");
						System.err.println("Cannot connect to " + name + "'s "
								+ "Node for Fuzzing" + "\n***TESTING "
								+ "ABORTED");
					} catch (SocketException e) {
						stuLogger.warning("Read timeout for " + name
								+ "\n***TESTING ABORTED");
						System.err.println("Read timeout for " + name
								+ "\n***TESTING ABORTED");
					} catch (EOFException e) {
						stuLogger.warning("Handshake response incorrect"
								+ "\n***TESTING ABORTED");
					} catch (IOException e) {
						stuLogger.warning(
								e.getMessage() + "\n***TESTING " + "ABORTED");
						System.err.println(
								e.getMessage() + "\n***TESTING " + "ABORTED");
					}
				} catch (IOException e) {
					System.err.println(e.getMessage());
				} catch (IllegalArgumentException e) {
					System.err.println(Arrays.toString(e.getStackTrace()));
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println("Can't find port file");
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println("Error reading in line from file");
		}
	}

	public SharOnFuzzer(Socket nodeSocket, Socket downloadSocket)
			throws IllegalArgumentException {
		// Check for null sockets
		if (nodeSocket == null || downloadSocket == null) {
			throw new IllegalArgumentException("Sockets cannot be null");
		}

		// Set sockets
		this.nodeSocket = nodeSocket;
		this.downloadSocket = downloadSocket;
	}

	/**
	 * Creates a new random byte array of size 'size'
	 *
	 * @return a random byte array of size 'size'
	 */
	private static byte[] getRandomByteArray(int size) {
		// Create a byte array of size 'size'
		byte[] arr = new byte[size];

		// Get a random array of bytes and return it
		new Random(System.currentTimeMillis()).nextBytes(arr);
		return arr;
	}

	/**
	 * Creates a random word of just lowercase letters
	 *
	 * @param size the size of the word
	 * @return the string word returned
	 */
	private static String createRandomWord(int size) {
		String r = "";
		for (int i = 0; i < size; i++) {
			r += (char) (Math.random() * 26 + 97);
		}
		return r;
	}

	/**
	 * Randomize up to 10 bytes in the SharOn packet header
	 *
	 * @param message the byte array containing the SharOn packet
	 */
	private static void randomizeHeader(byte[] message) {

		// Randomize things in the search header
		int numChanges = Math.min(rand.nextInt(RAND_HEADER), MAX_CHANGES);
		for (int i = 0; i < numChanges; i++) {
			int index = rand.nextInt(RAND_HEADER) + 1;
			message[index] = (byte) rand.nextInt(BYTE_MAX);
		}
	}

	/**
	 * Generate up to 255 SharOn random Results
	 *
	 * @param size the number of random Results to generate
	 * @return an ArrayList containing byte arrays of serialized Results
	 * @throws IOException if there is an issue serializing the Results
	 */
	private static ArrayList<byte[]> getRandomResults(int size)
			throws IOException {

		// Create result list and streams for encode
		ArrayList<byte[]> results = new ArrayList<>();
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(bout);

		for (int i = 0; i < size; i++) {
			// Discard all previous data in buffer
			bout.reset();

			// Create random file name
			String randFileName = createRandomWord(rand.nextInt(MAX_STR_LEN))
					+ "\n";

			// Encode result to byte array
			dout.writeLong(rand.nextLong());
			dout.writeInt(rand.nextInt());
			dout.write(randFileName.getBytes(StandardCharsets.US_ASCII));

			byte[] result = bout.toByteArray();

			// Do some randomization
			int numChanges = Math.min(randFileName.length(), MAX_CHANGES);
			for (int j = 0; j < numChanges; j++) {
				// Change different characters in file name
				int index = rand.nextInt(randFileName.length())
						+ FILENAME_OFFSET;
				result[index] = (byte) (rand.nextInt(NUM_NORMAL_ASCII)
						+ ASCII_OFFSET);
			}
			results.add(result);
		}

		return results;
	}

	/**
	 * Sends 'iterations' number of SharOn search packets to a node connected to
	 * by 'nodeSocket' and logs what was sent.
	 *
	 * @param log the logger
	 * @param iterations the number of times you want to send random search
	 *        packets
	 *
	 *        creating the base search (should never happen)
	 * @throws IOException If the OutputStream of the socket cannot be obtained
	 */
	public void fuzzNodeSearch(Logger log, int iterations) throws IOException {

		// Get socket OutputStream
		OutputStream out = nodeSocket.getOutputStream();

		for (int i = 0; i < iterations; i++) {

			// Set random search string
			String randString = createRandomWord(rand.nextInt(MAX_STR_LEN))
					+ "\n";

			// Write everything to a byte array
			try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
					DataOutputStream dout = new DataOutputStream(bout)) {

				writeRandomSharonHeader(dout, 1); // Write header
				dout.writeShort(randString.length()); // Write Payload Length
				dout.writeBytes(randString); // Write Payload

				// Get byte array
				byte[] searchArr = bout.toByteArray();

				// Randomize things in the search header
				randomizeHeader(searchArr);

				// Log what is sent to Node
				log.info(formatHeaderForOutput(searchArr) + "\n"
						+ "\tSearch String: " + randString);

				// Send Message to node
				out.write(searchArr);
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	/**
	 * Performs fuzz testing using Node Response packets
	 *
	 * @param log logger used to log packets sent and early termination
	 * @param iterations number of time you want to send packets
	 *
	 * @throws IOException If there is an issue connecting to socket
	 */
	public void fuzzNodeResponse(Logger log, int iterations)
			throws IOException {

		// Get socket OutputStream
		OutputStream out = nodeSocket.getOutputStream();

		for (int i = 0; i < iterations; i++) {

			// Generate a random number of random Results
			ArrayList<byte[]> results = getRandomResults(
					rand.nextInt(BYTE_MAX));

			// Write everything to a byte array
			try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
					DataOutputStream dout = new DataOutputStream(bout)) {

				writeRandomSharonHeader(dout, 2); // Write header

				// Create byte array for response and result list
				// Get total size of all the result arrays
				int totalResultSize = 0;
				for (byte[] b : results) {
					totalResultSize += b.length;
				}

				int port = nodeSocket.getLocalPort();
				byte[] IPv4 = InetAddress.getLocalHost().getAddress();

				// Finish writing header
				dout.writeShort(totalResultSize); // Write Payload Length
				dout.writeByte(results.size()); // Write Matches
				dout.writeShort(port); // Write Port
				dout.write(IPv4); // Write IP

				byte[] responseHeader = bout.toByteArray();

				randomizeHeader(responseHeader);

				// Log what fuzzer will send out
				String output = formatHeaderForOutput(responseHeader);
				output += ", Matches: " + results.size();
				output += ", Port: " + port;
				output += ", IPv4: "
						+ nodeSocket.getInetAddress().getHostAddress() + "\n";
				/*
				 * for(byte[] result: results) { output += "\t" +
				 * formatResults(result) + "\n"; }
				 */
				log.info(output);

				// Send out fuzzed response
				out.write(responseHeader);
				for (byte[] result : results) {
					out.write(result);
				}
			} catch (IOException e) {
				log.warning(e.getMessage());
				// If socket closed end response fuzzing
				throw new IOException(e.getMessage(), e);
			}
		}
	}

	/**
	 * Performs fuzz testing on the download port of the SharOn node by sending
	 * 'iterations' number of random File ID's
	 *
	 * @param log to log the File ID's sent to the download port
	 * @param iterations the number of packets you want to send
	 * @throws IOException if there is an issue connecting to the node
	 */
	public void fuzzDownloads(Logger log, int iterations) throws IOException {

		// Get socket OutputStream
		OutputStream out = downloadSocket.getOutputStream();
		InetAddress foreignHost = downloadSocket.getInetAddress();
		int foreignPort = downloadSocket.getPort();

		// Loop 'iterations' number of times
		for (int i = 0; i < iterations; i++) {

			try (Socket downSocket = new Socket(foreignHost, foreignPort)) {
				// Create random file ID
				byte[] fileID = new byte[FILE_ID_SIZE];

				// Generate random ID
				rand.nextBytes(fileID);

				// Log file ID sent
				log.info("File ID: " + Arrays.toString(fileID));

				// Send out file ID
				out.write(fileID);
			} catch (IOException e) {
				log.warning("Error opening download socket");
			}
		}
	}

	/**
	 * Formats a byte array SharOn Packet header into readable output
	 *
	 * @param packet the SharOn Message as a byte array
	 * @return a formatted String
	 * @throws IOException if there is an error reading byte array
	 */
	public String formatHeaderForOutput(byte[] packet) throws IOException {
		// Use streams to get info more easily
		ByteArrayInputStream bin = new ByteArrayInputStream(packet);
		DataInputStream din = new DataInputStream(bin);

		// Initialize a buffer byte array
		byte[] buf = new byte[ID_SIZE];

		// Read Type (Don't Output)
		byte type = din.readByte();

		// Initialize output String
		String output;
		if (type == 1) {
			output = "Search";
		} else if (type == 2) {
			output = "Response";
		} else {
			output = "Type: " + type;
		}
		output += " - ID: ";

		// Output ID
		din.read(buf, 0, ID_SIZE);
		output += Arrays.toString(buf) + ", TTL: ";

		// Output TTL
		output += din.readByte() + ", Routing: ";

		// Output Routing
		output += din.readByte() + ", Src: ";

		// Output Src Address
		din.read(buf, 0, SHAR_ADDR_SIZE);
		output += Arrays.toString(Arrays.copyOf(buf, SHAR_ADDR_SIZE))
				+ ", Dest: ";

		// Output Dest Address
		din.read(buf, 0, SHAR_ADDR_SIZE);
		output += Arrays.toString(Arrays.copyOf(buf, SHAR_ADDR_SIZE))
				+ ", Payload Length: ";

		// Output Payload Length
		output += din.readUnsignedShort();

		// Return Output
		return output;
	}

	/**
	 * Formats a SharOn result into readable output
	 *
	 * @param result Result byte array
	 * @return a formatted string
	 * @throws IOException if there is an issue reading bytes
	 */
	public String formatResults(byte[] result) throws IOException {
		// Use streams to get info more easily
		ByteArrayInputStream bin = new ByteArrayInputStream(result);
		DataInputStream din = new DataInputStream(bin);

		// Initialize String
		String output = "Result - FileID: ";

		// Output FileID
		output += (din.readInt() & UNSIGNED_INT_MASK) + ", File Size: ";

		// Output File Size
		output += (din.readInt() & UNSIGNED_INT_MASK) + ", File Name: ";

		// Output File Name
		byte[] buf = new byte[1024]; // Create buffer
		int bytesRead;
		while ((bytesRead = din.read(buf)) != -1) {
			output += new String(buf, 0, bytesRead, StandardCharsets.US_ASCII);
		}

		return output;
	}

	/**
	 * Writes the header of a SharOn packet to a DataOutputStream
	 *
	 * @param dout the DataOutputStream
	 * @param messageType the type of SharOn Message
	 * @throws IOException if there is an issue writing to dout
	 */
	void writeRandomSharonHeader(DataOutputStream dout, int messageType)
			throws IOException {

		dout.writeByte(messageType); // Write Message type
		dout.write(getRandomByteArray(ID_SIZE)); // Write ID
		dout.writeByte(rand.nextInt(BYTE_MAX)); // Write TTL
		dout.writeByte(0); // BFS RoutingService
		dout.write(getRandomByteArray(SHAR_ADDR_SIZE)); // Write Src
		dout.write(getRandomByteArray(SHAR_ADDR_SIZE)); // Write Dest
	}
}
