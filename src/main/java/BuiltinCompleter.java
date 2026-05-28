
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class BuiltinCompleter implements Completer {
    private static final List<String> BUILTINS = List.of("echo", "exit");
    private final ExecutableCache executableCache = new ExecutableCache();
    private String pendingWord = null;

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        if (line.wordIndex() != 0) {
            pendingWord = null;
            String word = line.word();
            Set<String> matches = findFileMatches(word);
            handleMatches(reader,word,matches,candidates);
            return;
        }

        String word = line.word();
        Set<String> matches = findCommandMatches(word);


        handleMatches(reader,word,matches,candidates);

    }
    private Set<String> findFileMatches(String input) {
        Set<String> matches = new TreeSet<>();
        String prefix = input;
        Path currentDir = Paths.get("");
        if(prefix.contains("/")){
            int lastSlashIndex = prefix.lastIndexOf("/");
            String addedpath =prefix.substring(0,lastSlashIndex);
            prefix =prefix.substring(lastSlashIndex + 1);
            currentDir = currentDir.resolve(addedpath);
        }

        String globPattern = prefix + "*";
        if (Files.exists(currentDir) && Files.isDirectory(currentDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentDir, globPattern)) {
                for (Path entry : stream) {
                    String name = entry.getFileName().toString();
                    if (Files.isDirectory(entry)) {
                        matches.add(name+ "/");
                    } else {
                        matches.add(name);
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return matches;
    }
    private Set<String> findCommandMatches(String prefix) {
        Set<String> matches =new TreeSet<>();

        for (String cmd : BUILTINS) {
            if (cmd.startsWith(prefix)) {
                matches.add(cmd);
            }
        }

        for (String cmd : executableCache.getCache()) {
            if (cmd.startsWith(prefix)) {
                matches.add(cmd);
            }
        }
        return matches;
    }



    private void handleMatches(LineReader reader, String word,
                               Set<String> matches, List<Candidate> candidates){
        // 情况1：无匹配
        if (matches.isEmpty()) {
            pendingWord = null;
            return;
        }

        // 情况2：唯一匹配，补全并加空格
        if (matches.size() == 1) {
            pendingWord = null;
            String cmd = matches.iterator().next();
            if(cmd.substring(cmd.length()-1).equals("/")){
                candidates.add(new Candidate(cmd, cmd, null, null, "", null, false));
            }else{
                candidates.add(new Candidate(cmd, cmd, null, null, " ", null, true));
            }
            return;
        }

        // 多个匹配，计算 LCP
        String lcp = computeLCP(matches);

        // 情况3：LCP 比当前输入长，补全到 LCP（不加空格）
        if (lcp.length() > word.length()) {
            pendingWord = null;
            candidates.add(new Candidate(lcp, lcp, null, null, null, null, false));
            return;
        }

        // 情况4：LCP == word，无法进一步补全，走响铃/列表逻辑
        if (Objects.equals(word, pendingWord)) {
            // 第二次 Tab，列出候选
            var out = reader.getTerminal().writer();
            out.println();
            out.println(String.join("  ", matches));
            out.print("$ " + reader.getBuffer().toString());
            out.flush();
            pendingWord = null;
        } else {
            // 第一次 Tab，响铃
            var out = reader.getTerminal().writer();
            out.print("\u0007");
            out.flush();
            pendingWord = word;
        }
    }
    /**
     * 计算一组字符串的最长公共前缀
     */
    private String computeLCP(Set<String> strings) {
        if (strings.isEmpty()) {
            return "";
        }

        Iterator<String> iter = strings.iterator();
        String lcp = iter.next();

        while (iter.hasNext()) {
            String s = iter.next();
            int i = 0;
            while (i < lcp.length() && i < s.length() && lcp.charAt(i) == s.charAt(i)) {
                i++;
            }
            lcp = lcp.substring(0, i);
            if (lcp.isEmpty()) {
                break;
            }
        }

        return lcp;
    }
}