package cli.command;

import app.AppConfig;
import app.ChordState;

public class BuyCommand implements CLICommand{

    @Override
    public String commandName() {
        return "buy";
    }

    @Override
    public void execute(String args) {

        String[] splitArgs = args.split(" ");

        if (splitArgs.length == 2) {
            String name = splitArgs[0];
            int count = Integer.parseInt(splitArgs[1]);
            int key = Math.abs(ChordState.chordHash(name.hashCode()));
            try {
                if (key < 0 || key >= ChordState.CHORD_SIZE) {
                    throw new NumberFormatException();
                }
                if (count < 0) {
                    throw new NumberFormatException();
                }

                AppConfig.chordState.buyValue(key, count,AppConfig.myServentInfo.getChordId());
            } catch (NumberFormatException e) {
                AppConfig.timestampedErrorPrint("Invalid name and count pair. Both should be ints. 0 <= key <= " + ChordState.CHORD_SIZE
                        + ". 0 <= value.");
            }
        } else {
            AppConfig.timestampedErrorPrint("Invalid arguments for list");
        }


    }
}
