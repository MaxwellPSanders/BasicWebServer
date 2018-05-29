import java.io.* ;
import java.net.* ;
import java.util.* ;

public class Server {
	public static void main(String argv[]) throws Exception{
		//setting the port number
		int port = 46209;
		
		//open up the port
		ServerSocket serve = new ServerSocket(port);
		
		//continually look for HTTPreqs
		while(true) {
			//try to connect to clients
			Socket connect = null;
			
			try {
				connect = serve.accept();
			
				//display that a connection was made
				System.out.println("Connection was made " + connect);
				
				//make an HTTP request that can be taken care of by a thread
				HTTPreq request = new HTTPreq(connect);
				
				//put that HTTP request in a thread
				Thread client = new Thread(request);
				
				//start that thread
				client.start();
			}
			catch(Exception e) {
				e.printStackTrace();
				connect.close();
			}
			
		}
	}
}

//the class that will handle the HTTPrequests
class HTTPreq implements Runnable{
	static String term = "\r\n";
	Socket connect;
	
	//constructor
	public HTTPreq(Socket socket) throws Exception{
		this.connect = socket;
	}
	
	//implement run as we said we would when we made the class
	public void run() {
		//try to process the request otherwise throw it
		try {
			process();
		} 
		catch(Exception e) {
			System.out.println(e);
		}
	}
	
	//have a method to process requests
	private void process() throws Exception{
		//do input and output streams
		DataInputStream input = new DataInputStream(connect.getInputStream());
		DataOutputStream output = new DataOutputStream(connect.getOutputStream());
		
		//wrap input stream into a convenient reader
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		
		//get the request line
		String request = reader.readLine();
		
		//show it
		System.out.println();
		System.out.println(request);
	
		//show the header lines
		String header;
		while((header = reader.readLine()).length() != 0) {
			System.out.println(header);
		}
	
		//scan the request line
		Scanner token = new Scanner(request);
		token.next();
		String fileName = token.next();
		//current directory
		fileName = "." + fileName;
		
		System.out.println(fileName);
		
		//try to open the file
		FileInputStream file = null;
		boolean fileFound = true;
		try {
			file = new FileInputStream(fileName);
			System.out.println("found " + fileName);
		} 
		catch (FileNotFoundException e) {
			System.out.println("Could not find file");
			fileFound = false;
		}
		
		//if the file was not found then check the redirectory
		boolean redirected = false;
		String location = null;
		if(!fileFound) {
			Scanner red = new Scanner( new File("redirect.txt"));
			//go through all the entries
			while(red.hasNextLine()) {
				//check the first column for the fileName if it has it set location
				//to the value of the next column
				if(red.next().equals(fileName)) {
					location = red.next();
					fileName = location;
					//check to see if the redirected file exists
					try {
						file = new FileInputStream(fileName);
						redirected = true;
						System.out.println("found " + fileName);
					} 
					catch (FileNotFoundException e) {
						System.out.println("Could not find file");
					}
				}
			}
		}
		
		//making the response if the file is found
		String status, content, body = null;
		if(fileFound) {
			status = "HTTP/1.1 200" + term;
			content = "Content-type: "  + contentType(fileName) + term;
		}
		//making the response if the file is redirected
		else if(redirected) {
			status = "HTTP/1.1 301" + term;
			content = "Content-type: "  + contentType(fileName) + term;
		}
		//making the response if the file is not found
		else {
			status = "HTTP/1.1 404" + term;
			content = "text/html" + term; 
			body = "<HTML><HEAD><TITLE>FILE NOT FOUND</TITLE></HEAD>"
					+ "<BODY>404 CAN'T FIND THE FILE ¯\\_(-_-)_/¯</BODY></HTML>";
		}
		
		//send status, content, and end header
		output.writeBytes(status);
		output.writeBytes(content);
		output.writeBytes(term);
		
		//send the body
		if (fileFound) {
			byte[] buffer = new byte[1024];
			int bytes = 0;
			
			//copy the file into the output
			while((bytes = file.read(buffer)) != -1) {
				output.write(buffer, 0, bytes);
			}
			file.close();
		}
		else {
			output.writeBytes(body);
		}
		
		//close everything
		token.close();
		output.close();
		reader.close();
		connect.close();
	}
	
	//returns the content type of a given file
	private static String contentType(String fileName) {
		if(fileName.endsWith(".htm") || fileName.endsWith(".html")) {
			return "text/html";
		}
		if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
			return "image/jpeg";
		}
		if(fileName.endsWith(".gif")) {
			return "image/gif";
		}
		if(fileName.endsWith(".png")) {
			return "image/png";
		}
		return "application/octet-stream";
	}
}