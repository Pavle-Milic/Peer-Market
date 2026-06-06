package servent.message;

public class TokenRequestMessage extends BasicMessage{

    int numReqests;
    int nodeId;

    public TokenRequestMessage( int senderPort, int receiverPort, int nodeId, int numReqests) {
        super(MessageType.TOKENREQUEST, senderPort, receiverPort);
        this.nodeId = nodeId;
        this.numReqests = numReqests;
    }

    public int getNumReqests() {
        return numReqests;
    }

    public int getNodeId() {
        return nodeId;
    }
}
