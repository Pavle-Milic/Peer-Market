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
    private final int value;
    private final int originalSenderId;

    public PutJob(int key, int value, int originalSenderId) {
        this.key = key;
        this.value = value;
        this.originalSenderId = originalSenderId;
    }

    @Override
    public void run() {
        AppConfig.timestampedStandardPrint("Obavljam put Job ===================================================");
        if (AppConfig.chordState.isKeyMine(key)) {
            if (!AppConfig.chordState.getValueMap().containsKey(key) && value >0) {
                ChordState.Pair noviPair = new ChordState.Pair(originalSenderId, value);
                AppConfig.chordState.getValueMap().put(key, noviPair);
                AppConfig.chordState.backupToSuccessor(key, noviPair);
                ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(originalSenderId);
                Message mes = new ConfirmPutMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(),originalSenderId,key,value);
                MessageUtil.sendMessage(mes);
            } else {
                ChordState.Pair currentPair = AppConfig.chordState.getValueMap().get(key);
                if (currentPair.value() == 0 && value >0) {
                    ChordState.Pair noviPair = new ChordState.Pair(originalSenderId, value);
                    AppConfig.chordState.getValueMap().put(key, noviPair);
                    AppConfig.chordState.backupToSuccessor(key, noviPair);
                    ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(originalSenderId);
                    Message mes = new ConfirmPutMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(),originalSenderId,key,value);
                    MessageUtil.sendMessage(mes);
                } else if (currentPair.nodeId() == originalSenderId && currentPair.value() + value>=0) {
                    ChordState.Pair noviPair = new ChordState.Pair(originalSenderId, currentPair.value() + value);
                    AppConfig.chordState.getValueMap().put(key, noviPair);
                    AppConfig.chordState.backupToSuccessor(key, noviPair);
                    ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(originalSenderId);
                    Message mes = new ConfirmPutMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(),originalSenderId,key,value);
                    MessageUtil.sendMessage(mes);
                } else {
                    AppConfig.timestampedErrorPrint("Odbijen upis za kljuc " + key + ". Id " + originalSenderId + " nije vlasnik ili je probao da oduzme vise nego sto ima na stanju");
                    ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(originalSenderId);
                    Message mes = new InfoMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(), originalSenderId,"Odbijen upis za kljuc " + key + ". Id " + originalSenderId + " nije vlasnik ili je probao da oduzme vise nego sto ima na stanju" );
                    MessageUtil.sendMessage(mes);
                }
            }
        } else {
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(key);
            PutMessage pm = new PutMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), key, value,originalSenderId);
            MessageUtil.sendMessage(pm);
        }
    }
}
