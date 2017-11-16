/*************************************************************
 *
 * Author: Hank Harrison, David MacDonald
 * Assignment: Program 7 EC - Compliance & Reliability Testing
 * Class: CSI 4321
 *
 *************************************************************/
package sharon.app.testsuite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import sharon.app.Node;
import sharon.serialization.BadAttributeValueException;
import sharon.serialization.Message;
import sharon.serialization.MessageInput;
import sharon.serialization.MessageOutput;
import sharon.serialization.Response;
import sharon.serialization.Result;
import sharon.serialization.RoutingService;
import sharon.serialization.Search;

public class ComplianceReliabilityTesting {

	private static final double SHARON_VERSION = 1.0;
	private static final int SAFE_INPUT_READ = 100;

	private static final String handShakeIn = "INIT SharOn/" + SHARON_VERSION
			+ "\n\n";
	private static final String handShakeInSuccess = "OK SharOn\n\n";
	private static final String handShakeInWrongClient = "REJECT " + "2XX" + " "
			+ "[\\w\\s]" + "\n\n";
	private static final String handShakeInWrongServer = "REJECT " + "3XX" + " "
			+ "[\\w\\s]+" + "\n\n";

	private static final Logger logger = Logger
			.getLogger(ComplianceReliabilityTesting.class.getName());
	private static FileHandler file;

	public static ArrayList<Socket> connectionsUp = new ArrayList<>();

	public static void main(String[] args)
			throws IOException, NullPointerException,
			BadAttributeValueException, InterruptedException {

		if (args.length != 3) {
			throw new IllegalArgumentException(
					"Error with the number of input arguments.");
		}

		file = new FileHandler("Compliance_Reliability_Log.log");
		file.setFormatter(new SimpleFormatter());
		logger.setUseParentHandlers(false);
		logger.addHandler(file);

		File ourFile = new File("ports");
		FileReader reader = new FileReader(ourFile);
		BufferedReader bufferedReader = new BufferedReader(reader);
		String line = null;

		while ((line = bufferedReader.readLine()) != null) {

			String[] info = line.split("\\s+");

			// Initialize our testing parameters
			String student = info[0];

			InetAddress ourAddress = InetAddress.getByName(args[0]);
			int currNodePort = Integer.parseInt(info[1]);
			int currDownloadPort = Integer.parseInt(info[2]);
			int mavenPort = Integer.parseInt(info[3]);

			System.out.println(student + "\t\t" + currNodePort + "\t"
					+ currDownloadPort + "\t" + mavenPort + "\t\t");

			logger.fine("Handling student " + student + " with node port "
					+ currNodePort + ", download port " + currDownloadPort
					+ ", and Maven Server Port " + mavenPort + ".");

			// Initialize the socket connections
			Socket ourSocket = null;

			try {

				ourSocket = new Socket(ourAddress, currNodePort);

			} catch (ConnectException e) {

				System.err.println("Error with the connection.");
				System.out.println();

				checkDownloadAndMavenConnectivity(ourSocket, ourAddress,
						currDownloadPort, mavenPort);

				continue;

			}

			// Run the SharOn connection check test
			checkConnection(ourSocket);
			testSharons(ourSocket, currNodePort, ourAddress);

			checkDownloadAndMavenConnectivity(ourSocket, ourAddress,
					currDownloadPort, mavenPort);

			System.out.println();

			TimeUnit.SECONDS.sleep(2);

			testDownloads(ourSocket, currDownloadPort, ourAddress);

		}

		bufferedReader.close();

	}

	/**
	 * This function will test the functionality of the download protocol.
	 * 
	 * @param ourSocket the socket to communicate over
	 * @param currDownloadPort the download port
	 * @param ourAddress the server address to connect to
	 * @throws IOException if error with I/O
	 */
	private static void testDownloads(Socket ourSocket, int currDownloadPort,
			InetAddress ourAddress) throws IOException {

		try {
			ourSocket = new Socket(ourAddress, currDownloadPort);

		} catch (ConnectException e) {
			System.err.println("Error with the sharon test connection port.");
			System.out.println();
			return;
		}

		// Connected to Downloads, run Download tests
	}

	/**
	 * This function will test the functionality of the given SharOn nodes.
	 * 
	 * @param ourSocket the socket to communicate over
	 * @param currNodePort the current Node to operate over
	 * @param ourAddress the address of the server
	 * @throws IOException if error with I/O
	 * @throws NullPointerException if null variables are parsed
	 * @throws BadAttributeValueException if bad attributes are parsed
	 */
	private static void testSharons(Socket ourSocket, int currNodePort,
			InetAddress ourAddress) throws IOException, NullPointerException,
			BadAttributeValueException {

		// Connected to Node, run SharOn tests

		byte[] id = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 };
		int ttl = 1;
		RoutingService routingService = RoutingService.BREADTHFIRSTBROADCAST;
		byte[] sourceSharOnAddress = { 0, 0, 0, 0, 0 };
		byte[] destinationSharOnAddress = { 0, 0, 0, 0, 0 };
		String searchString = "";

		try {
			Search ourSearch = new Search(id, ttl, routingService,
					sourceSharOnAddress, destinationSharOnAddress,
					searchString);
			MessageOutput ourOut = new MessageOutput(
					ourSocket.getOutputStream());

			List<Byte> ourList = new ArrayList<Byte>();

			for (int i = 0; i < 15; i++) {
				ourList.add(ourSearch.getID()[i]);
			}

			// Write the Message to the socket stream
			ourSearch.encode(ourOut);

			byte[] toStore = new byte[1000];
			byte[] reader = new byte[1];
			int bytes = 0;
			int i = 0;
			int charCounter = 0;

			ourSocket.setSoTimeout(5000);

			try {
				MessageInput messageIn = new MessageInput(
						ourSocket.getInputStream());
				Message ourMessage = Message.decode(messageIn);
				if (ourMessage.getMessageType() == 2) {
					System.out.println("Passed search test");
				}

				Response toOut = (Response) ourMessage;

				// Parse the byte array into a List of Bytes, since Map
				// affects the hashcode
				List<Byte> toCheck = new ArrayList<Byte>();

				List<Result> ourSecondList = toOut.getResultList();

				// Check the size of the files, get the first, search it
				if (ourSecondList.size() > 0) {
					Result toSendBack = ourSecondList.get(0);

					Search searchTwo = new Search(id, ttl, routingService,
							sourceSharOnAddress, destinationSharOnAddress,
							toSendBack.getFileName());

					ourSearch.encode(ourOut);

					MessageInput messageInTwo = new MessageInput(
							ourSocket.getInputStream());
					Message ourMessageTwo = Message.decode(messageIn);

					Response checker = (Response) ourMessageTwo;
					List<Result> ourSecondListCheck = toOut.getResultList();

					if (ourSecondListCheck.get(0).getFileName() == toSendBack
							.getFileName()) {
						System.out.println("Passed the second search test.");
					}

				}

			} catch (SocketTimeoutException e) {
				System.err.print(
						"Error with the SharOn connection - socket timeout.");
				System.out.println();
				logger.warning(
						"Error with the SharOn connection - socket timeout.");
				return;
			}

			String returnedString = new String(toStore, "UTF-8");
			System.out.println(returnedString);

		} catch (BadAttributeValueException | IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Check the connectivity of the download and maven ports for a specific
	 * SharOn node.
	 * 
	 * @param ourSocket the socket we communicate over
	 * @param ourAddress the server address
	 * @param currDownloadPort the download port to test
	 * @param mavenPort the maven port to test
	 * @throws IOException if problem with I/O
	 */
	private static void checkDownloadAndMavenConnectivity(Socket ourSocket,
			InetAddress ourAddress, int currDownloadPort, int mavenPort)
			throws IOException {
		try {
			ourSocket = new Socket(ourAddress, currDownloadPort);
			System.out.println("Successfully connected to download port.");

		} catch (ConnectException e) {
			System.err.println("Error with the connection to download port.");
			System.out.println();
		}

		try {
			ourSocket = new Socket(ourAddress, mavenPort);
			System.out.println("Successfully connected to maven port.");

		} catch (ConnectException e) {
			System.err.println("Error with the connection to maven port.");
			System.out.println();
		}

	}

	/**
	 * This function will test the connectivity of another SharOn node.
	 * 
	 * @param ourSocket the socket we communicate over
	 * @throws IOException if problem with I/O
	 * @throws NullPointerException if null values are processed
	 * @throws BadAttributeValueException if error with attributes
	 */
	private static void checkConnection(Socket ourSocket) throws IOException,
			NullPointerException, BadAttributeValueException {

		// Declaration of the string passed for initialization of connection

		byte[] toStore = new byte[SAFE_INPUT_READ];
		int i = 0;

		// Read the handshake response from the server
		try {

			ourSocket.getOutputStream().write(handShakeIn.getBytes());
			byte[] reader = new byte[1];
			int bytes = 0;

			int charCounter = 0;

			ourSocket.setSoTimeout(10000);

			try {
				while ((bytes = ourSocket.getInputStream().read(reader)) >= 0) {
					toStore[i] = reader[0];

					// Exit if we have reached the end of the stream
					if (toStore[i] == '\n') {
						charCounter++;
						if (charCounter == 2) {
							break;
						}
					}

					i++;
				}
			} catch (SocketTimeoutException e) {
				System.err.print(
						"Error with the SharOn connection - socket timeout.");
				System.out.println();
				logger.warning(
						"Error with the SharOn connection - socket timeout.");
				return;
			}

			String ourOut = new String(toStore, "UTF-8");

			// Check if the connection was successfully established
			if (ourOut.startsWith("REJECT") || !ourOut.startsWith("OK ")) {

				logger.warning("Connection was rejected.");

				throw new BadAttributeValueException(
						"Error with the establishment of the connection",
						"ourOut");

			} else {
				System.out.println("Sucessfully connected to SharOn.");
				logger.fine("Successfully connected to SharOn.");

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
