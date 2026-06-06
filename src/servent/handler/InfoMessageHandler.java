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
        int targetId = clientMessage.getTargetId();

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
