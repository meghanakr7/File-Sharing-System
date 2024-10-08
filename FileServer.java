import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private static final int PORT = 5001; // Define the port number

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");

                try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                     DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                    String action = dis.readUTF(); // Read action (upload/download)
                    
                    if (action.equalsIgnoreCase("upload")) {
                        receiveFile(dis, dos);  // Handle file upload
                    } else if (action.equalsIgnoreCase("download")) {
                        sendFile(dis, dos);  // Handle file download
                    } else {
                        dos.writeUTF("Unknown action");
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
        File file = new File("server_files/" + fileName);

        // Create directories if they don't exist
        new File("server_files").mkdirs();

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
    private static void sendFile(DataInputStream dis, DataOutputStream dos) throws IOException {
        String fileName = dis.readUTF();  // Read file name requested by client
        File file = new File("server_files/" + fileName);

        if (file.exists()) {
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
}
