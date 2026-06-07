package cli.command;

import app.AppConfig;
import mutex.GrindingRoom;

public class GiveTokenCommand implements CLICommand{
    @Override
    public String commandName() {
        return "givetoken";
    }

    @Override
    public void execute(String args) {
        AppConfig.chordState.setToken(true);

        AppConfig.timestampedStandardPrint("[MUTEX-ACQUIRED]");
        AppConfig.timestampedStandardPrint("KEY ACKQIERD ===========================================================");

        GrindingRoom.workWorkWorkWorkWorkWork();

        while (GrindingRoom.isWorking()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
