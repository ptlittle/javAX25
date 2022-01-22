/*
 * Soundcard: this class interfaces clients (such as javAPRSsrvr or trackers, etc)
 * to half-duplex modems like AX25's Afsk1200.
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
package org.jdamico.javax25.ax25;

import java.io.File;
import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
//import java.util.Arrays;
//import java.util.Properties;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Soundcard {

	protected int rate;
	protected final int channels = 1;
	protected final int samplebytes = 2;

	protected AudioFormat audioFormat = null;
	protected AudioInputStream audioInputStream = null;
	protected SourceDataLine sourceDataLine = null;
	protected byte[] capture_buffer;

	protected boolean display_audio_level = false;

	// protected Afsk1200 afsk;
	// protected HalfduplexSoundcardClient afsk;
	protected SoundcardConsumer soundcardConsumer;
	protected SoundcardProducer soundcardProducer;

	protected int latency_ms;

	public Soundcard(int rate, String input, String output, int latency_ms, SoundcardConsumer consumer,
			SoundcardProducer producer) {
		this.rate = rate;
		// this.afsk = afsk;
		soundcardProducer = producer;
		soundcardConsumer = consumer;
		this.latency_ms = latency_ms;

		audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, rate, 16, /* bits per value */
				channels, /* stereo */
				samplebytes, /* sample size */
				rate, false /* false=little endian */);

		setInputDevice(input) ;
		setOutputDevice(output) ;

	}

	
	public void transmit() {	
		
		sourceDataLine.flush();
		sourceDataLine.start();

		int n;
		float[] samples = soundcardProducer.getTxSamplesBuffer();
		byte[] buffer = new byte[2 * samples.length];
		ByteBuffer bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);

		while ((n = soundcardProducer.getSamples()) > 0) {
			bb.rewind();
			for (int i = 0; i < n; i++) {
				// Convert to a 16-bit signed integer and encode as little endian
				short s = (short) Math.round(32767.0f * samples[i]);
				bb.putShort(s);
			}
			sourceDataLine.write(buffer, 0, 2 * n);
		}
		sourceDataLine.drain(); // wait until all data has been sent
		sourceDataLine.stop();
	}

	public static void enumerate() {
		Mixer.Info[] mixers = AudioSystem.getMixerInfo();

		log.info("Available sound devices:");

		for (Mixer.Info mixer : mixers) {
			String name = mixer.getName();
			log.debug("mixer {}", mixer.toString());

			Line.Info[] sourceLines;
			sourceLines = AudioSystem.getMixer(mixer).getSourceLineInfo();
			for (Line.Info sourceLine : sourceLines) {
				log.debug("sourceLine {}", sourceLine.toString());
				if (SourceDataLine.class.equals(sourceLine.getLineClass()))
					log.info("  output: {}", name);
			}
			Line.Info[] targetLines;
			targetLines = AudioSystem.getMixer(mixer).getTargetLineInfo();
			for (Line.Info targetLine : targetLines) {
				log.debug("targetLine {}", targetLine.toString());
				if (TargetDataLine.class.equals(targetLine.getLineClass())) {
					log.info("  output: {}", name);
				}
			}
		}
	}

	public void displayAudioLevel() {
		display_audio_level = true;
	}

	
	public void receive() {

		try {
			int j = 0;
			int buffer_size_in_samples = (int) Math.round(latency_ms * ((double) rate / 1000.0) / 4.0);
			capture_buffer = new byte[2 * buffer_size_in_samples];
			if (audioInputStream == null) {
				log.error("No sound input device, receiver exiting.");
				return;
			}

			// float min = 1.0f;
			// float max = -1.0f;
			ByteBuffer byteBuffer = ByteBuffer.wrap(capture_buffer).order(ByteOrder.LITTLE_ENDIAN);
			float[] f = new float[capture_buffer.length / 2];
			log.debug("Listening for packets");
			
			log.debug("audioInputStream.isAvailable {}", audioInputStream.available()) ;
			
			while (true) {
				int audioData;

				// https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/javax/sound/sampled/AudioInputStream.html
				audioData = audioInputStream.read(capture_buffer, 0, capture_buffer.length);
				
				if (-1 == audioData) {
					// end of stream reached
					break;
				}
				
				log.debug("read {} bytes of audio",audioData);
				byteBuffer.rewind();
				for (int i = 0; i < audioData / 2; i++) {
					short s = byteBuffer.getShort();
					f[i] = (float) s / 32768.0f;
					if (!display_audio_level) {
						continue;
					}
					j++;
					// System.out.printf("j=%d\n",j);
					// if (f[i] > max) max = f[i];
					// if (f[i] < min) min = f[i];
					if (j == rate) {
						// System.err.printf("Audio in range [%f, %f]\n",min,max);
						System.err.printf("Audio level %d\n", soundcardConsumer.peak());
						j = 0;
						// min = 1.0f;
						// max = -1.0f;
					}
				}
				soundcardConsumer.addSamples(f, audioData / 2);
			}

		} catch (Exception e) {
			log.error(e.toString(), e);
		}
	}

	public void setInputDevice(String input_device) {
		if (input_device != null) {
			File inputDeviceFile = new File(input_device);
			if (inputDeviceFile.exists()) {
				// try from a file
				setInputDeviceFile(inputDeviceFile);
			} else {
				// try from a mixer
				setInputDeviceMixer(input_device);
			}
		}
	}
	
	public void setInputDeviceFile(File audioInputFile) {
		log.info("entering setInputDeviceFile for file {}",audioInputFile.getAbsolutePath()) ;
		
		// length - the length in sample frames of the data in this stream
		int numberOfFrames = 1;
		try {
			audioInputStream = new AudioInputStream(new FileInputStream(audioInputFile), audioFormat, numberOfFrames);
		} catch (Exception e) {
			log.error(e.toString(), e);
		}
		log.info("leaving setInputDeviceFile for file {}",audioInputFile.getAbsolutePath()) ;
	}

	public void setInputDeviceMixer(String desiredMixer) {
		log.info("entering setInputDeviceMixer for mixer device {}",desiredMixer) ;

		TargetDataLine targetDataLine = null;

		Mixer.Info[] mixers = AudioSystem.getMixerInfo();

		for (Mixer.Info mixer : mixers) {
			if (mixer.getName().equalsIgnoreCase(desiredMixer)) {
				try {
					// https://docs.oracle.com/en/java/javase/11/docs/api/java.desktop/javax/sound/sampled/TargetDataLine.html
					targetDataLine = AudioSystem.getTargetDataLine(audioFormat, mixer);
					System.err.println("Opened an input sound device (target line): " + mixer);
				} catch (LineUnavailableException lue) {
					System.err.println("Sound input device not available: " + desiredMixer);
				} catch (IllegalArgumentException iae) {
					System.err.println("Failed to open an input sound device: " + iae.getMessage());
				}
			}
		}

		if (targetDataLine == null) {
			System.err.println("Sound device not found (or is not an input device): " + desiredMixer);
			return;
		}

		// Control[] controls = tdl.getControls();
		// for (Control c: controls) {
		// System.out.println(" Control: +"+c.getType().getClass());
		// }

		int buffer_size_in_samples = (int) Math.round(latency_ms * ((double) rate / 1000.0));
		try {
			audioInputStream = new AudioInputStream(targetDataLine);
			targetDataLine.open(audioFormat, 2 * buffer_size_in_samples);
			targetDataLine.start();
		} catch (LineUnavailableException e) {
			targetDataLine = null;
			audioInputStream = null ;
			log.error(e.toString(),e) ;
		}
		log.info("leaving setInputDeviceMixer for mixer device {}",desiredMixer) ;
	}

	public void setOutputDevice(String desiredMixer) {

		Mixer.Info[] mixers = AudioSystem.getMixerInfo();

		for (Mixer.Info mixer : mixers) {
			if (mixer.getName().equalsIgnoreCase(desiredMixer)) {
				// System.out.println("@@@");
				try {
					sourceDataLine = AudioSystem.getSourceDataLine(audioFormat, mixer);
					System.err.println("Opened a sound output device (source data line): " + desiredMixer);
				} catch (LineUnavailableException lue) {
					System.err.println("Sound output device not available: " + desiredMixer);
				} catch (IllegalArgumentException iae) {
					System.err.println("Failed to open a sound output device: " + iae.getMessage());
				}
			}
		}

		if (sourceDataLine == null) {
			System.err.println("Sound output device not found (or is not a playback device): " + desiredMixer);

			System.err.println("Using the computer's default audio output");

			try {
				sourceDataLine = AudioSystem.getSourceDataLine(audioFormat);
			} catch (Exception e) {
				log.error(e.toString(), e);
			}

		}

		int buffer_size_in_samples = (int) Math.round(latency_ms * ((double) rate / 1000.0));
		try {
			sourceDataLine.open(audioFormat, 2 * buffer_size_in_samples);
			// sourceDataLine.start();
		} catch (LineUnavailableException lue) {
			sourceDataLine = null;
			System.err.println("Cannot open sound output device device");
		}
	}

}
