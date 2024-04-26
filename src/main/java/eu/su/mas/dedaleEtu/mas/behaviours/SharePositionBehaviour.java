package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.List;
import java.util.Map;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.FSMTRYOUTAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Behaviour qui permet à un agent d'envoyer régulièrement sa position aux
 * autres agents et de mettre à jour la table des positions.
 */
public class SharePositionBehaviour extends SimpleBehaviour {
	
	private static final long serialVersionUID = 21390580370145612L;
	
	/**
	 * La carte interne de l'agent
	 */
	private MapRepresentation myMap;
	
	private FSMTRYOUTAgent agent= (FSMTRYOUTAgent) this.myAgent;
	
	/**
	 * La liste des agents à qui partager la carte
	 */
	private List<String> receivers;
	
	/**
	 * La table des positions des agents	
	 */
	private Map<String, String> agentsPositions;
	
	
	public SharePositionBehaviour(final AbstractDedaleAgent myAgent, List<String> receivers,Map<String, String> agentsPositions) {
		super(myAgent);
		//this.myMap = myMap;
		this.receivers = receivers;
		this.agentsPositions = agentsPositions;
	}

	@Override
	public void action() {
		
		myMap= agent.getAgentMap();
		
		try {
			this.myAgent.doWait(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// On récupère la position courante de l'agent
		Location myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		
		// On crée le message
		ACLMessage ping = new ACLMessage(ACLMessage.INFORM);
		ping.setProtocol("SHARE-POSITION");
		ping.setSender(this.myAgent.getAID());
		ping.setContent(myPosition.getLocationId());
		
		// On partage le message à tous les autres agents
		for (String agentName : receivers) {
			ping.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}
		
		// Envoie
		((AbstractDedaleAgent)this.myAgent).sendMessage(ping);
		
		int i=0;
		while(i<10) {
			// Création d'un template qui s'attend à recevoir un ping
			MessageTemplate msgTemplate = MessageTemplate.and(
					MessageTemplate.MatchProtocol("SHARE-POSITION"),
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
			
			// L'agent a reçu un ping, il met donc à jour sa table des positions
			if (msgReceived != null) {
				// On récupère le nom de l'agent qui lui a envoyé sa position
				String sender = msgReceived.getSender().getLocalName();
				
				// On récupère la position de ce dernier
				String position = msgReceived.getContent();
				
				// On met à jour la table des positions
				this.agentsPositions.put(sender, position);
				
			}else {
				break;
			}
			i++;
			
		}
		
	}

	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return true;
	}

}
