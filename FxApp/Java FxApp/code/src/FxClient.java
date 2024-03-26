
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.io.*;
import java.math.BigInteger;
import java.util.StringTokenizer;

public class FxClient {
	public static void main(String[] args) throws Exception {
		String command = args[0];
	
		

		try (Socket connectionToServer = new Socket("localhost", 82)) {

			// I/O operations

			InputStream in = connectionToServer.getInputStream();
			OutputStream out = connectionToServer.getOutputStream();

			BufferedReader headerReader = new BufferedReader(new InputStreamReader(in));
			BufferedWriter headerWriter = new BufferedWriter(new OutputStreamWriter(out));
			DataInputStream dataIn = new DataInputStream(in);
			DataOutputStream dataOut = new DataOutputStream(out);
			
			if (command.equals("d")) {
					
				String fileName = args[1];
				
				File file = new File("C:\\Users\\Mes documents\\Downloads\\Java FxApp\\code\\ClientShare\\" + fileName);
				// If the file does Not exists locally, download  it normally by sending the "download" header
				if (!file.exists()) {
					String header = "download " + fileName + "\n";
					headerWriter.write(header, 0, header.length());
					headerWriter.flush();
				
					header = headerReader.readLine();
					// Check if the file is not found
					if (header.equals("NOT FOUND")) {
						System.out.println("We're extremely sorry, the file you specified is not available!");
					
					} else {
						StringTokenizer strk = new StringTokenizer(header, " ");

						String status = strk.nextToken();

						if (status.equals("OK")) {

							String temp = strk.nextToken();

							int size = Integer.parseInt(temp);

							byte[] space = new byte[size];

							dataIn.readFully(space);

							try (FileOutputStream fileOut = new FileOutputStream("C:\\Users\\Mes documents\\Downloads\\Java FxApp\\code\\ClientShare\\" + fileName)) {
								fileOut.write(space, 0, size);
							}
							System.out.println("The file is downloaded successfully!");
						} else {
							System.out.println("You're not connected to the right Server!");
						}

					}
				//here the file exists in the client share, we need to Check if it is Stale/dirty or not
			   }else{
						// send this header to client side indicating that the file exists in the client share
						String header = "exist " + fileName + "\n";
						headerWriter.write(header, 0, header.length());
						headerWriter.flush();

						// Read the contents of a file specified by the file path into a byte array
						byte[] Filedata = Files.readAllBytes(Paths.get("C:\\Users\\Mes documents\\Downloads\\Java FxApp\\code\\ClientShare\\" + fileName));
						// Compute the MD5 hash of the file's data and store it in a byte array
						byte[] hash = MessageDigest.getInstance("MD5").digest(Filedata);
						// Convert the MD5 hash byte array into a hexadecimal string representation
						String check_sum = new BigInteger(1, hash).toString(16);
						
								
						//send the checksum to the server side to check for equality
						header = "hash " + check_sum + "\n";
						headerWriter.write(header, 0, header.length());
						headerWriter.flush();
						
						// read the header sent by server indicating whether the file is dirty or not
						header = headerReader.readLine();
						
						// if the header is different than "Noneed" it means that the file is dirty and needs to be redownloaded
						if (!header.equals("Noneed\n")) {
							
							StringTokenizer strk = new StringTokenizer(header, " ");
							String status = strk.nextToken();
							// Check the header if it is "Ok" to procced with the download
							if (status.equals("OK")) {
								System.out.println("the file you have in your client share is DIRTY, it will be updated soon...");
								String temp = strk.nextToken();

								int size = Integer.parseInt(temp);

								byte[] space = new byte[size];

								dataIn.readFully(space);

								try (FileOutputStream fileOut = new FileOutputStream("C:\\Users\\Mes documents\\Downloads\\Java FxApp\\code\\ClientShare\\" + fileName)) {
									fileOut.write(space, 0, size);
								}
								System.out.println("file updated!");
							} 
						// otherwise, if the header sent is "Noneed", we will inform the client that he/she already has the file 	
						else {
								System.out.println("You already have the exact same file in your client share!");
							}
						}
						
			  }

			} else if (command.equals("u")) {

					String fileName = args[1];
					File file = new File("C:\\Users\\Mes documents\\Downloads\\Java FxApp\\code\\ClientShare\\" + fileName);
					// if the file exists, procced to download
					if(file.exists()){

						System.out.println("uploading the file is in progress...");
						// Create a FileInputStream to read the file to be uploaded from the client's local directory
						FileInputStream fileIn = new FileInputStream("C:\\Users\\Mes documents\\Downloads\\Java FxApp\\code\\ClientShare\\" + fileName);
						int fileSize = fileIn.available(); // Get the size of the file to be uploaded
						
						// Create a header message specifying the file upload command, filename, and file size
						String header = "upload " + fileName +" "+ fileSize + "\n";
						// Write the header message to the output stream for the server
						headerWriter.write(header, 0, header.length());
						headerWriter.flush();
						
						// Create a byte array to hold the file content
						byte[] bytes = new byte[fileSize];
						fileIn.read(bytes); // Read the file content into the byte array
						fileIn.close(); // Close the FileInputStream since the file content is now in the 'bytes' array

						dataOut.write(bytes, 0, fileSize);// Write the file content to the data output stream for transmission to the server
						System.out.println("The file is uploaded successfully!");
					// if the file does not exist print this message	
					}else{
						System.out.println("Sorry, but the file you specified is not available!");
					}	

			} else if(command.equals("l")){
					 // Create a header message indicating that the client is requesting a list of files
				    String header = "list "+ "\n";
					
					// Write the header message to the output stream for the server
					headerWriter.write(header, 0, header.length());
					headerWriter.flush();
					
					// Read the number of files in the server share sent by the server
					int numFiles = dataIn.readInt();
					// Read the header sent by the server
					header = headerReader.readLine();
				
					StringTokenizer strk = new StringTokenizer(header, " ");

					String status = strk.nextToken();
					// if the header is "empty", it means that there are noo files in the server share
					if(status.equals("empty")){
						System.out.println("the server share is empty");
					// the server sare is not empty, procced with printing the files	
					}else{
						System.out.println("There are currently "+numFiles+" files in the server share, which are:");
						//print the first file of the server share
						System.out.println("**"+header+"**");
						String File;
						// print the rest of file recieved by the server as long as there not null
						while ((File = headerReader.readLine()) != null) {
							System.out.println("**"+File+"**");
						
						}
				    }

			}else if (command.equals("r")) {

				// Get the file name from the command
				String fileName = args[1];
				
				try {

					// Create a FileInputStream to read the local file from the client's directory.
					FileInputStream localFileInputStream = new FileInputStream("C:\\Users\\Mes documents\\Downloads\\Java FxApp\\code\\ClientShare\\" + fileName);
					int bytesReceived = localFileInputStream.available();// get the bytes that are already received.

					// Send a request to the server to resume the download from the specific point where it stopped.
					String header = "resume " + fileName + " " + bytesReceived + " \n";
					headerWriter.write(header);
					headerWriter.flush();

					localFileInputStream.close();

					// Receive the server's response.
					header = headerReader.readLine();
					StringTokenizer strk = new StringTokenizer(header, " ");
					String responseStatus = strk.nextToken();
					// If the response is an acknowledgment, we will resume the download where it stopped
					if (responseStatus.equals("acknowledgment")) {

						String temp = strk.nextToken();

						int remainingBytes = Integer.parseInt(temp);

						byte[] space = new byte[remainingBytes];
						
						dataIn.readFully(space);

						// Append the received bytes to the local file in client share.
						try (FileOutputStream localFileOutputStream = new FileOutputStream("C:\\Users\\Mes documents\\Downloads\\Java FxApp\\code\\ClientShare\\" + fileName, true)) {
							localFileOutputStream.write(space, 0, remainingBytes);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						System.out.println("Download is resumed successfully!");
					
					// if the header sent by the server is "downloadComplete" it means that the download is already done.	
					} else if (responseStatus.equals("downloadComplete")) {
						System.out.println("The file download is already complete.");
					} else {
						System.out.println("Unable to resume the download due to a server response issue.");
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				}else {
				
				String invalidCommandResponse = "INVALID COMMAND, please go review the list of valid commands\n";
			    System.out.println(invalidCommandResponse);
			}
		}
	}
	
}