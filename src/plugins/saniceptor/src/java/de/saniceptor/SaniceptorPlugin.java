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
import java.util.HashMap;
import net.java.otr4j.OtrException;
import net.java.otr4j.session.SessionStatus;

public class SaniceptorPlugin implements Plugin, PacketInterceptor, Component {

	private InterceptorManager interceptorManager;
	private ComponentManager componentManager;
	private HashMap<String, SessionData> otrSessions = new HashMap<String, SessionData>();
	private Message newMes;
	private SessionData sd;

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

			if (message == newMes) {
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
		String key;
		
		if (from.toBareJID().compareTo(to.toBareJID()) < 0) {
			key = from.toBareJID() + to.toBareJID();
		} else {
			key = to.toBareJID() + from.toBareJID();
		}
		
		if(!otrSessions.containsKey(key)) {
			sd = new SessionData(from, to);
			otrSessions.put(key , sd);
		} else {
			sd = otrSessions.get(key);
		}
		
		try {

			// Wenn die Nachricht von A kommt (Client die OTR zuerst angefrage
			// hat)
			if (from.toBareJID().equals(sd.getSessImplAC().getSessionID().getAccountID())) {

				// Bei noch nicht verschluesselter Session (D-H Austausch)
				if (!sd.getSessImplAC().getSessionStatus().equals(SessionStatus.ENCRYPTED)) {

					sd.getSessImplAC().transformReceiving(message.getBody());
					newMes = new Message();
					newMes.setType(Message.Type.chat);
					newMes.setBody(sd.getHost().lastInjectedMessage);
					newMes.setFrom(to);
					newMes.setTo(from);
					componentManager.sendPacket(this, newMes);

				}

				// Bei verschluesselter Session (Entschluesseln, neu
				// verschluesseln, weiterleiten)
				else if (sd.getSessImplAC().getSessionStatus().equals(SessionStatus.ENCRYPTED)
						&& sd.getSessImplCB().getSessionStatus().equals(SessionStatus.ENCRYPTED)) {
					String[] msg = sd.getSessImplCB().transformSending(sd.getSessImplAC().transformReceiving(message.getBody()));
					if (sd.getSessImplAC().getSessionStatus().equals(SessionStatus.FINISHED)) {
						// System.out.println("not forwarding because finished");
					} else {
						System.out.println(from.toBareJID() + " -> " + to.toBareJID() + ": "
								+ sd.getSessImplAC().transformReceiving(message.getBody()));
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
			} else if (from.toBareJID().equals(sd.getSessImplCB().getSessionID().getAccountID())) {

				// Bei noch nicht verschluesselter Session (D-H Austausch)
				if (!sd.getSessImplCB().getSessionStatus().equals(SessionStatus.ENCRYPTED)) {
					sd.getSessImplCB().transformReceiving(message.getBody());
					newMes = new Message();
					newMes.setType(Message.Type.chat);
					newMes.setBody(sd.getHost().lastInjectedMessage);
					newMes.setFrom(to);
					newMes.setTo(from);
					componentManager.sendPacket(this, newMes);
				}

				// Bei verschluesselter Session (Entschluesseln, neu
				// verschluesseln, weiterleiten)
				else if (sd.getSessImplAC().getSessionStatus().equals(SessionStatus.ENCRYPTED)
						&& sd.getSessImplCB().getSessionStatus().equals(SessionStatus.ENCRYPTED)) {
					String[] msg = sd.getSessImplAC().transformSending(sd.getSessImplCB().transformReceiving(message.getBody()));
					if (sd.getSessImplCB().getSessionStatus().equals(SessionStatus.FINISHED)) {
						// System.out.println("not forwarding because finished");
					} else {
						System.out.println(from.toBareJID() + " -> " + to.toBareJID() + ": "
								+ sd.getSessImplCB().transformReceiving(message.getBody()));
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

			// Wenn A->C erstellt und verschluesselt ist, sende OTR Anfrage an
			// B, aber nur einmal
			if (sd.getSessImplAC().getSessionStatus().equals(SessionStatus.ENCRYPTED) && sd.isVar()) {
				sd.setVar(false);
				newMes = new Message();
				newMes.setType(Message.Type.chat);
				newMes.setBody("<p>?OTRv23?\n" + "<span style=\"font-weight: bold;\">" + from.toBareJID()
						+ "/</span> has requested an "
						+ "<a href=\"http://otr.cypherpunks.ca/\">Off-the-Record private conversation</a>. "
						+ "However, you do not have a plugin to support that.\n"
						+ "See <a href=\"http://otr.cypherpunks.ca/\">http://otr.cypherpunks.ca/</a> for more information.</p>");
				newMes.setFrom(from);
				newMes.setTo(to);
				componentManager.sendPacket(this, newMes);
			}

			// Wenn A die Session beenden will sende diese Info an B
			if (sd.getSessImplAC().getSessionStatus().equals(SessionStatus.FINISHED)) {
				//System.out.println("got a finished session from A, end B");
				sd.getSessImplCB().endSession();
				newMes = new Message();
				newMes.setType(Message.Type.chat);
				newMes.setBody(sd.getHost().lastInjectedMessage);
				newMes.setFrom(from);
				newMes.setTo(to);
				componentManager.sendPacket(this, newMes);

				sd.setSessImplAC(null);
				sd.setHost(null);
				otrSessions.remove(key);
			}

			// Wenn B die Session beenden will sende diese Info an A
			if (sd.getSessImplCB().getSessionStatus().equals(SessionStatus.FINISHED)) {
				//System.out.println("got a finished session from B, end A");
				sd.getSessImplAC().endSession();
				newMes = new Message();
				newMes.setType(Message.Type.chat);
				newMes.setBody(sd.getHost().lastInjectedMessage);
				newMes.setFrom(from);
				newMes.setTo(to);
				componentManager.sendPacket(this, newMes);

				sd.setSessImplCB(null);
				sd.setHost(null);
				otrSessions.remove(key);
			}

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

	

	@Override
	public void initialize(JID arg0, ComponentManager arg1) throws ComponentException {
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
