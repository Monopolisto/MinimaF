package org.minima.utils.messages;

import java.util.LinkedList;

import org.minima.utils.MinimaLogger;

public class TimerProcessor implements Runnable {

	/**
	 * Static function for all Timed Messages
	 */
	private static TimerProcessor mTimerProcessor = new TimerProcessor();
	public static TimerProcessor getTimerProcessor() {
		return mTimerProcessor;
	}
	public static void  stopTimerProcessor() {
		mTimerProcessor.stop();
	}
	
	/**
	 * Are we running
	 */
	private boolean mRunning;
	
	/**
	 * All the timed messages
	 */
	private LinkedList<TimerMessage> mTimerMessages;
	
	public TimerProcessor() {
		mRunning = true;
		mTimerMessages = new LinkedList<TimerMessage>();
		
		Thread runner = new Thread(this);
		runner.start();
	}
	
	public void stop() {
		mRunning = false;
	}
	
	public void PostMessage(TimerMessage zMessage) {
		synchronized (mTimerMessages) {
			mTimerMessages.add(zMessage);
		}
	}
	
	@Override
	public void run() {
		while(mRunning) {
			
			//Check the stack for messages..
			synchronized (mTimerMessages) {
				//New list to store the ongoing timers
				LinkedList<TimerMessage> newlist = new LinkedList<TimerMessage>();
				
				//Current time
				long time = System.currentTimeMillis();
				
				//Cycle through all the timers
				for(TimerMessage tm : mTimerMessages) {
					//Get the time..
					if(tm.getTimer()<time) {
						//Who get's it
						MessageProcessor process = tm.getProcessor();
						
						//And Post..
						if(process.isRunning()) {
							process.PostMessage(tm);
						} else {
							MinimaLogger.log("Timer Message NOT run as processor shutdown.. "+tm.toString());
						}
					}else {
						//Keep for next test
						newlist.add(tm);
					}
				}
				
				//Swap lists.
				mTimerMessages = newlist;
			}
			
			//Small sleep.. 10s..
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				MinimaLogger.log(e);
				mRunning = false;
			}
		}
	}

}