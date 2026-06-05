package servent.handler;

import app.AppConfig;
import servent.message.*;
import servent.message.util.MessageUtil;

public class AskPingHandler implements MessageHandler{
    private AskPingMessage clientMessage;

    public AskPingHandler(AskPingMessage clientMessage) {this.clientMessage = clientMessage;}

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.ASKPING) {
            Message message = new AskPongMessage(AppConfig.myServentInfo.getListenerPort(), clientMessage.getTargetPort(), clientMessage.getSenderPort());
            MessageUtil.sendMessage(message);
        } else {
            AppConfig.timestampedErrorPrint("Ask ping handler got a message that is not ASKPING");
        }

    }
}
