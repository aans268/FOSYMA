package eu.su.mas.dedaleEtu.mas.agents.dummies.explo;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.startMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.ExploWithGolemBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.GolemBlockedBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ShareMap2Behaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.HuntBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.SharePositionBehaviour;
//import eu.su.mas.dedaleEtu.mas.behaviours.GolemBlockedBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;

 public class FSMTRYOUTAgent extends AbstractDedaleAgent {
	
	private static final long serialVersionUID = -7969469610241668141L;
	
	private static final String s1= "s1";
	private static final String s2= "s2";
	private static final String s3= "s3";
	private static final String s4= "s4";
	private static final String s5= "s5";
	private Map<String, String> agentsPositions = new Hashtable<>();

	
	/**
	 * La carte interne de l'agent.
	 */
	private MapRepresentation myMap;
	
	private String blocked_golem_position=null;
	
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
		 * FSM
		 */
		
		FSMBehaviour fsm = new FSMBehaviour(this);
		
		
		
		fsm.registerFirstState(new ExploWithGolemBehaviour(this,list_agentNames),s1);
		fsm.registerState(new ShareMap2Behaviour(this,1000,list_agentNames),s2);
		fsm.registerState(new HuntBehaviour(this,list_agentNames),s3);
		//fsm.registerState(new SharePositionBehaviour(this,list_agentNames,agentsPositions),s4);
		fsm.registerState(new GolemBlockedBehaviour(this,list_agentNames),s5);
		
		fsm.registerTransition(s1, s2,0);
		fsm.registerTransition(s1, s3, 1);
		
		fsm.registerDefaultTransition(s2, s1);
		
		fsm.registerTransition(s3, s3, 0);
		fsm.registerTransition(s3, s5, 1);

		//fsm.registerDefaultTransition(s4, s3);//, 0);
		
		fsm.registerDefaultTransition(s5,s5);
		
		
		
		/***
		 * MANDATORY TO ALLOW YOUR AGENT TO BE DEPLOYED CORRECTLY
		 */
		List<Behaviour> lb = new ArrayList<Behaviour>();
		lb.add(fsm);
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
	
	public MapRepresentation getAgentMap() {
		if (this.myMap == null) {			
			this.myMap = new MapRepresentation();
		}
		return this.myMap;
	}
	
	public String getGolemPosition() {
		return this.blocked_golem_position;
	}
	
	public void updateGolemPosition(String pos) {
		this.blocked_golem_position=pos;
	}
}
