import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.*;
import java.math.BigInteger;
import java.util.StringTokenizer;


public class FxServer {

	public static void main(String[] args) throws Exception {

		try (ServerSocket ss = new ServerSocket(82)) {
			while (true) {
				System.out.println("Server waiting...");
				Socket connectionFromClient = ss.accept();
				System.out.println(
						"Server got a connection from a client whose port is: " + connectionFromClient.getPort());
				
				try {
					InputStream in = connectionFromClient.getInputStream();
					OutputStream out = connectionFromClient.getOutputStream();

					String errorMessage = "NOT FOUND\n";

					BufferedReader headerReader = new BufferedReader(new InputStreamReader(in));
					BufferedWriter headerWriter = new BufferedWriter(new OutputStreamWriter(out));
					
					DataInputStream dataIn = new DataInputStream(in);
					DataOutputStream dataOut = new DataOutputStream(out);

					String header = headerReader.readLine();
					StringTokenizer strk = new StringTokenizer(header, " ");

					String command = strk.nextToken();

					// if the command received is download, it means that we dont have the file in client share, so we will download the file normally 
					if (command.equals("download")) {
						try {
							String fileName = strk.nextToken();
							FileInputStream fileIn = new FileInputStream("C:\\Users\\Mes documents\\Downloads\\Java FxApp\\code\\ServerShare\\" + fileName);
							
							int fileSize = fileIn.available();
							 
							header = "OK " + fileSize + "\n";
                           
							headerWriter.write(header, 0, header.length());
							headerWriter.flush();

							byte[] bytes = new byte[fileSize];
							fileIn.read(bytes);

							fileIn.close();

							dataOut.write(bytes, 0, fileSize);

						} catch (Exception ex) {
							headerWriter.write(errorMessage, 0, errorMessage.length());
							headerWriter.flush();

						} finally {
							connectionFromClient.close();
						}
					// If the command received is exist,it means that we already have the file in client share, we just need to chek if the file is dirty/stale or not	
					} else if (command.equals("exist")){
						
						String fileName = strk.nextToken();
						try{
							FileInputStream fileIn = new FileInputStream("C:\\Users\\Mes documents\\Downloads\\Java FxApp\\code\\ServerShare\\" + fileName);
							int fileSize = fileIn.available();
							
							// Read the contents of a file specified by the file path into a byte array
							byte[] Filedata = Files.readAllBytes(Paths.get("C:\\Users\\Mes documents\\Downloads\\Java FxApp\\code\\ServerShare\\" + fileName));
							// Compute the MD5 hash of the file's data and store it in a byte array
							byte[] hash = MessageDigest.getInstance("MD5").digest(Filedata);
							// Convert the MD5 hash byte array into a hexadecimal string representation
							String check_sum = new BigInteger(1, hash).toString(16);
							
							
							header = headerReader.readLine();
							StringTokenizer tokenizer = new StringTokenizer(header, " ");
							//Get the first token which is the string "hash"
							
							String status =tokenizer.nextToken();
							// get the client checksum from the header sent by client
							String Client_Hash = tokenizer.nextToken();
							
							//check if the client and server files have the same hash.
							if(status.equals("hash")){	
									
								if(check_sum.equals(Client_Hash)) {
									// if both the hashes are equal, then the file is the same and there is no need to redownload
									header = "Noneed\n";
									headerWriter.write(header, 0, header.length());
									headerWriter.flush();

								} else {
									// if MD5 hashes are different, send header indicating the necessity to redownload it
									header = "OK " + fileSize + "\n";

									headerWriter.write(header, 0, header.length());
									headerWriter.flush();
							
									byte[] bytes = new byte[fileSize];
									fileIn.read(bytes);

									fileIn.close();

									dataOut.write(bytes, 0, fileSize);
								}

							}
							
					    } catch (Exception ex) {
							
							headerWriter.write(errorMessage, 0, errorMessage.length());
							headerWriter.flush();

					    } finally {
						connectionFromClient.close();
					    }


					}else if (command.equals("upload")) {
						String fileName = strk.nextToken();
						// Extract the file size from the received command
						String fileSize = strk.nextToken();
						int size = Integer.parseInt(fileSize);// Convert the file size to an integer
						
						// Create a byte array to store the file content
						byte[] space = new byte[size];
						dataIn.readFully(space);// Read the file content from the data input stream and store it in the 'space' array
						
						// Create a FileOutputStream to write the received file content to the server's directory
						try (FileOutputStream fileOut = new FileOutputStream("C:\\Users\\Mes documents\\Downloads\\Java FxApp\\code\\ServerShare\\" + fileName)){
							fileOut.write(space, 0, size);
						}

					}else if (command.equals("list")){

						//Create a File object to represent the directory where files are stored on the server
						File PathDirectory = new File("C:\\Users\\Mes documents\\Downloads\\Java FxApp\\code\\ServerShare\\");
						
						// List all the files in the directory and store their names in an array
						String FilesList[] = PathDirectory.list();
						
						//Get the number of files in the server share and send it to client
						dataOut.writeInt(FilesList.length);
						// If the files list is not empty send them to the client
						if (FilesList != null && FilesList.length > 0) {
						// Loop through the list of file names and send each name to the client
								for(int i=0; i <= FilesList.length; i++) {
								
								header=FilesList[i]+"\n"; // Get the current file name
								headerWriter.write(header, 0, header.length());// Write the file name to the output stream for the client
								headerWriter.flush();

								}
						// if the FilesList is empty send the following header to signal the client		
						}else{
								header = "empty " + "\n";
								headerWriter.write(header, 0, header.length());
								headerWriter.flush();

						}	

					}else if (command.equals("resume")) {

						int bytesTransferred = 0;
						int totalFileSize = 0;

						String fileName = strk.nextToken();
						// Extract the file size that were already received in the client share from the received command
						String fileSize = strk.nextToken();
						bytesTransferred = Integer.parseInt(fileSize);
						
						try {
								
							FileInputStream fileInputStream = new FileInputStream("C:\\Users\\Mes documents\\Downloads\\Java FxApp\\code\\ServerShare\\" + fileName);
							totalFileSize = fileInputStream.available();
							
					
							if ((totalFileSize - bytesTransferred) == 0) {
								// The file is already fully downloaded; notify the client.
								headerWriter.write("downloadComplete", 0, "downloadComplete".length());
								headerWriter.flush();
								continue;
							}
					
							// Send an acknowledgment with the remaining bytes to complete the download.
							int bytesRemaining = totalFileSize - bytesTransferred;
							header = "acknowledgment " + bytesRemaining + " \n";
							headerWriter.write(header);
							headerWriter.flush();
							
							// Prepare the byte array for the remaining file content.
							byte[] space = new byte[bytesRemaining];

							// Skip the bytes already sent.
							if(bytesTransferred > 0){
								
								fileInputStream.skip(bytesTransferred);
								fileInputStream.read(space);
								fileInputStream.close();
						
								// Send the remaining bytes to the client.
								dataOut.write(space, 0, bytesRemaining);
								dataOut.flush();
								bytesTransferred+= bytesRemaining;
							}

						} catch (Exception e) {
							// Handle exceptions and send an error message to the client if needed.
							headerWriter.write("downloadError", 0, "downloadError".length());
							headerWriter.flush();
						} finally {
							connectionFromClient.close();
						}
					}else {

						System.out.println("Connection got from an incompatible client");
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}