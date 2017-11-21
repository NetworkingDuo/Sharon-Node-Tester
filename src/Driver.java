import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Driver {

	private static final double SHARON_VERSION = 1.0;
	private static final int ITERATIONS = 1000;

	private static Logger complianceLogger;
	private static Logger reliabilityLogger;
	private static Logger fuzzerLogger;

	public static void main(String[] args)
			throws IOException, NullPointerException, InterruptedException {

		while (true) {

			File ourFile = new File("ports");
			FileReader reader = new FileReader(ourFile);
			BufferedReader bufferedReader = new BufferedReader(reader);
			String line;

			while ((line = bufferedReader.readLine()) != null) {

				long currTime = System.currentTimeMillis();

				// Read one line of student information at a time
				String[] info = line.split("\\s+");

				// Initialize our testing parameters
				String student = info[0];

				// Read in the necessary student attributes
				int currNodePort = Integer.parseInt(info[1]);
				int currDownloadPort = Integer.parseInt(info[2]);
				int mavenPort = Integer.parseInt(info[3]);
				String serverAddress = info[4];

				InetAddress ourAddress = InetAddress.getByName(serverAddress);

				System.out.println("Handling student " + student
						+ " with node port " + currNodePort + ", download port "
						+ currDownloadPort + ", and Maven Server Port "
						+ mavenPort + " on " + serverAddress + ".");

				// Setup Loggers
				complianceLogger = Logger
						.getLogger("tempComply" + currNodePort + ".log");
				FileHandler cfh = new FileHandler(
						"tempComply" + currNodePort + ".log");
				cfh.setFormatter(new SimpleFormatter());
				complianceLogger.setUseParentHandlers(false);
				complianceLogger.addHandler(cfh);

				reliabilityLogger = Logger
						.getLogger("tempRely" + currNodePort + ".log");
				FileHandler rfh = new FileHandler(
						"tempRely" + currNodePort + ".log");
				rfh.setFormatter(new SimpleFormatter());
				reliabilityLogger.setUseParentHandlers(false);
				reliabilityLogger.addHandler(rfh);

				fuzzerLogger = Logger
						.getLogger("tempFuzzer" + currNodePort + ".log");
				FileHandler fuzzfh = new FileHandler(
						"tempFuzzer" + currNodePort + ".log");
				fuzzfh.setFormatter(new SimpleFormatter());
				fuzzerLogger.setUseParentHandlers(false);
				fuzzerLogger.addHandler(fuzzfh);

				complianceLogger.info("Handling student " + student
						+ " with node port " + currNodePort + ", download port "
						+ currDownloadPort + ", and Maven Server Port "
						+ mavenPort + " on " + serverAddress + ".");

				reliabilityLogger.info("Handling student " + student
						+ " with node port " + currNodePort + ", download port "
						+ currDownloadPort + ", and Maven Server Port "
						+ mavenPort + " on " + serverAddress + ".");

				fuzzerLogger.info("Handling student " + student
						+ " with node port " + currNodePort + ", download port "
						+ currDownloadPort + ", and Maven Server Port "
						+ mavenPort + " on " + serverAddress + ".");

				// Initialize the socket connections
				try {

					Socket ourSocket = new Socket(ourAddress, currNodePort);

					// Run Compliance, Fuzzer, and Reliability tests
					// Run the Compliance tests

					// Test for the connection with the right handshake
					Compliance.checkConnection(ourSocket, currNodePort,
							serverAddress, complianceLogger);

					ourSocket.close();

					try {
						ourSocket = new Socket(ourAddress, currNodePort);
					} catch (ConnectException e) {
						System.out.println("Error with the reconnection.");
					}

					// Test for the connection with an incorrect handshake
					Compliance.checkConnectionReject(ourSocket, currNodePort,
							serverAddress, complianceLogger);

					ourSocket.close();

					try {
						ourSocket = new Socket(ourAddress, currNodePort);
						Compliance.checkConnection(ourSocket, currNodePort,
								serverAddress, complianceLogger);
					} catch (ConnectException e) {
						System.out.println("Error with the reconnection.");
					}

					// Test for an empty search, then search the first result
					Compliance.testSharons(ourSocket, currNodePort, ourAddress,
							currDownloadPort, serverAddress, complianceLogger);

					// Open the downloadPort
					try (Socket downloadSocket = new Socket(
							InetAddress.getByName(serverAddress),
							currDownloadPort)) {

						SharOnFuzzer sof = new SharOnFuzzer(ourSocket,
								downloadSocket);

						try {
							sof.fuzzNodeSearch(fuzzerLogger, ITERATIONS);
							sof.fuzzNodeResponse(fuzzerLogger, ITERATIONS);
							sof.fuzzDownloads(fuzzerLogger, ITERATIONS);
						}
						catch (IOException e) {
							if (ourSocket.isClosed()) {
								try {
									ourSocket.connect(new InetSocketAddress(
											InetAddress
													.getByName(serverAddress),
											currNodePort));
								} catch (IOException e2) {
									System.out.println(
											"Unable to reconnect to socket.");
									fuzzerLogger.warning(
											"Unable to reconnect to socket during fuzzer testing.");
								}
							}
						}

						// Run the Reliability tests
						ourSocket.close();
						try {
							ourSocket = new Socket(ourAddress, currNodePort);
							Reliability.checkConnection(ourSocket, currNodePort,
									serverAddress, reliabilityLogger);
						} catch (ConnectException e) {
							System.out.println("Error with the reconnection.");
						}

						// Simulate user interaction with the node
						for (int i = 0; i < 500; i++) {

							// Select a random function to run

							Random randNum = new Random();
							int randSelect = randNum.nextInt(3) + 0;

							if (randSelect == 0) {
								Reliability.testBlankSearches(serverAddress,
										currNodePort, ourSocket, i,
										reliabilityLogger);
							}
							if (randSelect == 1) {
								Reliability.testWordSearches(serverAddress,
										currNodePort, ourSocket, i,
										reliabilityLogger);
							}
							if (randSelect == 2) {
								Reliability.testSharons(ourSocket, currNodePort,
										ourAddress, currDownloadPort,
										serverAddress, i, reliabilityLogger);
							}
						}
					} catch (ConnectException e) {
						System.err.println("Error with the port connection.");
						System.out.println();
					}

					System.out.println("Done with student - " + student + ".");

					String fileNameStudentComply = "tempComply" + currNodePort
							+ ".log";
					String fileNameStudentRely = "tempRely" + currNodePort
							+ ".log";

					// Move the files to the public_html directory
					File ourComplianceLogger = new File(
							"tempComply" + currNodePort + ".log");
					ourComplianceLogger.renameTo(new File(
							"/home/csi/h/harrisonh/public_html/comply/tempComply"
									+ currNodePort + ".log"));

					File ourReliabilityLogger = new File(
							"tempRely" + currNodePort + ".log");
					ourReliabilityLogger.renameTo(new File(
							"/home/csi/h/harrisonh/public_html/comply/tempRely"
									+ currNodePort + ".log"));

					File ourFuzzerLogger = new File(
							"tempFuzzer" + currNodePort + ".log");
					ourFuzzerLogger.renameTo(new File(
							"/home/csi/h/harrisonh/public_html/fuzzer/tempFuzzer"
									+ currNodePort + ".log"));

				} catch (ConnectException e) {
					System.err.println(InetAddress.getByName(serverAddress)
							.getHostAddress() + " on port " + currNodePort
							+ " failed the connection test.");
					complianceLogger.warning(InetAddress
							.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " failed the connection test.");
					reliabilityLogger.warning(InetAddress
							.getByName(serverAddress).getHostAddress()
							+ " on port " + currNodePort
							+ " failed the connection test.");
					fuzzerLogger.warning(InetAddress.getByName(serverAddress)
							.getHostAddress() + " on port " + currNodePort
							+ " failed the connection test.");
				}

				long postRuntime = System.currentTimeMillis();
				long totalRunTime = postRuntime - currTime;
				long waitTime = (60000 * 60) - totalRunTime;

				Thread.sleep(waitTime);
			}
			bufferedReader.close();
		}
	}
}
