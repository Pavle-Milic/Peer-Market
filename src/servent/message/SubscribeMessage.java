package servent.message;

public class SubscribeMessage extends BasicMessage{
    public SubscribeMessage(int senderPort, int receiverPort) { super(MessageType.SUBSCRIBE, senderPort, receiverPort); }
}
