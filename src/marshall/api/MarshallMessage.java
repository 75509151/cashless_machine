package marshall.api;

import java.util.Arrays;


public abstract class MarshallMessage {

	public static boolean beginSession = false;
}

class MarshallInternalMessage extends MarshallMessage{
	
	public final static byte INIT_PAIRING = 0x01;
	public final static byte KEEP_ALIVE_MODE = 0x02;
	public static final byte RESTART = 0x03;
	public final static byte VEND_MODE = 0x04;
	public final static byte VEND_INTERNAL = 0x08;
	public final static byte IDLE_MODE = 0x05;
	public final static byte PRODUCT_SELECTED = 0x0A;
	public final static byte PRODUCT_TAKEN = 0x0B;
	public static final byte PRODUCT_FAILURE = 0x0C;
	public static final byte VEND_CANCELLED = 0X0D;
	
	
	byte opcode;
	byte subOpcode;

	public MarshallInternalMessage(byte opcode_){
		this.opcode = opcode_;
	}
	public MarshallInternalMessage(byte opcode_, byte subOpcode_){
		this.opcode = opcode_;
		this.subOpcode = subOpcode_;
	}
	public byte getOpCode()	{	
		return opcode;
	}
	
	public byte getSubOpcode()	{	
		return subOpcode;
	}
}

class MarshallProtocolMessage extends MarshallMessage {
		
	protected static final int HEADER_SIZE 		= 11;
	protected static final int CRC16_SIZE 			= 2;
	protected static final int OPTIONS 			= 2;
	protected static final int ID 				= 3;
	protected static final int SOURCE 			= 4;
	protected static final int SOURCE_LSB 		= 5;
	protected static final int DEST 			= 6;
	protected static final int DEST_LSB 		= 7;
	protected static final int CODE 			= 8;
	
	public final static byte RESET_OPCODE 		= 0x01;
	public final static byte CONFIG_OPCODE 		= 0x06;
	
	public static final byte TRANSFER_DATA 		= (byte)0x0A;
	public final static byte RESPONSE_OPCODE 	= 0x00;
	
	public final static byte ACK = 0x01;
	public final static byte ACK_REQUIRED 		= 0x01;
	
	public static final byte MDB_OPCODE			= (byte)0x80;
	public static final byte VEND_CMD			= (byte)0x13;
		
	public  final byte[] PRODUCT_PRICE 			= {0x00,0x05,0x00,0x06,0x00,0x07,0x00,0x08};
	public  final byte[] PRODUCT_CODE 			= {0x01,0x01,0x01,0x02,0x02,0x01,0x02,0x02};
	
	
	
	static byte source = 0;
	static byte sourceLSB = 0; 
	static byte dest = 0;
	static byte destLSB = 0; 
	
	byte opcode;
	byte mdbOpcode;
	static byte packetId = (byte)0xBB;
	byte[] mdbData;
	public static byte counter = 0;
	
	boolean ackResponse = false;
	
	public MarshallProtocolMessage(byte opcode_){
		opcode = opcode_;
		
	}
	
	public MarshallProtocolMessage(byte opcode_, byte mdbOpcode_){
		opcode = opcode_;
		mdbOpcode = mdbOpcode_;
		
	}
	public MarshallProtocolMessage(byte opcode_, byte[] data){
		opcode = opcode_;
		mdbData = data;
	}
	
	public static MarshallProtocolMessage factory(byte[] buffer){
		
		MarshallProtocolMessage message = null;
		
		
		if (!correctCrc(buffer))
			return null;
		
		switch(buffer[8]){
		
		}
				
		for (int j= buffer.length - 1;j >= 0; j--){						
			MarshallUI.consoleText.insert(String.format("%02X ", buffer[j]), 0);
		}
		MarshallUI.consoleText.insert(String.format("\n\nRX: "), 0);
		
		System.out.printf("RX: ");
		for (int j= 0; j < buffer.length; j++){
			System.out.printf("%02X ", buffer[j]);
		}
		System.out.println( "\n");	
		
		
		message = MarshallConfigMessage.factory(buffer); //RX
		if ( message != null)
			return message;
		
		message = MarshallResetMessage.factory(buffer); //RX
		if ( message != null)
			return message;
		
		message = MarshallResponeRxMessage.factory(buffer);//RX
		if ( message != null)
			return message;
		
		message = MdbBeginSessionMessage.factory(buffer); //RX
		if ( message != null)
			return message;
		
		message = MdbCancelSessionMessage.factory(buffer); //RX
		if ( message != null)
			return message;
		
		message = MdbVendApproveMessage.factory(buffer);//RX
		if ( message != null)
			return message;
			
		message = MdbEndSessionMessage.factory(buffer); //RX
		if ( message != null)
			return message; 
		
		message = MarshallCardDataMessage.factory(buffer); //RX
		if ( message != null)
			return message;
		
		
		message = MdbVendDeniedMessage.factory(buffer); //RX
		if ( message != null)
			return message;
		
		message = MarshallStatusMessage.factory(buffer);//RX
		if ( message != null)
			return message;
		
		return message;
	}
	
	public void setPacketId(byte packetId_){
		packetId = packetId_;
	}
	
	public byte getPacketId(){
		return packetId;
	}
	
	public byte getDataLength() {
		return 0;
	}
	
	public byte[] buildTxBuffer()//(byte source, byte sourceLSB, byte dest, byte destLSB)
	{
		int dataLength = 0;
		byte[] messageData = dataToBuffer();
		if (messageData == null)
			dataLength = 0 ;
		else 
			dataLength = messageData.length;
		
		short length = (short)(dataLength + HEADER_SIZE);
		
		byte[] decLength = new byte[2];
		utils.shortToByteArray(decLength, 0, (short)(length -2));
		
		byte[] message = new byte[length];
		
		message[0] = decLength[0];
		message[1] = decLength[1];
		
		message[CODE] = opcode;
		if (message[CODE] == RESPONSE_OPCODE){
			message[ID] = packetId;
			message[OPTIONS] = 0x00;
		}
		else{	
			message[OPTIONS] = ackRequired();
			message[ID] = counter;
			if (counter != (byte)0xFF)
				counter++; //update counter
			else
				counter = 0x00; //Max counter value	
		}
		
		message[SOURCE]=  source; 
		message[SOURCE_LSB]= sourceLSB;
		message[DEST]= dest;
		message[DEST_LSB] = destLSB;
		
		//Append data 
		if (dataLength > 0)
			System.arraycopy(messageData, 0 , message, 9, dataLength);
		//Append CRC
		byte[] rcvCRC = new byte[2];
		short crc;
		crc = utils.calcCRC(message, message.length - 2,utils.SEED_CCITT);
		utils.shortToByteArray(rcvCRC, 0 ,crc); 
		System.arraycopy(rcvCRC, 0, message, message.length-2, 2);
	
		return message;
	}
	
	public byte[] dataToBuffer() {

		return null; 
	}

	public static boolean correctCrc(byte [] message){
		short crc, rcvCRC;
		int messageLength = message.length;
		
		crc = utils.calcCRC(message, message.length - 2,utils.SEED_CCITT);
		byte[] decCRC ={message[messageLength-2], message[messageLength - 1]};
		rcvCRC = utils.byteArrToShort(decCRC, 0);		
		if (rcvCRC != crc){
			System.out.println("Rx CRC Error\n");
    		return false;
    	}	
		return true; 
	}
	
	public  byte ackRequired(){
		return ACK;
	}
	
	public byte getOpCode(){
		return opcode;
	}
	public byte getMdbOpcode(){
		return mdbOpcode;
	}

}

//===========================================================
// 				RECEIVED MESSAGES FROM AMIT
// ===========================================================
class MarshallResetMessage extends MarshallProtocolMessage{
	
	public final static byte RESET_OPCODE = 0x01;
	
	public MarshallResetMessage(){
		super(RESET_OPCODE);
	}
	
	public static MarshallProtocolMessage factory(byte[] buffer){
		
		MarshallProtocolMessage message = null;
		
		if (buffer[8] == RESET_OPCODE)
			message = new MarshallResetMessage();
		
		return message;
	}
}
	

class MarshallResponeRxMessage extends MarshallProtocolMessage{
	
	public final static byte RESPONSE_OPCODE	= 0x00;
	public final static byte ACK_OK = 0x00;
	
	public MarshallResponeRxMessage() {
		super(RESPONSE_OPCODE);		
	}
	
	public static MarshallProtocolMessage factory(byte[] buffer){
		MarshallProtocolMessage message = null;
	
	if (buffer[8] == RESPONSE_OPCODE)
		message = new MarshallResponeRxMessage();
		
	return message;
	}

 }

class MdbBeginSessionMessage extends MarshallProtocolMessage{
	
	public final static byte MDB_OPCODE = (byte)0x80;
	public static final byte BEGIN_SESSION = (byte)0x03;
	
	public MdbBeginSessionMessage(byte[] buffer){
		super(MDB_OPCODE, BEGIN_SESSION);
		isAckResponseRequired(buffer);
	}


	public static MarshallProtocolMessage factory(byte[] buffer){
	
	MarshallProtocolMessage message = null;
	
	if ((buffer[8] == MDB_OPCODE) && (buffer[9] == BEGIN_SESSION))
		message = new MdbBeginSessionMessage(buffer);
		
	return message;
	}
	
	public byte getMdbOpcode(){
		return BEGIN_SESSION;
	}
	
	public void isAckResponseRequired(byte[] buffer){
		if((buffer[OPTIONS] & 0x01) == ACK_REQUIRED){
			packetId = buffer[ID];
			ackResponse = true;
		}
	}
	
	public byte[] dataToBuffer(){		
		return  null;
	}
	
	public  byte ackRequired(){
		return ACK;
	}
	
}

class MdbVendApproveMessage extends MarshallProtocolMessage{
	
	public static final byte VEND_APPROVED = (byte)0x05;
	
	public MdbVendApproveMessage(byte[] buffer){
		super(MDB_OPCODE, VEND_APPROVED);
		isAckResponseRequired(buffer);
	}

	public static MarshallProtocolMessage factory(byte[] buffer){
	
	MarshallProtocolMessage message = null;
	
	if ((buffer[8] == MDB_OPCODE) && (buffer[9] == VEND_APPROVED))
		message = new MdbVendApproveMessage(buffer);
	
	return message;
	}
	public byte getMdbOpcode(){
		return VEND_APPROVED;
	}	
	
	public void isAckResponseRequired(byte[] buffer){
		if((buffer[OPTIONS] & 0x01) == ACK_REQUIRED){
			packetId = buffer[ID];
			ackResponse = true;
		}
	}
	
	public byte[] dataToBuffer(){		
		return  null;
	}
	
	public  byte ackRequired(){
		return ACK;
	}	
}

class MdbVendDeniedMessage extends MarshallProtocolMessage{
	
	public static final byte VEND_DENIED = (byte)0x06;
	
	public MdbVendDeniedMessage(byte[] buffer){
		super(MDB_OPCODE, VEND_DENIED);
		isAckResponseRequired(buffer);
	}

	public static MarshallProtocolMessage factory(byte[] buffer){
	
	MarshallProtocolMessage message = null;
	
	if ((buffer[8] == MDB_OPCODE) && (buffer[9] == VEND_DENIED))
		message = new MdbVendDeniedMessage(buffer);
	
	return message;
	}
	public byte getMdbOpcode(){
		return VEND_DENIED;
	}	
	
	public void isAckResponseRequired(byte[] buffer){
		if((buffer[OPTIONS] & 0x01) == ACK_REQUIRED){
			packetId = buffer[ID];
			ackResponse = true;
		}
	}
	
	public byte[] dataToBuffer(){		
		return  null;
	}
	
	public  byte ackRequired(){
		return ACK;
	}
	
}

class MdbCancelSessionMessage extends MarshallProtocolMessage{
	
	public static final byte CANCEL_SESSION = (byte)0x04;
	
	public MdbCancelSessionMessage(byte[] buffer){
		super(MDB_OPCODE, CANCEL_SESSION);
		isAckResponseRequired(buffer);
	}

	public static MarshallProtocolMessage factory(byte[] buffer){
	
	MarshallProtocolMessage message = null;
	
	if ((buffer[8] == MDB_OPCODE) && (buffer[9] == CANCEL_SESSION))
		message = new MdbCancelSessionMessage(buffer);	
	
	return message;
	}
	
	public void isAckResponseRequired(byte[] buffer){
		if((buffer[OPTIONS] & 0x01) == ACK_REQUIRED){
			packetId = buffer[ID];
			ackResponse = true;
		}
	}
	
	public byte[] dataToBuffer(){		
		return  null;
	}
	
	public  byte ackRequired(){
		return ACK;
	}	
}


class MdbEndSessionMessage extends MarshallProtocolMessage{
	
	public static final byte END_SESSION = (byte)0x07;
	
	public MdbEndSessionMessage(byte[] buffer){
		super(MDB_OPCODE, END_SESSION);
		isAckResponseRequired(buffer);
	}

	public static MarshallProtocolMessage factory(byte[] buffer){
	
	MarshallProtocolMessage message = null;
	
	if ((buffer[8] == MDB_OPCODE) && (buffer[9] == END_SESSION))
		message = new MdbEndSessionMessage(buffer);	
	
	return message;
	}
	
	public void isAckResponseRequired(byte[] buffer){
		if((buffer[OPTIONS] & 0x01) == ACK_REQUIRED){
			packetId = buffer[ID];
			ackResponse = true;
		}
	}
	
	public byte[] dataToBuffer(){		
		return  null;
	}
	
	public  byte ackRequired(){
		return ACK;
	}	
}


class MarshallConfigMessage extends MarshallProtocolMessage{
	
	public final static byte CONFIG_OPCODE = 0x06;	
	
	public MarshallConfigMessage() {
		super(CONFIG_OPCODE);			
	}
	
	public static MarshallProtocolMessage factory(byte[] buffer){
		
		MarshallProtocolMessage message = null;
		
		if ( buffer[8] == CONFIG_OPCODE){
			message = new MarshallConfigMessage();
		
			source = (byte)buffer[DEST];
			sourceLSB = (byte)buffer[DEST_LSB]; 
			dest = (byte)buffer[SOURCE];
			destLSB = (byte)buffer[SOURCE_LSB]; 	
			int msgLength = buffer.length - CRC16_SIZE;
			byte[] lengthField = Arrays.copyOfRange(buffer, 0, 1);
			short length = utils.byteArrToShort(lengthField, 0);
			
			updateConfigData(buffer);
			}
		return message;
		
	}
	public byte getDataLength(){
		return 1;
	}
	
	public byte[] dataToBuffer(){
		byte[] buffer = null;	
		return  buffer;
	}
	
	public static void updateConfigData(byte[] buffer){
		
		byte machineID;		
		byte[] currency = new byte[3];
		byte[] country = new byte[3];
		byte[] amitSerialNum = new byte[16];
		byte[] pollInterval = new byte[4];
		byte[] protocolVer = new byte[2];
		byte language;
		byte[] maxPacketLen = new byte[2];
		
		int index = 9;		
					
		protocolVer = Arrays.copyOfRange(buffer, index, index+2);
		index+=2;
		machineID = buffer[index];
		index+=1;
		pollInterval = Arrays.copyOfRange(buffer, index, index+4); 
		index+=4;
		//index+=1; //language
		language =  buffer[index];
		index+=1;
		country = Arrays.copyOfRange(buffer, index, index+3);
		index+=3;
		currency = Arrays.copyOfRange(buffer, index, index+3);
		index+=3; //point on serial number
		index+=1;
		amitSerialNum = Arrays.copyOfRange(buffer, index, index+16);		
		index+=16;
		maxPacketLen = Arrays.copyOfRange(buffer, index, index+2);			
	}	
}

//===========================================================
//			TRANSSMIT MESSAGES FROM MACHINE
//===========================================================

class MarshallKeepAliveMessage extends MarshallProtocolMessage{
	
	public final static byte KEEP_ALIVE_OPCODE = 0x07;
		
	public MarshallKeepAliveMessage(){
		super(KEEP_ALIVE_OPCODE);
	}
	
	public byte[] dataToBuffer(){		
		return  null;
	}
	
	public  byte ackRequired(){
		return ACK;
	}
	
}

class MarshallResponeTxMessage extends MarshallProtocolMessage{
	
	public final static byte RESPONSE_OPCODE = (byte)0x00;
	public final static byte ACK_OK = 0x00;
	
	
	public MarshallResponeTxMessage() {
		super(RESPONSE_OPCODE);		
	}
	
	public byte[] dataToBuffer(){		
		byte[] ack = {ACK_OK};
		return ack;
	}
	
	public  byte ackRequired(){
		return 0x00;
	}
 }

class ReaderEnableMessage extends MarshallProtocolMessage{
	
	public final static byte MDB_OPCODE = (byte)0x80;
	final private static byte READER_CMD 	= (byte)0x14;
	final private static byte READER_ENABLE = (byte)0x01;	
	
	byte[] mdbSubCommand = new byte[2];
	
	public ReaderEnableMessage(){
		super(MDB_OPCODE);
		mdbSubCommand[0] = READER_CMD;
		mdbSubCommand[1] = READER_ENABLE;		
	}
	
	public byte[] dataToBuffer(){		
		return  mdbSubCommand;
	}
	
	public  byte ackRequired(){
		return ACK;
	}
}

class ReaderDisableMessage extends MarshallProtocolMessage{
	
	public final static byte MDB_OPCODE = (byte)0x80;
	final private static byte READER_CMD 	= (byte)0x14;
	final private static byte READER_DISABLE = (byte)0x00;	
	
	byte[] mdbCommand = new byte[2];
	
	public ReaderDisableMessage(){
		super(MDB_OPCODE);
		mdbCommand[0] = READER_CMD;
		mdbCommand[1] = READER_DISABLE;		
	}
	
	public byte[] dataToBuffer(){		
		return  mdbCommand;
	}
	
	public  byte ackRequired(){
		return ACK;
	}
}

class VendRequestMessage extends MarshallProtocolMessage{
	
	public static final byte VEND_REQUEST = (byte)0x00;
	
	byte[] mdbCommand = new byte[6];
		
	public VendRequestMessage(){
		super(MDB_OPCODE);
		mdbCommand[0] = (byte)VEND_CMD;
		mdbCommand[1] = VEND_REQUEST;
		switch (MarshallUI.itemSelected){
			case MarshallUI.PROUCT_1:  
				mdbCommand[2] = PRODUCT_PRICE[1]; 
				mdbCommand[3] = PRODUCT_PRICE[0]; //little endians
				mdbCommand[4] = PRODUCT_CODE[1];  
				mdbCommand[5] = PRODUCT_CODE[0];
				break;
			case MarshallUI.PROUCT_2:  
				mdbCommand[2] = PRODUCT_PRICE[3]; 
				mdbCommand[3] = PRODUCT_PRICE[2]; //little endians
				mdbCommand[4] = PRODUCT_CODE[3];  
				mdbCommand[5] = PRODUCT_CODE[2];
				break;
			case MarshallUI.PROUCT_3:  
				mdbCommand[2] = PRODUCT_PRICE[5]; 
				mdbCommand[3] = PRODUCT_PRICE[4]; //little endians
				mdbCommand[4] = PRODUCT_CODE[5];  
				mdbCommand[5] = PRODUCT_CODE[4];
				break;
			case MarshallUI.PROUCT_4:  
				mdbCommand[2] = PRODUCT_PRICE[7]; 
				mdbCommand[3] = PRODUCT_PRICE[6]; //little endians
				mdbCommand[4] = PRODUCT_CODE[7];  
				mdbCommand[5] = PRODUCT_CODE[6];
				break;
			default:
					System.out.printf("UNDEFINED ITEM SELCETD IN VEND REQUEST\n");
				
		}
				
	}
	
	public byte[] dataToBuffer(){		
		return  mdbCommand;
	}
	
	public  byte ackRequired(){
		return ACK;
	}
	
}

class VendSuccessMessage extends MarshallProtocolMessage{
	
	public static final byte VEND_SUCCESS 	= (byte)0x02;
	
	byte[] mdbCommand = new byte[2];
		
	public VendSuccessMessage(){
		super(MDB_OPCODE);
		mdbCommand[0] = (byte)VEND_CMD;
		mdbCommand[1] = VEND_SUCCESS;
	}
	
	public byte[] dataToBuffer(){		
		return  mdbCommand;
	}
	
	public  byte ackRequired(){
		return ACK;
	}	
}

class VendCancelMessage extends MarshallProtocolMessage{
	
	public static final byte VEND_CANCEL	= (byte)0x01;
	
	byte[] mdbCommand = new byte[2];
		
	public VendCancelMessage(){
		super(MDB_OPCODE);
		mdbCommand[0] = (byte)VEND_CMD;
		mdbCommand[1] = VEND_CANCEL;
	}
	
	public byte[] dataToBuffer(){		
		return  mdbCommand;
	}
	
	public  byte ackRequired(){
		return ACK;
	}	
}

class VendFailurelMessage extends MarshallProtocolMessage{
	
	public static final byte VEND_FAILURE	= (byte)0x03;
	
	byte[] mdbCommand = new byte[2];
		
	public VendFailurelMessage(){
		super(MDB_OPCODE);
		mdbCommand[0] = (byte)VEND_CMD;
		mdbCommand[1] = VEND_FAILURE;
	}
	
	public byte[] dataToBuffer(){		
		return  mdbCommand;
	}
	
	public  byte ackRequired(){
		return ACK;
	}	
}

class SessionCompleteMessage extends MarshallProtocolMessage{
	
	public static final byte SESSION_COMPLETE	= (byte)0x04;
	
	byte[] mdbCommand = new byte[2];
		
	public SessionCompleteMessage(){
		super(MDB_OPCODE);
		mdbCommand[0] = (byte)VEND_CMD;
		mdbCommand[1] = SESSION_COMPLETE;
	}
	
	public byte[] dataToBuffer(){		
		return  mdbCommand;
		
	}
	
	public  byte ackRequired(){
		return ACK;
	}	
}

class MarshallFirmwareInfoMessage extends MarshallProtocolMessage{
	
	public final static byte FIRMWARE_INFO_OPCODE = 0x05;

	private static final int _MAJOR_SW_VER 		= 0x00;
	private static final int _MINOR_SW_VER 		= 0x09;
	private static final int MACHINE_TYPE 		= 0x03; //Photo Booth
	private static final int MACHINE_SUBTYPE 	= 0x01; //Pthotome Japan
	
	private static final String  MACHINE_MODEL 	= "123456";
	private static final String  MACHINE_SERIAL_NUMBER 	= "x1x2x3x4x5x6";
	private static final String  MACHINE_SW_VERSION 	= "v1.1.2";
	
	byte[] MachineParam;
	byte[]  InvData;
	String machineData;
	
	private String machineModel;
	private String machineSerNumber;
	private String machineSwVersion;
	
	public MarshallFirmwareInfoMessage() {
		super(FIRMWARE_INFO_OPCODE);
		
		MachineParam = new byte[6];	
		
		MachineParam[0] = _MAJOR_SW_VER;
		MachineParam[1] = _MINOR_SW_VER;
		MachineParam[2] = MACHINE_TYPE;
		MachineParam[3] = MACHINE_SUBTYPE;
		MachineParam[4] = 0x00; //capabilities
		MachineParam[5] = 0x00; //capabilities
		
		//"\0" or '\0' null terminated
		machineModel = MACHINE_MODEL;
		machineSerNumber = MACHINE_SERIAL_NUMBER;
		machineSwVersion = MACHINE_SW_VERSION;
	}
	
	public byte getDataLength(){
		return 1;
	}
	
	
	public byte[] dataToBuffer(){
		int dataLength = machineModel.length()+machineSerNumber.length()+machineSwVersion.length();
		byte[] buffer = new byte[MachineParam.length+ dataLength];
		int ind = 0;
		System.arraycopy(MachineParam, 0 , buffer, 0, MachineParam.length); 
		ind += MachineParam.length;
		System.arraycopy(machineModel.getBytes(), 0 , buffer, ind, machineModel.length());
		ind += machineModel.length();
		System.arraycopy(machineSerNumber.getBytes(), 0 , buffer, ind, machineSerNumber.length());
		ind += machineSerNumber.length();
		System.arraycopy(machineSwVersion.getBytes(), 0 , buffer, ind ,machineSwVersion.length());
		return  buffer;
	}
	public byte ackRequired(){
		return 0x00;
	}
}	
	

 
