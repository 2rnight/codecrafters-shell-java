import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class Main {
    private static final Set<String> BUILTIN_COMMANDS = Set.of("exit", "echo", "type","pwd","cd");
    static Path cwd = Paths.get("").toAbsolutePath();
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


            List<String> tokens = parseTokens(commandLine);
            processCommand(tokens);

        }

        scanner.close();
    }

    public static void processCommand(List<String> tokens) {
        List<String> commandArgs = new ArrayList<>();
        List<String> stdoutFiles = new ArrayList<>();
        List<String> stderrFiles = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if (">".equals(token)||"1>".equals(token)) {
                if (i + 1 >= tokens.size()) {
                    System.err.println("syntax error: no file after >");
                    return;
                }
                stdoutFiles.add(tokens.get(++i));
            } else if ("2>".equals(token)) {
                if (i + 1 >= tokens.size()) {
                    System.err.println("syntax error: no file after 2>");
                    return;
                }
                stderrFiles.add(tokens.get(++i));
            } else {
                commandArgs.add(token);
            }
        }

        // 如果 outputFiles 为空，说明没有重定向，逻辑自然退化为普通执行
        execute(commandArgs,stdoutFiles,stderrFiles);
    }
    private static void execute(List<String> commandArgs, List<String> stdoutFiles, List<String> stderrFiles) {
        String command = commandArgs.get(0);
        List<String> Args = commandArgs.subList(1,commandArgs.size());
        Result res = new Result();

        if ("exit".equals(command)) {
            System.exit(0);
        } else if ("echo".equals(command)) {
            res = handleEcho(Args);
        } else if ("type".equals(command)) {
            res =handleType(commandArgs.toArray(new String[0]));
        } else if ("pwd".equals(command)) {
            res.stdout = cwd.toString()+"\n";
        } else if ("cd".equals(command)){
            cwd = handleCd(cwd,commandArgs.toArray(new String[0]));
        } else {
            res = executeExternal(commandArgs);
        }
        if (stdoutFiles.isEmpty()) {
            System.out.print(res.stdout);
        } else {
            writeToFile(stdoutFiles, res.stdout);
        }

        if (stderrFiles.isEmpty()) {
            System.err.print(res.stderr);
        } else {
            writeToFile(stderrFiles, res.stderr);
        }

    }
    private static Result executeExternal(List<String> commandArgs) {
        Result res = new Result();
        try {
            ProcessBuilder pb = new ProcessBuilder(commandArgs);


            Process process = pb.start();

            // 1. 异步读取标准输出 (stdout)
            byte[] stdoutBytes = process.getInputStream().readAllBytes();
            res.stdout = new String(stdoutBytes);

            // 2. 异步读取标准错误 (stderr)
            byte[] stderrBytes = process.getErrorStream().readAllBytes();
            res.stderr = new String(stderrBytes);

            process.waitFor(); // 等待进程结束

        } catch (IOException | InterruptedException e) {
            res.stderr = commandArgs.getFirst()+": command not found\n";
        }
        return res;
    }

    private static void writeToFile(List<String> fileName, String content) {
        try {
            for(int i = 0;i<fileName.size();i++){
                Path filePath = cwd.resolve(fileName.get(i));

                if (filePath.getParent() != null) {
                    Files.createDirectories(filePath.getParent());
                }

                if(i ==fileName.size()-1){
                    Files.writeString(filePath, content);
                }else{
                    Files.writeString(filePath, "");
                }
                // 写入文件（不存在则创建，存在则覆盖）

            }


        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    private static List<String> parseTokens(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBackslash = false;
        for (char c : input.toCharArray()) {
            if (inBackslash) {
                current.append(c);
                inBackslash = false; // 用完马上重置
                continue;
            }
            if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false;
                } else {
                    current.append(c); // 引号内的字符全部保留
                }
            } else if (inDoubleQuote) {
                if(c =='\"'){
                    inDoubleQuote = false;
                } else if (c =='\\') {
                    inBackslash = true;
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


    private static Result handleEcho(List<String> parts) {
        Result res = new Result();
        if (parts.isEmpty()) {
            res.stdout="\n";
            return res;
        } else {
            res.stdout = String.join(" ",parts);
            res.stdout+="\n";
            return res;
        }
    }

    private static Result handleType(String[] parts) {
        Result res = new Result();
        if (parts.length < 2 || parts[1].isBlank()) {
            res.stderr = "type: missing argument\n";
            return res;
        }
        String target = parts[1];
        if (BUILTIN_COMMANDS.contains(target)) {
            res.stdout = target+" is a shell builtin\n";
            return res;
        }

        String executablePath = findExecutable(target);
        if (executablePath != null) {
            res.stdout = target+" is "+executablePath+"\n";
            return res;
        } else {
            res.stderr = target+": not found\n";
            return res;
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