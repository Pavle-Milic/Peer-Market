package servent.message;

public class InfoMessage extends BasicMessage{

    int targetId;
    public InfoMessage(int senderPort, int receiverPort,int targetId,  String messageText) {
        super(MessageType.INFO, senderPort, receiverPort, messageText);
        this.targetId = targetId;
    }
    public int getTargetId() {
        return targetId;
    }
}
