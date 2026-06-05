package servent.handler;

import app.AppConfig;
import app.Pinger;
import servent.message.Message;
import servent.message.MessageType;

public class PongHandler implements MessageHandler{

    private Message clientMessage;

    public PongHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.PONG) {
            Pinger.nodePonged(clientMessage.getSenderPort());
        } else {
            AppConfig.timestampedErrorPrint("Ping handler got a message that is not PING");
        }

    }
}