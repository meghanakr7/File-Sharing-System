
import java.io.*;
import java.net.*;

public class server {
    private volatile static boolean isRunning = true;  // Server running flag
    public static String SERVER_HOST = "localhost"; // Replace with server IP if remote
    public static final int SERVER_PORT = 8000;

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
        }
        
    }
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
   
   
    
}
