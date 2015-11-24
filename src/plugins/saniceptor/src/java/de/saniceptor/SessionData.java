package de.saniceptor;

import org.xmpp.packet.JID;

import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionImpl;

public class SessionData {
	
	private DummyOtrEngineHost host;
	private SessionImpl sessImplAC;
	private SessionImpl sessImplCB;
	private boolean var = true;
	private JID from;
	private JID to;
	
	SessionData(JID from, JID to) {
		this.from = from;
		this.to = to;
		var = true;
		String protocoll = "prpl-jabber";
		
		SessionID sessionCB = new SessionID(this.to.toBareJID(), this.from.toBareJID(), protocoll);
		SessionID sessionAC = new SessionID(this.from.toBareJID(), this.to.toBareJID(), protocoll);
		host = new DummyOtrEngineHost(new OtrPolicyImpl(OtrPolicy.ALLOW_V2 | OtrPolicy.ALLOW_V3 | OtrPolicy.ERROR_START_AKE));
		sessImplAC = new SessionImpl(sessionAC, host);
		sessImplCB = new SessionImpl(sessionCB, host);
		
		System.out.println("Beginne OTR Session zwischen " + from.toBareJID() + " und Server");
		System.out.println("Beginne OTR Session zwischen Server und " + to.toBareJID());
		
	}
	
	public DummyOtrEngineHost getHost() {
		return host;
	}
	public void setHost(DummyOtrEngineHost host) {
		this.host = host;
	}
	public SessionImpl getSessImplAC() {
		return sessImplAC;
	}
	public void setSessImplAC(SessionImpl sessImplAC) {
		this.sessImplAC = sessImplAC;
	}
	public SessionImpl getSessImplCB() {
		return sessImplCB;
	}
	public void setSessImplCB(SessionImpl sessImplCB) {
		this.sessImplCB = sessImplCB;
	}
	public boolean isVar() {
		return var;
	}
	public void setVar(boolean var) {
		this.var = var;
	}
}
