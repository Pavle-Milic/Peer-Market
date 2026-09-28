package mutex.jobs;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.message.InfoMessage;
import servent.message.Message;
import servent.message.util.MessageUtil;

import java.util.Map;

/**
 * Executed while this node holds the token. The job itself reads the current inventory,
 * validates the purchase, computes the new value and writes it, so nothing can change
 * between the check and the update.
 */
public class BuyJob implements Runnable {

    private final int key;
    private final int amount;
    private final int originalSenderId;

    public BuyJob(int key, int amount, int originalSenderId) {
        this.key = key;
        this.amount = amount;
        this.originalSenderId = originalSenderId;
    }

    @Override
    public void run() {
        AppConfig.timestampedStandardPrint("Obavljam buy Job ===================================================");

        Map<Integer, ChordState.Pair> valueMap = AppConfig.chordState.getValueMap();
        ChordState.Pair current = valueMap.get(key);

        if (current == null) {
            AppConfig.timestampedStandardPrint("[MARKET-BUY-FAIL] item_id:" + key + " reason:NO_SUCH_KEY");
            sendInfo(originalSenderId, "Kupovina je neuspesna, Ne postoji artikal sa tim imenom, tj pod tim klucem");
            return;
        }

        if (amount <= 0 || current.value() < amount) {
            AppConfig.timestampedStandardPrint("[MARKET-BUY-FAIL] item_id:" + key + " reason:OUT_OF_STOCK");
            sendInfo(originalSenderId, "Kupovina je neuspesna, pokusali ste da kupite vise nego sto ima na stanju");
            return;
        }

        ChordState.Pair noviPair = new ChordState.Pair(current.nodeId(), current.value() - amount);
        valueMap.put(key, noviPair);
        AppConfig.chordState.backupToSuccessor(key, noviPair);

        AppConfig.timestampedStandardPrint("[MARKET-BUY-SUCCESS] item_id:" + key + " qty_bought:" + amount + " remaining_qty:" + noviPair.value());

        // Seller
        sendInfo(noviPair.nodeId(), "Kupljeno je " + amount + " stvari sa kljucem " + key);
        // Buyer
        sendInfo(originalSenderId, "Uspesno ste obavili kupovinu. Kupljeno je " + amount + " stvari sa kljucem " + key);
    }

    private void sendInfo(int targetId, String text) {
        ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(targetId);
        Message mes = new InfoMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), targetId, text);
        MessageUtil.sendMessage(mes);
    }
}