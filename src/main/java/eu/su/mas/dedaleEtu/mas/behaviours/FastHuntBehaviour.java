package eu.su.mas.dedaleEtu.mas.behaviours;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;
import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.gsLocation;
import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;


/**
 * Behaviour qui chasse directement les golems sans explorer la map.
 */
public class FastHuntBehaviour extends SimpleBehaviour {
	
	private static final long serialVersionUID = 8567689731496787661L;
	
	/**
	 * La carte interne de l'agent.
	 */
	private MapRepresentation myMap;
	
	/**
	 * La liste des autres agents sur la carte
	 */
	private List<String> list_agentNames;
	
	/**
	 * La table des dernières positions connues des agents alliés
	 */
	private Map<String, String> agentsPositions = new Hashtable<>();
	
	/**
	 * L'ensemble des positions bloquées
	 */
	private Set<String> blockedPositions = new HashSet<>();
	
	private String golemPosition = null;
	
	
	public FastHuntBehaviour(final AbstractDedaleAgent myAgent, MapRepresentation myMap, List<String> agentNames) {
		super(myAgent);
		this.myMap = myMap;
		this.list_agentNames = agentNames;
		
		System.out.println("Déclenchement de FastHuntBehaviour pour " + myAgent.getLocalName());
		
		// On crée une entrée dans la table des positions pour chaque agent
		// initialement chaîne vide
		for (String agentName : agentNames) {
			agentsPositions.put(agentName, "");
		}
	}

	@Override
	public void action() {
		
		// Si la carte de l'agent n'existe pas (null), on la crée et on
		// ajoute le behaviour de partage de position avec les autres agents
		if (this.myMap == null) {			
			this.myMap = new MapRepresentation();
			//this.myAgent.addBehaviour(new SharePositionBehaviour((AbstractDedaleAgent)this.myAgent, myMap, list_agentNames, agentsPositions));
		}
		
		// On récupère la position courante de l'agent
		Location myPosition = ((AbstractDedaleAgent)this.myAgent).getCurrentPosition();
		
		try {
			this.myAgent.doWait(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (myPosition != null) {
			
			// Cas où l'agent a repéré un golem, il tente d'aller sur sa
			// position : soit il échoue et cela bloque le golem, soit
			// il réussi et il suit le golem
			if (golemPosition != null) {
				
				// Il a réussi à suivre le golem, ce dernier a donc changé de position
				if (((AbstractDedaleAgent)this.myAgent).moveTo(new gsLocation(golemPosition))) {
					golemPosition = null;
				}
			
			// Aucun golem de repéré, on continue l'exploration
			} else {
				
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
		
				// Il n'y a plus de noeuds ouverts, on recommence la recherche
				if (!this.myMap.hasOpenNode()) {
					
					// On supprime la carte
					this.myMap = null;
					System.out.println(this.myAgent.getLocalName() + " - Fin de l'exploration, on recommence.");
					
				} else {
					
					// On continue d'explorer la carte
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
						
						// Ce n'est pas un agent allié
						if (!agentsPositions.containsValue(nextNodeId)) {
							System.out.println("[" + this.myAgent.getLocalName() + "] Golem detected !");
							golemPosition = nextNodeId;
						}
						// C'est un agent allié, on essaye donc de trouver un autre chemin
						else {
							System.out.println("[" + this.myAgent.getLocalName() + "] Un allié bloque");
							blockedPositions.add(nextNodeId);
						}
					}
					
					// S'il n'y a plus de noeuds ouverts qui ne sont pas bloqués,
					// on regarde de nouveau les noeuds bloqués
					if (blockedPositions.containsAll(myMap.getOpenNodes())) {
						blockedPositions.clear();
					}
				}
			}
		}		
	}

	@Override
	public boolean done() {
		// TODO Auto-generated method stub
		return false;
	}

}
