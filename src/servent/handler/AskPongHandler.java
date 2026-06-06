package servent.handler;

import app.AppConfig;
import app.Pinger;
import servent.message.AskPongMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.PongMessage;
import servent.message.util.MessageUtil;

public class AskPongHandler implements MessageHandler {

    private Message clientMessage;

    public AskPongHandler(Message clientMessage) {this.clientMessage = clientMessage;}

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.ASKPONG) {
            int originalSender = Integer.parseInt(clientMessage.getMessageText());
            Message message = new PongMessage(AppConfig.myServentInfo.getListenerPort(), originalSender);
            MessageUtil.sendMessage(message);
            Pinger.nodePonged(clientMessage.getSenderPort());
        } else {
            AppConfig.timestampedErrorPrint("Ask pong handler got a message that is not ASKPONG");
        }
    }
}