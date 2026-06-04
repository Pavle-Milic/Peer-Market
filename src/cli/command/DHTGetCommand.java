package cli.command;

import app.AppConfig;
import app.ChordState;

import java.util.Objects;

public class DHTGetCommand implements CLICommand {

	@Override
	public String commandName() {
		return "dht_get";
	}

	@Override
	public void execute(String args) {
		try {
			int key = Integer.parseInt(args);
			
			ChordState.Pair val = AppConfig.chordState.getValue(key);
			
			if (Objects.equals(val, new ChordState.Pair(-2, -2))) {
				AppConfig.timestampedStandardPrint("Please wait...");
			} else if (Objects.equals(val, new ChordState.Pair(-1, -1))) {
				AppConfig.timestampedStandardPrint("No such key: " + key);
			} else {
				AppConfig.timestampedStandardPrint(key + ": " + val);
			}
		} catch (NumberFormatException e) {
			AppConfig.timestampedErrorPrint("Invalid argument for dht_get: " + args + ". Should be key, which is an int.");
		}
	}

}
