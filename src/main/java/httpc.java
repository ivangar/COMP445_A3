public class httpc {

    private boolean has_file_data = false;
    private boolean has_inline_data = false;

    public static void main(String[] args) {

        if (args.length > 0)
            new httpc().initApp(args);
        else
            System.out.println("Please enter a command or type help to see the documentation");
    }

    private void initApp(String[] args){

        CmdValidation cmd_validation = new CmdValidation(args);
        has_inline_data = cmd_validation.has_inline_data;
        has_file_data = cmd_validation.has_file_data;

        String first_arg = args[0];

        // Validate the command
        cmd_validation.wrong_cmd(args);

        if(first_arg.equalsIgnoreCase("get") || first_arg.equalsIgnoreCase("post")) {
            httpRequest request = new httpRequest(args, cmd_validation);
        }

        else if(first_arg.equalsIgnoreCase("help")){
            // httpc help
            if(args.length == 1){
                cmd_validation.help_msg();
            }
            // httpc help get
            else if(args[1].equalsIgnoreCase("get")) {
                cmd_validation.help_get();
            }
            // httpc help post
            else if(args[1].equalsIgnoreCase("post")){
                cmd_validation.help_post();
            }
            System.exit(0);
        }


    }



}
