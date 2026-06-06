package servent.message;

public class DeadNodeMessage extends BasicMessage{
    int portId;
    int messageId;
    public DeadNodeMessage(int senderPort, int receiverPort,int portId, int messageId) {
        super(MessageType.DEADNODE, senderPort, receiverPort);
        this.portId = portId;
        this.messageId = messageId;
    }

    public int getPortId() {
        return portId;
    }

    public int getMessageId() {
        return messageId;
    }
}
