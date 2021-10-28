package org.minima.system.commands.all;

import org.minima.database.MinimaDB;
import org.minima.objects.TxPoW;
import org.minima.system.commands.Command;
import org.minima.utils.json.JSONObject;

public class txpow extends Command {

	public txpow() {
		super("txpow","[txpowid:txpowid] - Search for a specific TxPoW");
	}
	
	@Override
	public JSONObject runCommand() throws Exception{
		JSONObject ret = getJSONReply();
		
		//Get the txpowid
		Object txpowid = getParams().get("txpowid");
		
		if(txpowid == null) {
			throw new Exception("No txpowid parameter specified");
		}
		
		//Search for a given txpow
		TxPoW txpow = MinimaDB.getDB().getTxPoWDB().getTxPoW((String)txpowid);
		
		if(txpow == null) {
			throw new Exception("TxPoW not found : "+txpowid);
		}
		
		ret.put("status", true);
		ret.put("response", txpow.toJSON());
	
		return ret;
	}

	@Override
	public Command getFunction() {
		return new txpow();
	}

}