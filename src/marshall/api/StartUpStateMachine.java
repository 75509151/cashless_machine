package marshall.api;


import java.util.Timer;
import java.util.TimerTask;

class InitComm implements State {
	
	StartUpStateMachine machine; 
	
	public InitComm(StartUpStateMachine machine_){
		this.machine = machine_;
	}
		
	@Override
	public void doAction(MarshallMessage message) {
		this.machine.doInitComm(message);
	}	
}

class WaitForReset implements State {

	StartUpStateMachine machine; 
	
	public WaitForReset(StartUpStateMachine machine_){
		this.machine = machine_;
	}
		
	@Override
	public void doAction(MarshallMessage message) {
		this.machine.doWaitForReset(message);
	}	
}

class WaitForConfig implements State {
	
	StartUpStateMachine machine; 
	
	public WaitForConfig(StartUpStateMachine machine_) {
		this.machine = machine_;
	}
		
	@Override
	public void doAction(MarshallMessage message) {
		this.machine.doWaitForConfig(message);
	}	
}

class KeepAliveMode implements State {
	
	StartUpStateMachine machine; 
	
	public KeepAliveMode(StartUpStateMachine machine_) {
		this.machine = machine_;
	}
		
	@Override
	public void doAction(MarshallMessage message) {
		this.machine.doKeepAlive(message);
	}	
}

class Idle implements State {
	
	StartUpStateMachine machine; 
	
	public Idle(StartUpStateMachine machine_) {
		this.machine = machine_;
	}
		
	@Override
	public void doAction(MarshallMessage message) {
		this.machine.doIdle(message);
	}	
}

public class StartUpStateMachine extends MachineContext{
	
	InitComm initComm;
	WaitForReset waitForReset;
	WaitForConfig waitForConfig; 
	KeepAliveMode keepAliveMode;
	Idle	idle;
	MarshallMain marshall;
	KeepAlive keepAlive;
	
	MarshallFirmwareInfoMessage firmwareInfoMessage;
	MarshallKeepAliveMessage keepAliveMessage;
	ReaderEnableMessage readerEnableMessage;
	ReaderDisableMessage readerDisableMessage;
	Timer kaTimer;
	boolean isTimerOn;
	
	public StartUpStateMachine(MarshallMain marshall_){  //Constructor
				
		this.marshall = marshall_;
		this.initComm =  new InitComm(this);
		this.waitForReset  = new WaitForReset(this);
		this.waitForConfig = new WaitForConfig(this);
		this.keepAliveMode = new KeepAliveMode(this);
		this.idle = new Idle(this);
		
		firmwareInfoMessage = new MarshallFirmwareInfoMessage();
		keepAliveMessage = new MarshallKeepAliveMessage();	
		readerEnableMessage = new ReaderEnableMessage();
		readerDisableMessage = new ReaderDisableMessage();
		
		this.setState(waitForReset);
		isTimerOn = false;
	}
		
	public void doInitComm(MarshallMessage message) {	
	
		if (isTimerOn){
			kaTimer.cancel();
			isTimerOn = false;
		}			
		marshall.initComm();
		this.setState(waitForReset);	
	}

	public void doWaitForReset(MarshallMessage message) {
					
		if (message instanceof MarshallProtocolMessage){
			MarshallProtocolMessage marshallMessage;
			marshallMessage = (MarshallProtocolMessage)message;
			
			switch (marshallMessage.getOpCode()){
				case MarshallResetMessage.RESET_OPCODE:	
					marshall.machineSerialPort.sendMarshallMessage(firmwareInfoMessage);
					this.setState(waitForConfig);
					break;
				default:
					System.out.println("UN EXPECTED MESSAGE IN WaitForReset !!\n");
					break;
			} 			
		}	
		else  
			System.out.printf("UN EXPECTED INTERNAL MESSAGE in WaitForReset \n");			
	}
	
	public void doWaitForConfig(MarshallMessage message) {
		
		if (message instanceof MarshallProtocolMessage){
			MarshallProtocolMessage marshallMessage;
			marshallMessage = (MarshallProtocolMessage)message;
			
			switch (marshallMessage.getOpCode())	{
				case MarshallConfigMessage.CONFIG_OPCODE:	
					this.setState(keepAliveMode);
					marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.KEEP_ALIVE_MODE);				
					break;
				default:
					System.out.printf("UN EXPECTED MARSHALL MESSAGE IN WaitForConfig 0x%02X\n",marshallMessage.getOpCode());
					break;
			} 				
		}	
		else  
			System.out.printf("UN EXPECTED INTERNAL MESSAGE in WaitForConfig x%02X\n");
		
	}
	
	public void doKeepAlive(MarshallMessage message) {
		
		if (message instanceof MarshallProtocolMessage){		
			System.out.printf("UN EXPECTED MARSHALL MESSAGE IN keepAlive\n");			
		}	
		else  if (message instanceof MarshallInternalMessage) {
			MarshallInternalMessage intMessage;
			intMessage = (MarshallInternalMessage)message;
			
			switch (intMessage.getOpCode()){			
				case MarshallInternalMessage.KEEP_ALIVE_MODE:
					marshall.machineSerialPort.sendMarshallMessage(readerDisableMessage);
					marshall.machineSerialPort.sendMarshallMessage(readerEnableMessage);
					if (!isTimerOn){
						kaTimer = new Timer();
						keepAlive = new KeepAlive(keepAliveMessage, marshall);
						kaTimer.schedule(keepAlive,500, 1000);		
						isTimerOn = true;
					}
					this.setState(idle);				
					break;
				default:
					System.out.printf("UN EXPECTED INTERNAL MESSAGE in KEEPALIVE x%02X\n",intMessage.getOpCode());
					break;
				} 
			}
		else
			System.out.printf("UNSUPPORTED MARSHALL MESSAGE\n");
		
	}
	
	public void doIdle(MarshallMessage message) {
		
		MarshallInternalMessage systemMessage;
		
		if (message instanceof MarshallProtocolMessage){
			MarshallProtocolMessage marshallMessage;
			marshallMessage = (MarshallProtocolMessage)message;
			
			switch (marshallMessage.getOpCode()){			
				case MarshallResponeRxMessage.RESPONSE_OPCODE :
					//System.out.printf("RECEIVED RESPONSE MESSAGE in doIdle\n");
					keepAlive.respCounter--;				
					break;
				case MarshallResetMessage.RESET_OPCODE:	
					System.out.printf("RECEIVED RESET MESSAGE in doIdle ??\n");
					break;
				case MdbBeginSessionMessage.MDB_OPCODE:
					if (marshallMessage.ackResponse){
						marshall.vendStateMachine.sendAckResponseMessage();	
						marshallMessage.ackResponse = false;
					}
					marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.VEND_MODE);	
					marshall.machineSerialPort.sendMdbInternalMessage(marshallMessage);	
					break;
				default:
					System.out.printf("UNSUPPORTED MARSHALL MESSAGE IN doIdle StartUp 0x%02X\n",marshallMessage.getOpCode());
					break;
			} 				
		}	
		else  
			System.out.printf("UN EXPECTED INTERNAL MESSAGE in doIdle\n");
		
	}
	

	class KeepAlive extends TimerTask {
	
		MarshallKeepAliveMessage keepAlive;  
		MarshallMain marshall;
		int respCounter = 0;
		
		public KeepAlive(MarshallKeepAliveMessage message,MarshallMain marshall_){
			keepAlive = message;
			marshall = marshall_;			
		}
	
		public void run(){
			marshall.machineSerialPort.sendMarshallMessage(keepAlive);
			respCounter++;
			if (this.respCounter > 1){
				marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.RESTART);
				this.respCounter = 0;
				marshall.machineSerialPort.close();
			}
		}
    
	}	

}


