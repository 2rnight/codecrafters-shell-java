import java.io.File;
import java.io.IOException;
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


            List<String> tokens = parseTokens(commandLine);
            String command = tokens.get(0);
            List<String> Args = tokens.subList(1,tokens.size());

            if ("exit".equals(commandLine) || "exit".equals(command)) {
                break;
            } else if ("echo".equals(command)) {
                handleEcho(Args);
            } else if ("type".equals(command)) {
                handleType(tokens.toArray(new String[0]));
            } else if ("pwd".equals(command)) {
                System.out.println(cwd);
            } else if ("cd".equals(command)){
                cwd = handleCd(cwd,tokens.toArray(new String[0]));
            } else {
                try {
                    List<String> fullArgs = new ArrayList<>();
                    fullArgs.add(command);
                    fullArgs.addAll(Args);

                    ProcessBuilder pb = new ProcessBuilder(fullArgs);

                    // 关键：这行代码代替了所有的 transferTo，它能处理标准输出和错误输出
                    pb.inheritIO();

                    Process process = pb.start();
                    process.waitFor(); // 必须等待子进程结束，否则提示符 $ 会乱入到输出中间
                } catch (IOException e) {
                    System.out.println(command + ": command not found");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        scanner.close();
    }

    private static List<String> parseTokens(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBackslash = false;
        for (char c : input.toCharArray()) {
            if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false;
                } else {
                    current.append(c); // 引号内的字符全部保留
                }
            } else if (inDoubleQuote) {
                if(c =='\"'){
                    inDoubleQuote = false;
                } else {
                    current.append(c);
                }
            } else {
                if (c == '\'') {
                    inSingleQuote = true;
                } else if (c =='\"') {
                    inDoubleQuote = true;
                } else if (c =='\\') {
                    inBackslash = true;
                } else if (inBackslash) {
                    current.append(c);
                    if(inSingleQuote) inSingleQuote = false;
                    if(inDoubleQuote) inDoubleQuote = false;
                    inBackslash = false;
                } else if (Character.isWhitespace(c)) {
                    // 遇到空格：如果是单词的结束，加入列表
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current.setLength(0); // 清空 buffer
                    }
                } else {
                    current.append(c); // 普通字符
                }
            }
        }

        // 补上最后一个单词
        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
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