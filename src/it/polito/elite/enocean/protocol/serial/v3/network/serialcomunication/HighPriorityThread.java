package it.polito.elite.enocean.protocol.serial.v3.network.serialcomunication;

import java.util.concurrent.Semaphore;

import it.polito.elite.enocean.protocol.serial.v3.network.packet.Packet;

/**
 * This class look if there are high priority message received (ones which needs response), process them and put eventually response into the high priority message in transmission thread
 * 
 * @author Andrea Biasi <biasiandrea04@gmail.com>
 *
 */

public class HighPriorityThread extends Thread {

	public HighPriorityThread(DataQueue highPriorityRxQueue, DataQueue highPriorityTxQueue, Semaphore mutexHpRxQueue, Semaphore mutexHpTxQueue, boolean flagResponse){
		super();
		this.highPriorityRxQueue = highPriorityRxQueue;
		this.highPriorityTxQueue = highPriorityTxQueue;
		this.mutexHpRxQueue = mutexHpRxQueue;
		this.mutexHpTxQueue = mutexHpTxQueue;
		this.flagResponse = flagResponse;
	}
	
	// Coda di messaggi dal thread di ricezione
	DataQueue highPriorityRxQueue;
	
	// Coda di messaggi ad alta priorit� dal thread di scrittura
	DataQueue highPriorityTxQueue;
	
	// Semaforo per l'accesso alla coda del thread di ricezione
	Semaphore mutexHpRxQueue;
	
	// Semaforo per l'accesso alla coda del thread di trasmissione
	Semaphore mutexHpTxQueue;
	
	// Pacchetto ESP3
	Packet pkt;
	
	boolean flagResponse;
	
	public void run(){
		boolean canRun = true;
		while(canRun){
			// Prende i pacchetti dalla coda RX e li mette nella coda in TX
			if (!this.highPriorityRxQueue.isEmpty()){
				//this.highPriorityTxQueue.add(this.highPriorityRxQueue.poll());
				
				synchronized(highPriorityRxQueue){
					while (!this.mutexHpRxQueue.tryAcquire()) {
						try {
							//Il semaforo mi dice che non � disponnibile quindi il thread corrente aspetta finche un altro thread non chiama notify
							highPriorityRxQueue.wait();
						} catch (InterruptedException e) {
							return;
						}
					}
					
					// Estae l'elemento dalla coda
					pkt = this.highPriorityRxQueue.poll();
					
					// Setto risposta attesa
					this.flagResponse = true;
					
					// Libero il thread di lettura che condivide la risorsa
					this.highPriorityRxQueue.notify();
				}
				
				synchronized(highPriorityTxQueue){
					while (!this.mutexHpTxQueue.tryAcquire()) {
						try {
							//Il semaforo mi dice che non � disponnibile quindi il thread corrente aspetta finche un altro thread non chiama notify
							highPriorityTxQueue.wait();
						} catch (InterruptedException e) {
							return;
						}
					}
					
					// Aggiungi l'elemento estratto dal thread di lettura e lo aggiunge al thread di scrittura
					this.highPriorityTxQueue.add(pkt);
					
					// Libera risposta attesa
					this.flagResponse = false;
					
					// Libera il thread di scrittura che condivide la risorsa
					this.highPriorityTxQueue.notify();
				}	
			}
		}
	}
}
