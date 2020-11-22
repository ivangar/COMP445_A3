import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdValidation {
    public boolean has_file_data;
    public boolean has_inline_data;
    private int count_v;
    private int count_d ;
    private int count_f;
    public List<String> requestHeaders ;

    public CmdValidation(String[] args){
        this.has_inline_data = Arrays.asList(args).contains("-d");
        this.has_file_data = Arrays.asList(args).contains("-f");
        this.count_v = count(args, "-v");
        this.count_d = count(args, "-d");
        this.count_f = count(args, "-f");
        this.requestHeaders = new ArrayList<String>();
    }

    public void wrong_cmd(String[] args){

        // print httpc help
        if(!(args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("get") || args[0].equalsIgnoreCase("post"))){
            help_msg();
            System.exit(0);
        }
        // print httpc help get
        if(args[0].equalsIgnoreCase("get")){
            if((has_file_data || has_inline_data) || count_v > 1){
                help_get();
                System.exit(0);
            }
        }
        // print httpc help post
        if(args[0].equalsIgnoreCase("post")){
            if((has_file_data && has_inline_data) || count_v > 1 || count_d > 1 || count_f > 1){
                help_post();
                System.exit(0);
            }
        }

    }

    public void help_msg(){
        System.out.println("httpc is a curl-like application but supports HTTP protocol only.");
        System.out.println("Usage:");
        System.out.println("    httpc command [arguments]");
        System.out.println("The commands are:");
        System.out.println("    get     executes a HTTP GET request and prints the response.");
        System.out.println("    post    executes a HTTP POST request and prints the response.");
        System.out.println("    help    prints this screen.");
        System.out.println("Use \"httpc help [command]\" for more information about a command.");
    }
    public void help_get(){
        System.out.println("usage: httpc get [-v] [-h key:value] URL");
        System.out.println("Get executes a HTTP GET request for a given URL.");
        System.out.println("    -v             Prints the detail of the response such as protocol, status, and headers.");
        System.out.println("    -h key:value   Associates headers to HTTP Request with the format 'key:value'.");
    }
    public void help_post(){
        System.out.println("usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL");
        System.out.println("Post executes a HTTP POST request for a given URL with inline data or from file.");
        System.out.println("    -v             Prints the detail of the response such as protocol, status, and headers.");
        System.out.println("    -h key:value   Associates headers to HTTP Request with the format 'key:value'.");
        System.out.println("    -d string      Associates an inline data to the body HTTP POST request.");
        System.out.println("    -f file        Associates the content of a file to the body HTTP POST request.");
        System.out.println("Either [-d] or [-f] can be used but not both.");
    }

    // Count how many how many -v/-h/-d are in the command.
    private int count(String[] args, String command){

        int total = 0;

        for(String arg: args){
            if(arg.equalsIgnoreCase(command)) {
                total++;
            }
        }

        return total;
    }

    public boolean validateHeaders(String header){

        if(!header.contains(":"))
            return false;

        String[] keyvalues = header.split(":");

        if(keyvalues.length != 2)
            return false;

        if(keyvalues.length == 2)
            if(keyvalues[0].isEmpty() || keyvalues[1].isEmpty())
                return false;

        return true;
    }

}
