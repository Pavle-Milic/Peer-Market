package servent.handler;

import app.AppConfig;
import app.Pinger;
import cli.CLIParser;
import mutex.GrindingRoom;
import servent.SimpleServentListener;
import servent.message.DeadNodeMessage;
import servent.message.MessageType;


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

        int deadNodeId = clientMessage.getPortId();
        int messageId       = clientMessage.getMessageId();

        if (deadNodeId == AppConfig.myServentInfo.getChordId()) {
            pinger.stop();
            cliParser.stop();
            sl.stop();
            GrindingRoom.finallyTimeForLeagueOfLegends();
            return;
        }

        synchronized (AppConfig.chordState) {
            if (AppConfig.chordState.hasSeenDeadNodeMessage(deadNodeId, messageId)) {
                AppConfig.timestampedStandardPrint("Vec vidjena DeadNode poruka za node " + deadNodeId + ", ignorisem.");
                return;
            }
        }
        Pinger.removeNode(deadNodeId);

        AppConfig.chordState.addSeenDeadNodeMessage(deadNodeId, messageId);
        AppConfig.chordState.notifyNeighbors(deadNodeId, messageId, MessageType.DEADNODE);
        AppConfig.chordState.removeDeadNode(deadNodeId);
    }
}