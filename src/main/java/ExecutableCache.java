import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public class ExecutableCache {
    private Set<String> cache = new TreeSet<>();
    // 上次扫描时的 PATH 值，用于检测变化
    private String lastPath = null;

    public ExecutableCache() {
        refresh(); // 启动时立即扫描一次
    }
    public Set<String> getCache() {
        refreshIfNeeded(); // 检查 PATH 是否变化
        return cache;
    }
    private void refresh() {
        Set<String> newCache = new TreeSet<>();
        String pathEnv = System.getenv("PATH");
        lastPath = pathEnv;

        if (pathEnv == null || pathEnv.isBlank()) {
            cache = newCache;
            return;
        }

        String[] dirs = pathEnv.split(java.util.regex.Pattern.quote(File.pathSeparator));

        for (String dir : dirs) {
            if (dir == null || dir.isBlank()) {
                continue;
            }

            Path path;
            try {
                path = Paths.get(dir);
            } catch (InvalidPathException e) {
                continue;
            }

            if (!Files.isDirectory(path)) {
                continue;
            }

            try (Stream<Path> stream = Files.list(path)) {
                stream.filter(p -> Files.isRegularFile(p) && Files.isExecutable(p))
                        .map(p -> p.getFileName().toString())
                        .forEach(newCache::add);
            } catch (IOException ignored) {
            }
        }

        cache = newCache;
    }
    private void refreshIfNeeded() {
        String currentPath = System.getenv("PATH");
        if (!Objects.equals(currentPath, lastPath)) {
            refresh();
        }
    }
}
