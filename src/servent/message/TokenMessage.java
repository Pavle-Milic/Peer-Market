package servent.message;

public class TokenMessage extends BasicMessage{

    private String mapa;
    private String list;
    private int targetId;

    public TokenMessage( int senderPort, int receiverPort, String mapa, String queue, int id) {
        super(MessageType.TOKEN, senderPort, receiverPort);
        this.mapa = mapa;
        this.list = queue;
        this.targetId = id;
    }

    public String getMapa() {
        return mapa;
    }

    public String getList() {
        return list;
    }

    public int getTargetId() {
        return targetId;
    }
}
