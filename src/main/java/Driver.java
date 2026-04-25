import com.cpp.project1011.jackAnalyzer.JackAnalyzer;

public class Driver {
    public static void main(String[] args) {
        JackAnalyzer analyzer = new JackAnalyzer("src/main/resources/Square.jack");

        try {
            analyzer.analyze();
        } catch (Exception e) {
            System.out.println("Unable to analyze the code: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
