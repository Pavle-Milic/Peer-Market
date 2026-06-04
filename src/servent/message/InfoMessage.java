package servent.message;

public class InfoMessage extends BasicMessage{
    public InfoMessage(int senderPort, int receiverPort, String messageText) {
        super(MessageType.INFO, senderPort, receiverPort, messageText);
    }
}
