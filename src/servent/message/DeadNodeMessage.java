package servent.message;

public class DeadNodeMessage extends BasicMessage{
    int deadPort;
    int id;
    public DeadNodeMessage(int senderPort, int receiverPort, int deadPort, int id) {
        super(MessageType.DEADNODE, senderPort, receiverPort);
        this.deadPort = deadPort;
        this.id = id;
    }

    public int getDeadPort() {
        return deadPort;
    }

    public int getId() {
        return id;
    }
}
