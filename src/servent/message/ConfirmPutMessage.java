package servent.message;

public class ConfirmPutMessage extends BasicMessage{
    public ConfirmPutMessage(int senderPort, int receiverPort, int targetPort, int key, int value, int ownerId) {
        super(MessageType.CONFIRMPUT, senderPort, receiverPort, targetPort+ ":"+key+":"+value+":"+ownerId);
    }
}
