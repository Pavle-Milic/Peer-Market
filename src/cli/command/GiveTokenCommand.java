package cli.command;

import app.AppConfig;

public class GiveTokenCommand implements CLICommand{
    @Override
    public String commandName() {
        return "givetoken";
    }

    @Override
    public void execute(String args) {
        AppConfig.chordState.setToken(true);
        AppConfig.timestampedStandardPrint("KEY ACKQIERD ===========================================================");
    }
}
