package servent.message;

public class NotifySubscribersMessage extends BasicMessage{
    public NotifySubscribersMessage( int senderPort, int receiverPort, String messageText) {
        super(MessageType.NOTIFYSUBSCRIBERS, senderPort, receiverPort, messageText);
    }
}
