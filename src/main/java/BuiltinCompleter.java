import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import java.util.List;


class BuiltinCompleter implements Completer {
    private static final List<String> BUILTINS = List.of("echo", "exit");

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        // 只补全第一个单词
        if (line.wordIndex() != 0) {
            return;
        }

        String word = line.word();

        for (String cmd : BUILTINS) {
            if (cmd.startsWith(word)) {
                candidates.add(new Candidate(
                        cmd,    // value
                        cmd,    // display
                        null,   // group
                        null,   // description
                        " ",    // suffix: 补全后追加空格
                        null,   // key
                        true    // complete
                ));
            }
        }
    }
}

