package de.saniceptor;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import net.java.otr4j.OtrEngineHost;
import net.java.otr4j.OtrException;
import net.java.otr4j.OtrPolicy;
import net.java.otr4j.crypto.OtrCryptoEngineImpl;
import net.java.otr4j.crypto.OtrCryptoException;
import net.java.otr4j.session.FragmenterInstructions;
import net.java.otr4j.session.InstanceTag;
import net.java.otr4j.session.SessionID;

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
			// String msgDisplay = (msg.length() > 10) ? msg.substring(0, 10) +
			// "..." : msg;
			// System.out.println("IM injects message: " + msgDisplay);
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