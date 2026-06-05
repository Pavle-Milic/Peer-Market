package servent.message;

public class BackupKeyMessage extends BasicMessage{
    public BackupKeyMessage( int senderPort, int receiverPort, int key, int value, int originalSenderPort) {
        super(MessageType.BACKUPKEY, senderPort, receiverPort, key + ":" + value + ":" + originalSenderPort);
    }
}
