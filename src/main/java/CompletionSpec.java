public class CompletionSpec {
    private final String scriptPath;

    public CompletionSpec(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    public String getScriptPath() {
        return scriptPath;
    }

    public String toDisplayString(String command) {
        return "complete -C '" + scriptPath + "' " + command;
    }
}
