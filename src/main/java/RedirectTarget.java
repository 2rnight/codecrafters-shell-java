public class RedirectTarget {

    String fileName;
    boolean append; // true = >>，false = >

    RedirectTarget(String fileName, boolean append) {
        this.fileName = fileName;
        this.append = append;
    }

}
