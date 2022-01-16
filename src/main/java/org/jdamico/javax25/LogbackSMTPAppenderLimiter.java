package org.jdamico.javax25;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.net.SMTPAppender;
import ch.qos.logback.core.helpers.CyclicBuffer;

/**
 * https://github.com/qos-ch/logback/blob/master/logback-classic/src/main/java/ch/qos/logback/classic/net/SMTPAppender.java
 * https://github.com/qos-ch/logback/blob/master/logback-core/src/main/java/ch/qos/logback/core/net/SMTPAppenderBase.java
 */
public class LogbackSMTPAppenderLimiter extends SMTPAppender {
	
	private long millis_since_last_email = 0;
	private long interval = 60000; // 60 seconds
	private int suppressed = 0; // may want to log this in the future

	protected void sendBuffer(CyclicBuffer<ILoggingEvent> cb, ILoggingEvent lastEventObject) {

		if (java.lang.System.currentTimeMillis() - interval > millis_since_last_email) {
			super.sendBuffer(cb, lastEventObject);
			millis_since_last_email = java.lang.System.currentTimeMillis();
			suppressed = 0;
		} else {
			suppressed++;
		}
	}

	/**
	 * 0 - no limit 30000 - one email per 30 seconds 60000 - one email per minute
	 * 300000 - one email per 5 minutes
	 */
	public void setOneEmailPerMillis(long one_email_per_millis) {
		this.interval = one_email_per_millis;
	}

	public long getOneEmailPerMillis() {
		return this.interval;
	}

} // end of class
