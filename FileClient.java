import java.io.*;
import java.net.Socket;

public class FileClient {

    private static final String SERVER_HOST = "localhost"; // Replace with server IP if remote
    private static final int SERVER_PORT = 5001;

    public static void main(String[] args) {
        // Specify the full path of the file to be uploaded
        String filePath = "client_files/sample.txt";  // Update this with the actual file path

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            System.out.println("Connected to the server");

            // Automatically extract the file name from the file path
            File file = new File(filePath);
            String fileName = file.getName(); // Extracting the file name

            // Send file name and file content to the server
            try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 FileInputStream fis = new FileInputStream(file)) {

                dos.writeUTF(fileName); // Send file name to the server

                byte[] buffer = new byte[4096];
                int bytesRead;

                // Read the file and send it to the server
                while ((bytesRead = fis.read(buffer)) > 0) {
                    dos.write(buffer, 0, bytesRead);
                }

                System.out.println("File " + fileName + " uploaded successfully.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
