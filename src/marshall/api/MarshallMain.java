package marshall.api;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import marshall.api.MarshallUI;
import marshall.api.MarshallHal.*;

public class MarshallMain {
	
	BlockingQueue<MarshallMessage> marshallQueue;
	MarshallHal machineSerialPort;
	StartUpStateMachine paringStateMachine;
	MachineContext  machineContext;
	VendStateMachine vendStateMachine;
	

	public MarshallMain(){
		
		this.marshallQueue = new ArrayBlockingQueue<MarshallMessage>(4);
		this.machineSerialPort = new MarshallHal(marshallQueue);
		
		paringStateMachine = new StartUpStateMachine(this);
		vendStateMachine = new VendStateMachine(this);
		
		machineContext = paringStateMachine;
		
		MarshallUI.createAndShowGUI(this);
		
	}
			
	public  void initComm(){
		
		while (true){				
			if (MarshallHal.getAvailableSerialPorts()){	
				if (MarshallUI.selectedPort != null){
					if (!machineSerialPort.open()){
						System.out.printf("Open Serial Port Failed !!\n");				
						goToRest();	
					} else {
						System.out.println("Open Serial Port OK !!\n");
						machineSerialPort.setPortParams();	
						return;
						}
				}else{
					System.out.println("Please Select Serial Port!!\n");
					goToRest();	
					}
			} else {			
				System.out.println("No Available Serial Ports!!\n");
				goToRest();	
			}
		}
			
	}
	
	private void goToRest(){
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}
		
	public void initMachine(){
		
		initComm();		
		machineSerialPort.initThreads();
		
		MarshallMessage message;
		message = new MarshallInternalMessage(MarshallInternalMessage.INIT_PAIRING);
		try {
			this.marshallQueue.put(message);
		} catch (InterruptedException e) {		
			e.printStackTrace();
		}
	}
	
	
	public void run(){
		
		initMachine();
		
		MarshallMessage message;
		
		while (true){
			try {
				message = this.marshallQueue.poll(1200,TimeUnit.MILLISECONDS);
				if (message != null){
			
					if  (message  instanceof MarshallInternalMessage) {
						MarshallInternalMessage intMessage;
						intMessage = (MarshallInternalMessage)message;
						
						switch (intMessage.getOpCode()){
							case MarshallInternalMessage.INIT_PAIRING:
								this.machineContext = paringStateMachine;
								break;
							case MarshallInternalMessage.RESTART:
								this.machineContext = paringStateMachine;
								this.machineContext.setState(paringStateMachine.initComm);
								machineContext.getState().doAction(message);
								break;	
							case MarshallInternalMessage.KEEP_ALIVE_MODE:
								this.machineContext = paringStateMachine;
								this.machineContext.setState(paringStateMachine.keepAliveMode);
								machineContext.getState().doAction(message);
								break;
							case MarshallInternalMessage.IDLE_MODE:
								this.machineContext = paringStateMachine;
								this.machineContext.setState(paringStateMachine.idle);					
								break;						
							case MarshallInternalMessage.VEND_MODE:
								this.machineContext = vendStateMachine;
								this.machineContext.setState(vendStateMachine.vendRequest);					
								break;
							case MarshallInternalMessage.VEND_INTERNAL:
								machineContext.getState().doAction(message);
								break;
							case MarshallInternalMessage.PRODUCT_SELECTED:
								if (machineContext.getState() == vendStateMachine.vendRequest)
									machineContext.doAction(message);
								break;
							case MarshallInternalMessage.VEND_CANCELLED:
								if ((machineContext.getState() == vendStateMachine.vendRequest)|| (machineContext.getState() == vendStateMachine.vendSuccess))
									machineContext.doAction(message);
								break;					
							case MarshallInternalMessage.PRODUCT_TAKEN:
								if (machineContext.getState() == vendStateMachine.vendSuccess)
									machineContext.doAction(message);
								break;
							case MarshallInternalMessage.PRODUCT_FAILURE:
								if (machineContext.getState() == vendStateMachine.vendSuccess)
									machineContext.doAction(message);
								break;
							default:
								System.out.println("UNSUPPORTED MARSHALL Internall MESSAGE in RUN !!\n");						
						}
						
					}else {	
						machineContext.getState().doAction(message);				
					}
				}else
					System.out.println("Communication Lost with AMIT\n"); 
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}		
		}		
	}	

	public static void main(final String[] args){	
		
		MarshallMain marshall = new MarshallMain();	
		marshall.run(); 
	}

}