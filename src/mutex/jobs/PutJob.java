package mutex.jobs;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.message.ConfirmPutMessage;
import servent.message.InfoMessage;
import servent.message.Message;
import servent.message.util.MessageUtil;

import java.util.Map;

/**
 * Executed while this node holds the token. The job itself reads the current inventory,
 * validates the request, computes the new value and writes it, so nothing can change
 * between the check and the update.
 */
public class PutJob implements Runnable {

    private final int key;
    private final int value;              // positive = add stock, negative = remove stock
    private final int originalSenderId;

    public PutJob(int key, int value, int originalSenderId) {
        this.key = key;
        this.value = value;
        this.originalSenderId = originalSenderId;
    }

    @Override
    public void run() {
        AppConfig.timestampedStandardPrint("Obavljam put Job ===================================================");

        Map<Integer, ChordState.Pair> valueMap = AppConfig.chordState.getValueMap();
        ChordState.Pair current = valueMap.get(key);
        ChordState.Pair noviPair;

        if ((current == null || current.value() == 0) && value > 0) {
            // New listing, or re-listing of a sold-out item
            noviPair = new ChordState.Pair(originalSenderId, value);
        } else if (current != null
                && current.nodeId() == originalSenderId
                && current.value() + value >= 0) {
            // Owner adjusting their own stock
            noviPair = new ChordState.Pair(originalSenderId, current.value() + value);
        } else {
            reject();
            return;
        }

        valueMap.put(key, noviPair);
        AppConfig.chordState.backupToSuccessor(key, noviPair);

        AppConfig.timestampedStandardPrint("[MARKET-LIST] item_id:" + key + " qty:" + noviPair.value());

        ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(originalSenderId);
        Message mes = new ConfirmPutMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), originalSenderId, key, noviPair.value(), noviPair.nodeId());
        MessageUtil.sendMessage(mes);
    }

    private void reject() {
        String msg = "Odbijen upis za kljuc " + key + ". Id " + originalSenderId + " nije vlasnik ili je probao da oduzme vise nego sto ima na stanju";

        AppConfig.timestampedStandardPrint("[MARKET-PUT-FAIL] item_id:" + key + " reason:NOT_ENOUGH_STOCK_TO_REMOVE or reason:NOT_THE_OWNER");
        AppConfig.timestampedErrorPrint(msg);

        ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(originalSenderId);
        Message mes = new InfoMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), originalSenderId, msg);
        MessageUtil.sendMessage(mes);
    }
}