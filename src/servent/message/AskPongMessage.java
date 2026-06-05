package servent.message;

public class AskPongMessage extends BasicMessage {

    private int originalSenderPort;

    public AskPongMessage(int senderPort, int receiverPort, int originalSenderPort) {
        super(MessageType.ASKPONG, senderPort, receiverPort);
        this.originalSenderPort = originalSenderPort;
    }

    public int getOriginalSenderPort() {
        return originalSenderPort;
    }
}