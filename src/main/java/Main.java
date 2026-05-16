import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // TODO: Uncomment the code below to pass the first stage
        while (true) {
            String command = null;
            System.out.print("$ ");
            Scanner scanner = new Scanner(System.in);
            command = scanner.nextLine();
            if(command!= null){
                System.out.printf("%s: command not found\n",command);
            }
        }


    }
}
