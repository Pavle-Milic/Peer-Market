package servent.message;

import app.ChordState;

import java.util.Map;

public class WelcomeMessage extends BasicMessage {

	private static final long serialVersionUID = -8981406250652693908L;

	private Map<Integer, ChordState.Pair> values;
	
	public WelcomeMessage(int senderPort, int receiverPort, Map<Integer, ChordState.Pair> values) {
		super(MessageType.WELCOME, senderPort, receiverPort);
		
		this.values = values;
	}
	
	public Map<Integer, ChordState.Pair> getValues() {
		return values;
	}
}
