import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.LineReaderImpl;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;


class BuiltinCompleter implements Completer {
    private static final List<String> BUILTINS = List.of("echo", "exit");
    private final ExecutableCache executableCache = new ExecutableCache();
    private String pendingWord = null;
    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        // 只补全第一个单词
        if (line.wordIndex() != 0) {
            return;
        }

        String word = line.word();

        Set<String> added = new TreeSet<>();

        // builtin
        for (String cmd : BUILTINS) {
            if (cmd.startsWith(word)) {
                added.add(cmd);
            }
        }

        // PATH executables
        for (String cmd : executableCache.getCache()) {
            if (cmd.startsWith(word)) {
                added.add(cmd);
            }
        }
        if(word.equals(pendingWord)){
            var out = reader.getTerminal().writer();

            // 从 "$ xyz_" 这一行移动到下一行
            out.println();

            // 打印候选
            out.println(String.join("  ", added));
            out.flush();

            // 让 JLine 重新画出 prompt + 当前输入
            ((LineReaderImpl) reader).redisplay();

            pendingWord = null;
            return;
        }
        if(added.size()>1){
            pendingWord = word;
            reader.getTerminal().writer().print("\u0007");
            reader.getTerminal().writer().flush();
            return;
        }
        for (String cmd : added) {
            candidates.add(new Candidate(
                    cmd,
                    cmd,
                    null,
                    null,
                    " ",
                    null,
                    true
            ));
        }
    }


}

