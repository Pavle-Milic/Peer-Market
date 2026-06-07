package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import mutex.GrindingRoom;
import mutex.jobs.BuyJob;
import mutex.jobs.PutJob;
import servent.message.*;
import servent.message.util.MessageUtil;

/**
 * This class implements all the logic required for Chord to function.
 * It has a static method <code>chordHash</code> which will calculate our chord ids.
 * It also has a static attribute <code>CHORD_SIZE</code> that tells us what the maximum
 * key is in our system.
 * <p>
 * Other public attributes and methods:
 * <ul>
 *   <li><code>chordLevel</code> - log_2(CHORD_SIZE) - size of <code>successorTable</code></li>
 *   <li><code>successorTable</code> - a map of shortcuts in the system.</li>
 *   <li><code>predecessorInfo</code> - who is our predecessor.</li>
 *   <li><code>valueMap</code> - DHT values stored on this node.</li>
 *   <li><code>init()</code> - should be invoked when we get the WELCOME message.</li>
 *   <li><code>isCollision(int chordId)</code> - checks if a servent with that Chord ID is already active.</li>
 *   <li><code>isKeyMine(int key)</code> - checks if we have a key locally.</li>
 *   <li><code>getNextNodeForKey(int key)</code> - if next node has this key, then return it, otherwise returns the nearest predecessor for this key from my successor table.</li>
 *   <li><code>addNodes(List<ServentInfo> nodes)</code> - updates the successor table.</li>
 *   <li><code>putValue(int key, int value)</code> - stores the value locally or sends it on further in the system.</li>
 *   <li><code>getValue(int key)</code> - gets the value locally, or sends a message to get it from somewhere else.</li>
 * </ul>
 * @author bmilojkovic
 *
 */
public class ChordState {

	public record Pair(int nodeId, int value) {}

	public static int CHORD_SIZE;
	public static int chordHash(int value) {
		return 61 * value % CHORD_SIZE;
	}
	
	private int chordLevel; //log_2(CHORD_SIZE)
	
	private ServentInfo[] successorTable;
	private ServentInfo predecessorInfo;
	
	//we DO NOT use this to send messages, but only to construct the successor table
	private List<ServentInfo> allNodeInfo;
	
	private Map<Integer, Pair> valueMap;

	private Queue<Pair> seenDeadNodeMessages = new ConcurrentLinkedQueue<>();
	private Queue<Pair> seenTokenRequestMessages = new ConcurrentLinkedQueue<>();
	private Map<Integer,Integer> numOfTokenRequests = new ConcurrentHashMap<>();

	private Map<Integer, Integer> tokenMap = new ConcurrentHashMap<>();

	private volatile boolean hasToken = false;
	
	public ChordState() {
		this.chordLevel = 1;
		int tmp = CHORD_SIZE;
		while (tmp != 2) {
			if (tmp % 2 != 0) { //not a power of 2
				throw new NumberFormatException();
			}
			tmp /= 2;
			this.chordLevel++;
		}
		
		successorTable = new ServentInfo[chordLevel];
		for (int i = 0; i < chordLevel; i++) {
			successorTable[i] = null;
		}
		
		predecessorInfo = null;
		valueMap = new ConcurrentHashMap<>();
		allNodeInfo = new ArrayList<>();
	}
	
	/**
	 * This should be called once after we get <code>WELCOME</code> message.
	 * It sets up our initial value map and our first successor so we can send <code>UPDATE</code>.
	 * It also lets bootstrap know that we did not collide.
	 */
	public void init(WelcomeMessage welcomeMsg) {
		//set a temporary pointer to next node, for sending of update message
		successorTable[0] = new ServentInfo("localhost", welcomeMsg.getSenderPort());
		this.valueMap = new ConcurrentHashMap<>(welcomeMsg.getValues());
		Pinger.addNode(welcomeMsg.getSenderPort());
		
		//tell bootstrap this node is not a collider
		try {
			Socket bsSocket = new Socket("localhost", AppConfig.BOOTSTRAP_PORT);
			
			PrintWriter bsWriter = new PrintWriter(bsSocket.getOutputStream());
			bsWriter.write("New\n" + AppConfig.myServentInfo.getListenerPort() + "\n");
			
			bsWriter.flush();
			bsSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int getChordLevel() {
		return chordLevel;
	}
	
	public ServentInfo[] getSuccessorTable() {
		return successorTable;
	}
	
	public int getNextNodePort() {
		return successorTable[0].getListenerPort();
	}
	
	public ServentInfo getPredecessor() {
		return predecessorInfo;
	}
	
	public void setPredecessor(ServentInfo newNodeInfo) {
		this.predecessorInfo = newNodeInfo;
	}

	public Map<Integer, Pair> getValueMap() {
		return valueMap;
	}
	
	public void setValueMap(Map<Integer, Pair> valueMap) {
		this.valueMap = valueMap;
	}
	
	public boolean isCollision(int chordId) {
		if (chordId == AppConfig.myServentInfo.getChordId()) {
			return true;
		}
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId() == chordId) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if we are the owner of the specified key.
	 */
	public boolean isKeyMine(int key) {
		if (predecessorInfo == null) {
			return true;
		}
		
		int predecessorChordId = predecessorInfo.getChordId();
		int myChordId = AppConfig.myServentInfo.getChordId();
		
		if (predecessorChordId < myChordId) { //no overflow
			if (key <= myChordId && key > predecessorChordId) {
				return true;
			}
		} else { //overflow
			if (key <= myChordId || key > predecessorChordId) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Main chord operation - find the nearest node to hop to to find a specific key.
	 * We have to take a value that is smaller than required to make sure we don't overshoot.
	 * We can only be certain we have found the required node when it is our first next node.
	 */
	public ServentInfo getNextNodeForKey(int key) {
		if (isKeyMine(key)) {
			return AppConfig.myServentInfo;
		}
		
		//normally we start the search from our first successor
		int startInd = 0;
		
		//if the key is smaller than us, and we are not the owner,
		//then all nodes up to CHORD_SIZE will never be the owner,
		//so we start the search from the first item in our table after CHORD_SIZE
		//we know that such a node must exist, because otherwise we would own this key
		if (key < AppConfig.myServentInfo.getChordId()) {
			int skip = 1;
			while (successorTable[skip].getChordId() > successorTable[startInd].getChordId()) {
				startInd++;
				skip++;
			}
		}
		
		int previousId = successorTable[startInd].getChordId();
		
		for (int i = startInd + 1; i < successorTable.length; i++) {
			if (successorTable[i] == null) {
				AppConfig.timestampedErrorPrint("Couldn't find successor for " + key);
				break;
			}
			
			int successorId = successorTable[i].getChordId();
			
			if (successorId >= key) {
				return successorTable[i-1];
			}
			if (key > previousId && successorId < previousId) { //overflow
				return successorTable[i-1];
			}
			previousId = successorId;
		}
		//if we have only one node in all slots in the table, we might get here
		//then we can return any item
		return successorTable[0];
	}

	private void updateSuccessorTable() {
		//first node after me has to be successorTable[0]
		
		int currentNodeIndex = 0;
		ServentInfo currentNode = allNodeInfo.get(currentNodeIndex);
		successorTable[0] = currentNode;
		
		int currentIncrement = 2;
		
		ServentInfo previousNode = AppConfig.myServentInfo;
		
		//i is successorTable index
		for(int i = 1; i < chordLevel; i++, currentIncrement *= 2) {
			//we are looking for the node that has larger chordId than this
			int currentValue = (AppConfig.myServentInfo.getChordId() + currentIncrement) % CHORD_SIZE;
			
			int currentId = currentNode.getChordId();
			int previousId = previousNode.getChordId();
			
			//this loop needs to skip all nodes that have smaller chordId than currentValue
			while (true) {
				if (currentValue > currentId) {
					//before skipping, check for overflow
					if (currentId > previousId || currentValue < previousId) {
						//try same value with the next node
						previousId = currentId;
						currentNodeIndex = (currentNodeIndex + 1) % allNodeInfo.size();
						currentNode = allNodeInfo.get(currentNodeIndex);
						currentId = currentNode.getChordId();
					} else {
						successorTable[i] = currentNode;
						break;
					}
				} else { //node id is larger
					ServentInfo nextNode = allNodeInfo.get((currentNodeIndex + 1) % allNodeInfo.size());
					int nextNodeId = nextNode.getChordId();
					//check for overflow
					if (nextNodeId < currentId && currentValue <= nextNodeId) {
						//try same value with the next node
						previousId = currentId;
						currentNodeIndex = (currentNodeIndex + 1) % allNodeInfo.size();
						currentNode = allNodeInfo.get(currentNodeIndex);
						currentId = currentNode.getChordId();
					} else {
						successorTable[i] = currentNode;
						break;
					}
				}
			}
		}
		
	}

	/**
	 * This method constructs an ordered list of all nodes. They are ordered by chordId, starting from this node.
	 * Once the list is created, we invoke <code>updateSuccessorTable()</code> to do the rest of the work.
	 * 
	 */
	public void addNodes(List<ServentInfo> newNodes) {
		allNodeInfo.addAll(newNodes);
		
		allNodeInfo.sort(new Comparator<ServentInfo>() {
			
			@Override
			public int compare(ServentInfo o1, ServentInfo o2) {
				return o1.getChordId() - o2.getChordId();
			}
			
		});
		
		List<ServentInfo> newList = new ArrayList<>();
		List<ServentInfo> newList2 = new ArrayList<>();
		
		int myId = AppConfig.myServentInfo.getChordId();
		for (ServentInfo serventInfo : allNodeInfo) {
			if (serventInfo.getChordId() < myId) {
				newList2.add(serventInfo);
			} else {
				newList.add(serventInfo);
			}
		}
		
		allNodeInfo.clear();
		allNodeInfo.addAll(newList);
		allNodeInfo.addAll(newList2);
		if (newList2.size() > 0) {
			predecessorInfo = newList2.get(newList2.size()-1);
		} else {
			predecessorInfo = newList.get(newList.size()-1);
		}

		updateSuccessorTable();
		clearOutdatedKeys();

		Set<Integer> neighbors = new LinkedHashSet<>();
		if (predecessorInfo != null) neighbors.add(predecessorInfo.getChordId());
		if (successorTable[0] != null) neighbors.add(successorTable[0].getChordId());

		StringBuilder sb = new StringBuilder();
		for (Integer n : neighbors) {
			sb.append(n).append(",");
		}
		if (!sb.isEmpty()) sb.setLength(sb.length() - 1);

		AppConfig.timestampedStandardPrint("[SYS-NEIGHBORS] my_id:" + AppConfig.myServentInfo.getChordId() + " neighbors:" + sb.toString());

		for (ServentInfo node : successorTable) {
			if (node != null && node.getListenerPort() != AppConfig.myServentInfo.getListenerPort()) {
				Pinger.addNode(node.getListenerPort());
			}
		}
		if (predecessorInfo != null) {
			Pinger.addNode(predecessorInfo.getListenerPort());
		}
	}

	/**
	 * Metoda čisti iz valueMap-a sve ključeve koji su izvan opsega (PredecessorOfPredecessor -> Me].
	 * Tačno implementira logiku brisanja viškova kada se ubaci novi čvor.
	 */
	private void clearOutdatedKeys() {
		if (allNodeInfo.size() <= 1) {
			return;
		}

		int myId = AppConfig.myServentInfo.getChordId();
		int predPredId = allNodeInfo.get(allNodeInfo.size() - 2).getChordId();

		for (Integer key : valueMap.keySet()) {
			boolean keep = false;

			if (predPredId < myId) { // Nema prelivanja preko nule
				if (key > predPredId && key <= myId) {
					keep = true;
				}
			} else { // Prelivanje preko nule (overflow)
				if (key > predPredId || key <= myId) {
					keep = true;
				}
			}

			if (!keep) {
				valueMap.remove(key);
			}
		}
	}

	/**
	 * Pomoćna metoda koja šalje izmenjeni par našem neposrednom sledbeniku
	 */
    public void backupToSuccessor(int key, Pair pair) {
		if (successorTable[0] != null && successorTable[0].getListenerPort() != AppConfig.myServentInfo.getListenerPort()) {
			Message rm = new BackupKeyMessage(AppConfig.myServentInfo.getListenerPort(), successorTable[0].getListenerPort(), key, pair.value(), pair.nodeId());
			MessageUtil.sendMessage(rm);
		}
	}

	public void handleKeyBackup(int key, Pair pair) {
		valueMap.put(key, pair);
	}

	/**
	 * The Chord put operation. Stores locally if key is ours, otherwise sends it on.
	 */
	public void putValue(int key, int value,int originalSenderId) {

		if (isKeyMine(key)) {
			if (!valueMap.containsKey(key) && value >0) {
				ChordState.Pair noviPair = new Pair(originalSenderId, value);
				if(!hasToken){
					GrindingRoom.addToJobQueue(new PutJob(key, noviPair));
					AppConfig.timestampedStandardPrint("[MUTEX-REQUEST] item_id:" + key);
					requestToken();
				} else {
					GrindingRoom.work(new PutJob(key, noviPair));
				}
			} else {
				ChordState.Pair currentPair = valueMap.get(key);
				if (currentPair.value() == 0 && value >0) {
					Pair noviPair = new Pair(originalSenderId, value);
					if(!hasToken){
						GrindingRoom.addToJobQueue(new PutJob(key, noviPair));
						AppConfig.timestampedStandardPrint("[MUTEX-REQUEST] item_id:" + key);
						requestToken();
					} else {
						GrindingRoom.work(new PutJob(key, noviPair));
					}
				} else if (currentPair.nodeId() == originalSenderId && currentPair.value() + value>=0) {
					Pair noviPair = new Pair(originalSenderId, currentPair.value() + value);
					if(!hasToken){
						GrindingRoom.addToJobQueue(new PutJob(key, noviPair));
						AppConfig.timestampedStandardPrint("[MUTEX-REQUEST] item_id:" + key);
						requestToken();
					} else {
						GrindingRoom.work(new PutJob(key, noviPair));
					}
				} else {
					AppConfig.timestampedStandardPrint("[MARKET-PUT-FAIL] item_id:" + key + " reason:NOT_ENOUGH_STOCK_TO_REMOVE or reason:NOT_THE_OWNER");
					AppConfig.timestampedErrorPrint("Odbijen upis za kljuc " + key + ". Id " + originalSenderId + " nije vlasnik ili je probao da oduzme vise nego sto ima na stanju");
					ServentInfo nextNode = getNextNodeForKey(originalSenderId);
					Message mes = new InfoMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(), originalSenderId,"Odbijen upis za kljuc " + key + ". Id " + originalSenderId + " nije vlasnik ili je probao da oduzme vise nego sto ima na stanju" );
					MessageUtil.sendMessage(mes);
				}
			}
		} else {
			ServentInfo nextNode = getNextNodeForKey(key);
			PutMessage pm = new PutMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), key, value,originalSenderId);
			MessageUtil.sendMessage(pm);
		}
	}

	/**
	 * The Chord buy operation. Processes locally if key is ours, otherwise sends it on.
	 */
	public void buyValue(int key, int amount, int originalSenderId) {

		if (isKeyMine(key)) {
			if (valueMap.containsKey(key)) {
				Pair currentPair = valueMap.get(key);

				if (currentPair.value() >= amount) {
					Pair noviPair = new Pair(currentPair.nodeId(), currentPair.value() - amount);

					if(!hasToken){
						GrindingRoom.addToJobQueue(new BuyJob(key, noviPair, originalSenderId, amount));
						AppConfig.timestampedStandardPrint("[MUTEX-REQUEST] item_id:" + key);
						requestToken();
					} else {
						GrindingRoom.work(new BuyJob(key, noviPair, originalSenderId, amount));
					}
				} else {
					AppConfig.timestampedStandardPrint("[MARKET-BUY-FAIL] item_id:" + key + " reason:OUT_OF_STOCK");
					ServentInfo nextNode = getNextNodeForKey(originalSenderId);
					Message mes = new InfoMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(), originalSenderId, "Kupovina je neuspesna, pokusali ste da kupite vise nego sto ima na stanju");
					MessageUtil.sendMessage(mes);
				}
			} else {
				AppConfig.timestampedStandardPrint("[MARKET-BUY-FAIL] item_id:" + key + " reason:NO_SUCH_KEY");
				ServentInfo nextNode = getNextNodeForKey(originalSenderId);
				Message mes = new InfoMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(), originalSenderId, "Kupovina je neuspesna, Ne postoji artikal sa tim imenom, tj pod tim klucem");
				MessageUtil.sendMessage(mes);
			}
		} else {
			ServentInfo nextNode = getNextNodeForKey(key);
			Message bm = new BuyMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), key, amount, originalSenderId);
			MessageUtil.sendMessage(bm);
		}
	}

	private void requestToken() {

		int myId = AppConfig.myServentInfo.getChordId();

		int requestNumber = numOfTokenRequests.merge(myId, 1, Integer::sum);

		seenTokenRequestMessages.add(new Pair(myId, requestNumber));

		Set<Integer> portsToNotify = new LinkedHashSet<>();

		for (ServentInfo node : successorTable) {
			if (node != null && node.getChordId() != myId) {
				portsToNotify.add(node.getListenerPort());
			}
		}

		if (predecessorInfo != null && predecessorInfo.getChordId() != myId) {
			portsToNotify.add(predecessorInfo.getListenerPort());
		}

		for (int port : portsToNotify) {
			Message msg = new TokenRequestMessage(AppConfig.myServentInfo.getListenerPort(), port, myId, requestNumber);
			MessageUtil.sendMessage(msg);
		}
	}

	public void subscribe(int subscribeTo, int subscriber){
		ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(subscribeTo);
		Message mes=new SubscribeMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), subscribeTo, subscriber);
		MessageUtil.sendMessage(mes);
	}

	public void notifySubscribers(int subscriber, String poruka){
		ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(subscriber);
		Message mes = new NotifySubscribersMessage(AppConfig.myServentInfo.getListenerPort(),nextNode.getListenerPort(),subscriber,poruka);
		MessageUtil.sendMessage(mes);
	}
	
	/**
	 * The chord get operation. Gets the value locally if key is ours, otherwise asks someone else to give us the value.
	 * @return <ul>
	 *			<li>The value, if we have it</li>
	 *			<li>-1 if we own the key, but there is nothing there</li>
	 *			<li>-2 if we asked someone else</li>
	 *		   </ul>
	 */
	public Pair getValue(int key, int originalSenderId) {
		if (isKeyMine(key)) {
			if (valueMap.containsKey(key)) {
				return valueMap.get(key);
			} else {
				return new Pair(-1,-1);
			}
		}
		
		ServentInfo nextNode = getNextNodeForKey(key);
		AskGetMessage agm = new AskGetMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), key + ":" + originalSenderId);
		MessageUtil.sendMessage(agm);
		
		return new Pair(-2,-2);
	}

	public void printAllValues() {
		if (valueMap.isEmpty()) {
			AppConfig.timestampedStandardPrint("Value map je prazna.");
			return;
		}

		for (Map.Entry<Integer, Pair> entry : valueMap.entrySet()) {
			int key = entry.getKey();
			Pair pair = entry.getValue();

			String tipPodatka = isKeyMine(key) ? "[MOJ ARTIKAL]" : "[BACKUP]";

			AppConfig.timestampedStandardPrint(tipPodatka + " Kljuc: " + key + " -> Vlasnik(ID): " + pair.nodeId() + ", Stanje: " + pair.value());
		}
	}

	public boolean hasSeenDeadNodeMessage(int deadNodeId, int id) {
		for (Pair seen : seenDeadNodeMessages) {
			if (seen.nodeId() == deadNodeId && seen.value() == id) {
				return true;
			}
		}
		return false;
	}

	public boolean hasSeenRequestTokenMessage(int requesterId, int requesterCount) {
		for (Pair seen : seenTokenRequestMessages) {
			if (seen.nodeId() == requesterId && seen.value() == requesterCount) {
				return true;
			}
		}
		return false;
	}

	public void addSeenDeadNodeMessage(int deadNodeId, int id) {
		seenDeadNodeMessages.add(new Pair(deadNodeId, id));
	}

	public void addSeenTokenRequestMessage(int requesterId, int requesterCount) {
		seenTokenRequestMessages.add(new Pair(requesterId, requesterCount));
	}

	public void updateTokenRequest(int requesterId, int requesterCount) {
		numOfTokenRequests.merge(requesterId, requesterCount, Math::max);
	}

	public void removeDeadNode(int deadNodeId) {
		int oldSuccessorId = (successorTable[0] != null) ? successorTable[0].getChordId() : -1;
		boolean predecessorDied = false;

		int oldRangeStart = (predecessorInfo != null) ? predecessorInfo.getChordId() : -1;


		if (predecessorInfo != null && predecessorInfo.getChordId() == deadNodeId) {
			predecessorDied = true;
			if(allNodeInfo.isEmpty()){
				predecessorInfo = null;
			} else {
				predecessorInfo = allNodeInfo.get(allNodeInfo.size()-1);
			}
		}

		allNodeInfo.removeIf(info -> info.getChordId() == deadNodeId);

		if (allNodeInfo.isEmpty()) {
			for (int i = 0; i < chordLevel; i++) {
				successorTable[i] = null;
			}
		} else {
			updateSuccessorTable();
		}

		boolean successorDied = false;
		if (successorTable[0] != null && successorTable[0].getChordId() != oldSuccessorId) {
			successorDied = true;
		}

		if (predecessorDied || successorDied) {
			pushPrimaryDataToSuccessor();
		}

		// Ispis kada se proširi opseg odgovornosti
		if (predecessorDied) {
			int myId = AppConfig.myServentInfo.getChordId();
			int newRangeStart = (predecessorInfo != null) ? predecessorInfo.getChordId() : -1;
			AppConfig.timestampedStandardPrint(
					"Prosiren opseg odgovornosti: (" + oldRangeStart + ", " + myId + "] -> (" + newRangeStart + ", " + myId + "]"
			);

			List<String> takenItems = new ArrayList<>();
			for (Map.Entry<Integer, Pair> entry : valueMap.entrySet()) {
				int key = entry.getKey();
				boolean wasDeadNodesKey = false;
				if (oldRangeStart < deadNodeId) {
					if (key > oldRangeStart && key <= deadNodeId) wasDeadNodesKey = true;
				} else {
					if (key > oldRangeStart || key <= deadNodeId) wasDeadNodesKey = true;
				}
				if (wasDeadNodesKey) {
					takenItems.add(String.valueOf(key));
				}
			}
			AppConfig.timestampedStandardPrint("[SYS-BACKUP-TAKEOVER] node:" + deadNodeId + " item_ids:" + String.join(",", takenItems));
		}

		AppConfig.timestampedStandardPrint("Cvor " + deadNodeId + " uklonjen iz chorda");
	}

	private void pushPrimaryDataToSuccessor() {
		if (successorTable[0] != null && successorTable[0].getListenerPort() != AppConfig.myServentInfo.getListenerPort()) {
			for (Map.Entry<Integer, Pair> entry : valueMap.entrySet()) {
				if (isKeyMine(entry.getKey())) {
					backupToSuccessor(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	public void notifyNeighbors(int deadNodeId, int id, MessageType type) {
		Set<Integer> portsToNotify = new LinkedHashSet<>();

		for (ServentInfo node : successorTable) {
			if (node != null) {
				int nodeId = node.getChordId();
				if (nodeId != AppConfig.myServentInfo.getChordId()) {
					portsToNotify.add(node.getListenerPort());
				}
			}
		}

		if (predecessorInfo != null) {
			int predId = predecessorInfo.getChordId();
			if (predId != AppConfig.myServentInfo.getChordId()) {
				portsToNotify.add(predecessorInfo.getListenerPort());
			}
		}

		for (int port : portsToNotify) {
			if(type==MessageType.DEADNODE){
			Message msg = new DeadNodeMessage(
					AppConfig.myServentInfo.getListenerPort(),
					port, deadNodeId, id);
			MessageUtil.sendMessage(msg);
			}else if(type==MessageType.TOKENREQUEST){
				Message msg = new TokenRequestMessage(
						AppConfig.myServentInfo.getListenerPort(),
						port, deadNodeId, id);
				MessageUtil.sendMessage(msg);
			}

		}
	}

	public void deadNode(int deadNodeId, int id) {
		addSeenDeadNodeMessage(deadNodeId, id);
		notifyNeighbors(deadNodeId, id, MessageType.DEADNODE);
		removeDeadNode(deadNodeId);
	}

	public boolean holingToken(){
		return hasToken;
	}

	public void setToken(boolean has){
		this.hasToken=has;
	}

	public Map<Integer, Integer> getNumOfTokenRequests() {
		return numOfTokenRequests;
	}

	public Map<Integer, Integer> getTokenMap() {
		return tokenMap;
	}

	public void setTokenMap(Map<Integer, Integer> tokenMap) {
		this.tokenMap = tokenMap;
	}
}