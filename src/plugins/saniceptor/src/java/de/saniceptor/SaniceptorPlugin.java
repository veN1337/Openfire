package de.saniceptor;

import java.io.File;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.OtrPolicyImpl;
import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.otr4j.session.FragmenterInstructions;
import net.java.otr4j.session.InstanceTag;
import net.java.otr4j.session.SessionID;
import net.java.otr4j.session.SessionImpl;
import net.java.otr4j.session.SessionStatus;

public class SaniceptorPlugin implements Plugin, PacketInterceptor, Component {

	private InterceptorManager interceptorManager;
	
	private ComponentManager componentManager;
	
	private Message newMes;
	
	private DummyOtrEngineHost hostA;
	private DummyOtrEngineHost hostB;
	private SessionImpl sessImplAC;
	private SessionImpl sessImplCB;
	private boolean var = true;

	public SaniceptorPlugin() {
		interceptorManager = InterceptorManager.getInstance();
		componentManager = ComponentManagerFactory.getComponentManager();
	}

	@Override
	public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed)
			throws PacketRejectedException {
		if (processed || !incoming) {
			return;
		} else if (packet instanceof Message) {
			Message message = (Message) packet;
			
			if(message == newMes) {
				return;
			}
			
			if (message.getType() == Message.Type.chat && message.getBody() != null) {
				
				if (message.getBody().startsWith("?OTR")) {
					doOTRStuff(message);
					throw new PacketRejectedException();
				}

			}

		}
	}

	@SuppressWarnings("finally")
	private void doOTRStuff(Message message) throws PacketRejectedException {
		
		JID from = message.getFrom();
		JID to = message.getTo();
		String protocoll = "prpl-jabber";
		
		if(sessImplAC == null || hostA == null) {
			
			System.out.println("Starte OTR Sitzung zwischen " + from.toBareJID() + " und Server");
			System.out.println("Starte OTR Sitzung zwischen Server und " + to.toBareJID());
			
			var = true;
			
			SessionID sessionCB = new SessionID(to.toBareJID(), from.toBareJID(), protocoll);
			SessionID sessionAC = new SessionID(from.toBareJID(), to.toBareJID(), protocoll);
			
			hostB = new DummyOtrEngineHost(new OtrPolicyImpl(OtrPolicy.ALLOW_V2 | OtrPolicy.ALLOW_V3
					| OtrPolicy.ERROR_START_AKE));
			hostA = new DummyOtrEngineHost(new OtrPolicyImpl(OtrPolicy.ALLOW_V2 | OtrPolicy.ALLOW_V3
					| OtrPolicy.ERROR_START_AKE));
			sessImplAC = new SessionImpl(sessionAC, hostA);
			sessImplCB = new SessionImpl(sessionCB, hostB);
			
		}
		
		try {

			// Wenn die Nachricht von A kommt (Client die OTR zuerst angefrage hat)
			if (from.toBareJID().equals(sessImplAC.getSessionID().getAccountID())) {
				
				// Bei noch nicht verschluesselter Session (D-H Austausch)
				if(!sessImplAC.getSessionStatus().equals(SessionStatus.ENCRYPTED)) {
					
					sessImplAC.transformReceiving(message.getBody());
					newMes = new Message();
					newMes.setType(Message.Type.chat);
					newMes.setBody(hostA.lastInjectedMessage);
					newMes.setFrom(to);
					newMes.setTo(from);
					componentManager.sendPacket(this, newMes);
					
				}
				
				// Bei verschluesselter Session (Entschluesseln, neu verschluesseln, weiterleiten)
				else if (sessImplAC.getSessionStatus().equals(SessionStatus.ENCRYPTED) && sessImplCB.getSessionStatus().equals(SessionStatus.ENCRYPTED) ) {
					String[] msg = sessImplCB.transformSending(sessImplAC.transformReceiving(message.getBody()));
					if (sessImplAC.getSessionStatus().equals(SessionStatus.FINISHED)) {
						//System.out.println("not forwarding because finished");					
					} else {
						System.out.println(from.toBareJID() + " -> " + to.toBareJID() + ": " + sessImplAC.transformReceiving(message.getBody()));
						for (String msgPart : msg) {
							newMes = new Message();
							newMes.setType(Message.Type.chat);
							newMes.setBody(msgPart);
							newMes.setFrom(from);
							newMes.setTo(to);
							componentManager.sendPacket(this, newMes);
						}
					}
				}
				
				
			// Wenn die Nachricht von B kommt
			} else if (from.toBareJID().equals(sessImplCB.getSessionID().getAccountID())) {
				
				// Bei noch nicht verschluesselter Session (D-H Austausch)
				if(!sessImplCB.getSessionStatus().equals(SessionStatus.ENCRYPTED)) {
					sessImplCB.transformReceiving(message.getBody());
					newMes = new Message();
					newMes.setType(Message.Type.chat);
					newMes.setBody(hostB.lastInjectedMessage);
					newMes.setFrom(to);
					newMes.setTo(from);
					componentManager.sendPacket(this, newMes);
				}
				
				// Bei verschluesselter Session (Entschluesseln, neu verschluesseln, weiterleiten)
				else if (sessImplAC.getSessionStatus().equals(SessionStatus.ENCRYPTED) && sessImplCB.getSessionStatus().equals(SessionStatus.ENCRYPTED) ) {
					String[] msg = sessImplAC.transformSending(sessImplCB.transformReceiving(message.getBody()));
					if (sessImplCB.getSessionStatus().equals(SessionStatus.FINISHED)) {
						//System.out.println("not forwarding because finished");
					} else {
						System.out.println(from.toBareJID() + " -> " + to.toBareJID() + ": " + sessImplCB.transformReceiving(message.getBody()));
						for (String msgPart : msg) {
							newMes = new Message();
							newMes.setType(Message.Type.chat);
							newMes.setBody(msgPart);
							newMes.setFrom(from);
							newMes.setTo(to);
							componentManager.sendPacket(this, newMes);
						}
					}
				}
			}
			
			
			//Wenn A->C erstellt und verschluesselt ist, sende OTR Anfrage an B, aber nur einmal
			if (sessImplAC.getSessionStatus().equals(SessionStatus.ENCRYPTED) && var) {
				var = false;
				newMes = new Message();
				newMes.setType(Message.Type.chat);
				newMes.setBody("<p>?OTRv23?\n" +
								"<span style=\"font-weight: bold;\">" + from.toBareJID() + "/</span> has requested an " + 
								"<a href=\"http://otr.cypherpunks.ca/\">Off-the-Record private conversation</a>. " +
								"However, you do not have a plugin to support that.\n" + 
								"See <a href=\"http://otr.cypherpunks.ca/\">http://otr.cypherpunks.ca/</a> for more information.</p>");
				newMes.setFrom(from);
				newMes.setTo(to);
				componentManager.sendPacket(this, newMes);
			}
			
			// Wenn A die Session beenden will sende diese Info an B
			if (sessImplAC.getSessionStatus().equals(SessionStatus.FINISHED)) {
				System.out.println("got a finished session from A, end B");
				sessImplCB.endSession();
				newMes = new Message();
				newMes.setType(Message.Type.chat);
				newMes.setBody(hostB.lastInjectedMessage);
				newMes.setFrom(from);
				newMes.setTo(to);
				componentManager.sendPacket(this, newMes);
				
				sessImplAC = null;
				hostA = null;
			}
			
			// Wenn B die Session beenden will sende diese Info an A
			if (sessImplCB.getSessionStatus().equals(SessionStatus.FINISHED)) {
				System.out.println("got a finished session from B, end A");
				sessImplAC.endSession();
				newMes = new Message();
				newMes.setType(Message.Type.chat);
				newMes.setBody(hostA.lastInjectedMessage);
				newMes.setFrom(from);
				newMes.setTo(to);
				componentManager.sendPacket(this, newMes);
				
				sessImplAC = null;
				hostA = null;
			}

			//System.out.println("SessionStatus: "+ sessImplAC.getSessionStatus().toString());

		} catch (OtrException e1) {
			e1.printStackTrace();
		} catch (ComponentException e) {
			e.printStackTrace();
		} catch (Exception ee) {
			ee.printStackTrace();
		} finally {
			throw new PacketRejectedException();
		}
		
	}

	@Override
	public void initializePlugin(PluginManager manager, File pluginDirectory) {
		System.out.println("init saniceptor");
		interceptorManager.addInterceptor(this);
	}

	@Override
	public void destroyPlugin() {
		System.out.println("destroying saniceptor");
		interceptorManager.removeInterceptor(this);
	}
	
	class DummyOtrEngineHost implements OtrEngineHost {

		public DummyOtrEngineHost(OtrPolicy policy) {
			this.policy = policy;
		}

		private OtrPolicy policy;
		public String lastInjectedMessage;

		public OtrPolicy getSessionPolicy(SessionID ctx) {
			return this.policy;
		}

		public void injectMessage(SessionID sessionID, String msg) {

			this.lastInjectedMessage = msg;
			//String msgDisplay = (msg.length() > 10) ? msg.substring(0, 10) + "..." : msg;
			//System.out.println("IM injects message: " + msgDisplay);
		}

		public void smpError(SessionID sessionID, int tlvType, boolean cheated)
				throws OtrException {
			System.out.println("SM verification error with user: " + sessionID);
		}

		public void smpAborted(SessionID sessionID) throws OtrException {
			System.out.println("SM verification has been aborted by user: "
					+ sessionID);
		}

		public void finishedSessionMessage(SessionID sessionID, String msgText) throws OtrException {
			System.out.println("SM session was finished. You shouldn't send messages to: "
					+ sessionID);
		}

		public void finishedSessionMessage(SessionID sessionID) throws OtrException {
			System.out.println("SM session was finished. You shouldn't send messages to: "
					+ sessionID);
		}

		public void requireEncryptedMessage(SessionID sessionID, String msgText)
				throws OtrException {
			System.out.println("Message can't be sent while encrypted session is not established: "
					+ sessionID);
		}

		public void unreadableMessageReceived(SessionID sessionID)
				throws OtrException {
			System.out.println("Unreadable message received from: " + sessionID);
		}

		public void unencryptedMessageReceived(SessionID sessionID, String msg)
				throws OtrException {
			System.out.println("Unencrypted message received: " + msg + " from "
					+ sessionID);
		}

		public void showError(SessionID sessionID, String error)
				throws OtrException {
			System.out.println("IM shows error to user: " + error);
		}

		public String getReplyForUnreadableMessage() {
			return "You sent me an unreadable encrypted message.";
		}

		public void sessionStatusChanged(SessionID sessionID) {
			// don't care.
		}

		public KeyPair getLocalKeyPair(SessionID paramSessionID) {
			KeyPairGenerator kg;
			try {
				kg = KeyPairGenerator.getInstance("DSA");
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				return null;
			}
			return kg.genKeyPair();
		}

		public void askForSecret(SessionID sessionID, String question) {
			System.out.println("Ask for secret from: " + sessionID + ", question: "
					+ question);
		}

		public void verify(SessionID sessionID, boolean approved) {
			System.out.println("Session was verified: " + sessionID);
			if (!approved)
				System.out.println("Your answer for the question was verified."
						+ "You should ask your opponent too or check shared secret.");
		}

		public void unverify(SessionID sessionID) {
			System.out.println("Session was not verified: " + sessionID);
		}

		public byte[] getLocalFingerprintRaw(SessionID sessionID) {
			try {
				return new OtrCryptoEngineImpl()
						.getFingerprintRaw(getLocalKeyPair(sessionID)
								.getPublic());
			} catch (OtrCryptoException e) {
				e.printStackTrace();
			}
			return null;
		}

		public void askForSecret(SessionID sessionID, InstanceTag receiverTag, String question) {

		}

		public void verify(SessionID sessionID, String fingerprint, boolean approved) {

		}

		public void unverify(SessionID sessionID, String fingerprint) {

		}

		public String getReplyForUnreadableMessage(SessionID sessionID) {
			return null;
		}

		public String getFallbackMessage(SessionID sessionID) {
			return null;
		}

		public void messageFromAnotherInstanceReceived(SessionID sessionID) {

		}

		public void multipleInstancesDetected(SessionID sessionID) {

		}

		public String getFallbackMessage() {
			return "Off-the-Record private conversation has been requested. However, you do not have a plugin to support that.";
		}
		
		public FragmenterInstructions getFragmenterInstructions(SessionID sessionID) {
			return new FragmenterInstructions(FragmenterInstructions.UNLIMITED,
					FragmenterInstructions.UNLIMITED);
		}

	}

	@Override
	public void initialize(JID arg0, ComponentManager arg1)
			throws ComponentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}
	
    public String getName() {
        // Get the name from the plugin.xml file.
        return "Saniceptor";
    }

    public String getDescription() {
        // Get the description from the plugin.xml file.
        return "Saniceptor";
    }

    public void processPacket(Packet packet) {
        
    }

}
