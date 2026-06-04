package servent.handler;

import app.AppConfig;
import servent.message.Message;
import servent.message.MessageType;

public class BuyHandler implements MessageHandler{

    private Message clientMessage;

    public BuyHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.BUY) {
            String[] splitText = clientMessage.getMessageText().split(":");
            if (splitText.length == 3) {
                int key = 0;
                int value = 0;
                int original_port=0;
                try {
                    key = Integer.parseInt(splitText[0]);
                    value = Integer.parseInt(splitText[1]);
                    original_port = Integer.parseInt(splitText[2]);

                    AppConfig.chordState.buyValue(key, value,original_port);
                } catch (NumberFormatException e) {
                    AppConfig.timestampedErrorPrint("Got buy message with bad text: " + clientMessage.getMessageText());
                }
            } else {
                AppConfig.timestampedErrorPrint("Got buy message with bad text: " + clientMessage.getMessageText());
            }


        } else {
            AppConfig.timestampedErrorPrint("Buy handler got a message that is not BUY");
        }

    }

}
