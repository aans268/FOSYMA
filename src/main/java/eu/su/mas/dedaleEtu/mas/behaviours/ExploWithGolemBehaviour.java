package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.agents.dummies.explo.FSMTRYOUTAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

/**
 * Exploration d'une carte avec potentiellement des golems immobiles sur la carte.
 */
public class ExploWithGolemBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = 8567689731496787661L;
	
	/**
	 * Attribut mis à vrai à la fin de l'exploration, cad quand la carte de
	 * l'agent ne contient plus de noeuds ouverts.
	 */
	private boolean finished = false;
	
	/**
	 * La carte interne de l'agent.
	 */
	private MapRepresentation myMap;
	
	private FSMTRYOUTAgent agent= (FSMTRYOUTAgent) this.myAgent;

	
	/**
	 * La liste des autres agents sur la carte
	 */
	private List<String> list_agentNames;
	
	/**
	 * L'ensemble des positions bloquées
	 */
	private Set<String> blockedPositions = new HashSet<>();
	
	private int transitionValue=0;
	
	
	public ExploWithGolemBehaviour(final AbstractDedaleAgent myAgent, List<String> agentNames) {
		super(myAgent);
		//this.myMap = myMap;
		this.list_agentNames = agentNames;
	}
	
	@Override
	public void action() {
		
		// Si la carte de l'agent n'existe pas (null), on l'a créée et on
		// ajoute le behaviour de partage de carte avec les autres agents
		
			//this.myAgent.addBehaviour(new ShareMap2Behaviour(this.myAgent, 1000, this.myMap, this.list_agentNames));
		
		myMap= agent.getAgentMap();

		transitionValue=0;
		
		// On récupère la position courante de l'agent
		Location myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		
		try {
			this.myAgent.doWait(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// A tout moment, l'agent s'attend à recevoir un message du protocole
		// SHARE-TOPO ?
		MessageTemplate msgTemplate = MessageTemplate.and(
				MessageTemplate.MatchProtocol("SHARE-TOPO"),
				MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		
		ACLMessage msgReceived = this.myAgent.receive(msgTemplate);
		
		if (msgReceived != null) {
			SerializableSimpleGraph<String, MapAttribute> sgreceived=null;
			try {
				sgreceived = (SerializableSimpleGraph<String, MapAttribute>)msgReceived.getContentObject();
			} catch (UnreadableException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.myMap.mergeMap(sgreceived);
			//System.out.println("Map merged.");
		}
		
		if (myPosition != null) {
			
			//List of observable from the agent's current position
			List<Couple<Location,List<Couple<Observation,Integer>>>> lobs = ((AbstractDedaleAgent)this.myAgent).observe();
			
			// Le noeud sur lequel est l'agent est maintenant fermé
			this.myMap.addNode(myPosition.getLocationId(), MapAttribute.closed);
	
			// ID du noeud de la prochaine destination
			String nextNodeId = null;
			
			// Parcours de tous les observables
			Iterator<Couple<Location, List<Couple<Observation, Integer>>>> iter = lobs.iterator();
			while (iter.hasNext()) {
				
				// On récupère la localisation du noeud observé
				Location accessibleNode = iter.next().getLeft();
				
				// On ajoute ce noeud à la carte interne s'il n'a pas encore été découvert
				boolean isNewNode = this.myMap.addNewNode(accessibleNode.getLocationId());
				
				// On ajoute les arêtes entre le noeud courant et les noeuds accessibles
				if (myPosition.getLocationId() != accessibleNode.getLocationId()) {
					
					this.myMap.addEdge(myPosition.getLocationId(), accessibleNode.getLocationId());
					
					// Si c'est un nouveau noeud (cad qu'il n'a pas été découvert)
					// et que la prochaine destination n'a pas encore été définie,
					// c'est maintenant la prochaine destination de l'agent.
					if (nextNodeId == null && isNewNode) {
						nextNodeId = accessibleNode.getLocationId();
					}
				}
			}
	
			// Il n'y a plus de noeuds ouverts
			if (!this.myMap.hasOpenNode()) {
				
				finished = true;
				transitionValue=2;
				System.out.println(this.myAgent.getLocalName() + " - Exploration successufully done, behaviour removed.");
				
			} else {
				
				// On continue d'explorer la carte tant qu'il y a des noeuds
				// ouverts qui ne sont pas bloqués.
				// La prochaine destination n'a pas déjà été déterminée
				if (nextNodeId == null) {
					
					// Le chemin courant à suivre
					List<String> currentPath = this.myMap.getShortestPathToClosestOpenNode(myPosition.getLocationId(), blockedPositions);
					
					// Impossible de trouver un chemin vers le prochain noeud ouvert
					if (currentPath == null) {						
						System.out.println("(" + this.myAgent.getLocalName() + ") Je suis bloqué !");
						
					} else {
						
						// La prochaine destination est le premier noeud du chemin
						// vers le premier noeud ouvert qui n'est pas bloqué
						nextNodeId = currentPath.get(0);
					}
				}
				
				// On a pas réussi à bouger donc il y a un agent qui bloque le passage
				if (nextNodeId != null && !((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(nextNodeId))) {
					// Si la position est bloquée, on l'ajoute à l'ensemble des
					// positions bloquées
					transitionValue=1;
					blockedPositions.add(nextNodeId);
					
					//////////////////////
					//////////////////////
					Location nextMove= getFreeMove(lobs);
					if(nextMove!=null) {
						((AbstractDedaleAgent)this.myAgent).moveTo(nextMove);
					}else {
						System.out.println("[" + this.myAgent.getLocalName() + "] est bloqué");
					}
					//////////////////////
					//////////////////////
					
					
					
				}
			}
			
			// S'il n'y a plus de noeuds ouverts qui ne sont pas bloqués,
			// on regarde de nouveau les noeuds bloqués.
			if (blockedPositions.containsAll(myMap.getOpenNodes())) {
				blockedPositions.clear();
			}
			finished=true;
			
			//System.out.println(myMap==null);
		}
	}

	@Override
	public boolean done() {
		//System.out.println("done "+ agent.getName()+ " "+ finished);
		return finished;
	}
	
	public int onEnd() {
		//System.out.println("transition value :"+transitionValue);
		return transitionValue;
	}
	
private Location getFreeMove(List<Couple<Location,List<Couple<Observation,Integer>>>> lobs) {
		
		// Liste des positions qui n'ont ni agents alliés ni golem
        List<Location> freePositions = new ArrayList<>();
        
        for(Couple<Location,List<Couple<Observation,Integer>>> loc : lobs) {
            if((!blockedPositions.contains(loc.getLeft().getLocationId()))) {
                freePositions.add(loc.getLeft());
            }
        }
        
        // On se déplace de manière aléatoire vers une position libre
        if(!freePositions.isEmpty() && freePositions.size()>1) {
            Random random = new Random();
            int randomIndex = 1+random.nextInt(freePositions.size()-1);
            return freePositions.get(randomIndex);
        }
        
        return null; // Return null if no free position found
    }
	
}
