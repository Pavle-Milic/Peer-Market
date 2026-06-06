package servent.handler;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.NotifySubscribersMessage;
import servent.message.SubscribeMessage;
import servent.message.util.MessageUtil;

public class SubscribeHandler implements MessageHandler {

    private Message clientMessage;

    public SubscribeHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.SUBSCRIBE) {
            String[] splitText = clientMessage.getMessageText().split(":");
            if (splitText.length == 2) {
                int subscribeTo = Integer.parseInt(splitText[0]);
                int subscriber = Integer.parseInt(splitText[1]);
                    if (AppConfig.chordState.isKeyMine(subscribeTo) && clientMessage.getSenderPort()!=AppConfig.myServentInfo.getListenerPort()) {
                        AppConfig.myServentInfo.addSubscriber(subscriber);
                        AppConfig.chordState.notifySubscribers(subscriber, "Uspesno ste se subscribe-ovali na node "+ AppConfig.myServentInfo.getChordId());
                    }
                    else{
                        ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(subscribeTo);
                        Message mes=new SubscribeMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(),subscribeTo,subscriber);
                        MessageUtil.sendMessage(mes);
                    }
            }
            else{
                AppConfig.timestampedErrorPrint("Subscribe handler got a message that has too many arguments, should be 2");
            }
        } else {
            AppConfig.timestampedErrorPrint("Subscribe handler got a message that is not SUBSCRIBE");
        }

    }

}