/**
 * 
 */
package org.minima.utils.messages;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.minima.utils.MinimaLogger;

/**
 * @author Spartacus Rex
 *
 */
public abstract class MessageProcessor extends MessageStack implements Runnable{

	/**
	 * Main Thread loop
	 */
    private Thread mMainThread;
    
    /**
     * Are we running
     */
    private boolean mRunning;
    
    /**
     * Have we finbished shutting down
     */
    private boolean mShutDownComplete;
    
	/**
	 * LOG messages ?
	 */
	protected boolean mTrace = false;
	
	/**
	 * Processor Name
	 */
	String mName;
	
	/**
	 * Constructor
	 */
    public MessageProcessor(String zName){
    	super();
    	
    	mName 				= zName;
    	mRunning 			= true;
    	mShutDownComplete 	= false;
    	
    	mMainThread = new Thread(this,zName);
        mMainThread.start();
    }
    
    public void setFullLogging(boolean zLogON) {
    	mTrace = zLogON;
    }
    
    public boolean isTrace() {
    	return mTrace;
    }
    
    public boolean isRunning(){
    	return mRunning;
    }
    
    public boolean isShutdownComplete() {
    	return mShutDownComplete;
    }
    
    public void stopMessageProcessor(){
        mRunning = false;
        
        //Wake it up if is locked..
        notifyLock();
    }
    
    public void PostTimerMessage(TimerMessage zMessage) {    	
    	//Set this is the processor..
    	zMessage.setProcessor(this);
    	
    	//Post it on the TimerProcessor
    	TimerProcessor.getTimerProcessor().PostMessage(zMessage);
    }
    
    public void run() {
    	//Format the time
    	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");

        //Loop while still running
        while(mRunning){
            //Check for valid mnessage
            Message msg = getNextMessage();
            
            //Cycle through available messages
            while(msg != null && mRunning){          
                //Process that message
                try{
                	//Are we logging  ?
                	if(mTrace) {
                		MinimaLogger.log("["+getSize()+"] "+sdf.format(new Date())+" [ "+mMainThread.getName()+" ] \t"+msg);
                	}
                
                	//Process Message
                    processMessage(msg);
                
                }catch(Error noclass){
                	MinimaLogger.log("**SERIOUS SETUP ERROR "+msg.getMessageType()+" "+noclass.toString());
                	
                }catch(Exception exc){
                	MinimaLogger.log("MESSAGE PROCESSING ERROR @ "+msg.getMessageType());
                	MinimaLogger.log(exc);
                } 
                
                //Are there more messages..
                msg = getNextMessage();
            }
            
            //Wait.. for a notify.. 
            try {
            	synchronized (mLock) {
            		//Last check.. inside the LOCK
            		if(!isNextMessage() && mRunning) {
            			//Wait for a message to be posted on the stack
            			mLock.wait();	
            		}
				}
			} catch (InterruptedException e) {
				MinimaLogger.log("MESSAGE_PROCESSOR "+mName+" INTERRUPTED");
			}
        }

        //All done..
        mShutDownComplete = true;
    }
    
    /**
     * This is the main processing unit, must be overloaded in extends classes
     * @param zMessage The Full Message
     * @throws Exception
     */
    protected abstract void processMessage(Message zMessage) throws Exception;
}

