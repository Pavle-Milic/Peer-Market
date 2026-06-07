package mutex.jobs;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.message.InfoMessage;
import servent.message.Message;
import servent.message.util.MessageUtil;

public class BuyJob implements Runnable{

    private final int key;
    private final ChordState.Pair noviPair;
    private final int originalSenderId;
    private final int amount;

    public BuyJob(int key, ChordState.Pair noviPair, int originalSenderId, int amount) {
        this.key = key;
        this.noviPair = noviPair;
        this.originalSenderId = originalSenderId;
        this.amount = amount;
    }

    @Override
    public void run() {
        AppConfig.timestampedStandardPrint("Obavljam buy Job ===================================================");
        AppConfig.chordState.getValueMap().put(key, noviPair);
        AppConfig.chordState.backupToSuccessor(key, noviPair);

        AppConfig.timestampedStandardPrint("[MARKET-BUY-SUCCESS] item_id:" + key + " qty_bought:" + amount + " remaining_qty:" + noviPair.value());

        ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(noviPair.nodeId());
        Message mes = new InfoMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(), noviPair.nodeId(), "Kupljeno je " + amount + " stvari sa kljucem " +  key);
        MessageUtil.sendMessage(mes);
        nextNode = AppConfig.chordState.getNextNodeForKey(originalSenderId);
        mes = new InfoMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(), originalSenderId, "Uspesno ste obavili kupovinu. Kupljeno je " + amount + " stvari sa kljucem " +  key);
        MessageUtil.sendMessage(mes);
    }
}
