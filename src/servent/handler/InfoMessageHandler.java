package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.ConfirmPutMessage;
import servent.message.InfoMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.util.MessageUtil;

public class InfoMessageHandler implements MessageHandler {
    private InfoMessage clientMessage;

    public InfoMessageHandler(InfoMessage clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.INFO) {
            AppConfig.timestampedErrorPrint("Info handler got a message that is not INFO");
            return;
        }
        int targetId;
        String[] splitText = clientMessage.getMessageText().split(":");
        if (splitText.length == 3) {
            targetId = clientMessage.getTargetId();
        } else {
            AppConfig.timestampedErrorPrint("Info message with bad text: " + clientMessage.getMessageText());
            return;
        }

        if(AppConfig.chordState.isKeyMine(targetId)){
            AppConfig.timestampedStandardPrint(clientMessage.getMessageText());
        }
        else {
            ServentInfo next = AppConfig.chordState.getNextNodeForKey(targetId);
            Message message= new InfoMessage(AppConfig.myServentInfo.getListenerPort(),next.getListenerPort(), targetId, clientMessage.getMessageText() );
            MessageUtil.sendMessage(message);
        }
    }
}
