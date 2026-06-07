package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.ConfirmPutMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class ConfirmPutHandler implements MessageHandler {

    private Message clientMessage;

    public ConfirmPutHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.CONFIRMPUT) {
            AppConfig.timestampedErrorPrint("Confirm put handler got a message that is not CONFIRMPUT");
            return;
        }
        int targetId, key, value, ownerId;
        String[] splitText = clientMessage.getMessageText().split(":");
        if (splitText.length == 4) {
            targetId = Integer.parseInt(splitText[0]);
            key= Integer.parseInt(splitText[1]);
            value = Integer.parseInt(splitText[2]);
            ownerId = Integer.parseInt(splitText[3]);

        } else {
            AppConfig.timestampedErrorPrint("Confirm put message with bad text: " + clientMessage.getMessageText());
            return;
        }

        if(AppConfig.chordState.isKeyMine(targetId)){
            for(int sub: AppConfig.myServentInfo.getSubscribers()) {
                AppConfig.chordState.notifySubscribers(sub, "Node "+ ownerId+" je dodao jos " + value + " proizvoda sa id-em " + key);
                AppConfig.chordState.notifySubscribers(sub, "[MARKET-NOTIFICATION] node:" + ownerId + " posted item_id:" + key);
            }
            AppConfig.timestampedStandardPrint("Uspesno listirani proizvodi sa id-em " + key);
        }
        else {
            ServentInfo next = AppConfig.chordState.getNextNodeForKey(targetId);
            Message message= new ConfirmPutMessage(AppConfig.myServentInfo.getListenerPort(),next.getListenerPort(), targetId, key,value, ownerId );
            MessageUtil.sendMessage(message);
        }
    }
}
