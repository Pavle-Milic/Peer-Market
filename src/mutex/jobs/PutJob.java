package mutex.jobs;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.message.ConfirmPutMessage;
import servent.message.InfoMessage;
import servent.message.Message;
import servent.message.PutMessage;
import servent.message.util.MessageUtil;

public class PutJob implements Runnable{

    private final int key;
    private final ChordState.Pair noviPair;
    private final int originalSenderId;

    public PutJob(int key, ChordState.Pair noviPair) {
        this.key = key;
        this.noviPair = noviPair;
        originalSenderId = noviPair.nodeId();
    }

    @Override
    public void run() {
        AppConfig.timestampedStandardPrint("Obavljam put Job ===================================================");
        AppConfig.chordState.getValueMap().put(key, noviPair);
        AppConfig.chordState.backupToSuccessor(key, noviPair);
        ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(originalSenderId);
        Message mes = new ConfirmPutMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(),originalSenderId,key,noviPair.value());
        MessageUtil.sendMessage(mes);
    }
}
