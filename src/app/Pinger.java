package app;

import servent.message.AskPingMessage;
import servent.message.Message;
import servent.message.PingMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class Pinger implements Runnable,Cancellable{

    private static ConcurrentHashMap<Integer,Integer> mapa = new ConcurrentHashMap<>();

    private boolean working = true;

    private static final int TICK = 500;//ms

    private static Random random = new Random();

    @Override
    public void stop() {
        this.working = false;
    }

    @Override
    public void run() {
        while (working) {
            try {
                Thread.sleep(TICK);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            for (Map.Entry<Integer, Integer> entry : mapa.entrySet()) {
                int port = entry.getKey();
                int elapsedTime = entry.getValue();

                if (elapsedTime == 0) {
                    Message mes = new PingMessage(AppConfig.myServentInfo.getListenerPort(), port);
                    MessageUtil.sendMessage(mes);
                }

                if (elapsedTime >= AppConfig.myServentInfo.getLow() && elapsedTime < AppConfig.myServentInfo.getLow() + TICK) {
                    int randomNeighbor = getRandomPortExcept(port);
                    if (randomNeighbor != -1) {
                        Message mes = new AskPingMessage(AppConfig.myServentInfo.getListenerPort(), randomNeighbor,port);
                        MessageUtil.sendMessage(mes);
                    }
                }

                if (elapsedTime >= AppConfig.myServentInfo.getHigh() && elapsedTime <= AppConfig.myServentInfo.getHigh() + TICK) {
                    tellRemoveNode(port);
                }
                else{
                    mapa.put(port, elapsedTime + TICK);
                }
            }
        }
    }

    private int getRandomPortExcept(int suspectPort) {
        List<Integer> availablePorts = new ArrayList<>(mapa.keySet());
        availablePorts.remove(Integer.valueOf(suspectPort));

        if (availablePorts.isEmpty()) {
            return -1;
        }

        return availablePorts.get(random.nextInt(availablePorts.size()));
    }

    public static void nodePonged(int port){
        if (mapa.containsKey(port)) {
            mapa.put(port, -(AppConfig.myServentInfo.getLow()));
        }
    }

    private static void tellRemoveNode(int port){
        mapa.remove(port);
        int id = random.nextInt();
        int deadId=ChordState.chordHash(port);
        AppConfig.chordState.deadNode(deadId,id);
    }

    public static void addNode(int port){
        mapa.putIfAbsent(port, -(AppConfig.myServentInfo.getLow()));
    }

    public static void removeNode(int deadNodeId){
        for(ServentInfo s: AppConfig.chordState.getSuccessorTable())
        {
            if(s != null && s.getChordId()==deadNodeId){
                mapa.remove(s.getListenerPort());
            }
        }
    }

}
