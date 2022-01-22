package org.jdamico.javax25.ax25;


import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Microphone extends Thread implements PacketHandler {

	protected Soundcard soundcard;
	protected PacketDemodulator demodulator;
	protected PacketModulator modulator;
	protected OutputStream outputStream;

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
