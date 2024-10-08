import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class FileClient {

    private static final String SERVER_HOST = "localhost"; // Replace with server IP if remote
    private static final int SERVER_PORT = 5001;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Enter 'upload' to upload a file or 'download' to download a file:");
        String action = scanner.nextLine();

        if (action.equalsIgnoreCase("upload")) {
            uploadFile();
        } else if (action.equalsIgnoreCase("download")) {
            downloadFile();
        } else {
            System.out.println("Unknown action.");
        }

        scanner.close();
    }

    // Method to handle file upload
    private static void uploadFile() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the file path to upload:");
        String filePath = scanner.nextLine();
        scanner.close();

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(filePath)) {

            // Send upload request
            dos.writeUTF("upload");

            File file = new File(filePath);
            String fileName = file.getName();

            dos.writeUTF(fileName); // Send file name to the server

            byte[] buffer = new byte[4096];
            int bytesRead;

            // Read the file and send it to the server
            while ((bytesRead = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, bytesRead);
            }

            System.out.println("File " + fileName + " uploaded successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to handle file download
    private static void downloadFile() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the file name to download:");
        String fileName = scanner.nextLine();
        scanner.close();

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            // Send download request
            dos.writeUTF("download");
            dos.writeUTF(fileName); // Send file name to the server

            String serverResponse = dis.readUTF();  // Check if file exists
            if ("File found".equals(serverResponse)) {
                try (FileOutputStream fos = new FileOutputStream("client_files/" + fileName)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    // Receive the file from the server and save it locally
                    while ((bytesRead = dis.read(buffer)) > 0) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    System.out.println("File " + fileName + " downloaded successfully.");
                }
            } else {
                int errorCode = dis.readInt();  // Get error code
                System.err.println("Error: File not found. Error code: " + errorCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
