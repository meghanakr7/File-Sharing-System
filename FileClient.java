import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class FileClient {

    private static final String SERVER_HOST = "localhost"; // Replace with server IP if remote
    private static final int SERVER_PORT = 5001;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String action = "";

        while (!action.equalsIgnoreCase("exit")) {
            System.out.println("\n--- File Sharing System Menu ---");
            System.out.println("1. Upload a file");
            System.out.println("2. Download a file");
            System.out.println("3. List directory contents");
            System.out.println("4. Exit");
            System.out.print("Enter your choice (1-4): ");
            action = scanner.nextLine();

            switch (action) {
                case "1":
                    uploadFile(scanner);
                    break;
                case "2":
                    downloadFile(scanner);
                    break;
                case "3":
                    listDirectory(scanner);
                    break;
                case "4":
                    System.out.println("Exiting...");
                    action = "exit";
                    break;
                default:
                    System.out.println("Invalid choice, please try again.");
            }
        }

        scanner.close();
    }

    // Method to handle file upload
    private static void uploadFile(Scanner scanner) {
        System.out.print("Enter the file path to upload: ");
        String filePath = scanner.nextLine();

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
private static void downloadFile(Scanner scanner) {
    System.out.print("Enter the relative file path to download (within 'server_files', e.g., 'subdir/file.txt'): ");
    String fileName = scanner.nextLine();

    try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
         DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
         DataInputStream dis = new DataInputStream(socket.getInputStream())) {

        // Send download request
        dos.writeUTF("download");
        dos.writeUTF(fileName); // Send the relative file path to the server

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

    // Method to handle directory listing (within 'server_files' only)
    private static void listDirectory(Scanner scanner) {
        System.out.print("Enter the relative path in 'server_files' to list (e.g., '/'): ");
        String dirPath = scanner.nextLine();

        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            // Send list directory request
            dos.writeUTF("list");
            dos.writeUTF(dirPath); // Send relative directory path to the server

            String serverResponse = dis.readUTF();  // Check if directory exists
            if ("Directory found".equals(serverResponse)) {
                int fileCount = dis.readInt();  // Number of files/directories in the directory
                System.out.println("Directory contents:");

                for (int i = 0; i < fileCount; i++) {
                    System.out.println(dis.readUTF());  // List the file/directory names
                }
            } else {
                int errorCode = dis.readInt();  // Get error code
                System.err.println("Error: Directory not found. Error code: " + errorCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
