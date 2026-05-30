import java.util.HashMap;
import java.util.Map;

public class CompletionRegistry {
    private final Map<String, CompletionSpec> registry = new HashMap<>();

    public boolean isContain(String key){
        return registry.containsKey(key);
    }
    public String getCompletionSpecToString(String key){
        return registry.get(key).getScriptPath();
    }
    public void register(String command, String scriptPath) {
        registry.put(command, new CompletionSpec(scriptPath));
    }
    public String print(String command) {
        CompletionSpec spec = registry.get(command);
        if (spec == null) {
            return null;
        }
        return spec.toDisplayString(command);
    }
}
