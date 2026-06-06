package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.NotifySubscribersMessage;
import servent.message.SubscribeMessage;
import servent.message.util.MessageUtil;

public class NotifySubscribersHandler implements MessageHandler {

    private NotifySubscribersMessage clientMessage;

    public NotifySubscribersHandler(NotifySubscribersMessage clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.NOTIFYSUBSCRIBERS) {
            if(AppConfig.chordState.isKeyMine(clientMessage.getSubscriberId())){
                AppConfig.timestampedStandardPrint(clientMessage.getMessageText());
            }
            else{
                AppConfig.chordState.notifySubscribers(clientMessage.getSubscriberId(), clientMessage.getMessageText());
            }
        } else {
            AppConfig.timestampedErrorPrint("Notify subscribers handler got a message that is not NOTIFYSUBSCRIBERS");
        }

    }


}
