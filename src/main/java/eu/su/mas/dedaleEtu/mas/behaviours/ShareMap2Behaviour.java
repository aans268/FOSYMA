package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.util.List;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.FSMTRYOUTAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

/**
 * Partage simple de carte
 */
public class ShareMap2Behaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 21390580370145612L;
	private boolean finished = false;
	
	private MapRepresentation myMap;
	
	private FSMTRYOUTAgent agent;

	private List<String> receivers;

	
	public ShareMap2Behaviour(Agent a, long period, List<String> receivers) {
		super(a);
		//this.myMap = myMap;
		this.receivers = receivers;
		this.agent = (FSMTRYOUTAgent) this.myAgent;
	}

	@Override
	public void action() {
		
		//System.out.println(agent);
		myMap= agent.getAgentMap();
		
		try {
			this.myAgent.doWait(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Création du ping qui permet de savoir s'il y a des agents à
		// proximité avec qui on peut partager la carte
		ACLMessage ping = new ACLMessage(ACLMessage.INFORM);
		ping.setProtocol("PING");
		ping.setSender(this.myAgent.getAID());
		
		for (String agentName : receivers) {
			ping.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}
		
		// Envoie du ping
		((AbstractDedaleAgent)this.myAgent).sendMessage(ping);
		
		// Création d'un template qui s'attend à recevoir un ping
		MessageTemplate msgTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("PING"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
		
		// L'agent a reçu un ping donc l'agent envoie sa carte
		if (msgReceived != null) {
			
			// On crée le message contenant la carte et on l'envoie
			// à tous les agents qui peuvent la recevoir
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.setProtocol("SHARE-TOPO");
			msg.setSender(this.myAgent.getAID());
			for (String agentName : receivers) {
				msg.addReceiver(new AID(agentName,AID.ISLOCALNAME));
			}
			
			// Sérialisation
			SerializableSimpleGraph<String, MapAttribute> sg = this.myMap.getSerializableGraph();
			
			try {					
				msg.setContentObject(sg);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// Envoie de la carte
			((AbstractDedaleAgent)this.myAgent).sendMessage(msg);
		}
		finished=true;
	}
	
	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	@Override
	public boolean done() {
		return finished;
	}

}
