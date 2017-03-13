package marshall.api;

import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;


class VendRequest implements State {
		
	VendStateMachine machine; 
	
	public VendRequest(VendStateMachine machine) {
		this.machine = machine;	
	}
		
	@Override
	public void doAction(MarshallMessage message) {
		this.machine.doVendRequest(message);
	}	
}

class VendSuccess implements State {  
		
	VendStateMachine machine; 
			
	public VendSuccess(VendStateMachine machine) {
		this.machine = machine;
	}
	
	@Override
	public void doAction(MarshallMessage message) {
		this.machine.doVendSuccess(message);
		}	
}

class CompleteSession implements State {
	
	VendStateMachine machine; 
		
	public CompleteSession(VendStateMachine machine) {
		this.machine = machine;
	}
		
	@Override
	public void doAction(MarshallMessage message) {		
		this.machine.doSessionComplete(message);				
	}	
}

class EndSession implements State { 
	
	VendStateMachine machine; 
	
	public EndSession(VendStateMachine machine) {
		this.machine = machine;
	}
	
	@Override
	public void doAction(MarshallMessage message) {
		this.machine.doEndSession(message);	
	}	
}


public class VendStateMachine extends MachineContext{
	
	VendRequest vendRequest; 
	VendSuccess vendSuccess;
	CompleteSession completeSession; 
	EndSession endSession;
	
	VendRequestMessage mdbVendRequest;
	VendSuccessMessage mdbVendSuccess;
	VendFailurelMessage mdbVendFailure;
	VendCancelMessage mdbVendCancel;
	SessionCompleteMessage sessionCompleteMessage;
	MarshallResponeTxMessage marshallResponeTxMessage;
	MarshallMain marshall;
	Timer timer;
	boolean timerOn = false;
		
	public VendStateMachine(MarshallMain marshall_){  //Constructor
				
		endSession =  new EndSession(this);	
		completeSession  = new CompleteSession(this);
		vendSuccess = new VendSuccess(this);
		vendRequest = new VendRequest(this);
		
		this.marshall = marshall_;
		this.mdbVendSuccess = new VendSuccessMessage();
		this.sessionCompleteMessage = new SessionCompleteMessage();		
		this.marshallResponeTxMessage = new MarshallResponeTxMessage();
		this.mdbVendFailure = new VendFailurelMessage();
		this.mdbVendCancel = new VendCancelMessage();
		
		this.setState(vendRequest);
	}
		
	public void initMachine(){		
		this.setState(vendRequest);	
	}
	
	public boolean MarshallMessageType(MarshallMessage message){
		if (message instanceof MarshallProtocolMessage)
			return true;
		else 
			return false;
	}
	
	public void doVendRequest(MarshallMessage message) {
						
		MarshallInternalMessage systemMessage = null;
		MarshallProtocolMessage marshallMessage;
				
		if (MarshallMessageType(message)){	
			marshallMessage = (MarshallProtocolMessage)message;
			
			if (marshallMessage.ackResponse)
				sendAckResponseMessage();	
			
			switch (marshallMessage.getOpCode()){	
				case MdbBeginSessionMessage.MDB_OPCODE:		
					if (marshallMessage.getMdbOpcode() ==  MdbBeginSessionMessage.BEGIN_SESSION){	
						System.out.printf("\nBEGIN SESSION - PLEASE SELECT PRODUCT\n");
						MarshallMessage.beginSession = true;
						MarshallUI.button1.setEnabled(true);
						MarshallUI.logText.insert("\nBEGIN SESSION - PLEASE SELECT PRODUCT\n",0);
						if (! timerOn){
							timer = new Timer();
							timer.schedule(new SelectProductTimeout(this),22000); //timeout for select product
							timerOn = true;
						}						
					}else 
						if (marshallMessage.getMdbOpcode() ==  MdbCancelSessionMessage.CANCEL_SESSION){
							System.out.printf("\nVEND CANCELLED BY AMIT - SENDING SESSION COMPLETE\n");
							MarshallUI.logText.insert("\nVEND CANCELLED BY AMIT\n",0);
							MarshallUI.button1.setEnabled(false);
							timer.cancel();
							timerOn = false;
							marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.VEND_INTERNAL);
							this.setState(completeSession);
							break;
					}
					else if 
						(marshallMessage.getMdbOpcode() == MdbVendDeniedMessage.VEND_DENIED){
						System.out.println("RECEIVED VEND DENIED IN VEND REQUEST !!\n");
						MarshallUI.logText.insert("\nRECEIVED VEND DENIED !!\n",0);
						marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.VEND_INTERNAL);
						this.setState(completeSession);	
					}
					else
						System.out.printf("UNEXPECTED MDB OPCODE IN VEND REQUEST 0x%02X!!\n", marshallMessage.getMdbOpcode());
					break;				
				case MarshallCardDataMessage.TRANSFER_DATA:
					break;
				case MarshallResponeRxMessage.RESPONSE_OPCODE:
					marshall.paringStateMachine.keepAlive.respCounter--;
					//System.out.println("RECEIVED RESPONSE IN VEND REQUEST\n");
					break;
				case MarshallStatusMessage.CODE_STATUS:
					System.out.println("RECEIVED STATUS in VEND REQUEST\n");
					break;
				case MarshallResetMessage.RESET_OPCODE:
					System.out.println("RECEIVED RESET in VEND REQUEST\n");
					break;
				default:
					System.out.printf("UNEXPECTED MARSHALL MESSAGE IN VEND REQUEST  0x%02X!!\n", marshallMessage.getOpCode());
					break;
			} 		
		} 
		else if (! MarshallMessageType(message)){
				systemMessage = (MarshallInternalMessage)message;
				switch (systemMessage.getOpCode()){
					case MarshallInternalMessage.PRODUCT_SELECTED:
						System.out.printf("\nPRODUCT BEEN SELECTED - SENDING VEND REQUEST\n");
						MarshallUI.logText.insert("\nPRODUCT BEEN SELECTED - SENDING VEND REQUEST\n",0);
						timer.cancel();
						timerOn = false;
						mdbVendRequest = new VendRequestMessage();
						marshall.machineSerialPort.sendMarshallMessage(mdbVendRequest);
						this.setState(vendSuccess);
						break;
					case MarshallInternalMessage.VEND_CANCELLED:
						System.out.printf("\nVEND CANCELLED BY MACHINE - SENDING VEND CANCEL RERQUEST\n");
						MarshallUI.logText.insert("\nVEND CANCELLED BY MACHINE - SENDING VEND CANCEL RERQUEST\n",0);
						MarshallUI.button1.setEnabled(false);
						timer.cancel();
						timerOn = false;
						marshall.machineSerialPort.sendMarshallMessage(mdbVendCancel);					
						break;
					default:
						System.out.printf("\nUNEXPECTED INTERNAL MESSAGE in doVendRequest\n");  	 
				}
		}
	}
	
	public void doVendSuccess(MarshallMessage message){
		
		MarshallInternalMessage systemMessage;
		MarshallProtocolMessage marshallMessage;
		
		if (message instanceof MarshallProtocolMessage){
			marshallMessage = (MarshallProtocolMessage)message;
			
			if (marshallMessage.ackResponse)
				sendAckResponseMessage();
			
			switch (marshallMessage.getOpCode()){
				case MdbVendApproveMessage.MDB_OPCODE: 
					if (marshallMessage.getMdbOpcode() == MdbVendApproveMessage.VEND_APPROVED){
						MarshallUI.button2.setEnabled(true);
						System.out.println("** RECEIVED VEND APPROVED - PLEASE TAKE PRODUCT **\n");
						MarshallUI.logText.insert("\nRECEIVED VEND APPROVED - PLEASE TAKE PRODUCT\n",0);
						if (! timerOn){
							timer = new Timer();
							timer.schedule(new TakeProductTimeout(this),10000);
							timerOn = true;
						}
					}else if 
						(marshallMessage.getMdbOpcode() == MdbVendDeniedMessage.VEND_DENIED){
						System.out.println("RECEIVED VEND DENIED !!\n");
						MarshallUI.logText.insert("\nRECEIVED VEND DENIED !!\n",0);
						marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.VEND_INTERNAL);
						this.setState(completeSession);	
					}
					else
						System.out.printf("UNEXPECTED MDB OPCODE IN VEND SUCCESS 0x%02X Ignoring ...\n", marshallMessage.getMdbOpcode());						
					break;						
				case MarshallCardDataMessage.TRANSFER_DATA:
					System.out.println("CARD DATA RECEIVED\n");
					break;
				case MarshallResponeRxMessage.RESPONSE_OPCODE:
					marshall.paringStateMachine.keepAlive.respCounter--;
					break;
				case MarshallResetMessage.RESET_OPCODE:
					System.out.println("RECEIVED RESET in VEND SUCCESS\n");
					break;
				default:
					System.out.printf("UNEXPECTED MARSHALL MESSAGE IN VEND SUCCESS 0x%02X!!\n", marshallMessage.getOpCode());
					break;
			}	
		}else  
			if (! MarshallMessageType(message)){
				systemMessage = (MarshallInternalMessage)message;
				switch (systemMessage.getOpCode()){
					case MarshallInternalMessage.PRODUCT_TAKEN:
						System.out.printf("\nPRODUCT BEEN TAKEN - VEND SUCCESS\n");
						MarshallUI.logText.insert("\nPRODUCT BEEN TAKEN - VEND SUCCESS\n",0);
						timer.cancel();
						timerOn = false;
						marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.VEND_INTERNAL);	
						marshall.machineSerialPort.sendMarshallMessage(mdbVendSuccess);
						//byte packetId = marshallMessage.counter;
						this.setState(completeSession);
						break;
					case MarshallInternalMessage.VEND_CANCELLED:
						System.out.printf("\nVEND CANCELLED BY USER - VEND CANCELL\n");
						MarshallUI.logText.insert("\nVEND CANCELLED BY USER - VEND CANCELL\n", 0);
						MarshallUI.button2.setEnabled(false);
						if (timerOn){
							timer.cancel();
							timerOn = false;
						}
						marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.VEND_INTERNAL);	
						marshall.machineSerialPort.sendMarshallMessage(mdbVendFailure); //waiting for vend denied from AMIT
						//byte packetId = marshallMessage.counter;	
						this.setState(completeSession);
						break;
					case MarshallInternalMessage.PRODUCT_FAILURE:
						System.out.printf("\nPRODUCT NOT TAKEN - VEND FAILURE\n");
						MarshallUI.logText.insert("\nPRODUCT NOT TAKEN/VEND CANCELLED BY USER - VEND FAILURE\n", 0);
						if (timerOn){
							timer.cancel();
							timerOn = false;
						}
						marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.VEND_INTERNAL);	
						marshall.machineSerialPort.sendMarshallMessage(mdbVendFailure);
						//byte packetId = marshallMessage.counter;
						this.setState(completeSession);
						break;
					default:
						System.out.printf("UN EXPECTED INTERNAL MESSAGE in doVendSuccess\n");
				}	
			}else
				System.out.printf("UN DEFINED INTERNAL MESSAGE in doVendSuccess\n"); 
			
	}
		
	
	public void doSessionComplete(MarshallMessage message) {
		
		MarshallInternalMessage systemMessage;
		
		if (message instanceof MarshallProtocolMessage){
			if (((MarshallProtocolMessage) message).opcode == MarshallResponeRxMessage.RESPONSE_OPCODE){
				System.out.println("RECEIVED RESPONSE in SESSION COMPLETE\n");	
				marshall.paringStateMachine.keepAlive.respCounter--;
			}
			else if
				(((MarshallProtocolMessage) message).opcode == MarshallStatusMessage.CODE_STATUS)
				;
			else
				System.out.println("UNEXPECTED MARSHALL MESSAGE IN SESSION COMPLETE !!\n");
		}	
		
		if (message  instanceof MarshallInternalMessage) {			
			systemMessage = (MarshallInternalMessage)message;
			
			if (systemMessage.getOpCode() == systemMessage.VEND_INTERNAL){
				System.out.printf("\nSEND SESSION COMPLETE\n");	
				MarshallUI.logText.insert("\nSEND SESSION COMPLETE\n", 0);
				marshall.machineSerialPort.sendMarshallMessage(sessionCompleteMessage);
				//byte packetId = sessionCompleteMessage.counter;
				if (! timerOn){
					timer = new Timer();
					this.timer.schedule(new EndSessionTimeout(this),5000);
					timerOn = true;
				}
				this.setState(endSession);		
			} else 
				System.out.println("UNEXPECTED SYSTEM MESSAGE IN SESSION COMPLETE !!\n");			
		}
	}
	
	public void doEndSession(MarshallMessage message) {
		
		MarshallInternalMessage systemMessage;
		MarshallProtocolMessage marshallMessage;
		
		if (MarshallMessageType(message)){	
			marshallMessage = (MarshallProtocolMessage)message;
					
			if (marshallMessage.ackResponse)
				sendAckResponseMessage();
			
			switch (marshallMessage.getOpCode()){
				case MdbEndSessionMessage.MDB_OPCODE: 
					if (marshallMessage.getMdbOpcode() == MdbEndSessionMessage.END_SESSION){
						System.out.println("RECEIVED END_SESSION\n");
						MarshallMessage.beginSession = false;
						MarshallUI.logText.insert("\nRECEIVED END_SESSION\n", 0);
						MarshallUI.logText.insert("\n----------------------------\n", 0);
						marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.IDLE_MODE);		
						timer.cancel();
						timerOn = false;
					}else
						System.out.printf("UNEXPECTED MDB OPCODE IN END_SESSION 0x%02X!!\n", marshallMessage.getMdbOpcode());						
					break;						
				case MarshallResponeRxMessage.RESPONSE_OPCODE:
					marshall.paringStateMachine.keepAlive.respCounter--;
					break;
				case MarshallStatusMessage.CODE_STATUS:
					break;
				default:
					System.out.printf("UNEXPECTED MARSHALL MESSAGE IN END_SESSION 0x%02X!!\n", marshallMessage.getOpCode());
					break;
			}								
		}	
		else   
			System.out.println("UNEXPECTED SYSTEM MESSAGE IN END SESSION !!\n");
	}
	
	public void sendAckResponseMessage(){	
		marshall.machineSerialPort.sendMarshallMessage(marshallResponeTxMessage);				
	}
	
}//class ends

class SelectProductTimeout extends TimerTask{
	
	VendStateMachine machine;
	MarshallInternalMessage systemMessage;
	
	public SelectProductTimeout(VendStateMachine machine){
		this.machine = machine;			
	}

	@Override
	public void run() {
		State state;	
		state = machine.getState();
		if (state == machine.vendRequest){
			System.out.printf("\nTIMEOUT FOR PRODUCT SELECTION EXPIRED\n");
			MarshallUI.logText.insert("\nTIMEOUT FOR PRODUCT SELECTION EXPIRED\n", 0);
			machine.marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.VEND_INTERNAL);
			MarshallUI.button1.setEnabled(false);
			machine.setState(machine.completeSession);
			machine.timer.cancel();
			machine.timerOn = false;
		}
	}
}
	
class TakeProductTimeout extends TimerTask{
	
	VendStateMachine machine;
	MarshallInternalMessage systemMessage;
	
	public TakeProductTimeout(VendStateMachine machine){
		this.machine = machine;			
	}

	@Override
	public void run() {
		State state;	
		state = machine.getState();
		if (state == machine.vendSuccess){
			System.out.printf("\nTIMEOUT FOR TAKING PRODUCT EXPIRED");
			MarshallUI.logText.insert("\nTIMEOUT FOR TAKING PRODUCT EXPIRED\n", 0);
			machine.marshall.machineSerialPort.sendInternalMessage(MarshallInternalMessage.PRODUCT_FAILURE);
			MarshallUI.button2.setEnabled(false);
			machine.timer.cancel();
			machine.timerOn = false;
		}
	}
}

class EndSessionTimeout extends TimerTask{
	
	VendStateMachine machine;
	
	public EndSessionTimeout(VendStateMachine machine){
		this.machine = machine;	
	}

	@Override
	public void run() {
		State state;	
		state = machine.getState();
		if (state != machine.endSession){
			System.out.printf("\nTIMEOUT FOR END SESSION EXPIRED");
			MarshallUI.logText.insert("\nTIMEOUT FOR END SESSION EXPIRED\n", 0);
			machine.setState(machine.endSession);
			machine.timer.cancel();
			machine.timerOn = false;
		}
	}
}





