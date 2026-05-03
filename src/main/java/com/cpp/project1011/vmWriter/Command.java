package com.cpp.project1011.vmWriter;

public enum Command {
    add("add"),
    sub("sub"),
    neg("neg"),
    eq("eq"),
    gt("gt"),
    lt("lt"),
    and("and"),
    or("or"),
    not("not"),
    push("push"),
    pop("pop"),
    label("label"),
    _goto("goto"),
    if_goto("if-goto"),
    function("function"),
    call("call"),
    _return("return");

    private final String value;

    Command(String value) {
        this.value = value;
    }

    public static Command fromValue(String value) {
        for (Command command : values()) {
            if (command.value.equals(value)) {
                return command;
            }
        }
        throw new IllegalArgumentException("invalid segment value: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
