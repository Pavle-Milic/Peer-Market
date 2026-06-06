package servent.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import app.AppConfig;
import app.ChordState;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.NewNodeMessage;
import servent.message.SorryMessage;
import servent.message.WelcomeMessage;
import servent.message.util.MessageUtil;

public class NewNodeHandler implements MessageHandler {

	private Message clientMessage;
	
	public NewNodeHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.NEW_NODE) {
			int newNodePort = clientMessage.getSenderPort();
			ServentInfo newNodeInfo = new ServentInfo("localhost", newNodePort);
			
			//check if the new node collides with another existing node.
			if (AppConfig.chordState.isCollision(newNodeInfo.getChordId())) {
				Message sry = new SorryMessage(AppConfig.myServentInfo.getListenerPort(), clientMessage.getSenderPort());
				MessageUtil.sendMessage(sry);
				return;
			}
			
			//check if he is my predecessor
			boolean isMyPred = AppConfig.chordState.isKeyMine(newNodeInfo.getChordId());
			if (isMyPred) { //if yes, prepare and send welcome message
				ServentInfo hisPred = AppConfig.chordState.getPredecessor();
				if (hisPred == null) {
					hisPred = AppConfig.myServentInfo;
				}
				
				AppConfig.chordState.setPredecessor(newNodeInfo);
				
				Map<Integer, ChordState.Pair> myValues = AppConfig.chordState.getValueMap();
				Map<Integer, ChordState.Pair> hisValues = new HashMap<>();
				
				int myId = AppConfig.myServentInfo.getChordId();
				int hisPredId = hisPred.getChordId();
				int newNodeId = newNodeInfo.getChordId();

				for (Entry<Integer, ChordState.Pair> valueEntry : myValues.entrySet()) {
					int key = valueEntry.getKey();
					boolean isMyNewPrimary = false;

					if (newNodeId < myId) {
						if (key > newNodeId && key <= myId) isMyNewPrimary = true;
					} else {
						if (key > newNodeId || key <= myId) isMyNewPrimary = true;
					}

					if (!isMyNewPrimary) {
						hisValues.put(key, valueEntry.getValue());
					}
				}

				List<Integer> keysToRemove = new ArrayList<>();
				for (Integer key : myValues.keySet()) {
					boolean shouldKeep = false;
					if (hisPredId < myId) {
						if (key > hisPredId && key <= myId) shouldKeep = true;
					} else {
						if (key > hisPredId || key <= myId) shouldKeep = true;
					}

					if (!shouldKeep) {
						keysToRemove.add(key);
					}
				}

				for (Integer key : keysToRemove) {
					myValues.remove(key);
				}

				AppConfig.chordState.setValueMap(myValues);
				
				WelcomeMessage wm = new WelcomeMessage(AppConfig.myServentInfo.getListenerPort(), newNodePort, hisValues);
				MessageUtil.sendMessage(wm);
			} else { //if he is not my predecessor, let someone else take care of it
				ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(newNodeInfo.getChordId());
				NewNodeMessage nnm = new NewNodeMessage(newNodePort, nextNode.getListenerPort());
				MessageUtil.sendMessage(nnm);
			}
			
		} else {
			AppConfig.timestampedErrorPrint("NEW_NODE handler got something that is not new node message.");
		}

	}

}
