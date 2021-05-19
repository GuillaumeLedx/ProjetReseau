import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Server {
	private static String optionPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
	private static String folder;
	
    public static void main( String[] args ) throws Exception {
    	File options = new File(optionPath+"options.txt");
		
		// fichier options pour le path du site voulu
		// Créer le fichier options s'il n'existe pas
		// valeur par defaut du chemin : le disque C
		
		if(options.createNewFile()) {
			PrintWriter writer = new PrintWriter(options, "UTF-8");
			writer.print("DossierSiteWeb=");
			try ( Scanner scanner = new Scanner( System.in ) ) {
	            
	            System.out.print( "Entrez le chemin du site: " );
	            String a = scanner.nextLine();
	            writer.println(a);
	            writer.close();
	        }catch(Exception e) {
	        	System.out.println(e.getMessage());
	        }
			
			
		}
		
		BufferedReader in = new BufferedReader(new FileReader(options));
		String line;
		String[] option;
		while ((line = in.readLine()) != null)
		{
			  option = line.split("=");
			  folder = option[1];
		}
		in.close();
    	
		System.out.println("Serveur lancé, en attente du chargement de la page");
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            while (true) {
                try (Socket client = serverSocket.accept()) {
                    handleClient(client, folder);
                }
            }
        }
    }

    private static void handleClient(Socket client, String folder) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));

        StringBuilder requestBuilder = new StringBuilder();
        String line;
        
        while (!(line = br.readLine()).isBlank()) {
            requestBuilder.append(line + "\r\n");
        }

        String request = requestBuilder.toString();
        String[] requestsLines = request.split("\r\n");
        String[] requestLine = requestsLines[0].split(" ");
        String method = requestLine[0];
        String path = requestLine[1];
        String version = requestLine[2];
        String host = requestsLines[1].split(" ")[1];

        List<String> headers = new ArrayList<>();
        for (int h = 2; h < requestsLines.length; h++) {
            String header = requestsLines[h];
            headers.add(header);
        }

        //String accessLog = String.format("Client %s, method %s, path %s, version %s, host %s, headers %s", client.toString(), method, path, version, host, headers.toString());
        //System.out.println(accessLog);


        Path filePath = getFilePath(path, folder);

        if (Files.exists(filePath)) {
            // file exist
            String contentType = guessContentType(filePath);
            sendResponse(client, "200 OK", contentType, Files.readAllBytes(filePath));
        } else {
            // 404
            byte[] notFoundContent = "<h1>Not found :(</h1>".getBytes();
            sendResponse(client, "404 Not Found", "text/html", notFoundContent);
        }

    }

    private static void sendResponse(Socket client, String status, String contentType, byte[] content) throws IOException {
        OutputStream clientOutput = client.getOutputStream();
        clientOutput.write(("HTTP/1.1 \r\n" + status).getBytes());
        clientOutput.write(("ContentType: " + contentType + "\r\n").getBytes());
        clientOutput.write("\r\n".getBytes());
        clientOutput.write(content);
        clientOutput.write("\r\n\r\n".getBytes());
        clientOutput.flush();
        client.close();
    }

    private static Path getFilePath(String path, String folder) {
        if ("/".equals(path)) {
            path = "/index.html";
        }
        
        return Paths.get(folder, path);
    }

    private static String guessContentType(Path filePath) throws IOException {
        return Files.probeContentType(filePath);
    }

}