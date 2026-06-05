package app;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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

	public record Pair(int port, int value) {}

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
		// Ako smo sami ili nas je samo dvoje u prstenu, čuvamo kompletan prsten podataka
		if (allNodeInfo.size() <= 2) {
			return;
		}

		int myId = AppConfig.myServentInfo.getChordId();
		// Na osnovu allNodeInfo rasporeda, drugi čvor otpozadi je prethodnik našeg prethodnika
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
	private void backupToSuccessor(int key, Pair pair) {
		if (successorTable[0] != null && successorTable[0].getListenerPort() != AppConfig.myServentInfo.getListenerPort()) {
			Message rm = new BackupKeyMessage(AppConfig.myServentInfo.getListenerPort(), successorTable[0].getListenerPort(), key, pair.value(), pair.port());
			MessageUtil.sendMessage(rm);
		}
	}

	public void handleKeyBackup(int key, Pair pair) {
		this.valueMap.put(key, pair);
	}

	/**
	 * The Chord put operation. Stores locally if key is ours, otherwise sends it on.
	 */
	public void putValue(int key, int value,int originalSenderPort) {
		if (isKeyMine(key)) {
			if (!valueMap.containsKey(key) && value >0) {
				Pair noviPair = new Pair(originalSenderPort, value);
				valueMap.put(key, noviPair);
				backupToSuccessor(key, noviPair);
			} else {
				Pair currentPair = valueMap.get(key);
				if (currentPair.value() == 0 && value >0) {
					Pair noviPair = new Pair(originalSenderPort, value);
					valueMap.put(key, noviPair);
					backupToSuccessor(key, noviPair);
				} else if (currentPair.port() == originalSenderPort && currentPair.value() + value>=0) {
					Pair noviPair = new Pair(originalSenderPort, currentPair.value() + value);
					valueMap.put(key, noviPair);
					backupToSuccessor(key, noviPair);
				} else {
					AppConfig.timestampedErrorPrint("Odbijen upis za kljuc " + key + ". Port " + originalSenderPort + " nije vlasnik ili je probao da oduzme vise nego sto ima na stanju");
				}
			}
		} else {
			ServentInfo nextNode = getNextNodeForKey(key);
			PutMessage pm = new PutMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), key, value,originalSenderPort);
			MessageUtil.sendMessage(pm);
		}
	}

	/**
	 * The Chord buy operation. Processes locally if key is ours, otherwise sends it on.
	 */
	public void buyValue(int key, int amount, int originalSenderPort) {
		if (isKeyMine(key)) {
			if (valueMap.containsKey(key)) {
				Pair currentPair = valueMap.get(key);

				if (currentPair.value() >= amount) {
					Pair noviPair = new Pair(currentPair.port, currentPair.value() - amount);
					valueMap.put(key, noviPair);
					backupToSuccessor(key, noviPair);

					if(currentPair.port != AppConfig.myServentInfo.getListenerPort()) {
						Message message = new InfoMessage(AppConfig.myServentInfo.getListenerPort(), currentPair.port, "servant on port " + originalSenderPort + " bought " + amount + " things with the key " + key);
						MessageUtil.sendMessage(message);
					}
					if(originalSenderPort != AppConfig.myServentInfo.getListenerPort()) {
						Message message = new InfoMessage(AppConfig.myServentInfo.getListenerPort(), originalSenderPort, "Buy successful");
						MessageUtil.sendMessage(message);
					}
				} else {
					if(originalSenderPort != AppConfig.myServentInfo.getListenerPort()) {
						Message message = new InfoMessage(AppConfig.myServentInfo.getListenerPort(), originalSenderPort, "Buy denied");
						MessageUtil.sendMessage(message);
					}
				}
			} else {
				if(originalSenderPort != AppConfig.myServentInfo.getListenerPort()) {
					Message message = new InfoMessage(AppConfig.myServentInfo.getListenerPort(), originalSenderPort, "Buy denied no such key");
					MessageUtil.sendMessage(message);
				}
			}
		} else {
			ServentInfo nextNode = getNextNodeForKey(key);
			Message bm = new BuyMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), key, amount, originalSenderPort);
			MessageUtil.sendMessage(bm);
		}
	}
	
	/**
	 * The chord get operation. Gets the value locally if key is ours, otherwise asks someone else to give us the value.
	 * @return <ul>
	 *			<li>The value, if we have it</li>
	 *			<li>-1 if we own the key, but there is nothing there</li>
	 *			<li>-2 if we asked someone else</li>
	 *		   </ul>
	 */
	public Pair getValue(int key) {
		if (isKeyMine(key)) {
			if (valueMap.containsKey(key)) {
				return valueMap.get(key);
			} else {
				return new Pair(-1,-1);
			}
		}
		
		ServentInfo nextNode = getNextNodeForKey(key);
		AskGetMessage agm = new AskGetMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), String.valueOf(key));
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

			AppConfig.timestampedStandardPrint(tipPodatka + " Kljuc: " + key + " -> Vlasnik(Port): " + pair.port() + ", Stanje: " + pair.value());
		}
	}

	public boolean hasSeenDeadNodeMessage(int deadPort, int id) {
		for (Pair seen : seenDeadNodeMessages) {
			if (seen.port() == deadPort && seen.value() == id) {
				return true;
			}
		}
		return false;
	}

	public void addSeenDeadNodeMessage(int deadPort, int id) {
		seenDeadNodeMessages.add(new Pair(deadPort, id));
	}

	public void removeDeadNode(int deadPort) {
		allNodeInfo.removeIf(info -> info.getListenerPort() == deadPort);

		if (predecessorInfo != null && predecessorInfo.getListenerPort() == deadPort) {
			if(allNodeInfo.isEmpty()){
				predecessorInfo = null;
			}
			else {
				predecessorInfo = allNodeInfo.get(allNodeInfo.size()-1);
			}
		}

		if (allNodeInfo.isEmpty()) {
			for (int i = 0; i < chordLevel; i++) {
				successorTable[i] = null;
			}
		} else {
			updateSuccessorTable();
		}

		AppConfig.timestampedStandardPrint("Cvor " + deadPort + " uklonjen iz chorda");
	}

	public void notifyNeighbors(int deadPort, int id) {
		Set<Integer> portsToNotify = new LinkedHashSet<>();

		for (ServentInfo node : successorTable) {
			if (node != null) {
				int port = node.getListenerPort();
				if (port != AppConfig.myServentInfo.getListenerPort() && port != deadPort) {
					portsToNotify.add(port);
				}
			}
		}

		if (predecessorInfo != null) {
			int predPort = predecessorInfo.getListenerPort();
			if (predPort != AppConfig.myServentInfo.getListenerPort() && predPort != deadPort) {
				portsToNotify.add(predPort);
			}
		}

		for (int port : portsToNotify) {
			Message msg = new DeadNodeMessage(
					AppConfig.myServentInfo.getListenerPort(),
					port, deadPort, id);
			MessageUtil.sendMessage(msg);
		}
	}

	public void deadNode(int deadPort, int id) {
		addSeenDeadNodeMessage(deadPort, id);
		notifyNeighbors(deadPort, id);
		removeDeadNode(deadPort);
	}
}