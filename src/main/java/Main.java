import java.io.File;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Set;

public class Main {
    private static final Set<String> BUILTIN_COMMANDS = Set.of("exit", "echo", "type","pwd");

    public static void main(String[] args) throws Exception{
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");

            if (!scanner.hasNextLine()) {
                break;
            }

            String commandLine = scanner.nextLine();

            if (commandLine == null || commandLine.isBlank()) {
                continue;
            }

            String[] parts = commandLine.split(" ", 2);
            String command = parts[0];

            if ("exit".equals(commandLine) || "exit".equals(command)) {
                break;
            } else if ("echo".equals(command)) {
                handleEcho(parts);
            } else if ("type".equals(command)) {
                handleType(parts);
            } else if ("pwd".equals(command)) {
                handlePwd();
            } else {
                if(findExecutable(command) !=null){
                    Process process = Runtime.getRuntime().exec(commandLine.split(" "));
                    process.getInputStream().transferTo(System.out);
                }else {
                    System.out.printf("%s: command not found%n", command);
                }
            }
        }

        scanner.close();
    }

    private static void handlePwd() {
        System.out.println(Paths.get("").toAbsolutePath());
    }

    private static void handleEcho(String[] parts) {
        if (parts.length < 2) {
            System.out.println();
        } else {
            System.out.println(parts[1]);
        }
    }

    private static void handleType(String[] parts) {
        if (parts.length < 2 || parts[1].isBlank()) {
            System.out.println("type: missing argument");
            return;
        }

        String target = parts[1];

        if (BUILTIN_COMMANDS.contains(target)) {
            System.out.printf("%s is a shell builtin%n", target);
            return;
        }

        String executablePath = findExecutable(target);
        if (executablePath != null) {
            System.out.printf("%s is %s%n", target, executablePath);
        } else {
            System.out.printf("%s: not found%n", target);
        }
    }

    private static String findExecutable(String command) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isBlank()) {
            return null;
        }

        String[] directories = pathEnv.split(File.pathSeparator);

        for (String dir : directories) {
            File file = new File(dir, command);
            if (file.exists() && file.isFile() && file.canExecute()) {
                return file.getAbsolutePath();
            }
        }

        return null;
    }
}