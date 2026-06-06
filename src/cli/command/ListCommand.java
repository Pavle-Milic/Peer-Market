package cli.command;

import app.AppConfig;
import app.ChordState;
import servent.message.Message;
import servent.message.NotifySubscribersMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;

public class ListCommand implements CLICommand{

    @Override
    public String commandName() {
        return "list_item";
    }

    @Override
    public void execute(String args) {

        String[] splitArgs = args.split(" ");

        if (splitArgs.length == 2) {
            String name = splitArgs[0];
            int count = Integer.parseInt(splitArgs[1]);
            int key = ChordState.chordHash(Math.abs(name.hashCode()));
            try {
                if (key < 0 || key >= ChordState.CHORD_SIZE) {
                    throw new NumberFormatException();
                }

                AppConfig.chordState.putValue(key, count,AppConfig.myServentInfo.getChordId());

            } catch (NumberFormatException e) {
                AppConfig.timestampedErrorPrint("Invalid name and count pair. Both should be ints. 0 <= key <= " + ChordState.CHORD_SIZE
                        + ". 0 <= value.");
            }
        } else {
            AppConfig.timestampedErrorPrint("Invalid arguments for list");
        }


    }
}
