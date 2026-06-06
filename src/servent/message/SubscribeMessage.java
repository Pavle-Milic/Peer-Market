package servent.message;

public class SubscribeMessage extends BasicMessage{

    private int subscribeToId;

    public SubscribeMessage(int senderPort, int receiverPort, int subscribeToId, int subscriberId) {
        super(MessageType.SUBSCRIBE, senderPort, receiverPort,subscribeToId + ":" + subscriberId);
        this.subscribeToId = subscribeToId;
    }

    public int getSubscribeToId() {
        return subscribeToId;
    }
}
