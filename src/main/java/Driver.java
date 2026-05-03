import com.cpp.project1011.jackAnalyzer.JackAnalyzer;
import com.cpp.project1011.jackCompiler.JackCompiler;

public class Driver {
    public static void main(String[] args) {
        JackAnalyzer analyzer = new JackAnalyzer("src/main/resources/Square");
        try {
            analyzer.analyze();
        } catch (Exception e) {
            System.out.println("Unable to analyze the code: " + e.getMessage());
            throw new RuntimeException(e);
        }

        JackCompiler compiler = new JackCompiler("src/main/resources/Square");
        try {
            compiler.compile();
        } catch (Exception e) {
            System.out.println("Unable to compile the code: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
