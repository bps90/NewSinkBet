package projects.BetEtt.nodes.nodeImplementations;

import java.awt.Color;
import java.awt.Graphics;

import projects.BetEtt.nodes.edges.EdgeBetEtt;
import projects.BetEtt.nodes.messages.BetEttHelloMessage;
import projects.BetEtt.nodes.nodeImplementations.NodeRoleBetEtt;
import projects.BetEtt.nodes.timers.BetEttMessageTimer;
import projects.defaultProject.nodes.timers.MessageTimer;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import sinalgo.nodes.messages.NackBox;
import sinalgo.tools.Tools;

public class NodeBetEtt extends Node {
	// Qual o papel do nodo
		private NodeRoleBetEtt role;

		// ID do no sink
		private int sinkID;

		// numero de hops ate o sink
		private int hops;

		// id do prox no usando a metrica numero de hops
		private int nextHop;
		
		//numero de caminhos para o sink
		private int pathsToSink;	
		
		//rate acumulado do caminho
		private double pathEtt = Integer.MAX_VALUE;

		// Flag para indicar se o nodo ja enviou seu pkt hello
		private boolean sentMyHello = false;

		// Flag para indicar se o nodo ja enviou seu pkt border
		private boolean sentMyReply = false;
		
		//Disparadores de flood
		BetEttMessageTimer fhp;
		BetEttMessageTimer frp;

		@Override
		public void handleMessages(Inbox inbox) {
			// TODO Auto-generated method stub
			while (inbox.hasNext()) {
				Message m = inbox.next();
				 
				
				if(m instanceof BetEttHelloMessage){
					BetEttHelloMessage msg = (BetEttHelloMessage) m;
					System.out.println("-------------MSG arrive------------------");
					System.out.println("Conteúdo: "+msg.toString());
					System.out.println("De: "+inbox.getSender());
					System.out.println("Para: "+inbox.getReceiver());
					System.out.println("Chegou em: "+inbox.getArrivingTime());
					System.out.println("-------------MSG END------------------");
					 
					
					handleHello(inbox.getSender(), inbox.getReceiver(), (EdgeBetEtt) inbox.getIncomingEdge(), msg);
				} 
				
			}

		}

		public void handleNAckMessages(NackBox nackBox) {
			while (nackBox.hasNext()) {
				/*Message msg = nackBox.next();
				StringMessage m = (StringMessage) msg;
				
				 * System.out.println("-------------NACK arrive------------------");
				 * System.out.println("Conteúdo: "+m.text);
				 * System.out.println("De: "+nackBox.getSender());
				 * System.out.println("Para: "+nackBox.getReceiver());
				 * System.out.println("Chegou em: "+nackBox.getArrivingTime());
				 * System.out.println("-------------NACK END------------------");
				 * System.out.println("\n\nResending"); (new MessageTimer(m,
				 * nackBox.getReceiver())).startRelative(1, this);
				 */

			}
		}

		
		private void handleHello(Node sender, Node receiver, EdgeBetEtt incomingEdge, BetEttHelloMessage msg) {
			
			// no sink nao manipula pacotes hello
			if(this.ID == msg.getSinkID()){
				msg = null;
				return;
			}
			
			EdgeBetEtt edgeToSender = (EdgeBetEtt) incomingEdge.oppositeEdge;
			
			// o nodo e vizinho direto do sink (armazena o nextHop como id do sink
			if(msg.getSinkID() == sender.ID){
				nextHop = sender.ID;
			}
			
			// nodo acaba de ser descoberto ou acabou de encontrar um caminho mais curto
			if((msg.getPathEtt() + edgeToSender.getEtt() < pathEtt) 
					|| pathEtt == Integer.MAX_VALUE){
				
				sinkID = msg.getSinkID();
				
				hops = msg.getHops() + 1;
				msg.setHops(hops);
				
				pathsToSink = msg.getPaths();
				
				nextHop = sender.ID;
				
				pathEtt = msg.getPathEtt() + edgeToSender.getEtt();
				msg.setPathEtt(pathEtt);
				
				sentMyHello = false;
			}
			
			// existe mais de um caminho deste no ate o sink com a mesmo ett acumulado
			if(msg.getPathEtt() + edgeToSender.getEtt() == pathEtt){
				pathsToSink += msg.getPaths();
				fhp.updateTimer(1, this);
				
			}
			
			if(!isSentMyHello()){
				fhp = new BetEttMessageTimer(msg);
				fhp.startRelative(1, this);
				sentMyHello = true;
			}
			
			
		}
		
		@Override
		public void preStep() {
			// TODO Auto-generated method stub

		}

		@Override
		public void init() {
			// TODO Auto-generated method stub
			if (this.ID == 1) {
				this.setColor(Color.GREEN);
			}
		}

		@Override
		public void neighborhoodChange() {
			// TODO Auto-generated method stub

		}

		@Override
		public void postStep() {
			// TODO Auto-generated method stub

		}

		@Override
		public void checkRequirements() throws WrongConfigurationException {
			// TODO Auto-generated method stub

		}

		@NodePopupMethod(menuText = "Start")
		public void start() {
			
			BetEttHelloMessage hellomsg = new BetEttHelloMessage(0, 1, this.ID, 0);
			MessageTimer mt = new MessageTimer(hellomsg);
			mt.startRelative(1, this);
			
			this.setColor(Color.BLUE);
			
			Tools.appendToOutput("Node "+ this.ID +" start hello flood from.");
		}

		public void draw(Graphics g, PositionTransformation pt, boolean highlight) {

			String str = Integer.toString(this.ID);

			super.drawNodeAsSquareWithText(g, pt, highlight, str, 30, Color.YELLOW);
			// super.drawAsRoute(g, pt, highlight, 30);
		}

		public void sendUnicastRateMsg(Message msg, Node receiver) {
			// TODO Auto-generated method stub
			
		}

		public void broadcastRateMsg(Message msg) {
			if(msg instanceof BetEttHelloMessage){
				BetEttHelloMessage m = (BetEttHelloMessage) msg;
				m.setHops(hops);
				m.setPathEtt(pathEtt);
				m.setPaths(pathsToSink);
				m.setSinkID(sinkID);
				
				broadcast(m);
			}
			
		}
		
		
		@Override
		public String toString() {
			return "NodeEtt [role=" + role + 
					"\nsinkID=" + sinkID + 
					"\nhops="+ hops + 
					"\npathsToSink="+ pathsToSink + 
					"\nnextHop=" + nextHop +
					"\npathEtt="+ String.format("%.2f", pathEtt) + 
					"\nsentMyHello=" + sentMyHello+ 
					"\nsentMyReply=" + sentMyReply + "]";
		}

		public boolean isSentMyHello() {
			return sentMyHello;
		}
		
		public boolean isSentMyReply() {
			return sentMyReply;
		}
		
}