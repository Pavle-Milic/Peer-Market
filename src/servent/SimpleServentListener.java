package servent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.AppConfig;
import app.Cancellable;
import app.Pinger;
import cli.CLIParser;
import servent.handler.*;
import servent.message.*;
import servent.message.util.MessageUtil;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;

	private Pinger pinger;
	private CLIParser cliParser;
	
	public SimpleServentListener( Pinger pinger,CLIParser cliParser) {
		this.pinger = pinger;
		this.cliParser = cliParser;
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
			System.exit(0);
		}
		
		
		while (working) {
			try {
				Message clientMessage;
				
				Socket clientSocket = listenerSocket.accept();
				
				//GOT A MESSAGE! <3
				clientMessage = MessageUtil.readMessage(clientSocket);
				
				MessageHandler messageHandler = new NullHandler(clientMessage);
				
				/*
				 * Each message type has it's own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */
				switch (clientMessage.getMessageType()) {
				case NEW_NODE:
					messageHandler = new NewNodeHandler(clientMessage);
					break;
				case WELCOME:
					messageHandler = new WelcomeHandler(clientMessage);
					break;
				case SORRY:
					messageHandler = new SorryHandler(clientMessage);
					break;
				case UPDATE:
					messageHandler = new UpdateHandler(clientMessage);
					break;
				case PUT:
					messageHandler = new PutHandler(clientMessage);
					break;
				case ASK_GET:
					messageHandler = new AskGetHandler(clientMessage);
					break;
				case TELL_GET:
					messageHandler = new TellGetHandler(clientMessage);
					break;
					case SUBSCRIBE:
						messageHandler = new SubscribeHandler(clientMessage);
						break;
					case BUY:
						messageHandler = new BuyHandler(clientMessage);
						break;
					case BACKUPKEY:
						messageHandler = new BackupKeyHandler(clientMessage);
						break;
					case PING:
						messageHandler = new PingHandler(clientMessage);
						break;
					case ASKPING:
						messageHandler = new AskPingHandler((AskPingMessage) clientMessage);
						break;
					case PONG:
						messageHandler = new PongHandler(clientMessage);
						break;
					case ASKPONG:
						messageHandler = new AskPongHandler((AskPongMessage) clientMessage);
						break;
					case DEADNODE:
						messageHandler = new DeadNodeHandler((DeadNodeMessage) clientMessage,pinger,cliParser,this);
						break;
					case INFO:
						break;
					case NOTIFYSUBSCRIBERS:
						break;
				case POISON:
					break;
				}
				
				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}

}
