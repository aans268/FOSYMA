package eu.su.mas.dedaleEtu.mas.behaviours;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Random;

import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.FSMTRYOUTAgent;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class HuntBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;

	private boolean finished = true;

	private MapRepresentation myMap;
	
	private FSMTRYOUTAgent agent= (FSMTRYOUTAgent) this.myAgent;

	private List<String> list_agentNames;
	
	private Map<String, String> agentsPositions = new Hashtable<>();
	
	private Set<String> blockedPositions = new HashSet<>();
	
	private int transitionValue=0;
	
	private List<String> nodesToIgnore=null;
	
	private String golemPosition = null;
	
	
	/**
	 * Le chemin (liste de positions) vers une odeur partagée
	 */
	//private List<String> currentPath;
	
	
	public HuntBehaviour(final AbstractDedaleAgent myagent,List<String> agentNames) {
		super(myagent);
		//this.myMap = myMap;
		this.list_agentNames = agentNames;
		System.out.println("La chasse est ouverte pour "+ myAgent.getLocalName());
		//this.myAgent.addBehaviour(new SharePositionBehaviour((AbstractDedaleAgent)this.myAgent, myMap, list_agentNames, agentsPositions));
		for (String agentName : agentNames) {
			agentsPositions.put(agentName, "");
		}
	}

	public void action() {
		
		// Normalement, lorsque ce behaviour est activé, la map est totalement explorée
		//assertNotNull("La map est vide après l'exploration", this.myMap);

		//0) Retrieve the current position
		Location myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		//System.out.println("Agent " + this.myAgent.getLocalName() + " at position " + myPosition);
		
		myMap= agent.getAgentMap();
		
		try {
			this.myAgent.doWait(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		if (myPosition != null) {
			checkPositionReceived();
			for(String loc: agentsPositions.values()) {
				if(loc != "") {
					blockedPositions.add(loc);
				}
			}
			
			//List<Couple<Location,List<Couple<Observation,Integer>>>> lobs = ((AbstractDedaleAgent)this.myAgent).observe();
			//List<Location> smells= getSmells(lobs);
			
			
			if(golemPosition!=null) {
				List<String >golem_neighbours=this.myMap.getNeighbours(golemPosition);
				boolean flag=true;
				for(String neigh : golem_neighbours) {
					if(!this.agentsPositions.containsValue(neigh) && !((((AbstractDedaleAgent) this.myAgent).getCurrentPosition().getLocationId())==neigh)) {
						flag=false;
					}
				}
				if(flag) {
					System.out.println("Golem blocked");
					transitionValue=1;
					((FSMTRYOUTAgent) myAgent).updateGolemPosition(golemPosition);
					finished=true;
				}
			}
			
			
			//if(golemPosition!=null) {
			//	if(!smells.contains(((AbstractDedaleAgent) this.myAgent).getCurrentPosition())) {
			//		golemPosition=null;
			//	}
			//}
			
			// Cas où l'agent a repéré un golem, il tente d'aller sur sa
			// position : soit il échoue et cela bloque le golem, soit
			// il réussi et il suit le golem
			
			// S'il y a un golem dans cette position on l'ajoute des positions bloquées
			if (golemPosition != null) {
				if (agentsPositions.containsValue(golemPosition)) {
					blockedPositions.add(golemPosition);
					golemPosition = null;	
				}else{
					// Il a réussi à suivre le golem, ce dernier a donc changé de position
					if (((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(golemPosition))) {
						golemPosition = null;
					}
				}
						
				// Aucun golem de repéré, on continue l'exploration
			} else {
			
				checkGolemBlocked();
				// List of observable from the agent's current position
				List<Couple<Location,List<Couple<Observation,Integer>>>> lobs = ((AbstractDedaleAgent)this.myAgent).observe();
			
			
				String nextNodeId = null;
		
				// Liste de toutes les cases avec une odeur
				List<Location> list_smells = getSmells(lobs);
				if (list_smells != null) {
					nextNodeId = chooseSmell(list_smells).getLocationId();
					
					// L'agent partage la première odeur qu'il détecte avec les autres agents
					/*ACLMessage ping = new ACLMessage(ACLMessage.INFORM);
					ping.setProtocol("SHARE-SMELL");
					ping.setSender(this.myAgent.getAID());
					ping.setContent(list_smells.get(0).getLocationId());
					
					// On partage le message à tous les autres agents
					for (String agentName : list_agentNames) {
						ping.addReceiver(new AID(agentName,AID.ISLOCALNAME));
					}
					
					// Envoie
					((AbstractDedaleAgent)this.myAgent).sendMessage(ping);
					*/
				}
				/*
				else {
					// On regarde s'il reçoit une position pour une odeur
					MessageTemplate msgTemplate = MessageTemplate.and(
							MessageTemplate.MatchProtocol("SHARE-SMELL"),
							MessageTemplate.MatchPerformative(ACLMessage.INFORM));
					ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
					
					// Il n'y a pas d'odeur
					// On a reçu une position pour une odeur donc on essaye d'y accéder
					if (msgReceived != null) {
						this.currentPath = this.myMap.getShortestPath(myPosition.getLocationId(), msgReceived.getContent());
					}
					
					if (currentPath != null && currentPath.size() >= 1) {
						nextNodeId = currentPath.get(0);
						currentPath.remove(0);
						if (currentPath.isEmpty()) {
							currentPath = null;
						}
					} else {
						currentPath = null;
					}
				}*/
	
				// S'il n'y a pas d'odeur (ni à proximité, ni partagée), il bouge de manière aléatoire
				if (nextNodeId == null) {
					Location nextMove= getFreeMove(lobs);
					if(nextMove!=null) {
						((AbstractDedaleAgent)this.myAgent).moveTo(nextMove);
					}else {
						System.out.println("jss bloqué");
					}
				
				// Sinon l'agent se déplace vers l'odeur
				} else {
					
					//Si la case est occupée par un agent
					if (!((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(nextNodeId))) {
						sendPosition();
						try {
							this.myAgent.doWait(100);
						} catch (Exception e) {
							e.printStackTrace();
						}
						checkPositionReceived();
						
						
						// Ce n'est pas un agent allié
						if (!agentsPositions.containsValue(nextNodeId)) {
							System.out.println("[" + this.myAgent.getLocalName() + "] Golem detected !");
							golemPosition = nextNodeId;
						}
						// C'est un agent allié, on essaye donc de trouver un autre chemin
						else {
							System.out.println("[" + this.myAgent.getLocalName() + "] Un allié bloque");
							blockedPositions.add(nextNodeId);
							
							
							list_smells = getSmells(lobs);
							String nextMoveId=null;
							if (list_smells != null) {
								nextMoveId = chooseSmell(list_smells).getLocationId();
							}
							if(nextMoveId!=null) {
								Location nextMove= getFreeMove(lobs);
								if(nextMove!=null) {
									((AbstractDedaleAgent)this.myAgent).moveTo(nextMove);
								}else {
									System.out.println("jss bloqué");
								}
							}else {
								((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(nextMoveId));
							}
							
						}
						
						// Si l'agent ne peut pas bouger on arrête d'essayer d'aller vers l'odeur partagée
						//this.currentPath = null;
					}
					
					blockedPositions.clear();
				}
			}
		}
	}

	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return finished;
	}
	
	public int onEnd() {
		return transitionValue;
	}
	
	/**
	 * Retourne la localisation de la première rencontrée ou null s'il n'y a aucune odeur
	 * @param lobs
	 * @return
	 */
	
	
	private List<Location> getSmells(List<Couple<Location,List<Couple<Observation,Integer>>>> lobs){
		Iterator<Couple<Location, List<Couple<Observation, Integer>>>> iter = lobs.iterator();
	    List<Location> smells = new ArrayList<>();
	    
		while(iter.hasNext()) {
			
			Couple<Location, List<Couple<Observation, Integer>>> couple= iter.next();
			Location loc_case=couple.getLeft();
			List<Couple<Observation, Integer>> val_obs=couple.getRight();

			for (Couple<Observation,Integer> couple_obs : val_obs) {
				Observation obs= couple_obs.getLeft();
				
				if (obs.equals(Observation.STENCH)) {
					smells.add(loc_case);
				} 
			}
			
		}
        if (smells.isEmpty()) {
		    return null;
        }
        else {
            return smells;
        }
	} 
	
	/**
	 * Choisi une odeur au hasard OU la première rencontrée s'il y en qu'une.
	 * @param l_smell
	 * @return la position de l'odeur
	 */
	private Location chooseSmell(List<Location> l_smell){
		/*
		List<Location> l_smell=new ArrayList<>();
		for(Location smell: l_smells) {
			if(!nodesToIgnore.contains(smell)) {
				l_smell.add(smell);
			}
		}
		*/
		
		
		// S'il n'y a qu'une odeur on retourne cette dernière 
		if (l_smell.size() == 1 && !this.nodesToIgnore.contains(l_smell.get(0))) {
			return l_smell.get(0);
		}
		
		
		// S'il y a plus d'une odeur et que l'agent se situe déjà sur une odeur
		// Si l'agent est sur une odeur, c'est forcément la première odeur
		if( l_smell.size()>1 && l_smell.get(0) == ((AbstractDedaleAgent) this.myAgent).getCurrentPosition()) {
		
			// On se déplace de manière aléatoire
			Random r = new Random();
			int moveId = 1 + r.nextInt(l_smell.size()-1);
			return l_smell.get(moveId);	
		}
		
		Random r = new Random();
		int moveId = r.nextInt(l_smell.size());
		return l_smell.get(moveId);
	}
	
	
	/**
	 * Permet de trouver une position libre, cad sans agent.
	 * @param lobs
	 * @return la position libre
	 */
	private Location getFreeMove(List<Couple<Location,List<Couple<Observation,Integer>>>> lobs) {
		
		// Liste des positions qui n'ont ni agents alliés ni golem
        List<Location> freePositions = new ArrayList<>();
        
        for(Couple<Location,List<Couple<Observation,Integer>>> loc : lobs) {
            if((!blockedPositions.contains(loc.getLeft().getLocationId()))||(!agentsPositions.containsValue(loc.getLeft().getLocationId()))) {
                freePositions.add(loc.getLeft());
            }
        }
        
        // On se déplace de manière aléatoire vers une position libre
        if(!freePositions.isEmpty()) {
            Random random = new Random();
            int randomIndex = 1+random.nextInt(freePositions.size()-1);
            return freePositions.get(randomIndex);
        }
        
        return null; // Return null if no free position found
    }
	
	
	private void sendPosition() {
		ACLMessage ping = new ACLMessage(ACLMessage.INFORM);
		ping.setProtocol("SHARE-POSITION");
		ping.setSender(this.myAgent.getAID());
		ping.setContent(((AbstractDedaleAgent) this.myAgent).getCurrentPosition().getLocationId());
		
		// On partage le message à tous les autres agents
		for (String agentName : list_agentNames) {
			ping.addReceiver(new AID(agentName,AID.ISLOCALNAME));
		}
		
		// Envoie
		((AbstractDedaleAgent)this.myAgent).sendMessage(ping);
		
	}
	
	private void checkPositionReceived() {
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
	
	private void checkGolemBlocked() {
		MessageTemplate msgTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("BLOCKED-GOLEM-POSITION"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
		
		String position=null;
		
		// L'agent a reçu un ping
		if (msgReceived != null) {			
			// On récupère la position du golem bloqué
			position = msgReceived.getContent();
			
		}
		this.nodesToIgnore=getNeighbours(position,3);
		
		
	}
	
	private List<String> getNeighbours(String pos,int range){
		List<String>result=new ArrayList<>();
		result.add(pos);
		int cpt=0;
		while(cpt<range) {
			for(String p:result) {
				List<String >neighbours=this.myMap.getNeighbours(p);
				for(String neigh:neighbours) {
					result.add(neigh);
				}
			
			}
		}
		
		return result;
	}

}
