package org.jdamico.javax25;

import static org.junit.Assert.*;

import org.jdamico.javax25.ax25.Packet;
import org.junit.Before;
import org.junit.Test;

public class AprsWeatherDataTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test() {
		String callsign = "NOCALL";
		System.out.println("Callsign in test packet is: "+callsign);
		
		String hms_position = "092122z2332.53S/04645.51W";
		String weather_data = "_220/004g005t-07r000p000P000h50b09900xSCI";
		
		String complete_weather_data = hms_position+weather_data;
		
		
		
		Packet packet = new Packet("APRS",
				callsign,
				new String[] {"WIDE1-1", "WIDE2-2"},
				Packet.AX25_CONTROL_APRS,
				Packet.AX25_PROTOCOL_NO_LAYER_3,
				complete_weather_data.getBytes());
		
		System.out.println(packet);
		
	}

}
