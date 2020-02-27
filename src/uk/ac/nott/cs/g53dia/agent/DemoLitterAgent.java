package uk.ac.nott.cs.g53dia.agent;

import uk.ac.nott.cs.g53dia.library.*;

import java.util.ArrayList;
import java.util.Random;

/**
 * A simple example LitterAgent
 * 
 * @author Julian Zappala
 */
/*
 * Copyright (c) 2011 Julian Zappala
 * 
 * See the file "license.terms" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
enum AgentState {
	INIT, EXPLORING, CHARGING, MOVETOCHARGER, MOVETOLITTERBIN, MOVETORECYCLINGBIN, LITTERCOLLECTION, LITTERDISPOSAL, RECYCLINGCOLLECTION, RECYCLINGDISPOSAL;
}

public class DemoLitterAgent extends LitterAgent {
		private static final String WASTEBIN = "class uk.ac.nott.cs.g53dia.library.WasteBin";
		private static final String WASTESTATION = "class uk.ac.nott.cs.g53dia.library.WasteStation";
		private static final String RECYCLINGBIN = "class uk.ac.nott.cs.g53dia.library.RecyclingBin";
		private static final String RECYCLINGSTATION = "class uk.ac.nott.cs.g53dia.library.RecyclingStation";
		private static final String RECHARGEPOINT = "class uk.ac.nott.cs.g53dia.library.RechargePoint";
		private static final String EMPTYCELL = "class uk.ac.nott.cs.g53dia.library.EmptyCell";
		private boolean collectingWaste = true;
		public ArrayList<WasteBin> wasteBins = new ArrayList<>();
		public ArrayList<WasteStation> wasteStations = new ArrayList<>();
		public ArrayList<RecyclingBin> recyclingBins = new ArrayList<>();
		public ArrayList<RecyclingStation> recyclingStations = new ArrayList<>();
		public ArrayList<RechargePoint> rechargingStations = new ArrayList<>();
		public ArrayList<Task> wasteTasks = new ArrayList<>();
		public ArrayList<Task> recyclingTasks = new ArrayList<>();
		private AgentState agentState;


	public void scanCells(Cell[][] view){
		for (int i = 0; i < view.length; i++) {
			for (int j = 0; j < view.length; j++) {
				Cell currentCell = view[i][j];
				switch(view[i][j].getClass().toString()){
					case WASTEBIN:
						if(!wasteBins.contains(currentCell)){
							WasteBin wasteBin = (WasteBin) currentCell;
							if(wasteBin.getTask()!= null) {
								Task newTask = wasteBin.getTask();
								wasteTasks.add(newTask);
							}
							wasteBins.add((WasteBin) view[i][j]);
							System.out.println("new cell is a waste bin");
						}
						break;
					case WASTESTATION:
						if(!wasteStations.contains(currentCell)) {
							wasteStations.add((WasteStation) view[i][j]);
							System.out.println("new cell is a waste station");
						}
						break;
					case RECYCLINGBIN:
						if(!recyclingBins.contains(currentCell)) {
							RecyclingBin recycleBin = (RecyclingBin) currentCell;
							if(recycleBin.getTask()!= null) {
								Task newTask = recycleBin.getTask();
								recyclingTasks.add(newTask);
							}
							recyclingBins.add((RecyclingBin) view[i][j]);
							System.out.println("new cell is a recycling bin");
						}
						break;
					case RECYCLINGSTATION:
						if(!rechargingStations.contains(currentCell)) {
							recyclingStations.add((RecyclingStation) view[i][j]);
							System.out.println("new cell is a recycling station");
						}
						break;
					case RECHARGEPOINT:
						if(!rechargingStations.contains(currentCell)) {
							rechargingStations.add((RechargePoint) view[i][j]);
							System.out.println("new cell is a recharging point");
						}
						break;
					case EMPTYCELL:
						//System.out.println("Cell is empty");
						break;
					default:
						break;
				}
			}
		}
	}

	public DemoLitterAgent() {
		this(new Random());
	}

	/**
	 * The tanker implementation makes random moves. For reproducibility, it
	 * can share the same random number generator as the environment.
	 * 
	 * @param r
	 *            The random number generator.
	 */
	public DemoLitterAgent(Random r) {
		this.r = r;
		agentState = agentState.EXPLORING;
	}

	/*
	 * The following is a simple demonstration of how to write a tanker. The
	 * code below is very stupid and simply moves the tanker randomly until the
	 * charge agt is half full, at which point it returns to a charge pump.
	 */
	public Cell closestPoint(ArrayList<?> list){
		int distance, closestDistance;
		closestDistance = 1000;
		Cell closestPoint = null;
		for(Object element : list) {
			Cell point = (Cell) element;
			distance = point.getPoint().distanceTo(getPosition());
			if(distance < closestDistance){
				closestDistance = distance;
				closestPoint = point;
			}
		}
		return closestPoint;
	}

	public Task closestTask(ArrayList<Task> seenTasks){
		int distance, closestDistance;
		closestDistance = 1000;
		Task closestTask = null;
		for(Task t : seenTasks) {
			distance = t.getPosition().distanceTo(getPosition());
			if(distance < closestDistance){
				closestDistance = distance;
				closestTask = t;
			}
		}
		return closestTask;
	}


	public Action senseAndAct(Cell[][] view, long timestep) {
		Point explorationLocation = new Point(400,400);
		scanCells(view);
		Cell nearestCharge = closestPoint(rechargingStations);
		Task currentTask = closestTask(wasteTasks);
		switch(agentState){
			case INIT:
				scanCells(view);
				agentState = AgentState.EXPLORING;
			case EXPLORING:
				explorationLocation = new Point(getPosition().getX()+ r.nextInt(100), getPosition().getY() + r.nextInt(100));
				if (getChargeLevel() == nearestCharge.getPoint().distanceTo(getPosition())) {
					agentState = AgentState.MOVETOCHARGER;
				} else if(!wasteTasks.isEmpty() && wasteTasks.size() > recyclingTasks.size()){
					currentTask = closestTask(wasteTasks);
					agentState = AgentState.MOVETOLITTERBIN;
				} else if(!recyclingTasks.isEmpty()){
					currentTask = closestTask(recyclingTasks);
					agentState = AgentState.MOVETORECYCLINGBIN;
				} else {
					return new MoveTowardsAction(explorationLocation);
				}

			case MOVETOLITTERBIN:
				if(getChargeLevel() > nearestCharge.getPoint().distanceTo(getPosition()) && !getPosition().equals(currentTask.getPosition())) {
					return new MoveTowardsAction(currentTask.getPosition());
				} else if(getChargeLevel() == nearestCharge.getPoint().distanceTo(getPosition())){
					agentState = AgentState.MOVETOCHARGER;
					return new MoveTowardsAction(nearestCharge.getPoint());
				} else if(getPosition().equals(currentTask.getPosition())){
					agentState = AgentState.LITTERCOLLECTION;
				}
			case LITTERCOLLECTION:
				if(!currentTask.isComplete()) {
					return  new LoadAction(currentTask);
				} else if (getLitterLevel() > MAX_LITTER) {
					agentState = AgentState.LITTERCOLLECTION;
				} else {
					agentState = AgentState.LITTERDISPOSAL;
				}

			case MOVETOCHARGER:
				if (!(getCurrentCell(view) instanceof RechargePoint)) {
					//System.out.println("Going to charger");
					return new MoveTowardsAction(RECHARGE_POINT_LOCATION);
				} else if(getChargeLevel()!= MAX_CHARGE && getPosition().equals(RECHARGE_POINT_LOCATION)) {
					//System.out.println("recharging");
					return new RechargeAction();
				}
			case LITTERDISPOSAL:
				if(getChargeLevel() > nearestCharge.getPoint().distanceTo(getPosition()) && !getPosition().equals(currentTask.getPosition())) {
					return new MoveTowardsAction(closestPoint(wasteStations).getPoint());
				} else if(getChargeLevel() == nearestCharge.getPoint().distanceTo(getPosition())){
					agentState = AgentState.MOVETOCHARGER;
					return new MoveTowardsAction(nearestCharge.getPoint());
				} else if(getPosition().equals(closestPoint(wasteStations).getPoint())){
					return new DisposeAction();
				}
			case RECYCLINGCOLLECTION:
				if(getChargeLevel() > nearestCharge.getPoint().distanceTo(getPosition()) && !getPosition().equals(currentTask.getPosition())) {
					return new MoveTowardsAction(currentTask.getPosition());
				} else if(getChargeLevel() == nearestCharge.getPoint().distanceTo(getPosition())){
					agentState = AgentState.MOVETOCHARGER;
					return new MoveTowardsAction(nearestCharge.getPoint());
				} else if(getPosition().equals(currentTask.getPosition())){
					agentState = AgentState.LITTERCOLLECTION;
				}
			case RECYCLINGDISPOSAL:
				if(getChargeLevel() > nearestCharge.getPoint().distanceTo(getPosition()) && !getPosition().equals(currentTask.getPosition())) {
					return new MoveTowardsAction(closestPoint(recyclingStations).getPoint());
				} else if(getChargeLevel() == nearestCharge.getPoint().distanceTo(getPosition())){
					agentState = AgentState.MOVETOCHARGER;
					return new MoveTowardsAction(nearestCharge.getPoint());
				} else if(getPosition().equals(closestPoint(recyclingStations).getPoint())){
					return new DisposeAction();
				}
			default:
				return new MoveTowardsAction(explorationLocation);
		}
	}
}
