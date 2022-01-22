/*
 * Test program for the Afsk1200 sound-card modem.
 * For examples, see test.bat
 * 
 * Copyright (C) Sivan Toledo, 2012
 * 
 *      This program is free software; you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation; either version 2 of the License, or
 *      (at your option) any later version.
 *
 *      This program is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with this program; if not, write to the Free Software
 *      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.jdamico.javax25.agwpe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Properties;

import org.jdamico.javax25.ax25.Afsk1200Modulator;
import org.jdamico.javax25.ax25.Afsk1200MultiDemodulator;
import org.jdamico.javax25.ax25.Packet;
import org.jdamico.javax25.ax25.PacketDemodulator;
import org.jdamico.javax25.ax25.PacketHandler;
import org.jdamico.javax25.ax25.PacketModulator;
import org.jdamico.javax25.soundcard.Soundcard;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Microphone extends Thread implements PacketHandler {

	private Soundcard soundcard;
	private PacketDemodulator demodulator;
	private PacketModulator modulator;
	private OutputStream outputStream;

	public Microphone(Properties properties, String saveFile) {
		log.info("entering constuctor");

		int rate = 48000;

		try {
			rate = Integer.parseInt(properties.getProperty("rate", "48000").trim());
		} catch (Exception e) {
			System.err.println("Exception parsing rate " + e.toString());
		}

		try {
			// demodulator calls this class' PacketHandler.handlePacket to save the data
			demodulator = new Afsk1200MultiDemodulator(rate, this);
			
			modulator = new Afsk1200Modulator(rate);
		} catch (Exception e) {
			log.error(e.toString(), e);
			System.out.println("Exception trying to create an Afsk1200 object: " + e.getMessage());
			System.exit(1);
		}

		String input = properties.getProperty("input", "default [default]");
		// not using output at the moment
		String output = properties.getProperty("output", "default [default]");

		int buffer_size = -1;
		try {
			// our default is 100ms
			buffer_size = Integer.parseInt(properties.getProperty("latency", "100").trim());
			// if (buffer_size ==-1) buffer_size = sc.rate/10;
			// ae.capture_buffer = new byte[2*buffer_size];
		} catch (Exception e) {
			log.error(e.toString(), e);
			System.err.println("Exception parsing buffersize " + e.toString());
		}

		soundcard = new Soundcard(rate, input, output, buffer_size, demodulator, modulator);

		if (properties.containsKey("audio-level")) {
			soundcard.displayAudioLevel();
		}

		File outputFile = new File(System.getProperty("java.io.tmpdir") + File.separator + saveFile);

		try {
			outputStream = new FileOutputStream(outputFile);
		} catch (Exception e) {
			log.error(e.toString(), e);
		}
		log.info("leaving constuctor");

	}

	public void handlePacket(byte[] bytes) {
		log.info("handlePacket: {}", Packet.format(bytes));

		byte[] header_data = new byte[37 + bytes.length];
		ByteBuffer bb = ByteBuffer.wrap(header_data).order(ByteOrder.LITTLE_ENDIAN);
		bb.position(28);
		bb.putInt(bytes.length);
		System.arraycopy(bytes, 0, header_data, 36, bytes.length);
		try {
			outputStream.write(header_data);
		} catch (Exception e) {
			log.error(e.toString(), e);
		}
	}

	public void run() {
		log.info("entering run");
		soundcard.receive();
		log.info("leaving run");
	}

	public void close() {
		log.info("entering close");
		try {
			if (null != outputStream) {
				outputStream.close();
				outputStream = null ;
			}
		} catch (Exception e) {
			log.error(e.toString(), e) ;
			outputStream = null ;
		}
		log.info("leaving close");
	}
	
	public static void main(String[] args) {

		Microphone microphone = new Microphone(System.getProperties(), "microphone_main.ax25");

		try {
			microphone.start();
		} catch (Exception e) {
			log.error(e.toString(), e);
		}

		do {
		} while (microphone.isAlive());

	}

}
