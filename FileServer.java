import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private static final int PORT = 5001; // Define the port number
    private static final String BASE_DIRECTORY = "server_files"; // Base directory for the server
    private static boolean isRunning = true;  // Control server loop
    private static ServerSocket serverSocket;  // Make this accessible for shutdown

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server is listening on port " + PORT);

            while (isRunning) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("New client connected");

                    // Handle each client connection in a separate thread
                    ClientHandler clientHandler = new ClientHandler(socket);
                    new Thread(clientHandler).start();  // Start the client handler in a new thread
                } catch (IOException e) {
                    if (!isRunning) {
                        System.out.println("Server has been shut down.");
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();  // Ensure the server socket is closed when shutting down
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Class to handle client connections in separate threads
    private static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                String action = dis.readUTF(); // Read action (upload/download/list/remove/shutdown)

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
                    case "remove":
                        removeFile(dis, dos);  // Handle file removal
                        break;
                    case "shutdown":
                        shutdownServer(dis, dos);  // Handle server shutdown
                        break;
                    default:
                        dos.writeUTF("Unknown action");
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close(); // Ensure the socket is closed after the client handler is done
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Method to handle server shutdown
        private void shutdownServer(DataInputStream dis, DataOutputStream dos) throws IOException {
            System.out.println("Shutdown request received from client.");
            dos.writeUTF("Server is shutting down...");
            isRunning = false;  // Stop the server's main loop
            try {
                serverSocket.close();  // Close the server socket immediately to stop accepting new clients
            } catch (IOException e) {
                System.err.println("Error closing server socket.");
            }
        }


        // Method to handle file upload (same as before)
        // Method to handle file upload
        private void receiveFile(DataInputStream dis, DataOutputStream dos) throws IOException {
            try {
                String fileName = dis.readUTF();  // Read the file name sent by client
                File file = new File(BASE_DIRECTORY + "/" + fileName);

                // Create directories if they don't exist
                new File(BASE_DIRECTORY).mkdirs();

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    // Read the file content from client and save it on the server
                    while ((bytesRead = dis.read(buffer)) > 0) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    dos.writeUTF("File " + fileName + " received successfully.");
                }
            } catch (EOFException e) {
                System.err.println("Client disconnected unexpectedly during file upload.");
            }
        }


        // Method to handle file download (same as before)
        private void sendFile(DataInputStream dis, DataOutputStream dos) throws IOException {
            String fileName = dis.readUTF();  // Read file name requested by client
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

        // Method to handle directory listing (same as before)
        private void listDirectory(DataInputStream dis, DataOutputStream dos) throws IOException {
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

        // Method to handle file removal (same as before)
        private void removeFile(DataInputStream dis, DataOutputStream dos) throws IOException {
            String fileName = dis.readUTF();  // Read file name (or path) requested by client
            File file = new File(BASE_DIRECTORY, fileName);  // Ensure it's in 'server_files'

            if (file.exists() && file.isFile()) {
                if (file.delete()) {
                    dos.writeUTF("File deleted successfully.");  // File was successfully deleted
                    System.out.println("File " + fileName + " deleted from server.");
                } else {
                    dos.writeUTF("File could not be deleted.");  // Failed to delete the file
                    dos.writeInt(1);  // Send non-zero error code
                    System.err.println("Failed to delete file " + fileName);
                }
            } else {
                dos.writeUTF("File not found");  // File does not exist
                dos.writeInt(1);  // Send non-zero error code
                System.err.println("File " + fileName + " does not exist.");
            }
        }
    }
}
