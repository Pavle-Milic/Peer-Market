package servent.handler;

import app.AppConfig;
import app.Pinger;
import servent.message.*;
import servent.message.util.MessageUtil;

public class AskPingHandler implements MessageHandler{
    private Message clientMessage;

    public AskPingHandler(Message clientMessage) {this.clientMessage = clientMessage;}

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.ASKPING) {
            int targetPort = Integer.parseInt(clientMessage.getMessageText());
            Message message = new AskPongMessage(AppConfig.myServentInfo.getListenerPort(), targetPort, clientMessage.getSenderPort());
            MessageUtil.sendMessage(message);
            Pinger.nodePonged(clientMessage.getSenderPort());
        } else {
            AppConfig.timestampedErrorPrint("Ask ping handler got a message that is not ASKPING");
        }

    }
}
