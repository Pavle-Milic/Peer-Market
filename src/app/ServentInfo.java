package app;

import java.io.Serializable;

/**
 * This is an immutable class that holds all the information for a servent.
 */
public class ServentInfo implements Serializable {

	private static final long serialVersionUID = 5304170042791281555L;
	private final String ipAddress;
	private final int listenerPort;
	private final int chordId;
	private int low;
	private int high;
	
	public ServentInfo(String ipAddress, int listenerPort) {
		this.ipAddress = ipAddress;
		this.listenerPort = listenerPort;
		this.chordId = ChordState.chordHash(listenerPort);
		this.low = -1;
		this.high = -1;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public int getListenerPort() {
		return listenerPort;
	}

	public int getChordId() {
		return chordId;
	}

	public void setLow(int low) {
		this.low=low;
	}

	public void setHigh(int high) {
		this.high=high;
	}

	public int getLow(){
		return low;
	}

	public int getHigh(){
		return high;
	}
	
	@Override
	public String toString() {
		return "[" + chordId + "|" + ipAddress + "|" + listenerPort + "|" + low + "|" + high + "]";
	}

}
