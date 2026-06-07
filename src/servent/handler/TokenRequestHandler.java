package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import mutex.GrindingRoom;
import mutex.Stringifyer;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TokenMessage;
import servent.message.TokenRequestMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;

public class TokenRequestHandler implements MessageHandler {
    private TokenRequestMessage clientMessage;

    public TokenRequestHandler(TokenRequestMessage clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.TOKENREQUEST) {
            AppConfig.timestampedErrorPrint("Token request handler got a message that is not TOKENREQUEST");
            return;
        }

        int reqNodeId = clientMessage.getNodeId();
        int reqCount  = clientMessage.getNumReqests();

        if (!AppConfig.chordState.hasSeenRequestTokenMessage(reqNodeId, reqCount)) {

            AppConfig.chordState.addSeenTokenRequestMessage(reqNodeId, reqCount);
            AppConfig.chordState.updateTokenRequest(reqNodeId, reqCount);
            AppConfig.chordState.notifyNeighbors(reqNodeId, reqCount, MessageType.TOKENREQUEST);

            if (AppConfig.chordState.holingToken()) {
                while (GrindingRoom.isWorking()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                synchronized (AppConfig.chordState) {
                    if (AppConfig.chordState.holingToken()) {
                        int tokenMapValue = AppConfig.chordState.getTokenMap().getOrDefault(reqNodeId, 0);

                        if (reqCount > tokenMapValue) {
                            AppConfig.chordState.setToken(false);
                            ServentInfo serventInfo = AppConfig.chordState.getNextNodeForKey(reqNodeId);
                            String strMap = Stringifyer.stringifyMap(AppConfig.chordState.getTokenMap());
                            String strList = Stringifyer.stringifyList(new ArrayList<>());
                            Message msg = new TokenMessage(AppConfig.myServentInfo.getListenerPort(), serventInfo.getListenerPort(), strMap, strList, reqNodeId);
                            MessageUtil.sendMessage(msg);
                        }
                    }
                }
            }
        }
    }
}