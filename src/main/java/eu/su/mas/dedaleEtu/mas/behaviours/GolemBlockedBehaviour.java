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
public class GolemBlockedBehaviour extends SimpleBehaviour {
	
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
	
	
	public GolemBlockedBehaviour(final AbstractDedaleAgent myAgent, List<String> receivers) {
		super(myAgent);
		//this.myMap = myMap;
		this.receivers = receivers;
		this.agentsPositions = agentsPositions;
	}

	@Override
	public void action() {
		
		myMap= agent.getAgentMap();
		
		try {
			this.myAgent.doWait(3000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// On récupère la position courante de l'agent
		Location myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		
		// On crée le message
		ACLMessage ping = new ACLMessage(ACLMessage.INFORM);
		ping.setProtocol("BLOCKED-GOLEM-POSITION");
		ping.setSender(this.myAgent.getAID());
		//ping.setContent(myPosition.getLocationId());
		ping.setContent(agent.getGolemPosition());
		
		// On partage le message à tous les autres agents
		for (String agentName : receivers) {
			ping.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}
		
		// Envoie
		((AbstractDedaleAgent)this.myAgent).sendMessage(ping);
		
		
	}

	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return false;
	}

}
