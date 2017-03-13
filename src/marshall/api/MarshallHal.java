package marshall.api;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;








import javax.swing.JComboBox;



//import jssc.SerialPortList;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;


public class MarshallHal {
	
	static SerialPort serialPort;
	public static String[] portNames = null;
	
	Thread rxThread;
	Thread txThread;
	BlockingQueue<MarshallMessage> marshallQueue;
	serialWrite Tx;
	serialRead Rx;
	
	//constructor
	public MarshallHal(BlockingQueue<MarshallMessage> marshallQueue_){
		marshallQueue = marshallQueue_;		
		
	}
	
	public static boolean getAvailableSerialPorts() {
		portNames = SerialPortList.getPortNames();
		if (portNames != null)
			return true;
		else 
			return false;
	
	}
	
	public boolean open() {					
		serialPort = new SerialPort(MarshallUI.selectedPort);
		try {     
	         if (serialPort.openPort()){
	        	MarshallUI.selectedPort = null;
	          	return true;
	         }else
	        	 return false;
	        }catch (SerialPortException ex) {
				return false;
	        }
		}
		
	
	
	public boolean isOpen() {	
		if (serialPort.isOpened())
			return true;
		  else 
		    return false;
	}		
	
	public void setPortParams(){
		try {
			serialPort.setParams(
					serialPort.BAUDRATE_115200,
					serialPort.DATABITS_8,
					serialPort.STOPBITS_1, 
					serialPort.PARITY_NONE);
		} catch (SerialPortException e) {
			
			e.printStackTrace();
		}
	}
	
	public void initThreads() {
       
		//starting Receiver Thread
    	Rx = new serialRead(this);
    	rxThread = new Thread(Rx);
    	rxThread.start();
    		
    	//starting Transmitter Thread
    	Tx = new serialWrite(this);
    	txThread = new Thread(Tx);
    	txThread.start();   			
	}
	
	
	public void close(){
		try {
			serialPort.closePort();
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}
	

	 class serialRead implements Runnable {

		Timer rxTimer = new Timer();
		byte[] rxBuffer = null;
		
		MarshallHal ma;
		MarshallProtocolMessage message;
		
		public serialRead(MarshallHal ma_) { 
			ma = ma_;		
   		}

		public void run(){

			while(true){  				
			try{
				TimeUnit.MILLISECONDS.sleep(50);
				if (ma.isOpen()){
					rxBuffer = serialPort.readBytes();
					
					if ((rxBuffer != null) && (rxBuffer.length >= 11)){	
						int totalLength = rxBuffer.length;
						int offset = 0;
						short encLength = 0;
											
						while (offset < totalLength){
							encLength = getEncodedLength(offset);	
							if (encLength == 0) 
								break;
							if (totalLength - offset < 2)
								break;
							if (totalLength < encLength + 2){						
								System.out.printf("Encoded Length %02d\n", encLength);
								System.out.printf("Total Length  %02d\n", totalLength);
								break;
							}
							byte[] rawMessage = new byte[encLength + 2];		
							System.arraycopy(rxBuffer, offset , rawMessage, 0 , encLength + 2);
							offset += (encLength + 2);
							
							message = MarshallProtocolMessage.factory(rawMessage);
							if (message != null)
								ma.marshallQueue.put(message); //message
							else
								System.out.printf("MarshallProtocolMessage Null\n");	
						}
					}
				}
				}catch (SerialPortException e) {
				 		System.out.println(e);  
				 	}catch (InterruptedException e) {				
						e.printStackTrace();
					}
			}	
			
		}
		private short getEncodedLength(int offset) {
			
			short length = 0;
			byte[]encodedLength = new byte[2];
						
			encodedLength[0] = rxBuffer[offset];  
			encodedLength[1] = rxBuffer[offset + 1];			
			length = utils.byteArrToShort(encodedLength, 0);	
			return length;
		}
	 }
	
	public class serialWrite implements Runnable {
		
		MarshallHal ma;
		
		BlockingQueue<MarshallProtocolMessage> marshallTx;
			
		public serialWrite(MarshallHal ma_) {  
			ma = ma_;
			this.marshallTx = new ArrayBlockingQueue<MarshallProtocolMessage>(2);
   		}
				
		public void run(){
		for(;;){					
			try {
				MarshallProtocolMessage message = this.marshallTx.take();
				byte[] TxBuffer = message.buildTxBuffer(); 	
				
				serialPort.writeBytes(TxBuffer);
			
				for (int i = TxBuffer.length - 1; i >= 0 ; i--){
					MarshallUI.consoleText.insert(String.format("%02X ", TxBuffer[i]), 0);
				}
				MarshallUI.consoleText.insert(String.format("\nTX: "),0);
				
				System.out.printf("\nTX: ");
				for (int i= 0;i < TxBuffer.length; i++){
					System.out.printf("%02X ", TxBuffer[i]);						
				}
				System.out.println( "\n");
			
				
			}catch (SerialPortException e) {
        	}catch (InterruptedException e1) {
				System.out.println(e1); ;
				}
			}
		}	
	}//End Class serial Tx	

	
	public void sendInternalMessage(byte type){	
		
		MarshallInternalMessage systemMessage = new MarshallInternalMessage(type);
		
		try {
			marshallQueue.put(systemMessage);
		} catch (InterruptedException e) {				
			e.printStackTrace();
			}
	}

	
	public void sendMdbInternalMessage(MarshallProtocolMessage message){	
		
		try {
			marshallQueue.put(message);
		} 
		catch (InterruptedException e) {				
				e.printStackTrace();
		}
	}
	
	public void sendMarshallMessage(MarshallMessage message){
		
		try {
			Tx.marshallTx.put((MarshallProtocolMessage) message);
		} catch (InterruptedException e) {				
			e.printStackTrace();
		}		
	}

}//class	

	

