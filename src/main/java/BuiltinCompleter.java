import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.*;

class BuiltinCompleter implements Completer {
    private static final List<String> BUILTINS = List.of("echo", "exit");
    private final ExecutableCache executableCache = new ExecutableCache();
    private String pendingWord = null;

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        if (line.wordIndex() != 0) {
            pendingWord = null;
            return;
        }

        String word = line.word();
        Set<String> matches = new TreeSet<>();

        // 收集所有匹配项
        for (String cmd : BUILTINS) {
            if (cmd.startsWith(word)) {
                matches.add(cmd);
            }
        }

        for (String cmd : executableCache.getCache()) {
            if (cmd.startsWith(word)) {
                matches.add(cmd);
            }
        }

        // 情况1：无匹配
        if (matches.isEmpty()) {
            pendingWord = null;
            return;
        }

        // 情况2：唯一匹配，补全并加空格
        if (matches.size() == 1) {
            pendingWord = null;
            String cmd = matches.iterator().next();
            candidates.add(new Candidate(cmd, cmd, null, null, "", null, true));
            return;
        }

        // 多个匹配，计算 LCP
        String lcp = computeLCP(matches);

        // 情况3：LCP 比当前输入长，补全到 LCP（不加空格）
        if (lcp.length() > word.length()) {
            pendingWord = null;
            candidates.add(new Candidate(lcp, lcp, null, null, null, null, true));
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