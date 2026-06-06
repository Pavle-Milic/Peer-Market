package servent.message;

public class AskPongMessage extends BasicMessage {
    public AskPongMessage(int senderPort, int receiverPort, int originalSenderPort) {
        super(MessageType.ASKPONG, senderPort, receiverPort, String.valueOf(originalSenderPort));
    }
}