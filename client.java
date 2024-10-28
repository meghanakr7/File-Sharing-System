
import java.io.*;
import java.net.*;

public class client {

    public static String SERVER_HOST = "localhost"; // Replace with server IP if remote
    public static final int SERVER_PORT = 8000;

    public static void main(String[] args) throws Exception {
        // if (args.length > 0 && args[0].equalsIgnoreCase("client")) {
            // Run the client
            String serverAddress = System.getenv("PA1_SERVER");
            if (serverAddress == null || !serverAddress.contains(":")) {
                System.out.println("PA1_SERVER environment variable not set correctly.");
                return;
            }
            String[] serverInfo = serverAddress.split(":");
            // SERVER_HOST = serverInfo[0];
            // SERVER_PORT = Integer.parseInt(serverInfo[1]);
    
            // Upload command
            if (args.length > 2 && args[0].equalsIgnoreCase("upload")) {
                String clientFilePath = args[1];  // Client file path from command line
                String serverFilePath = args[2];  // Server file path from command line
                uploadFile(clientFilePath, serverFilePath); // Call the upload method
            } 
            // Download command
            else if (args.length > 2 && args[0].equalsIgnoreCase("download")) {
                String serverFilePath = args[1];  // Server file path from command line
                String clientFilePath = args[2];  // Client file path from command line
                downloadFile(serverFilePath, clientFilePath);  // Call the download method
            } 
            // List directory command
            else if (args.length > 1 && args[0].equalsIgnoreCase("dir")) {
                String serverDirectoryPath = args[1];  // Server directory path from command line
                listDirectory(serverDirectoryPath);  // Call the list directory method
            } 
            // Create directory command
            else if (args.length > 1 && args[0].equalsIgnoreCase("mkdir")) {
                String newDirectoryPath = args[1];  // New directory path from command line
                createDirectory(newDirectoryPath);  // Call the create directory method
            } 
            else if (args.length > 1 && args[0].equalsIgnoreCase("rmdir")) {
                String newDirectoryPath = args[1];  // New directory path from command line
                removeDirectory(newDirectoryPath);  // Call the create directory method
            } 
            else if (args.length > 1 && args[0].equalsIgnoreCase("rm")) {
                String filePath = args[1];  // New directory path from command line
                removeFile(filePath);  // Call the create directory method
            } 
            else if (args.length > 0 && args[0].equalsIgnoreCase("shutdown")) {
                shutdownServer();  // Call the shutdown server method
            }
            // Invalid command
            else {
                System.out.println("Invalid command. Usage:");
                System.out.println("  java -cp pa1.jar client mkdir <newDirectoryPath>");
                System.out.println("  java -cp pa1.jar client rmdir <directoryPath>");
                System.out.println("  java -cp pa1.jar client upload <clientFilePath> <serverFilePath>");
                System.out.println("  java -cp pa1.jar client download <serverFilePath> <clientFilePath>");
                System.out.println("  java -cp pa1.jar client dir <serverDirectoryPath>");
                System.out.println("  java -cp pa1.jar client shutdown");
            }
        // } else {
        //     System.out.println("Usage:");
        //     System.out.println("  java -cp pa1.jar server start <portnumber>");
        //     System.out.println("  java -cp pa1.jar client mkdir <newDirectoryPath>");
        //     System.out.println("  java -cp pa1.jar client rmdir <directoryPath>");
        //     System.out.println("  java -cp pa1.jar client upload <clientFilePath> <serverFilePath>");
        //     System.out.println("  java -cp pa1.jar client download <serverFilePath> <clientFilePath>");
        //     System.out.println("  java -cp pa1.jar client dir <serverDirectoryPath>");
        //     System.out.println("  java -cp pa1.jar client shutdown");
        // }
    }

    public static void shutdownServer() {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {
    
            dos.writeUTF("shutdown");  // Send shutdown command to the server
    
        
            String response = dis.readUTF();
            System.out.println(response);
       
            if (response.equalsIgnoreCase("success")) {
                System.out.println("Server shutdown initiated successfully.");
            } else {
                System.err.println("Error: Failed to initiate server shutdown.");
            }
    
        } catch (Exception e) {
            System.err.println("Error occurred while attempting to shut down the server.");
            System.err.println("Detailed error message: " + e.getMessage());
            int errorCode = getErrorCode(e);  // Dynamically get the error code
            System.err.println("Error code: " + errorCode);
            System.exit(errorCode);
        }
    }
    
    public static void uploadFile(String clientFilePath, String serverFilePath) throws IOException {
        String serverHost = System.getenv("PA1_SERVER");
        if (serverHost == null || !serverHost.contains(":")) {
            System.err.println("Invalid server address.");
            return;
        }
    
        String[] serverInfo = serverHost.split(":");
        String host = serverInfo[0];
        int port = Integer.parseInt(serverInfo[1]);
    
        File file = new File(clientFilePath);
        long fileSize = file.length();
        long alreadyUploaded = 0;  // Bytes already uploaded
    
        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream());
             FileInputStream fileIn = new FileInputStream(file)) {
    
            // Send upload request
            out.writeUTF("upload");
            out.writeUTF(serverFilePath);
            out.writeLong(fileSize);  // Send total file size
    
            // Read the server's response ("new", "resume", or "success")
            String response = in.readUTF();
    
            // If the file has already been uploaded, print the message and exit
            if (response.equalsIgnoreCase("success")) {
                System.out.println("File has already been uploaded successfully.");
                return;  // No need to proceed further
            }
    
            // Handle "resume" or "new" cases where a long is expected
            if (response.equalsIgnoreCase("resume") || response.equalsIgnoreCase("new")) {
                alreadyUploaded = in.readLong();  // Read the number of bytes already uploaded
                System.out.println("Resuming upload from " + alreadyUploaded + " bytes.");
            }
    
            // Skip the already uploaded bytes
            if (alreadyUploaded > 0) {
                fileIn.skip(alreadyUploaded);
            }
    
            // Calculate how many bytes per 1% of the file
            long bytesPerPercent = fileSize / 100;
            byte[] buffer = new byte[16];  // Smaller buffer for a slower upload
            long totalBytesUploaded = alreadyUploaded;
            int bytesRead;
    
            // Start the progress percentage based on the already uploaded amount
            int progressPercentage = (int) ((alreadyUploaded * 100) / fileSize);
            int previousPercentage = progressPercentage;
    
            // Upload the file and update progress
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytesUploaded += bytesRead;
    
                // Calculate the new progress percentage
                int newProgressPercentage = (int) ((totalBytesUploaded * 100) / fileSize);
    
                // Only update when the percentage increases
                if (newProgressPercentage > previousPercentage) {
                    progressPercentage = newProgressPercentage;
                    previousPercentage = progressPercentage;
    
                    // Create and print the progress bar in the same line
                    StringBuilder progressBar = new StringBuilder("[");
                    int progressBars = progressPercentage / 2;  // Scale 0-100% to 50 characters
                    for (int i = 0; i < 50; i++) {
                        if (i < progressBars) {
                            progressBar.append("=");
                        } else {
                            progressBar.append(" ");
                        }
                    }
                    progressBar.append("] ").append(progressPercentage).append("%");
    
                    // Print the progress bar on the same line
                    System.out.print("\r" + progressBar.toString());
    
                    // Introduce a longer delay to slow down the upload process
                    try {
                        Thread.sleep(500);  // Sleep for 2 seconds
                    } catch (InterruptedException e) {
                        System.err.println("Upload interrupted.");
                        Thread.currentThread().interrupt();
                    }
                }
            }
    
            // Final confirmation from the server
            String uploadStatus = in.readUTF();
            if (uploadStatus.equals("success")) {
                System.out.println("\nFile uploaded successfully.");
            } else if (uploadStatus.equals("interrupted")) {
                System.out.println("\nUpload interrupted.");
            }
    
        } catch (IOException e) {
            System.err.println("Error during file upload: " + e.getMessage());
            e.printStackTrace();  // Print the full stack trace to diagnose the issue
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Client method to download a file from the server
    public static void downloadFile(String serverFilePath, String clientFilePath) throws IOException {
        String serverHost = System.getenv("PA1_SERVER");  // Get the server host and port from the environment variable
        if (serverHost == null || !serverHost.contains(":")) {
            System.err.println("Invalid server address.");
            return;
        }
    
        String[] serverInfo = serverHost.split(":");
        String host = serverInfo[0];
        int port = Integer.parseInt(serverInfo[1]);
    
        File file = new File(clientFilePath);
        long existingFileSize = 0;  // The size of the already downloaded portion
    
        // Check if the file already exists on the client side
        if (file.exists()) {
            existingFileSize = file.length();  // Get the size of the partially or fully downloaded file
        }
    
        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
    
            // Send download command to the server
            out.writeUTF("download");
            out.writeUTF(serverFilePath);
            out.writeLong(existingFileSize);  // Send the number of bytes already downloaded
            out.flush();  // Flush the command to the server
    
            // Read the server response
            String response = in.readUTF();
            if (response.equalsIgnoreCase("alreadyDownloaded")) {
                System.out.println("File has already been fully downloaded.");
                return;  // No error, exit gracefully
            } else if (!response.equalsIgnoreCase("resume") && !response.equalsIgnoreCase("new")) {
                System.err.println("Server returned an error, aborting download.");
                return;
            }
    
            // Get the total file size from the server
            long totalFileSize = in.readLong();
            System.out.println("File size received from server: " + totalFileSize + " bytes");
    
            // Validate the file size
            if (totalFileSize <= 0) {
                System.err.println("Invalid file size received. Exiting.");
                return;
            }
    
            // Check if the existing file size is larger than or equal to the total file size
            if (existingFileSize >= totalFileSize) {
                System.out.println("File has already been downloaded.");
                return;  // No need to download again
            }
    
            // Resume download from the existing file size
            try (RandomAccessFile fileOut = new RandomAccessFile(clientFilePath, "rw")) {
                long totalBytesDownloaded = existingFileSize;
                fileOut.seek(existingFileSize);  // Move the file pointer to the already downloaded part
                byte[] buffer = new byte[16];  // Smaller buffer for a slower download
                int bytesRead;
    
                // Calculate how many bytes per 1% of the file
                long bytesPerPercent = totalFileSize / 100;
                int progressPercentage = (int) ((totalBytesDownloaded * 100) / totalFileSize);
                int previousPercentage = progressPercentage;
    
                // Download the file in chunks and write to the client file path
                while (totalBytesDownloaded < totalFileSize && (bytesRead = in.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    totalBytesDownloaded += bytesRead;
    
                    // Calculate and display the progress
                    int newProgressPercentage = (int) ((totalBytesDownloaded * 100) / totalFileSize);
    
                    // Update progress only if percentage increases
                    if (newProgressPercentage > previousPercentage) {
                        progressPercentage = newProgressPercentage;
                        previousPercentage = progressPercentage;
    
                        // Update progress bar
                        displayDownloadProgress(progressPercentage);
    
                        // Introduce a longer delay to slow down the download process
                        try {
                            Thread.sleep(200);  // Sleep for 2 seconds
                        } catch (InterruptedException e) {
                            System.err.println("Download interrupted.");
                            Thread.currentThread().interrupt();
                        }
                    }
                }
    
                // Check if download completed successfully
                if (totalBytesDownloaded == totalFileSize) {
                    System.out.println("\nFile downloaded successfully.");
                } else {
                    System.err.println("Download incomplete. Expected " + totalFileSize + " bytes, but received " + totalBytesDownloaded + " bytes.");
                }
            }
    
        } catch (IOException e) {
            System.err.println("Error during file download: " + e.getMessage());
        }
    }
    
    
    // Method to dynamically assign error codes based on exception type
    public static int getErrorCode(Exception e) {
        if (e instanceof FileNotFoundException) {
            return 1;  // Error code for file not found
        } else if (e instanceof SocketException) {
            return 2;  // Error code for network-related issues
        } else if (e instanceof EOFException) {
            return 3;  // Error code for end-of-file reached unexpectedly
        } else if (e instanceof IOException) {
            return 4;  // General I/O error code
        } else {
            return 99;  // Generic error code for unexpected exceptions
        }
    }


    public static void listDirectory(String serverDirectoryPath) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {
    
            // Send the request to the server
            dos.writeUTF("listDirectory");
            dos.writeUTF(serverDirectoryPath);  // Send the directory path to list
            dos.flush();  // Flush the output to ensure it's sent
    
            // Check if the server responds with an error
            String response = dis.readUTF();
            if (response.equalsIgnoreCase("error")) {
                String errorMessage = dis.readUTF();  // Read the error message from the server
                System.err.println(errorMessage);
                System.exit(1);  // Return non-zero error code if the directory doesn't exist
            }
    
            // Receive and print the directory tree structure
            String treeStructure = dis.readUTF();
            System.out.println("Contents of directory '" + serverDirectoryPath + "':");
            System.out.println(treeStructure);
    
        } catch (Exception e) {
            System.err.println("Error occurred while listing the directory '" + serverDirectoryPath + "'.");
    System.err.println("Detailed error message: " + e.getMessage());
            int errorCode = getErrorCode(e);  // Dynamically get the error code
            System.err.println("Error code: " + errorCode);
            System.exit(errorCode);
        }
    }
    
    public static void removeFile(String serverFilePath) throws IOException {
        String serverHost = System.getenv("PA1_SERVER");  // Get the server host and port from the environment variable
        if (serverHost == null || !serverHost.contains(":")) {
            System.err.println("Invalid server address.");
            return;
        }
    
        String[] serverInfo = serverHost.split(":");
        String host = serverInfo[0];
        int port = Integer.parseInt(serverInfo[1]);
    
        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
    
            // Send remove (rm) command to the server
            out.writeUTF("rm");
            out.writeUTF(serverFilePath);
            out.flush();  // Ensure the command is flushed to the server
    
            // Read the server's response
            String response = in.readUTF();
            if (response.equalsIgnoreCase("success")) {
                System.out.println("File '" + serverFilePath + "' removed successfully.");
            } else if (response.equalsIgnoreCase("error")) {
                String errorMessage = in.readUTF();
                System.err.println(errorMessage);
                System.exit(1);  // Return a non-zero error code
            }
    
        } catch (IOException e) {
            System.err.println("Error occurred while attempting to remove the file.");
            System.err.println("Detailed error message: " + e.getMessage());
            System.exit(1);  // Return a non-zero error code
        }
    }
    
    public static void createDirectory(String newDirectoryPath) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {
    
            dos.writeUTF("mkdir");  // Send mkdir command
            dos.writeUTF(newDirectoryPath);  // Send the new directory path to the server
    
            // Receive and display the server's response
            String response = dis.readUTF();
            if (response.equalsIgnoreCase("success")) {
                System.out.println("Directory created successfully at '" + newDirectoryPath + "'.");
            } else if (response.equalsIgnoreCase("error")) {
                String errorMessage = dis.readUTF();  // Read the error message
                System.err.println(errorMessage);
                System.exit(1);  // Return non-zero error code
            }
    
        } catch (Exception e) {
            System.err.println("Error occurred while creating the directory '" + newDirectoryPath + "'.");
            System.err.println("Detailed error message: " + e.getMessage());
            int errorCode = getErrorCode(e);  // Dynamically get the error code
            System.err.println("Error code: " + errorCode);
            System.exit(errorCode);
        }
    }
    
    public static void removeDirectory(String directoryPath) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {
    
            dos.writeUTF("rmdir");  // Send rmdir command
            dos.writeUTF(directoryPath);  // Send the directory path to the server
    
            // Receive and display the server's response
            String response = dis.readUTF();
            if (response.equalsIgnoreCase("success")) {
                System.out.println("Directory '" + directoryPath + "' removed successfully.");
            } else if (response.equalsIgnoreCase("error")) {
                String errorMessage = dis.readUTF();  // Read the error message
                System.err.println(errorMessage);
                System.exit(1);  // Return non-zero error code
            }
    
        } catch (Exception e) {
            System.err.println("Error occurred while removing the directory '" + directoryPath + "'.");
            System.err.println("Detailed error message: " + e.getMessage());
            int errorCode = getErrorCode(e);  // Dynamically get the error code
            System.err.println("Error code: " + errorCode);
            System.exit(errorCode);
        }
    }
    
    
 

// Client-side function to remove a directory
public static void removeDirectory(String serverHost, int serverPort, String dirPath) throws IOException {
    try (Socket socket = new Socket(serverHost, serverPort);
         DataOutputStream out = new DataOutputStream(socket.getOutputStream());
         DataInputStream in = new DataInputStream(socket.getInputStream())) {

        out.writeUTF("removedir");  // Tell server it's a remove directory request
        out.writeUTF(dirPath);

        String response = in.readUTF();
        if (response.equalsIgnoreCase("OK")) {
            System.out.println("Directory removed successfully.");
        } else {
            System.out.println("Error: Could not remove directory.");
        }
    }
}

    
   
    
    
    
    public static void displayDownloadProgress(int progressPercentage) {
        StringBuilder progressBar = new StringBuilder("[");
        int progressBars = progressPercentage / 2;  // Scale 0-100% to 50 characters
        for (int i = 0; i < 50; i++) {
            if (i < progressBars) {
                progressBar.append("=");
            } else {
                progressBar.append(" ");
            }
        }
        progressBar.append("] ").append(progressPercentage).append("%");
    
        // Print the progress bar on the same line
        System.out.print("\r" + progressBar.toString());
    }
    
}
