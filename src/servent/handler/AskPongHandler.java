package servent.handler;

import app.AppConfig;
import servent.message.AskPongMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.PongMessage;
import servent.message.util.MessageUtil;

public class AskPongHandler implements MessageHandler {

    private AskPongMessage clientMessage;

    public AskPongHandler(AskPongMessage clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.ASKPONG) {
            Message message = new PongMessage(AppConfig.myServentInfo.getListenerPort(), clientMessage.getOriginalSenderPort());
            MessageUtil.sendMessage(message);
        } else {
            AppConfig.timestampedErrorPrint("Ask pong handler got a message that is not ASKPONG");
        }
    }
}