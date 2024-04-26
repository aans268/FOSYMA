package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import java.util.ArrayList;
import java.util.List;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploCoopBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploPingBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploWithGolemBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.FastHuntBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.HuntBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.Behaviour;

public class AansTestAgent extends AbstractDedaleAgent {
	
	private static final long serialVersionUID = -7969469610241668141L;
	
	/**
	 * La carte interne de l'agent.
	 */
	private MapRepresentation myMap;
	
	/**
	 * This method is automatically called when "agent".start() is executed.
	 * Consider that Agent is launched for the first time. 
	 * 			1) set the agent attributes 
	 *	 		2) add the behaviours
	 *          
	 */
	protected void setup() {

		super.setup();
		
		//get the parameters added to the agent at creation (if any)
		final Object[] args = getArguments();
		
		// La liste des autres agents sur la carte
		List<String> list_agentNames = new ArrayList<String>();
		
		if (args.length == 0) {
			System.err.println("Error while creating the agent, names of agent to contact expected");
			System.exit(-1);
		} else {
			int i = 2;// WARNING YOU SHOULD ALWAYS START AT 2. This will be corrected in the next release.
			while (i < args.length) {
				list_agentNames.add((String)args[i]);
				i++;
			}
		}
		
		System.out.println("(" + this.getLocalName() + ") list_agentNames " + list_agentNames);

		/**
		 * La liste des behaviours de l'agent
		 */
		List<Behaviour> lb = new ArrayList<Behaviour>();
		
		/************************************************
		 * 
		 * ADD the behaviours of the Dummy Moving Agent
		 * 
		 ************************************************/
		
		//lb.add(new ExploWithGolemBehaviour(this, myMap, list_agentNames));
		//lb.add(new ExploWithGolemBehaviour(this, this.myMap, list_agentNames));
		//lb.add(new ExploPingBehaviour(this, this.myMap, list_agentNames));

		/***
		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 */
		addBehaviour(new startMyBehaviours(this, lb));
		System.out.println("the agent "+ this.getLocalName() + " is started");
	}
	
	/**
	 * Permet d'obtenir la taille de la carte interne de l'agent.
	 * @return la taille de la carte.
	 */
	public int getSizeMap() {
		return this.myMap.getOpenNodes().size();
	}
}
