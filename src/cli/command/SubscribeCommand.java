package cli.command;

import app.AppConfig;

public class SubscribeCommand implements CLICommand{

    @Override
    public String commandName() {
        return "subscribe";
    }

    @Override
    public void execute(String args) {
        try {
            int subscribeToId = Integer.parseInt(args);
            int subscriberId = AppConfig.myServentInfo.getChordId();
            AppConfig.chordState.subscribe(subscribeToId, subscriberId);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Invalid argument for soubscribe: " + args + ". Should be id, which is an int.");
        }
    }
}
