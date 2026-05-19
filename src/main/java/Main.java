import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Main {
    private static final Set<String> BUILTIN_COMMANDS = Set.of("exit", "echo", "type","pwd","cd");

    public static void main(String[] args) throws Exception{
        Scanner scanner = new Scanner(System.in);
        Path cwd = Paths.get("").toAbsolutePath();
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
            List<String> Args = handleQuetes(parts[1]);
            String command = parts[0];

            if ("exit".equals(commandLine) || "exit".equals(command)) {
                break;
            } else if ("echo".equals(command)) {
                handleEcho(Args);
            } else if ("type".equals(command)) {
                handleType(parts);
            } else if ("pwd".equals(command)) {
                System.out.println(cwd);
            } else if ("cd".equals(command)){
                cwd = handleCd(cwd,parts);
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

    private static List<String> handleQuetes(String part) {
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean tokenStarted = false;
        for(char c:part.toCharArray()){
            if(inSingleQuote){
                if(c == '\''){
                    inSingleQuote = false;
                }else{
                    current.append(c);
                }
            }else{
                if(c =='\''){
                    inSingleQuote = true;
                    tokenStarted = true;
                } else if (Character.isWhitespace(c)) {
                    args.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }else {
                    current.append(c);
                    tokenStarted = true;
                }
            }
        }
        if (tokenStarted) {
            args.add(current.toString());
        }
        return args;
    }

    private static Path handleCd(Path cwd, String[] parts) {
        if(parts.length<2) return cwd;
        Path targetPath = Paths.get(parts[1]);
        if("~".equals(parts[1])){
            return getCurrentDirectory(cwd,Path.of(System.getenv("HOME")));
        }
        if(targetPath.isAbsolute()){
            return getCurrentDirectory(cwd,targetPath);
        } else{
            Path newpath = cwd.resolve(targetPath).normalize();
            return getCurrentDirectory(cwd,newpath);
        }
    }

    private static Path getCurrentDirectory(Path originPath, Path targetPath) {
        if(Files.exists(targetPath) && Files.isDirectory(targetPath)) return targetPath;
        else System.out.println("cd: " + targetPath + ": No such file or directory");
        return originPath;
    }


    private static void handleEcho(List<String> parts) {
        System.out.println(parts+"test");
        if (parts.isEmpty()) {
            System.out.println();
        } else {
            System.out.println(String.join(" ",parts));
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