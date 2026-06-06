package servent.handler;

import app.AppConfig;
import app.ChordState;
import app.Pinger;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.PongMessage;
import servent.message.util.MessageUtil;

public class PingHandler implements MessageHandler{

    private Message clientMessage;

    public PingHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.PING) {
            Message message = new PongMessage( AppConfig.myServentInfo.getListenerPort(),clientMessage.getSenderPort());
            MessageUtil.sendMessage(message);
            Pinger.nodePonged(clientMessage.getSenderPort());
        } else {
            AppConfig.timestampedErrorPrint("Ping handler got a message that is not PING");
        }

    }
}
