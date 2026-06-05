package servent.handler;

import app.AppConfig;
import app.ChordState;
import servent.message.Message;
import servent.message.MessageType;

public class BackupKeyHandler implements MessageHandler {

    private Message clientMessage;

    public BackupKeyHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.BACKUPKEY) {
            String[] splitText = clientMessage.getMessageText().split(":");
            if (splitText.length == 3) {
                int key = 0;
                int value = 0;
                int original_port=0;
                try {
                    key = Integer.parseInt(splitText[0]);
                    value = Integer.parseInt(splitText[1]);
                    original_port = Integer.parseInt(splitText[2]);

                    AppConfig.chordState.handleKeyBackup(key,new ChordState.Pair(original_port,value));
                } catch (NumberFormatException e) {
                    AppConfig.timestampedErrorPrint("Got backupKey message with bad text: " + clientMessage.getMessageText());
                }
            } else {
                AppConfig.timestampedErrorPrint("Got backupKey message with bad text: " + clientMessage.getMessageText());
            }


        } else {
            AppConfig.timestampedErrorPrint("BackupKey handler got a message that is not BACKUPKEY");
        }

    }
}
