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

                // Handling file reception
                try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
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
                        System.out.println("File " + fileName + " received successfully.");
                    }
                }

                socket.close(); // Close the client connection
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
