package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TellGetMessage;
import servent.message.util.MessageUtil;

public class TellGetHandler implements MessageHandler {

	private Message clientMessage;
	
	public TellGetHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}
	
	@Override
	public void run() {
		if (clientMessage.getMessageType() == MessageType.TELL_GET) {
			String parts[] = clientMessage.getMessageText().split(":");
			
			if (parts.length == 3) {
				try {
					int key = Integer.parseInt(parts[0]);
					int originalSednerId= Integer.parseInt(parts[1]);
					int value = Integer.parseInt(parts[2]);
					if(AppConfig.chordState.isKeyMine(originalSednerId)){
						if (value == -1) {
							AppConfig.timestampedStandardPrint("No such key: " + key);
						} else {
							AppConfig.timestampedStandardPrint("Search je nasao stanje "+ value+ " na kljucu " + key);
						}
					}
					else{
						ServentInfo next = AppConfig.chordState.getNextNodeForKey(originalSednerId);
						Message m = new TellGetMessage(AppConfig.myServentInfo.getListenerPort(), next.getListenerPort(), key,originalSednerId, value);
						MessageUtil.sendMessage(m);
					}

				} catch (NumberFormatException e) {
					AppConfig.timestampedErrorPrint("Got TELL_GET message with bad text: " + clientMessage.getMessageText());
				}
			} else {
				AppConfig.timestampedErrorPrint("Got TELL_GET message with bad text: " + clientMessage.getMessageText());
			}
		} else {
			AppConfig.timestampedErrorPrint("Tell get handler got a message that is not TELL_GET");
		}
	}

}
