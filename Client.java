import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.*;

public class Client {
	public static void main(String[] args){	
		try{			
			if (args.length == 2){
				int expectedAck = 1;
				String filename = args[0];								//set the filename as the first argument
				int port = Integer.parseInt(args[1]);							//set the port number as the second command line argument
				DatagramSocket clientDS = new DatagramSocket();						//create a new datagram socket (to send/recieve the packets)		
				byte[] filenameArray = filename.getBytes(); 						//make a byte array of the filename
				
				DatagramPacket RRQ = TftpUtility.packRRQDatagramPacket(filenameArray);			//create an RRQ packet to send the file request
				RRQ.setPort(port);									//set the port to send the file to
				RRQ.setAddress(InetAddress.getByName("localhost"));					//set the IP address to send the file to the port number given in the command line
			
				clientDS.send(RRQ);									//send the RRQ packet to the server	
				
				byte[] buf = new byte[1472];							//create a new byte array
				DatagramPacket p = new DatagramPacket(buf, 1472);				//create a new datagram packet
				clientDS.receive(p);

				if(TftpUtility.checkPacketType(p) == 4){					//if the datagram packet is an error packet
						TftpUtility.printErrorString(p);					//print the error message to the screen
						System.exit(0);
				}

				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(filename)));

				while(true) {
					clientDS.receive(p);								//wait to receive a datagram packet from the server

					if(TftpUtility.checkPacketType(p) == 4){					//if the datagram packet is an error packet
						TftpUtility.printErrorString(p);					//print the error message to the screen
						System.exit(0);
					}
					else if(TftpUtility.checkPacketType(p) == 2){				//if the datagram packet is a data packet
								
								System.out.println("Packet Received...");			
								byte ack = TftpUtility.extractBlockSeq(p);	//get the sequence number of the packet
								byte[] ackBuf = new byte[2];				//create a new byte array with length of 2, used for an ack packet
								ackBuf[0] = 3;							//set the first byte (type) to 3, signalling an ACK packet
									
								if(expectedAck == ack){				//if it is the data that the client was expecting, add the data to the output file
									ackBuf[1] = ack;				//set the ack to send back
									byte[] dataArray = p.getData();		//get the array of data
									int dataLength = p.getLength();		//get the length of the packet
									ByteBuffer bBuffer = ByteBuffer.allocate(dataLength - 2);	//remove the packet type and block number from the satarft of the packet
									bBuffer.put(dataArray, 2, dataLength - 2);
									bos.write(bBuffer.array());		//write the data to the file
									bos.flush();
									
									if(expectedAck < 127 && expectedAck == ack)	//if the ack is less than 127, increment the expected ack
										expectedAck++;
									else if (expectedAck == 127)		//set the expectedAckl to -128
										expectedAck = -128;
								}
								else{
									ackBuf[1] = (byte)expectedAck;
								}
								
								DatagramPacket ackDP = new DatagramPacket(ackBuf, 2, InetAddress.getByName("localhost"), p.getPort());
								clientDS.send(ackDP);				//send the ack packet to the server
								System.out.println(ack);
								
								if(p.getLength() < 514){
									System.out.println("Last Packet Ack");
									break;
								}
						}
				}
				bos.close();

		    	}
				
			else
			 System.out.println("Usage: filename, port number");
		}
		catch(Exception e){
			System.out.println(e);
		}
	}	
}
