import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


class BuiltinCompleter implements Completer {
    private static final List<String> BUILTINS = List.of("echo", "exit");
    private final ExecutableCache executableCache = new ExecutableCache();

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

