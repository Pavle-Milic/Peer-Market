package servent.message;

public class AskPingMessage extends BasicMessage {

    private int targetPort;

    public AskPingMessage( int senderPort, int receiverPort, int targetPort) {
        super(MessageType.ASKPING, senderPort, receiverPort);
        this.targetPort= targetPort;
    }

    public int getTargetPort() {
        return targetPort;
    }
}
