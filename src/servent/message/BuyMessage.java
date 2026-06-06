package servent.message;

public class BuyMessage extends BasicMessage{

    public BuyMessage(int senderPort, int receiverPort, int key,int value, int originalSenderId) {
        super(MessageType.BUY, senderPort, receiverPort, key + ":" + value + ":" + originalSenderId);
    }
}
