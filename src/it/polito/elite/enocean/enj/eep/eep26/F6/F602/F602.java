/*
 * EnJ - EnOcean Java API
 * 
 * Copyright 2014 Andrea Biasi, Dario Bonino 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package it.polito.elite.enocean.enj.eep.eep26.F6.F602;

import it.polito.elite.enocean.enj.communication.EnJConnection;
import it.polito.elite.enocean.enj.eep.EEP;
import it.polito.elite.enocean.enj.eep.Rorg;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A class representing the F6-02 family of EnOcean Equipment Profiles (Rocker
 * Switches).
 * 
 * @author <a href="mailto:dario.bonino@gmail.com">Dario Bonino</a>
 *
 */
public abstract class F602 extends EEP
{
	// the EEP26 definition, according to the EEP26 specification
	public static final Rorg rorg = new Rorg((byte) 0xf6);
	public static final byte func = (byte) 0x02;

	// func must be defined by extending classes

	// Executor Thread Pool for handling attribute updates
	protected transient ExecutorService attributeNotificationWorker;

	// -------------------------------------------------
	// Parameters defined by this EEP, which
	// might change depending on the network
	// activity.
	// --------------------------------------------------

	// --------------------------------------------------

	// the class constructor
	public F602()
	{
		// call the superclass constructor
		super("2.6");

		// build the attribute dispatching worker
		this.attributeNotificationWorker = Executors.newFixedThreadPool(1);
	}

	/**
	 * F6.02 CMD 0x1 Implements the CMD 0x1 of the F6.02 EnOcean Equipment
	 * Profile, which allows setting the output level of a F6.02 device (On, Off, (dimming not supported yet)).
	 *
	 * @param connection
	 *            The {@EnJConnection} link to be used for
	 *            sending the command, packet encapsulation will be performed at
	 *            such level.
	 * @param outputValue
	 *            The required ouput value. A byte representing the value of click
	 *            between ON (0x50) and OFF (0x70).
	 */
	public void actuatorSetOutput(EnJConnection connection, byte[] deviceAddress, byte outputValue)
	{
		// prepare the data payload to host "desired" values
		byte dataByte[] = new byte[2];

		// add the packet rorg
		dataByte[0] = F602.rorg.getRorgValue();

		// Output value bit 6 to 0
		dataByte[1] = outputValue;

		// send the payload for connection-layer encapsulation
		connection.sendRadioCommand(deviceAddress, dataByte);
	}
}
