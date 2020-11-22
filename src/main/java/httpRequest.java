import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class httpRequest {

    String[] args;
    private String host = "";
    private int portNumber;
    private String requestMethod;
    private URL request_url;
    private Socket socket;
    private PrintWriter writer;
    public BufferedReader reader;
    private String request_URI;
    private boolean is_verbose = false;
    private boolean has_headers = false;
    private boolean has_file_data = false;
    private boolean has_inline_data = false;
    private List<String> requestHeaders = new ArrayList<String>();
    public CmdValidation cmd_validation;
    public httpResponse response;

    public httpRequest(String[] args, CmdValidation cmd_validation){
        this.cmd_validation = cmd_validation;
        setArgs(args);
        sendRequest();
    }

    private void setArgs(String[] args){
        this.args = args;
        this.requestMethod = this.args[0];
        this.is_verbose = Arrays.asList(this.args).contains("-v");
        this.has_headers = Arrays.asList(this.args).contains("-h");
        this.has_inline_data = Arrays.asList(this.args).contains("-d");
        this.has_file_data = Arrays.asList(this.args).contains("-f");
        addRequestHeaders();
    }

    private void sendRequest(){

        try {
            connectSocket();

            if(this.requestMethod.equalsIgnoreCase("get"))
                get_request();
            else if(this.requestMethod.equalsIgnoreCase("post"))
                post_request();

            disconnectSocket();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void connectSocket() throws IOException{
        try{
            this.request_url = new URL(this.args[this.args.length-1]);  //I checked in curl documentation, the url is always the last argument
            this.host = this.request_url.getHost();
            this.portNumber = (this.request_url.getPort() == -1) ? this.request_url.getDefaultPort() : this.request_url.getPort(); //If the port number is not specified it returns -1
            this.socket = new Socket(this.host, this.portNumber);
            this.writer = new PrintWriter(socket.getOutputStream(), true);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.request_URI = request_url.getFile();  //gets the path + query if there is one
            this.response = new httpResponse(this.args);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void disconnectSocket() throws IOException{
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void get_request() throws IOException{


        this.writer.println("GET " + this.request_URI + " HTTP/1.0");
        this.writer.println("Host: " + this.host);
        this.writer.println("Connection: keep-alive");  //important to close the connection with server after receiving the response

        //Check for all the headers passed from console
        if(this.has_headers)
            printRequestHeaders();
        else {
            this.writer.println("User-Agent:COMP445");
            this.writer.println("Accept-Language:en-US");
        }

        this.writer.println();

        this.response.printHttpResponse(reader);

    }

    private void post_request() throws IOException{

        String data = getData();
        this.writer.println("POST " + request_URI + " HTTP/1.0");
        this.writer.println("Host: " + this.host);
        this.writer.println("Content-Length: " + data.length());
        this.writer.println("Connection: close");  //important to close the connection with server after receiving the response

        //Check for all the headers passed from console
        if(this.has_headers)
            printRequestHeaders();
        else {
            this.writer.println("User-Agent:COMP445");
            this.writer.println("Accept-Language:en-US");
        }

        this.writer.println();

        //print data from the command
        if(this.has_inline_data || this.has_file_data){
            this.writer.println(data);
            this.writer.println();
        }

        //Print response from Server
        this.response.printHttpResponse(reader);

    }

    private String getData(){

        boolean data_line = false;
        String data = "";

        // Only one data
        for (String arg : this.args) {
            if(arg.equalsIgnoreCase("-d") || arg.equalsIgnoreCase("-f")){
                data_line = true;
                continue;
            }
            if(data_line){
                data = arg;
                break;
            }
        }

        // If data is the path of a file, then read the file.
        if(this.has_file_data){
            BufferedReader readFile;
            try{
                readFile = new BufferedReader(new FileReader(data));
                String line = readFile.readLine();
                if(line == null){
                    data ="";
                }else{
                    data = line;
                }

                while(line != null){
                    line = readFile.readLine();
                    if(line != null){
                        data = data + "\n" + line;
                    }
                }
                readFile.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return data;
    }

    private void addRequestHeaders(){

        boolean header_line = false;

        for (String arg : this.args) {
            //check it's -h and we don't have two or more -h consecutive
            if(arg.equalsIgnoreCase("-h") && !header_line){
                header_line = true;
                continue;
            }

            if(header_line){
                if(!cmd_validation.validateHeaders(arg))
                {
                    System.out.println("\nERROR \nInvalid httpc Request header syntax, please check the documentation\n\n");
                    cmd_validation.help_msg();
                    System.exit(0);
                }

                //adding header to array list
                this.requestHeaders.add(arg);
                header_line = false;
            }
        }
    }

    private void printRequestHeaders(){

        for (String header : this.requestHeaders) {
            this.writer.println(header);
        }
    }

}
