package servent.message;

public class AskPingMessage extends BasicMessage {
    public AskPingMessage( int senderPort, int receiverPort, int targetPort) {
        super(MessageType.ASKPING, senderPort, receiverPort, String.valueOf(targetPort));
    }
}
