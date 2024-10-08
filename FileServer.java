import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private static final int PORT = 5001; // Define the port number
    private static final String BASE_DIRECTORY = "server_files"; // Base directory for the server

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");

                try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                     DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                    String action = dis.readUTF(); // Read action (upload/download/list)
                    
                    switch (action.toLowerCase()) {
                        case "upload":
                            receiveFile(dis, dos);  // Handle file upload
                            break;
                        case "download":
                            sendFile(dis, dos);  // Handle file download
                            break;
                        case "list":
                            listDirectory(dis, dos);  // Handle directory listing
                            break;
                        default:
                            dos.writeUTF("Unknown action");
                            break;
                    }
                }

                socket.close(); // Close the client connection
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to handle file upload
    private static void receiveFile(DataInputStream dis, DataOutputStream dos) throws IOException {
        String fileName = dis.readUTF();  // Read the file name
        File file = new File(BASE_DIRECTORY + "/" + fileName);

        // Create directories if they don't exist
        new File(BASE_DIRECTORY).mkdirs();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            // Read the file from client and save it on the server
            while ((bytesRead = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, bytesRead);
            }
            dos.writeUTF("File " + fileName + " received successfully.");
        }
    }

    // Method to handle file download
   // Method to handle file download
private static void sendFile(DataInputStream dis, DataOutputStream dos) throws IOException {
    String fileName = dis.readUTF();  // Read file name (or path) requested by client
    File file = new File(BASE_DIRECTORY, fileName);  // Use the relative path from 'server_files'

    if (file.exists() && file.isFile()) {
        dos.writeUTF("File found"); // Let client know file exists
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, bytesRead); // Send file content
            }
            System.out.println("File " + fileName + " sent to the client.");
        }
    } else {
        dos.writeUTF("File not found"); // Send error if file does not exist
        dos.writeInt(1); // Send non-zero error code
        System.err.println("File " + fileName + " does not exist.");
    }
}


    // Method to handle listing directory contents within the server_files directory
    private static void listDirectory(DataInputStream dis, DataOutputStream dos) throws IOException {
        String relativePath = dis.readUTF();  // Read the directory path requested by client
        File dir = new File(BASE_DIRECTORY, relativePath); // Ensure the base directory is 'server_files'

        if (dir.exists() && dir.isDirectory()) {
            dos.writeUTF("Directory found");

            // List the files and directories
            File[] files = dir.listFiles();
            if (files != null) {
                dos.writeInt(files.length);
                for (File file : files) {
                    dos.writeUTF(file.getName() + (file.isDirectory() ? " (dir)" : ""));
                }
            } else {
                dos.writeInt(0); // No files or directories found
            }
        } else {
            dos.writeUTF("Directory not found"); // Send error if directory does not exist
            dos.writeInt(1); // Send non-zero error code
            System.err.println("Directory " + dir.getPath() + " does not exist.");
        }
    }
}
