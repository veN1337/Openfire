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

public class SaniceptorPlugin implements Plugin, PacketInterceptor {
	
	private InterceptorManager interceptorManager;
	
	public SaniceptorPlugin() {
		interceptorManager = InterceptorManager.getInstance();
	}

	@Override
	public void interceptPacket(Packet packet, Session session,	boolean incoming, boolean processed) throws PacketRejectedException {
//		System.out.println("executing interceptPacket");
		if (processed || !incoming) {
			return;
		} else if (packet instanceof Message) {
			Message message = (Message) packet;
			if(message.getType() == Message.Type.chat && message.getBody().startsWith("?OTR")) {
				PacketRejectedException ex = new PacketRejectedException();
				ex.setRejectionMessage("SERVER MESSAGE: OTR temporarily disabled!");
				throw ex;
			}
//			if(message.getType() == Message.Type.chat && message.getBody() != null) {
//				System.out.println(message.getFrom() + " -> " + message.getTo() + ": " + message.getBody());
//				message.setBody(message.getBody() + " appended");
//				processed = true;
//			}
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

}
