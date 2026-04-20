package jackAnalyzer;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.security.InvalidParameterException;

public class JackAnalyzer {
    private final String[] fileNames;
    private String sourcePath;

    public JackAnalyzer(String name) {
        File file = new File(name);
        String targetFileName;

        if (file.isFile()) {
            fileNames = new String[]{FilenameUtils.getName(name)};
            sourcePath = FilenameUtils.getPath(name);
        } else if (file.isDirectory()) {
            fileNames = file.list();
            sourcePath = file.getPath();
        } else {
            throw new InvalidParameterException("Unable to read path: " + name);
        }

        if (!sourcePath.endsWith("/")) {
            sourcePath += "/";
        }
    }
}
