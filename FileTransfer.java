import java.io.*;
import java.net.*;


public class FileTransfer {
    private volatile static boolean isRunning = true;  // Server running flag

    private static String SERVER_HOST = "localhost"; // Replace with server IP if remote
    private static final int SERVER_PORT = 8000;

    public static void main(String[] args) throws Exception {
        String serverAdd = System.getenv("PA1_SERVER");
        System.out.println("Server Address: " + serverAdd);
    
        if (args.length > 0 && args[0].equalsIgnoreCase("server")) {
            if (args.length > 1 && args[1].equalsIgnoreCase("start")) {
                if (args.length > 2) {
                    int portNumber = Integer.parseInt(args[2]);  // Parse the port number from args[2]
                    startServer(portNumber);
                } else {
                    System.out.println("Please provide a port number.");
                }
            } else {
                System.out.println("Invalid server command. Use 'start <portnumber>'.");
            }
        } else if (args.length > 0 && args[0].equalsIgnoreCase("client")) {
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
            if (args.length > 3 && args[1].equalsIgnoreCase("upload")) {
                String clientFilePath = args[2];  // Client file path from command line
                String serverFilePath = args[3];  // Server file path from command line
                uploadFile(clientFilePath, serverFilePath); // Call the upload method
            } 
            // Download command
            else if (args.length > 3 && args[1].equalsIgnoreCase("download")) {
                String serverFilePath = args[2];  // Server file path from command line
                String clientFilePath = args[3];  // Client file path from command line
                downloadFile(serverFilePath, clientFilePath);  // Call the download method
            } 
            // List directory command
            else if (args.length > 2 && args[1].equalsIgnoreCase("dir")) {
                String serverDirectoryPath = args[2];  // Server directory path from command line
                listDirectory(serverDirectoryPath);  // Call the list directory method
            } 
            // Create directory command
            else if (args.length > 2 && args[1].equalsIgnoreCase("mkdir")) {
                String newDirectoryPath = args[2];  // New directory path from command line
                createDirectory(newDirectoryPath);  // Call the create directory method
            } 
            else if (args.length > 2 && args[1].equalsIgnoreCase("rmdir")) {
                String newDirectoryPath = args[2];  // New directory path from command line
                removeDirectory(newDirectoryPath);  // Call the create directory method
            } 
            else if (args.length > 2 && args[1].equalsIgnoreCase("rm")) {
                String filePath = args[2];  // New directory path from command line
                removeFile(filePath);  // Call the create directory method
            } 
            else if (args.length > 1 && args[1].equalsIgnoreCase("shutdown")) {
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
        } else {
            System.out.println("Usage:");
            System.out.println("  java -cp pa1.jar server start <portnumber>");
            System.out.println("  java -cp pa1.jar client mkdir <newDirectoryPath>");
            System.out.println("  java -cp pa1.jar client rmdir <directoryPath>");
            System.out.println("  java -cp pa1.jar client upload <clientFilePath> <serverFilePath>");
            System.out.println("  java -cp pa1.jar client download <serverFilePath> <clientFilePath>");
            System.out.println("  java -cp pa1.jar client dir <serverDirectoryPath>");
            System.out.println("  java -cp pa1.jar client shutdown");
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
            }
    
            if (response.equalsIgnoreCase("resume")) {
                System.out.println("Resuming upload from " + alreadyUploaded + " bytes.");
                fileIn.skip(alreadyUploaded);  // Skip the already uploaded part of the file
            } else {
                System.out.println("Starting new upload.");
            }
    
            byte[] buffer = new byte[4096];
            long totalBytesUploaded = alreadyUploaded;
            int bytesRead;
    
            // Initialize progress
            displayProgress(0);  // Initialize the progress bar
    
            // Upload the file and display the progress bar
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytesUploaded += bytesRead;
    
                // Calculate and display progress
                int progressPercentage = (int) ((totalBytesUploaded * 100) / fileSize);
                displayProgress(progressPercentage);
            }
    
            // Final confirmation from the server
            String uploadStatus = in.readUTF();
            if (uploadStatus.equals("success")) {
                System.out.println("\nFile uploaded successfully.");
            } else if (uploadStatus.equals("interrupted")) {
                System.out.println("Upload interrupted.");
            }
    
        } catch (IOException e) {
            System.err.println("Error during file upload: " + e.getMessage());
            e.printStackTrace();  // Print the full stack trace to diagnose the issue
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Method to display a progress bar (client-side)
    public static void displayProgress(int percentage) {
        final int width = 50;  // Width of the progress bar in characters
        int progress = (percentage * width) / 100;
    
        StringBuilder progressBar = new StringBuilder();
        progressBar.append("[");
        for (int i = 0; i < width; i++) {
            if (i < progress) {
                progressBar.append("=");
            } else {
                progressBar.append(" ");
            }
        }
        progressBar.append("] ");
        progressBar.append(percentage).append("%");
    
        // Print progress on the same line
        System.out.print("\r" + progressBar.toString());
    }
    
    // Method to dynamically assign error codes based on exception type
    private static int getErrorCode(Exception e) {
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
    

    // public static void downloadFile(String serverFilePath, String clientFilePath) throws IOException {
    //     String serverHost = System.getenv("PA1_SERVER");  // Get the server host and port from the environment variable
    //     if (serverHost == null || !serverHost.contains(":")) {
    //         System.err.println("Invalid server address.");
    //         return;
    //     }
    
    //     String[] serverInfo = serverHost.split(":");
    //     String host = serverInfo[0];
    //     int port = Integer.parseInt(serverInfo[1]);
    
    //     try (Socket socket = new Socket(host, port);
    //          DataOutputStream out = new DataOutputStream(socket.getOutputStream());
    //          DataInputStream in = new DataInputStream(socket.getInputStream());
    //          FileOutputStream fileOut = new FileOutputStream(clientFilePath)) {
    
    //         // Send download command to the server
    //         out.writeUTF("download");
    //         out.writeUTF(serverFilePath);
    
    //         // Get the file size from the server
    //         long totalFileSize = in.readLong();
    //         long totalBytesDownloaded = 0;
    
    //         // DEBUG: Log the file size received from the server
    //         System.out.println("File size received from server: " + totalFileSize + " bytes");
    
    //         if (totalFileSize <= 0) {
    //             System.err.println("Invalid file size received. Exiting.");
    //             return;
    //         }
    
    //         System.out.println("Downloading file: " + serverFilePath + " (" + totalFileSize + " bytes)");
    
    //         // Buffer for downloading the file in chunks
    //         byte[] buffer = new byte[4096];
    //         int bytesRead;
    
    //         // Download the file in chunks and write to client file path
    //         while (totalBytesDownloaded < totalFileSize && (bytesRead = in.read(buffer)) != -1) {
    //             fileOut.write(buffer, 0, bytesRead);
    //             totalBytesDownloaded += bytesRead;
    
    //             // DEBUG: Print how many bytes have been downloaded so far
    //             System.out.println("Bytes downloaded so far: " + totalBytesDownloaded + "/" + totalFileSize);
    
    //             // Calculate the percentage of file downloaded
    //             int progressPercentage = (int) ((totalBytesDownloaded * 100) / totalFileSize);
    
    //             // Display the progress bar
    //             displayProgress(progressPercentage);
    //         }
    
    //         // DEBUG: Check if the download completed
    //         if (totalBytesDownloaded == totalFileSize) {
    //             System.out.println("\nFile downloaded successfully.");
    //         } else {
    //             System.err.println("File download incomplete. Expected: " + totalFileSize + " bytes, but got: " + totalBytesDownloaded + " bytes.");
    //         }
    //     } catch (IOException e) {
    //         System.err.println("Error during file download: " + e.getMessage());
    //     }
    // }

    private static void listDirectory(String serverDirectoryPath) {
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
    

    private static void createDirectory(String newDirectoryPath) {
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
    
    private static void removeDirectory(String directoryPath) {
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
    
    private static void shutdownServer() {
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
  
    // Client-side function to list directory contents
public static void listDirectoryContents(String serverHost, int serverPort, String dirPath) throws IOException {
    try (Socket socket = new Socket(serverHost, serverPort);
         DataOutputStream out = new DataOutputStream(socket.getOutputStream());
         DataInputStream in = new DataInputStream(socket.getInputStream())) {

        out.writeUTF("listdir");  // Tell server it's a list directory request
        out.writeUTF(dirPath);

        String response = in.readUTF();
        if (response.equalsIgnoreCase("OK")) {
            int fileCount = in.readInt();
            System.out.println("Directory contents:");
            for (int i = 0; i < fileCount; i++) {
                System.out.println(in.readUTF());
            }
        } else {
            System.out.println("Error: Directory not found or could not be listed.");
        }
    }
}

// Client-side function to create a directory
public static void createDirectory(String serverHost, int serverPort, String dirPath) throws IOException {
    try (Socket socket = new Socket(serverHost, serverPort);
         DataOutputStream out = new DataOutputStream(socket.getOutputStream());
         DataInputStream in = new DataInputStream(socket.getInputStream())) {

        out.writeUTF("createdir");  // Tell server it's a create directory request
        out.writeUTF(dirPath);

        String response = in.readUTF();
        if (response.equalsIgnoreCase("OK")) {
            System.out.println("Directory created successfully.");
        } else {
            System.out.println("Error: Could not create directory.");
        }
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

// Client-side function to remove a file
public static void removeFile(String serverHost, int serverPort, String filePath) throws IOException {
    try (Socket socket = new Socket(serverHost, serverPort);
         DataOutputStream out = new DataOutputStream(socket.getOutputStream());
         DataInputStream in = new DataInputStream(socket.getInputStream())) {

        out.writeUTF("removefile");  // Tell server it's a remove file request
        out.writeUTF(filePath);

        String response = in.readUTF();
        if (response.equalsIgnoreCase("OK")) {
            System.out.println("File removed successfully.");
        } else {
            System.out.println("Error: Could not remove file.");
        }
    }
}

// Client-side function to shut down the server
public static void shutdownServer(String serverHost, int serverPort) throws IOException {
    try (Socket socket = new Socket(serverHost, serverPort);
         DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

        out.writeUTF("shutdown");  // Tell server it's a shutdown request
        System.out.println("Shutdown request sent to server.");
    }
}


    // Display menu for client
    public static void displayMenu() {
        System.out.println("File Transfer Menu:");
        System.out.println("1. Upload file");
        System.out.println("2. Download file");
        System.out.println("3. List directory contents");
        System.out.println("4. Create directory");
        System.out.println("5. Remove directory");
        System.out.println("6. Remove File");
        System.out.println("7. Shutdown Server");
        System.out.println("8. Exit");
        System.out.print("Select an option (1-8): ");
    }

    // Server method to start and listen on a given port
    // public static void startServer(int port) throws IOException {
    //     ServerSocket serverSocket = new ServerSocket(port);
    //     System.out.println("Server started on port " + port);

    //     while (true) {
    //         Socket clientSocket = serverSocket.accept();
    //         handleClientRequest(clientSocket);
    //     }
    // }
    public static void startServer(int portNumber) {
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Server started on port " + portNumber);
    
            // Main server loop
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();  // Accept client connection
                    System.out.println("Client connected");
    
                    // Handle the client request in a separate method
                    handleClientRequest(clientSocket);
                } catch (SocketException se) {
                    if (!isRunning) {
                        System.out.println("Server is shutting down...");
                    } else {
                        System.err.println("Server error: " + se.getMessage());
                    }
                }
            }
    
            System.out.println("Server shut down successfully.");
    
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
    
    private static void handleClientRequest(Socket clientSocket) {
        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {
    
            String requestType = dis.readUTF();  // Read the request type from the client
    
            // Handle creating a directory
            if (requestType.equalsIgnoreCase("mkdir")) {
                String newDirectoryPath = dis.readUTF();  // Read the directory path to create
                File newDir = new File(newDirectoryPath);
                if (newDir.exists()) {
                    dos.writeUTF("error");  // Directory already exists
                    dos.writeUTF("Error: Directory '" + newDirectoryPath + "' already exists.");
                } else {
                    boolean success = newDir.mkdir();  // Try to create the directory
                    if (success) {
                        dos.writeUTF("success");  // Directory created successfully
                    } else {
                        dos.writeUTF("error");  // Failed to create the directory
                        dos.writeUTF("Error: Failed to create directory at '" + newDirectoryPath + "'.");
                    }
                }
            }
    
            // Handle removing a directory
            else if (requestType.equalsIgnoreCase("rmdir")) {
                String directoryPath = dis.readUTF();  // Read the directory path to remove
                File dir = new File(directoryPath);
                if (!dir.exists() || !dir.isDirectory()) {
                    dos.writeUTF("error");  // Directory doesn't exist or isn't a directory
                    dos.writeUTF("Error: Directory '" + directoryPath + "' does not exist.");
                } else if (dir.list().length > 0) {
                    dos.writeUTF("error");  // Directory is not empty
                    dos.writeUTF("Error: Directory '" + directoryPath + "' is not empty.");
                } else {
                    boolean success = dir.delete();  // Try to remove the directory
                    if (success) {
                        dos.writeUTF("success");  // Directory removed successfully
                    } else {
                        dos.writeUTF("error");  // Failed to remove the directory
                        dos.writeUTF("Error: Failed to remove directory '" + directoryPath + "'.");
                    }
                }
            }

            else if (requestType.equalsIgnoreCase("rm")) {
                String serverFilePath = dis.readUTF();  // Read the file path to remove
    
                File file = new File(serverFilePath);
                if (!file.exists() || !file.isFile()) {
                    dos.writeUTF("error");
                    dos.writeUTF("Error: File '" + serverFilePath + "' does not exist.");
                } else {
                    boolean success = file.delete();  // Attempt to remove the file
                    if (success) {
                        dos.writeUTF("success");  // File successfully deleted
                        System.out.println("File '" + serverFilePath + "' removed successfully.");
                    } else {
                        dos.writeUTF("error");
                        dos.writeUTF("Error: Failed to remove the file '" + serverFilePath + "'.");
                    }
                }
            }
    
            // Handle file upload
            else if (requestType.equalsIgnoreCase("upload")) {
                String remoteFilePath = dis.readUTF();  // Read file path to save on the server
                long fileSize = dis.readLong();  // Read the total file size
    
                File file = new File(remoteFilePath);
                long existingFileSize = 0;
    
                // If the file already exists, check if it's fully uploaded
                if (file.exists()) {
                    existingFileSize = file.length();
    
                    // If the file is fully uploaded, send "success" and return without sending a long
                    if (existingFileSize >= fileSize) {
                        dos.writeUTF("success");  // No upload needed, file already exists
                        System.out.println("The entire file has already been uploaded.");
                        return;  // No need to send any more data
                    }
    
                    // Resume the upload from where it left off
                    dos.writeUTF("resume");
                    dos.writeLong(existingFileSize);  // Send the number of bytes already uploaded
                    System.out.println("Resuming upload from " + existingFileSize + " bytes.");
                } else {
                    // Start a new upload if the file does not exist
                    dos.writeUTF("new");
                    dos.writeLong(0);  // No existing file, start from 0
                    System.out.println("Starting new upload.");
                }
    
                // Begin receiving the file
                try (FileOutputStream fos = new FileOutputStream(file, true)) {
                    byte[] buffer = new byte[4096];
                    long totalBytesRead = existingFileSize;
                    int bytesRead;
    
                    // Read and write the file in chunks
                    while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
    
                        // Update and display progress
                        int progressPercentage = (int) ((totalBytesRead * 100) / fileSize);
                        displayProgress(progressPercentage);
                    }
    
                    // Check if upload was successful or interrupted
                    if (totalBytesRead == fileSize) {
                        System.out.println("\nFile '" + remoteFilePath + "' uploaded successfully.");
                        dos.writeUTF("success");  // Notify the client that the upload was successful
                    } else {
                        System.out.println("\nUpload interrupted at " + totalBytesRead + " bytes.");
                        dos.writeUTF("interrupted");  // Notify the client that the upload was interrupted
                    }
    
                } catch (IOException e) {
                    System.err.println("Error handling client request: " + e.getMessage());
                    // Handle the broken pipe exception gracefully
                    if (e.getMessage().equals("Broken pipe")) {
                        // Log or perform any clean-up tasks needed
                        System.out.println("Client disconnected abruptly");
                    }
                }
            }
            // Handle file download
            else if (requestType.equalsIgnoreCase("download")) {
                String filePath = dis.readUTF();  // Read the file path requested by the client
                long existingFileSize = dis.readLong();  // Number of bytes the client has already downloaded
    
                File file = new File(filePath);
                if (!file.exists()) {
                    dos.writeUTF("error");
                    System.out.println("File not found on the server.");
                    return;
                }
    
                long fileSize = file.length();
    
                // If the file has already been fully downloaded
                if (existingFileSize >= fileSize) {
                    dos.writeUTF("alreadyDownloaded");
                    System.out.println("Client already has the full file.");
                    return;  // Stop further processing
                }
    
                // If resuming, send "resume", otherwise send "new"
                if (existingFileSize > 0) {
                    dos.writeUTF("resume");
                    System.out.println("Resuming file download from byte " + existingFileSize);
                } else {
                    dos.writeUTF("new");
                    System.out.println("Starting new file download.");
                }
    
                dos.writeLong(fileSize);  // Send the total file size to the client
    
                // Send the remaining part of the file
                try (FileInputStream fis = new FileInputStream(file)) {
                    fis.skip(existingFileSize);  // Skip already downloaded part of the file
                    byte[] buffer = new byte[4096];
                    int bytesRead;
    
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, bytesRead);
                    }
    
                    System.out.println("File download completed.");
                }
    
            }

    
            // Handle listing directories
            else if (requestType.equalsIgnoreCase("listDirectory")) {
                String serverDirectoryPath = dis.readUTF();  // Read the directory path
                File directory = new File(serverDirectoryPath);
    
                if (!directory.exists() || !directory.isDirectory()) {
                    dos.writeUTF("error");  // Directory doesn't exist
                    dos.writeUTF("Error: Directory '" + serverDirectoryPath + "' not found.");
                } else {
                    dos.writeUTF("success");  // Directory found
                    String treeStructure = getDirectoryTree(directory, 0);  // Get the directory tree structure
                    dos.writeUTF(treeStructure);  // Send the directory tree
                }
            }
            else if (requestType.equalsIgnoreCase("shutdown")) {
                System.out.println("Shutdown request received.");  // Debugging statement
                dos.writeUTF("success");  // Send acknowledgment back to the client
                clientSocket.close();  // Close the client socket
                shutdownServer();  // Call method to shut down the server
            }
    
        } catch (IOException e) {
            if (e.getMessage().equals("Broken pipe")) {
                // Log or perform any clean-up tasks needed
                System.out.println("Client disconnected abruptly");
            } else {
            System.err.println("Error handling client request: " + e.getMessage());
            }
        }
    }
    
    
    // Helper method to generate a tree-like structure for the directory
    private static String getDirectoryTree(File directory, int depth) {
        StringBuilder tree = new StringBuilder();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                // Add indentation based on depth
                for (int i = 0; i < depth; i++) {
                    tree.append("  ");  // Add two spaces for each depth level
                }
    
                if (file.isDirectory()) {
                    tree.append("|-- ").append(file.getName()).append("/\n");  // Mark directories
                    tree.append(getDirectoryTree(file, depth + 1));  // Recursively list subdirectories
                } else {
                    tree.append("|-- ").append(file.getName()).append("\n");  // List files
                }
            }
        }
        return tree.toString();
    }
    
    
    // // Handle client requests for upload/download and directory operations
    // public  static void handleClientRequest(Socket clientSocket) {
    //     try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
    //          DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

    //         String requestType = in.readUTF(); // Read request type

    //         switch (requestType.toLowerCase()) {
    //             case "upload":
    //                 receiveFile(in, out);
    //                 break;
    //             case "download":
    //                 handleDownload(in, out);
    //                 break;
    //             case "listdir":
    //                 handleListDirectory(in, out);
    //                 break;
    //             case "createdir":
    //                 handleCreateDirectory(in, out);
    //                 break;
    //             case "removedir":
    //                 handleRemoveDirectory(in, out);
    //                 break;
    //             case "removefile":
    //                 handleRemoveFile(in, out);
    //                 break;
    //             case "shutdown":
    //                 System.out.println("Shutting down server...");
    //                 System.exit(0);
    //                 break;
    //             default:
    //                 System.out.println("Unknown request: " + requestType);
    //         }
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }
    private static void receiveFile(DataInputStream dis, DataOutputStream dos) throws IOException {
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();
        File file = new File(fileName);


        long alreadyReceived = file.exists() ? file.length() : 0;
        dos.writeLong(alreadyReceived);  // Tell client how much was already received

        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalReceived = alreadyReceived;

            while ((bytesRead = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, bytesRead);
                totalReceived += bytesRead;
            //    displayProgress(totalReceived, fileSize);  // Display progress bar
            }

            System.out.println("\nFile " + fileName + " uploaded successfully.");
        } 
    }

    // Handle upload
    private static void handleUpload(DataInputStream in, DataOutputStream out) throws IOException {
        String targetPath = in.readUTF();
        long fileSize = in.readLong();

        try (FileOutputStream fileOut = new FileOutputStream(targetPath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while (fileSize > 0 && (bytesRead = in.read(buffer, 0, Math.min(buffer.length, (int) fileSize))) != -1) {
                fileOut.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }
        }
        System.out.println("File uploaded and saved at " + targetPath);
    }

    // Handle download
    private static void handleDownload(DataInputStream in, DataOutputStream out) throws IOException {
        String filePath = in.readUTF(); // Path of the file on the server
        File file = new File(filePath);

        if (file.exists()) {
            out.writeUTF("OK");
            out.writeLong(file.length());

            try (FileInputStream fileIn = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("File sent to client: " + filePath);
        } else {
            out.writeUTF("ERROR");
            System.out.println("File not found: " + filePath);
        }
    }

    // Handle list directory contents
    private static void handleListDirectory(DataInputStream in, DataOutputStream out) throws IOException {
        String dirPath = in.readUTF();
        File dir = new File(dirPath);

        if (dir.exists() && dir.isDirectory()) {
            String[] files = dir.list();
            out.writeUTF("OK");
            out.writeInt(files.length);
            for (String file : files) {
                out.writeUTF(file);
            }
        } else {
            out.writeUTF("ERROR");
            System.out.println("Directory not found: " + dirPath);
        }
    }

    // Handle create directory
    private static void handleCreateDirectory(DataInputStream in, DataOutputStream out) throws IOException {
        String dirPath = in.readUTF();
        File dir = new File(dirPath);

        if (dir.mkdir()) {
            out.writeUTF("OK");
            System.out.println("Directory created: " + dirPath);
        } else {
            out.writeUTF("ERROR");
            System.out.println("Failed to create directory: " + dirPath);
        }
    }

    // Handle remove directory
    private static void handleRemoveDirectory(DataInputStream in, DataOutputStream out) throws IOException {
        String dirPath = in.readUTF();
        File dir = new File(dirPath);

        if (dir.exists() && dir.isDirectory() && dir.delete()) {
            out.writeUTF("OK");
            System.out.println("Directory removed: " + dirPath);
        } else {
            out.writeUTF("ERROR");
            System.out.println("Failed to remove directory: " + dirPath);
        }
    }

    // Handle remove file
    private static void handleRemoveFile(DataInputStream in, DataOutputStream out) throws IOException {
        String filePath = in.readUTF();
        File file = new File(filePath);

        if (file.exists() && file.delete()) {
            out.writeUTF("OK");
            System.out.println("File removed: " + filePath);
        } else {
            out.writeUTF("ERROR");
            System.out.println("Failed to remove file: " + filePath);
        }
    }

    // Client method to upload a file to the server

    // Client method to upload a file to the server
    public static void uploadFile(String serverHost, int serverPort, String localFilePath, String remoteFilePath) throws IOException {
        try (Socket socket = new Socket(serverHost, serverPort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             FileInputStream fileIn = new FileInputStream(localFilePath)) {

            File file = new File(localFilePath);
            out.writeUTF("upload");  // Tell server it's an upload request
            out.writeUTF(remoteFilePath);
            out.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            System.out.println("File uploaded successfully.");
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
                byte[] buffer = new byte[4096];
                int bytesRead;
    
                // Download the file in chunks and write to the client file path
                while (totalBytesDownloaded < totalFileSize && (bytesRead = in.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    totalBytesDownloaded += bytesRead;
    
                    // Calculate and display the progress
                    int progressPercentage = (int) ((totalBytesDownloaded * 100) / totalFileSize);
                    displayProgress(progressPercentage);
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
    
    
}
