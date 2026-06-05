package cli.command;

import app.AppConfig;

public class PrintValuesCommand implements CLICommand{
    @Override
    public String commandName() {
        return "print_values";
    }

    @Override
    public void execute(String args) {
        AppConfig.chordState.printAllValues();
    }
}
