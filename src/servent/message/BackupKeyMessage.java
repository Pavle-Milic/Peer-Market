package servent.message;

public class BackupKeyMessage extends BasicMessage{
    public BackupKeyMessage( int senderPort, int receiverPort, int key, int value, int ownerId) {
        super(MessageType.BACKUPKEY, senderPort, receiverPort, key + ":" + value + ":" + ownerId);
    }
}
