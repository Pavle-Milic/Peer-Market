package servent.handler;

import app.AppConfig;
import app.Pinger;
import app.ServentInfo;
import cli.CLIParser;
import servent.SimpleServentListener;
import servent.message.DeadNodeMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

import java.util.LinkedHashSet;
import java.util.Set;

public class DeadNodeHandler implements MessageHandler {

    private DeadNodeMessage clientMessage;
    private Pinger pinger;
    private CLIParser cliParser;
    private SimpleServentListener sl;

    public DeadNodeHandler(DeadNodeMessage clientMessage, Pinger pinger,
                           CLIParser cliParser, SimpleServentListener sl) {
        this.clientMessage = clientMessage;
        this.pinger = pinger;
        this.cliParser = cliParser;
        this.sl = sl;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.DEADNODE) {
            AppConfig.timestampedErrorPrint("Dead node handler got a message that is not DEADNODE");
            return;
        }

        int deadPort = clientMessage.getDeadPort();
        int id       = clientMessage.getId();

        if (deadPort == AppConfig.myServentInfo.getListenerPort()) {
            pinger.stop();
            cliParser.stop();
            sl.stop();
            return;
        }

        if (AppConfig.chordState.hasSeenDeadNodeMessage(deadPort, id)) {
            AppConfig.timestampedStandardPrint("Vec vidjena DeadNode poruka za port " + deadPort + ", ignorisem.");
            return;
        }
        Pinger.removeNode(deadPort);

        AppConfig.chordState.addSeenDeadNodeMessage(deadPort, id);
        AppConfig.chordState.notifyNeighbors(deadPort, id);
        AppConfig.chordState.removeDeadNode(deadPort);
    }
}