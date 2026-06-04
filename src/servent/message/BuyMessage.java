package servent.message;

public class BuyMessage extends BasicMessage{

    public BuyMessage(int senderPort, int receiverPort, int key,int value, int originalSenderPort) {
        super(MessageType.BUY, senderPort, receiverPort, key + ":" + value + ":" + originalSenderPort);
    }
}
