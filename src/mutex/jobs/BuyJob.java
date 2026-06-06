package mutex.jobs;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.message.BuyMessage;
import servent.message.InfoMessage;
import servent.message.Message;
import servent.message.SubscribeMessage;
import servent.message.util.MessageUtil;

public class BuyJob implements Runnable{

    private final int key;
    private final int amount;
    private final int originalSenderId;

    public BuyJob(int key, int value, int originalSenderId) {
        this.key = key;
        this.amount = value;
        this.originalSenderId = originalSenderId;
    }

    @Override
    public void run() {
        AppConfig.timestampedStandardPrint("Obavljam buy Job ===================================================");
        if (AppConfig.chordState.isKeyMine(key)) {
            if (AppConfig.chordState.getValueMap().containsKey(key)) {
                ChordState.Pair currentPair = AppConfig.chordState.getValueMap().get(key);

                if (currentPair.value() >= amount) {
                    ChordState.Pair noviPair = new ChordState.Pair(currentPair.nodeId(), currentPair.value() - amount);
                    AppConfig.chordState.getValueMap().put(key, noviPair);
                    AppConfig.chordState.backupToSuccessor(key, noviPair);

                    ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(noviPair.nodeId());
                    Message mes = new InfoMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(), noviPair.nodeId(), "Kupljeno je  " + amount + "stvari sa kljucem" +  key);
                    MessageUtil.sendMessage(mes);
                    nextNode = AppConfig.chordState.getNextNodeForKey(originalSenderId);
                    mes = new InfoMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(), originalSenderId, "Uspesno ste obavili kupovinu. Kupljeno je  " + amount + "stvari sa kljucem" +  key);
                    MessageUtil.sendMessage(mes);
                } else {
                    ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(originalSenderId);
                    Message mes = new InfoMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(), originalSenderId, "Kupovina je neuspesna, pokusali ste da kupite vise nego sto ima na stanju");
                    MessageUtil.sendMessage(mes);
                }
            } else {
                ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(originalSenderId);
                Message mes = new InfoMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(), originalSenderId, "Kupovina je neuspesna, Ne postoji artikal sa tim imenom, tj pod tim klucem");
                MessageUtil.sendMessage(mes);
            }
        } else {
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(key);
            Message bm = new BuyMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), key, amount, originalSenderId);
            MessageUtil.sendMessage(bm);
        }
    }

    public void subscribe(int subscribeTo, int subscriber){
        ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(subscribeTo);
        Message mes=new SubscribeMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), subscribeTo, subscriber);
        MessageUtil.sendMessage(mes);

    }
}
