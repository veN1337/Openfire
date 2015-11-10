package de.saniceptor;

import java.io.File;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
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

public class SaniceptorPlugin implements Plugin, PacketInterceptor {

	private InterceptorManager interceptorManager;

	public SaniceptorPlugin() {
		interceptorManager = InterceptorManager.getInstance();
	}

	@Override

	public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed)
			throws PacketRejectedException {
		if (processed || !incoming) {
			return;
		} else if (packet instanceof Message) {
			Message message = (Message) packet;
			if (message.getType() == Message.Type.chat && message.getBody() != null) {
				System.out.println(message.getFrom() + " -> " + message.getTo() + ": " + message.getBody());
				// message.setBody(message.getBody() + " appended");
				// processed = true;
				if (message.getBody().startsWith("?OTR")) {
					doOTRStuff(message);
				}
			}
		}
	}

	private void doOTRStuff(Message message) {
		String From = message.getFrom().toString();
		String To = message.getID().toString();
		String protocoll = "xmpp";

		SessionID session = new SessionID(To, From, protocoll);
		SessionImpl sessImp = new SessionImpl(session, new DummyOtrEngineHostImpl());

		String ses = "";
		try {
			ses = sessImp.transformReceiving(message.getBody());
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("JUHU!!!" + session.toString());
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

}

class DummyOtrEngineHostImpl implements OtrEngineHost {

	public void injectMessage(SessionID sessionID, String msg) throws OtrException {

		// connection.send(sessionID.getUserID(), msg);

		String msgDisplay = (msg.length() > 10) ? msg.substring(0, 10) + "..." : msg;
		System.out.println("IM injects message: " + msgDisplay);
	}

	public void smpError(SessionID sessionID, int tlvType, boolean cheated) throws OtrException {
		System.out.println("SM verification error with user: " + sessionID);
	}

	public void smpAborted(SessionID sessionID) throws OtrException {
		System.out.println("SM verification has been aborted by user: " + sessionID);
	}

	public void finishedSessionMessage(SessionID sessionID, String msgText) throws OtrException {
		System.out.println("SM session was finished. You shouldn't send messages to: " + sessionID);
	}

	public void finishedSessionMessage(SessionID sessionID) throws OtrException {
		System.out.println("SM session was finished. You shouldn't send messages to: " + sessionID);
	}

	public void requireEncryptedMessage(SessionID sessionID, String msgText) throws OtrException {
		System.out.println("Message can't be sent while encrypted session is not established: " + sessionID);
	}

	public void unreadableMessageReceived(SessionID sessionID) throws OtrException {
		System.out.println("Unreadable message received from: " + sessionID);
	}

	public void unencryptedMessageReceived(SessionID sessionID, String msg) throws OtrException {
		System.out.println("Unencrypted message received: " + msg + " from " + sessionID);
	}

	public void showError(SessionID sessionID, String error) throws OtrException {
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

	public OtrPolicy getSessionPolicy(SessionID ctx) {
		return new OtrPolicy() {

			@Override
			public void setWhitespaceStartAKE(boolean value) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setSendWhitespaceTag(boolean value) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setRequireEncryption(boolean value) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setErrorStartAKE(boolean value) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setEnableManual(boolean value) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setEnableAlways(boolean value) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setAllowV3(boolean value) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setAllowV2(boolean value) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setAllowV1(boolean value) {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean getWhitespaceStartAKE() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean getSendWhitespaceTag() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean getRequireEncryption() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public int getPolicy() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public boolean getErrorStartAKE() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean getEnableManual() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean getEnableAlways() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean getAllowV3() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean getAllowV2() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean getAllowV1() {
				// TODO Auto-generated method stub
				return false;
			}
		}; // TODO shit einfügen
	}

	public void askForSecret(SessionID sessionID, String question) {
		System.out.println("Ask for secret from: " + sessionID + ", question: " + question);
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
			return new OtrCryptoEngineImpl().getFingerprintRaw(getLocalKeyPair(sessionID).getPublic());
		} catch (OtrCryptoException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
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
		return new FragmenterInstructions(FragmenterInstructions.UNLIMITED, FragmenterInstructions.UNLIMITED);
	}

}
