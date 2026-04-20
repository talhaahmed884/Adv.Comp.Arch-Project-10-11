package jackTokenizer;

import java.util.ArrayList;
import java.util.List;

public class CommandCleanserImpl implements CommandCleanser {
    private final List<String> commandsList;
    private boolean isInsideBlockComment;

    public CommandCleanserImpl(List<String> commandsList) {
        this.commandsList = new ArrayList<>(commandsList);
        this.isInsideBlockComment = false;
    }

    public List<String> CleanseCommand() {
        List<String> cleansedCommands = new ArrayList<>();

        for (String command : commandsList) {
            if (!isInsideBlockComment) {
                command = this.removeLineComments(command);
            }
            command = this.removeBlockComments(command);
            command = this.trimCommand(command);

            if (!command.isEmpty()) {
                cleansedCommands.add(command);
            }
        }

        return cleansedCommands;
    }

    private String removeLineComments(String command) {
        command = this.trimCommand(command);

        if (command.length() >= 2 && command.charAt(0) == '/' && command.charAt(1) == '/') {
            return "";
        }

        StringBuilder commandBuilder = new StringBuilder();
        boolean isInsideDoubleQuote = false;

        for (int i = 0; i < command.length(); i++) {
            if (command.charAt(i) == '/' && (i + 1 < command.length() && command.charAt(i + 1) == '/')
                    && !isInsideDoubleQuote) {
                break;
            }

            if (command.charAt(i) == '"' && (i - 1 >= 0 && command.charAt(i - 1) != '\\')) {
                isInsideDoubleQuote = !isInsideDoubleQuote;
            }

            commandBuilder.append(command.charAt(i));
        }
        return commandBuilder.toString();
    }

    private String removeBlockComments(String command) {
        command = this.trimCommand(command);

        StringBuilder commandBuilder = new StringBuilder();
        boolean isInsideDoubleQuote = false;

        for (int i = 0; i < command.length(); i++) {
            char currentChar = command.charAt(i);
            char nextChar = i + 1 < command.length() ? command.charAt(i + 1) : ' ';
            char prevChar = i - 1 >= 0 ? command.charAt(i - 1) : ' ';

            if (this.isInsideBlockComment) {
                if (currentChar == '*' && nextChar == '/') {
                    this.isInsideBlockComment = false;
                    i++;
                }
                continue;
            }

            if (currentChar == '/' && nextChar == '*' && !isInsideDoubleQuote) {
                this.isInsideBlockComment = true;
                i++;
                continue;
            }

            if (currentChar == '"' && prevChar != '\\') {
                isInsideDoubleQuote = !isInsideDoubleQuote;
            }

            commandBuilder.append(command.charAt(i));
        }

        return commandBuilder.toString();
    }

    private String trimCommand(String command) {
        String trimmedCommand = command.trim();
        // removing extra spaces in a single line command
        String[] splitCommand = trimmedCommand.split("\\s+");

        return String.join(" ", splitCommand);
    }
}
