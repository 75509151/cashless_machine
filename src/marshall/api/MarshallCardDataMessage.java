package marshall.api;

import java.util.Arrays;

public class MarshallCardDataMessage extends MarshallProtocolMessage {
					
	public static final byte TRANSFER_DATA 		= (byte)0x0A;
	
	public MarshallCardDataMessage(byte[] buffer){
		super(TRANSFER_DATA);	
		isAckResponseRequired(buffer);
	}	
		
	public static MarshallProtocolMessage factory(byte[] buffer){
		
		MarshallProtocolMessage message = null;
	
		if ( buffer[8] == TRANSFER_DATA){
			message = new MarshallCardDataMessage(buffer);
			updateCardData(buffer);		
		}
	return message;
	}
	
	public void isAckResponseRequired(byte[] buffer){
		if((buffer[OPTIONS] & 0x01) == ACK_REQUIRED){
			packetId = buffer[ID];
			ackResponse = true;
		}
	}	
		
	public static void updateCardData(byte[] buffer){
		
		final byte TRANSACTION_ID = 0x01;
		final byte CHOOSE_PRODUCT_TIMEOUT = 0x02;
		final byte CARD_TYPE = 0x03;
		final byte CARD_ENTRY_MODE = 0x04;
		final byte CARD_BIN = 0x05;
		final byte CARD_BIN_HASH = 0x06;
		final byte PROPRAIETRY_CARD_UID = 0x07;
		final byte MACHINE_AUTHORIZATION_STATUS = 0x08;
		final byte COMM_STATUS = 0x09;
		
		
		byte[] transactionID;
		byte[] chooseProductTimeout;
		byte[] cardType;
		byte[] cardEntryMode;
		byte[] cardBIN;
		byte[] cardBinHash;
		byte[] proprietyCardUID; 
		byte[] machineAuthorizationStatus;
		byte[] commStatus;
			
		int index = 9;
		int type = 0;
		int typeLength = 0;
		
		int cardDataLength = buffer.length - 11;
			
		while (index < cardDataLength){
		
			type = buffer[index++];
			typeLength = buffer[index++];
			
			switch (type){
				case TRANSACTION_ID:
					transactionID = new byte[typeLength]; 
					System.arraycopy(buffer, index , transactionID, 0 , typeLength);
					index += typeLength;				
					break;
				case CHOOSE_PRODUCT_TIMEOUT:
					chooseProductTimeout = new byte[typeLength]; 
					System.arraycopy(buffer, index , chooseProductTimeout, 0 , typeLength);
					index += typeLength;
					break;
				case CARD_TYPE:
					cardType = new byte[typeLength]; 
					System.arraycopy(buffer, index , cardType, 0 , typeLength);
					index += typeLength;
					break;
				case CARD_ENTRY_MODE:
					cardEntryMode = new byte[typeLength]; 
					System.arraycopy(buffer, index , cardEntryMode, 0 , typeLength);
					index += typeLength;
					break;
				case CARD_BIN:
					cardBIN = new byte[typeLength]; 
					System.arraycopy(buffer, index , cardBIN, 0 , typeLength);
					index += typeLength;
					break;
				case CARD_BIN_HASH:
					cardBinHash = new byte[typeLength];
					System.arraycopy(buffer, index , cardBinHash, 0 , typeLength);
					index += typeLength;
					break;
				case PROPRAIETRY_CARD_UID:
					proprietyCardUID = new byte[typeLength];
					System.arraycopy(buffer, index , proprietyCardUID, 0 , typeLength);
					index += typeLength;
					break;
				case MACHINE_AUTHORIZATION_STATUS:
					machineAuthorizationStatus = new byte[typeLength];
					System.arraycopy(buffer, index , machineAuthorizationStatus, 0 , typeLength);
					index += typeLength;
					break;
				case COMM_STATUS:
					commStatus = new byte[typeLength];
					System.arraycopy(buffer, index , commStatus, 0 , typeLength);
					index += typeLength;
					break;
				default:
						System.out.printf( "unsupported type in transfer data 0x%02x\n", buffer[type]);
			}	
		}	
	}
	
	public byte[] dataToBuffer(){		
		return  null;
	}
	
	public  byte ackRequired(){
		return ACK;
	}
		
}

class MarshallStatusMessage extends MarshallProtocolMessage{
	
	public static final byte CODE_STATUS        = (byte)0x0B;
	
	public MarshallStatusMessage(byte[] buffer){
		super(CODE_STATUS);
		isAckResponseRequired(buffer);
	}
	
	public static MarshallProtocolMessage factory(byte[] buffer){
		
		MarshallProtocolMessage message = null;
	
		if ( buffer[8] == CODE_STATUS){
			message = new MarshallStatusMessage(buffer);
			checkStatusMessage(buffer);		
		}
	return message;
	}
	
	public void isAckResponseRequired(byte[] buffer){
		if((buffer[OPTIONS] & 0x01) == ACK_REQUIRED){
			packetId = buffer[ID];
			ackResponse = true;
		}
	}	
		
	public static void checkStatusMessage(byte[] buffer){	
		final int UNEXPECTED_ERROR = 0;
		final int TIMEOUT = 1;
		final int OUT_OF_SEQUENCE = 2;  
		final int PENDING_COMMUNICATION_RECEIVED = 3;
		final int SETTELMENT_STATUS = 5; 
		
		byte status = buffer[9];
		
		switch(status){
			case UNEXPECTED_ERROR:
				System.out.println( "\nRX: UNEXPECTED_ERROR");
				break;
			case TIMEOUT:
				System.out.println( "\nRX: TIMEOUT");
				break;
			case OUT_OF_SEQUENCE:
				System.out.println( "\nRX: OUT_OF_SEQUENCE");
				break;
			case PENDING_COMMUNICATION_RECEIVED:
				System.out.println( "\nRX: PENDING_COMMUNICATION");
				break;
			default:
				System.out.println( "\nRX: UNKNOWN STATUS CODE");
		}
	}
}

