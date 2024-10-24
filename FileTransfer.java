import java.io.*;
import java.net.*;

public class FileTransfer {

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equalsIgnoreCase("server") && args[1].equalsIgnoreCase("start")) {
            // Start the server
            int portNumber = Integer.parseInt(args[2]);  // Parse the port number from args[2]
            startServer(portNumber);
        } else if (args.length > 0 && args[0].equalsIgnoreCase("client")) {
            // Run the client
            String serverAddress = System.getenv("PA1_SERVER");
            if (serverAddress == null || !serverAddress.contains(":")) {
                System.out.println("PA1_SERVER environment variable not set correctly.");
                return;
            }
            String[] serverInfo = serverAddress.split(":");
            String serverHost = serverInfo[0];
            int serverPort = Integer.parseInt(serverInfo[1]);
    
            if (args[1].equalsIgnoreCase("upload")) {
                uploadFile(serverHost, serverPort, args[2], args[3]);
            } else {
                System.out.println("Invalid client command.");
            }
        } else {
            System.out.println("Usage:");
            System.out.println("  java -cp pa1.jar server start <portnumber>");
            System.out.println("  java -cp pa1.jar client upload <path_on_client> </path/filename/on/server>");
        }
    }
    
    // Server method to start and listen on a given port
    public static void startServer(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            handleClientRequest(clientSocket);
        }
    }

    // Handle client file upload request
    public static void handleClientRequest(Socket clientSocket) {
        try (BufferedInputStream in = new BufferedInputStream(clientSocket.getInputStream());
             DataInputStream dataInputStream = new DataInputStream(in)) {

            String targetPath = dataInputStream.readUTF();
            long fileSize = dataInputStream.readLong();

            try (FileOutputStream fileOut = new FileOutputStream(targetPath)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while (fileSize > 0 && (bytesRead = in.read(buffer, 0, Math.min(buffer.length, (int) fileSize))) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                    fileSize -= bytesRead;
                }
            }
            System.out.println("File received and saved at " + targetPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Client method to upload a file to the server
    public static void uploadFile(String serverHost, int serverPort, String localFilePath, String remoteFilePath) throws IOException {
        try (Socket socket = new Socket(serverHost, serverPort);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             FileInputStream fileIn = new FileInputStream(localFilePath)) {

            File file = new File(localFilePath);
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
}
