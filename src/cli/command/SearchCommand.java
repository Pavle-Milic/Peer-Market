package cli.command;

import app.AppConfig;
import app.ChordState;

import java.util.Objects;

public class SearchCommand implements CLICommand{
    @Override
    public String commandName() {
        return "search";
    }

    @Override
    public void execute(String args) {
        String[] splitArgs = args.split(" ");

        if (splitArgs.length == 1) {
            String name = splitArgs[0];
            int key = ChordState.chordHash(Math.abs(name.hashCode()));
            ChordState.Pair val = AppConfig.chordState.getValue(key);

            if (Objects.equals(val, new ChordState.Pair(-2, -2))) {
                AppConfig.timestampedStandardPrint("Please wait...");
            } else if (Objects.equals(val, new ChordState.Pair(-1, -1))) {
                AppConfig.timestampedStandardPrint("No such key: " + key);
            } else {
                AppConfig.timestampedStandardPrint(key + ": " + val);
            }
        }else {
            AppConfig.timestampedErrorPrint("Invalid arguments for search,should be only one");
        }
    }
}
