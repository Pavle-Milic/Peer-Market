package servent.handler;

import app.AppConfig;
import app.ServentInfo;
import mutex.GrindingRoom;
import mutex.Stringifyer;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TokenMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.Map;

public class TokenHandler implements MessageHandler {
    private TokenMessage clientMessage;

    public TokenHandler(TokenMessage clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.TOKEN) {
            AppConfig.timestampedErrorPrint("Token handler got a message that is not TOKEN");
            return;
        }

        ArrayList<Integer> red = Stringifyer.destringifyList(clientMessage.getList());
        Map<Integer, Integer> mapaTokena = Stringifyer.destringifyMap(clientMessage.getMapa());

        if (AppConfig.chordState.isKeyMine(clientMessage.getTargetId())) {
            AppConfig.chordState.setToken(true);

            GrindingRoom.workWorkWorkWorkWorkWork();

            while (GrindingRoom.isWorking()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            int myId = AppConfig.myServentInfo.getChordId();

            int myCurrentRequestNum = AppConfig.chordState.getNumOfTokenRequests().getOrDefault(myId, 0);
            mapaTokena.put(myId, myCurrentRequestNum);
            AppConfig.chordState.setTokenMap(mapaTokena);

            Map<Integer, Integer> numOfTokenRequests = AppConfig.chordState.getNumOfTokenRequests();

            for (Map.Entry<Integer, Integer> entry : numOfTokenRequests.entrySet()) {
                int nodeId = entry.getKey();
                int requestCount = entry.getValue();

                int tokenCount = mapaTokena.getOrDefault(nodeId, 0);

                if (requestCount > tokenCount) {
                    int lastIndex = red.lastIndexOf(nodeId);
                    boolean jePreblizu = (lastIndex != -1) && ((red.size() - lastIndex) <= 10);
                    if (!jePreblizu) {
                        red.add(nodeId);
                    }
                }
            }

            if (!red.isEmpty()) {
                AppConfig.chordState.setToken(false);
                int nextNodeId = red.remove(0);
                ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(nextNodeId);
                String strMap = Stringifyer.stringifyMap(mapaTokena);
                String strList = Stringifyer.stringifyList(red);
                Message msg = new TokenMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), strMap, strList, nextNodeId);
                MessageUtil.sendMessage(msg);
            }
        } else {
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(clientMessage.getTargetId());
            Message msg = new TokenMessage(AppConfig.myServentInfo.getListenerPort(), nextNode.getListenerPort(), clientMessage.getMapa(), clientMessage.getList(), clientMessage.getTargetId());
            MessageUtil.sendMessage(msg);
        }
    }
}