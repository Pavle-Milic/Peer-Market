package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.NotifySubscribersMessage;
import servent.message.util.MessageUtil;

public class SubscribeHandler implements MessageHandler {

    private Message clientMessage;

    public SubscribeHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.SUBSCRIBE) {
            AppConfig.subscribers.add(clientMessage.getSenderPort());
            Message msg= new NotifySubscribersMessage(AppConfig.myServentInfo.getListenerPort(), clientMessage.getSenderPort(),"Sucessfully Subscribed to the node on port" + clientMessage.getReceiverPort());
            MessageUtil.sendMessage(msg);
        } else {
            AppConfig.timestampedErrorPrint("Subscribe handler got a message that is not SUBSCRIBE");
        }

    }

}