package org.jdamico.javax25;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Properties;

import org.jdamico.javax25.TestHelper;
import org.jdamico.javax25.ax25.Afsk1200Modulator;
import org.jdamico.javax25.ax25.Afsk1200MultiDemodulator;
import org.jdamico.javax25.ax25.Packet;
import org.jdamico.javax25.ax25.PacketDemodulator;
import org.jdamico.javax25.radiocontrol.SerialTransmitController;
import org.jdamico.javax25.radiocontrol.TransmitController;
import org.jdamico.javax25.soundcard.Soundcard;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
    	Soundcard.enumerate();
    	SerialTransmitController.enumerate();
    	
    	Properties p = System.getProperties();
    	
    	int rate = 48000;
		int filter_length = 32;
    	
    	TestHelper t = new TestHelper();
		//System.out.printf("%d %d\n",rate,filter_length);
		Afsk1200Modulator mod = null;
		//Afsk1200 afsk = null;
		//PacketDemodulator afsk0 = null;
		//PacketDemodulator afsk6 = null;
		PacketDemodulator multi = null;
		try {
		  //afsk = new Afsk1200(rate,filter_length,0,t);
		  //afsk0 = new Afsk1200Demodulator(rate,filter_length,0,t);
		  //afsk6 = new Afsk1200Demodulator(rate,filter_length,6,t);
		  multi = new Afsk1200MultiDemodulator(rate,t);
		  mod = new Afsk1200Modulator(rate);
		} catch (Exception e) {
			System.out.println("Exception trying to create an Afsk1200 object: "+e.getMessage());
			System.exit(1);
		}
		
		/*** create test packet to transmit ***/
		
	  //byte[] contents = "Test Packet Hex FF=?; Done!".getBytes();
	  //contents[19] = (byte) 0xff;
	  //Packet packet = new Packet(contents);
		String callsign = "NOCALL";
	  
		System.out.println("Callsign in test packet is: "+callsign);
		
	  Packet packet = new Packet("APRS",
        callsign,
        new String[] {"WIDE1-1", "WIDE2-2"},
        Packet.AX25_CONTROL_APRS,
        Packet.AX25_PROTOCOL_NO_LAYER_3,
        ">Java Modem Test Packet".getBytes());

	  /*** loopback: testing the modem without sound ***/
	  
//	  if (p.containsKey("loopback")) {
//		  //ae.afsk.transmit(new Packet(contents));
//	  	System.out.println("Loopback test");
//		  mod.prepareToTransmit(packet);
//		  float[] tx_samples = mod.getTxSamplesBuffer();
//		  int n;
//		  while ((n = mod.getSamples()) > 0){
//		  	//System.out.printf("sending %d samples",n);
//		  	//afsk.addSamples(Arrays.copyOf(tx_samples, n));
//		  	multi.addSamples(tx_samples, n);
//		  }
//			System.exit(0);
//		}
    	
	  
	  /*** write a test packet to a text file ***/

		String fout = "/tmp/japrs.txt";
		if (fout != null) {	
			System.out.printf("Writing transmit packets to <%s>\n",fout);
			FileOutputStream f = null;
			PrintStream ps = null;
			try {
				f = new FileOutputStream(fout);
				ps = new PrintStream(f);
			} catch (FileNotFoundException fnfe) {
				System.err.println("File "+fout+" not found: "+fnfe.getMessage());
				
			}
		  mod.prepareToTransmit(packet);
		  int n;
		  float[] tx_samples = mod.getTxSamplesBuffer();
		  while ((n = mod.getSamples()) > 0) {
		  	for (int i=0; i<n; i++)
		  	  ps.printf("%09e\n",tx_samples[i]);
		  }
		  ps.close();
		}
		
		/*** preparing to generate or capture audio packets ***/
		  
		String input = null;
		String output = "PulseAudio Mixer";

		int buffer_size = -1;
		try {
			// our default is 100ms
			buffer_size = Integer.parseInt(p.getProperty("latency", "100").trim());
			//if (buffer_size ==-1) buffer_size = sc.rate/10;
			//ae.capture_buffer = new byte[2*buffer_size];
		} catch (Exception e){
			System.err.println("Exception parsing buffersize "+e.toString());
		}
		
		Soundcard sc = new Soundcard(rate,input,output,buffer_size,multi,mod);

		if (p.containsKey("audio-level")) {
			sc.displayAudioLevel();
		}

		/*** generate test tones and exit ***/

		TransmitController ptt = null;
		
		int tones_duration = -1; // in seconds
		try {
			tones_duration = Integer.parseInt(p.getProperty("tones", "-1").trim());
		} catch (Exception e){
			System.err.println("Exception parsing tones "+e.toString());
		}
		if (tones_duration > 0) {
  		//sc.openSoundOutput(output);			
		  mod.prepareToTransmitFlags(tones_duration);
		  sc.transmit();
		}

		/*** sound a test packet and exit ***/

		if (output != null) {
  		//sc.openSoundOutput(output);			
		  mod.prepareToTransmit(packet);
		  System.out.printf("Start transmitter\n");
		  //sc.startTransmitter();
			if (ptt != null) ptt.startTransmitter();
		  sc.transmit();
		  System.out.printf("Stop transmitter\n");
			if (ptt != null) ptt.stopTransmitter();
			if (ptt != null) ptt.close();
			//if (ptt != null) ptt.stopTransmitter());
		  //sc.stopTransmitter();
		  //int n;
		  //while ((n = ae.afsk.getSamples()) > 0){
		 // 	ae.afsk.addSamples(Arrays.copyOf(tx_samples, n));
		  //}
			System.exit(0);
		}
		
		
	  
    }
}
