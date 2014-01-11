/**
 * 
 */
package it.polito.elite.enocean.protocol.serial.v3.network.serialcommunication;

import gnu.io.SerialPort;
import it.polito.elite.enocean.protocol.serial.v3.network.packet.Packet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Andrea Biasi <biasiandrea04@gmail.com>
 *
 */

public class ThreadWrite extends Thread{
	
	/**
	 * @param highPriorityQueue
	 * @param lowPriorityQueue
	 * @param serialPort
	 * @param expectedResponse
	 */
	public ThreadWrite(ConcurrentLinkedQueue<Packet> highPriorityQueue,
			ConcurrentLinkedQueue<ElementQueue> lowPriorityQueue,
			SerialPort serialPort, Semaphore expectedResponse) {
		super();
		this.highPriorityTxQueue = highPriorityQueue;
		this.lowPriorityTxQueue = lowPriorityQueue;
		this.serialPort = serialPort;
		this.expectedResponse = expectedResponse;
	}

	// Coda di messaggi ad alta priorit� (Risposte da inviare)
	ConcurrentLinkedQueue<Packet> highPriorityTxQueue;

	// Coda di messaggi a bassa priorit�
	ConcurrentLinkedQueue<ElementQueue> lowPriorityTxQueue;

	// Elemento estratto dalla coda a bassa priotit�, contiene pacchetto e un contatore
	ElementQueue element;

	// Pacchetto ESP3
	Packet pkt;

	// Porta seriale
	SerialPort serialPort;

	// Stream dati in uscita
	OutputStream out;

	// Flag per risposta attesa
	Semaphore expectedResponse;

	public void run(){
		try {
			// Ottieni lo stream di dati in uscita
			out = serialPort.getOutputStream();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Flag per segnalare continuazione del thread
		boolean canRun = true;
		
		long startTime = 0;
		
		while(canRun){
			// Se la coda ad alta priorit� non � vuota, invia prima i dati ad alta priorit�
			if (!highPriorityTxQueue.isEmpty()){
				// Prelevo il pacchetto dalla coda e lo scrivo sulla seriale
				try {
					out.write(this.highPriorityTxQueue.poll().getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//Se la coda ad alta priorit� � vuota posso mandare dati a bassa priorit�
			else{
				// Se non aspetto risposta
				if(this.expectedResponse.availablePermits()>0){ // Equivale a risp attesa = FALSE

					//Se il pacchetto � gi� stato inviato correttamente al primo tentativo posso eliminarlo dalla coda
					if(this.lowPriorityTxQueue.peek().counter<3) this.lowPriorityTxQueue.poll();

					// Se c'� qualcosa nella coda a bassa priorit�
					if(!this.lowPriorityTxQueue.isEmpty()){

						// Estraggo il pacchetto senza eliminarlo e lo invio
						try {
							out.write(this.lowPriorityTxQueue.peek().pkt.getBytes());
						} catch (IOException e) {
							e.printStackTrace();
						}

						// Setto risp attesa = true
						try {
							this.expectedResponse.acquire();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						// Decremento il contatore numero di invii tentati
						this.lowPriorityTxQueue.peek().counter--;

						//Start timer
						startTime = System.currentTimeMillis();
				
					}//Fine if(!this.lowPriorityQueue.isEmpty())
			
				} // Fine if(this.expectedResponse.availablePermits()>0)
				
				// Se sto ancora attendendo la risposta al comando inviato				
				else{
					// Se sono passati pi� di 500 ms e non ho ricevuto risposta
					if((System.currentTimeMillis() - startTime)>500){
						
					// Se ho esaurito i tentativi di invio segnalo il fallimento della trasmissione
						if(this.lowPriorityTxQueue.peek().counter==0){
							this.lowPriorityTxQueue.poll();
							System.out.print("Trasmissione fallita dopo 3 tentativi");
						}
						// Altrimenti re invio il pacchetto
						else{
						
						//Invio il pacchetto
						try {
							out.write(this.lowPriorityTxQueue.peek().pkt.getBytes());
						} catch (IOException e) {
							e.printStackTrace();
						}

						// Setto risp attesa = true
						try {
							this.expectedResponse.acquire();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						// Decremento il contatore numero di invii tentati
						this.lowPriorityTxQueue.peek().counter--;

						//Start timer
						startTime = System.currentTimeMillis();
						
						}
					} // Fine if((System.currentTimeMillis() - startTime)>500)
				
				} // Fine else if(this.expectedResponse.availablePermits()>0)

			}// Fine else if (!highPriorityQueue.isEmpty())

			//Sleep per 5 ms
			try {
				sleep(5);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}// Fine while(canRun)
	}
}