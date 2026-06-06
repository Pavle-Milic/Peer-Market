package servent.message;

public class NotifySubscribersMessage extends BasicMessage{

    private int subscriberId;

    public NotifySubscribersMessage( int senderPort, int receiverPort, int subscriberId, String messageText) {
        super(MessageType.NOTIFYSUBSCRIBERS, senderPort, receiverPort, messageText);
        this.subscriberId= subscriberId;
    }

    public int getSubscriberId() {
        return subscriberId;
    }
}
