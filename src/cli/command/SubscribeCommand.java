package cli.command;

import app.AppConfig;
import app.ChordState;
import servent.message.SubscribeMessage;
import servent.message.util.MessageUtil;

import java.util.Objects;

public class SubscribeCommand implements CLICommand{

    @Override
    public String commandName() {
        return "subscribe";
    }

    @Override
    public void execute(String args) {
        try {
            int port = Integer.parseInt(args);
            SubscribeMessage mes = new SubscribeMessage(AppConfig.myServentInfo.getListenerPort(),port);
            MessageUtil.sendMessage(mes);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Invalid argument for soubscribe: " + args + ". Should be port, which is an int.");
        }
    }
}
