import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // TODO: Uncomment the code below to pass the first stage
        // 使用 List 接口接收，更符合面向对象设计规范
        final HashSet<String> BUILDINCOMMAND = new HashSet<>(List.of("exit", "echo", "type"));
        while (true) {
            String command = null;
            System.out.print("$ ");
            Scanner scanner = new Scanner(System.in);
            command = scanner.nextLine();
            String path_commands = System.getenv("PATH");
            String[] path_command = path_commands.split(":");
            Boolean sign = false;
            if(command!= null){
                String[] commandsplit = command.split(" ",2);
                if(command.equals("exit")){
                    break;
                }
                else if(commandsplit[0].equals("echo")){
                    System.out.println(command.substring(5));
                }
                else if(commandsplit[0].equals("type")){
                    if(BUILDINCOMMAND.contains(commandsplit[1])){
                        System.out.printf("%s is a shell builtin\n",commandsplit[1]);
                    }
                    else {
                        for(String p:path_command){
                            File file = new File(p,commandsplit[1]);
                            if(file.exists() && file.canExecute()){
                                System.out.println(commandsplit[1]+" is "+p+commandsplit[1]);
                                sign = true;
                            }
                        }
                        if(!sign) {
                            System.out.printf("%s: not found\n", commandsplit[1]);
                        }
                    }
                }
                else {
                    System.out.printf("%s: command not found\n", command);
                }
            }
        }


    }
}
