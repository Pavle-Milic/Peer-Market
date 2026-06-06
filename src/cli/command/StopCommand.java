package cli.command;

import app.AppConfig;
import app.Pinger;
import cli.CLIParser;
import mutex.GrindingRoom;
import servent.SimpleServentListener;

public class StopCommand implements CLICommand {

	private CLIParser parser;
	private SimpleServentListener listener;
	private Pinger pinger;
	
	public StopCommand(CLIParser parser, SimpleServentListener listener, Pinger pinger) {
		this.parser = parser;
		this.listener = listener;
		this.pinger = pinger;
	}
	
	@Override
	public String commandName() {
		return "stop";
	}

	@Override
	public void execute(String args) {
		AppConfig.timestampedStandardPrint("Stopping...");
		parser.stop();
		listener.stop();
		pinger.stop();
		GrindingRoom.finallyTimeForLeagueOfLegends();
	}

}
