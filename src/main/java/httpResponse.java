import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;

public class httpResponse {
    private boolean is_verbose;
    private int responseStatusCode;
    private String responseStatus;

    public httpResponse(String[] args){
        this.is_verbose = Arrays.asList(args).contains("-v");
    }

    public void printHttpResponse(BufferedReader reader) throws IOException {

        String responseLine; //response from server
        boolean response_content = false;  //body from response

        System.out.println("\n---------------------- Http Server Response ----------------------------\n");

        try{
            while ((responseLine = reader.readLine()) != null) {

                if(responseLine.startsWith("HTTP")){
                    this.responseStatusCode = Integer.parseInt(responseLine.split("\\s+")[1]);
                    this.responseStatus = responseLine.substring(responseLine.indexOf(Integer.toString(this.responseStatusCode))+4);
                    checkResponseError();
                }

                if(responseLine.isEmpty())  //empty line separates the content from the headers
                    response_content = true;

                if(!this.is_verbose && response_content)
                    System.out.println(responseLine);
                else if(this.is_verbose)
                    System.out.println(responseLine);

            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void checkResponseError(){
        if(this.responseStatusCode >= 400 && this.responseStatusCode < 500)
            System.out.println("\nClient side error\nStatus Code: " + this.responseStatusCode + "\nStatus: " + this.responseStatus + "\n");
        else if(this.responseStatusCode > 500)
            System.out.println("\nServer side error\nStatus Code: " + this.responseStatusCode + "\nStatus: " + this.responseStatus + "\n");
    }
}
