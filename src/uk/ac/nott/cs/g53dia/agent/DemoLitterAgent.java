package uk.ac.nott.cs.g53dia.agent;

import javafx.scene.shape.MoveTo;
import uk.ac.nott.cs.g53dia.library.*;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Random;

import static java.lang.Math.abs;

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
    INIT, EXPLORING, CHARGING, MOVETOCHARGER, FORAGING, MOVETOLITTERBIN, MOVETORECYCLINGBIN, LITTERCOLLECTION, WASTEDISPOSAL, RECYCLINGDISPOSAL, LITTERDISPOSAL;
}

public class DemoLitterAgent extends LitterAgent {
    private static final String WASTEBIN = "class uk.ac.nott.cs.g53dia.library.WasteBin";
    private static final String WASTESTATION = "class uk.ac.nott.cs.g53dia.library.WasteStation";
    private static final String RECYCLINGBIN = "class uk.ac.nott.cs.g53dia.library.RecyclingBin";
    private static final String RECYCLINGSTATION = "class uk.ac.nott.cs.g53dia.library.RecyclingStation";
    private static final String RECHARGEPOINT = "class uk.ac.nott.cs.g53dia.library.RechargePoint";
    private static final String EMPTYCELL = "class uk.ac.nott.cs.g53dia.library.EmptyCell";
    private static final String WASTETASK = "class uk.ac.nott.cs.g53dia.library.WasteTask";
    private static final String RECYCLINGTASK = "class uk.ac.nott.cs.g53dia.library.RecyclingTask";
    /*public ArrayList<WasteBin> wasteBins = new ArrayList<>();
    public ArrayList<WasteStation> wasteStations = new ArrayList<>();
    public ArrayList<RecyclingBin> recyclingBins = new ArrayList<>();
    public ArrayList<RecyclingStation> recyclingStations = new ArrayList<>();
    public ArrayList<RechargePoint> rechargingStations = new ArrayList<>();
    */
    public ArrayList<Task> wasteTasks = new ArrayList<>();
    public ArrayList<Task> recyclingTasks = new ArrayList<>();
    private AgentState agentState;
    public Point explorationLocation, originalPoint;
    public boolean forageForRecycling, forageForWaste;
    public ArrayList<AreaScan> regions = new ArrayList<>();
    AreaScan currentRegion;



    public DemoLitterAgent() {
        this(new Random());
    }

    /**
     * The tanker implementation makes random moves. For reproducibility, it
     * can share the same random number generator as the environment.
     *
     * @param r The random number generator.
     */
    public DemoLitterAgent(Random r) {
        this.r = r;
        agentState = agentState.INIT;
    }

    /*
     * The following is a simple demonstration of how to write a tanker. The
     * code below is very stupid and simply moves the tanker randomly until the
     * charge agt is half full, at which point it returns to a charge pump.
     */
    public Cell closestPoint(ArrayList<?> list) {
        int distance, closestDistance;
        closestDistance = 1000;
        Cell closestPoint = null;
        for (Object element : list) {
            Cell point = (Cell) element;
            distance = point.getPoint().distanceTo(getPosition());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPoint = point;
            }
        }
        return closestPoint;
    }

    public Task closestTask(ArrayList<Task> seenTasks) {
        int distance, closestDistance;
        closestDistance = 1000;
        Task closestTask = null;
        for (Task t : seenTasks) {
            distance = t.getPosition().distanceTo(getPosition());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestTask = t;
            }
        }
        return closestTask;
    }

    public Task evaluateTask(ArrayList<Task> list, Task currentTask) {
        int newScore, currentScore, distanceToTask;
        int distanceToNewTask;
        if(currentTask == null){
            distanceToTask = 1000;
        } else {
            distanceToTask = currentTask.getPosition().distanceTo(getPosition());
        }
        Task bestTask = currentTask;
        for (Object task : list) {
            Task potentialTask = (Task) task;
            newScore = potentialTask.getAmount();
            currentScore = currentTask.getAmount();
            distanceToNewTask = potentialTask.getPosition().distanceTo(getPosition());
            if ((newScore/currentScore) > distanceToNewTask/distanceToTask) {
                bestTask = potentialTask;
                System.out.println("new task is close enough to be worthy of switching");
            } else {
                //System.out.println("new task was unworthy, no change");
                if (distanceToTask == -1) {
                    System.out.println("Silly me I already ate that");
                    bestTask = potentialTask;
                }
            }
        }
        return bestTask;
    }

    public Task superiorTask(Task currentTask, Cell[][] view) {
        scanCells(view);
        if(forageForWaste) {
            currentTask = closestTask(wasteTasks);
        } else if(forageForRecycling){
            currentTask = closestTask(recyclingTasks);
        } else if(currentTask == null){
            agentState = AgentState.EXPLORING;
        }
        Task currentBest = currentTask;
        if (!recyclingTasks.isEmpty()) {
            System.out.println("trying to find the best recycling");
            currentBest = evaluateTask(recyclingTasks, currentBest);
            if(forageForRecycling){
                return currentBest;
            }
        }
        if (!wasteTasks.isEmpty()) {
            System.out.println("trying to find the best waste");
            if(forageForWaste){
                return currentBest;
            }
        }
        currentBest = evaluateTask(wasteTasks, currentBest);

        return currentBest;
    }

    public void initCurrentTask() {
        if (!wasteTasks.isEmpty()) {
            this.currentTask = closestTask(wasteTasks);
        }
        if (!recyclingTasks.isEmpty()) {
            this.currentTask = closestTask(recyclingTasks);
        }

    }

    private Task currentTask;

    public Action senseAndAct(Cell[][] view, long timestep) {
        switch (agentState) {
            case INIT:
                forageForRecycling = false;
                forageForWaste = false;
                currentRegion = new AreaScan();
                currentRegion.scanCells(view, getPosition());
                regions.add(currentRegion);
                this.originalPoint = getPosition();
                this.explorationLocation = new Point(this.originalPoint.getX() + r.nextInt(100), this.originalPoint.getY() + r.nextInt(100));

                if (wasteTasks.isEmpty()) {
                    if (recyclingTasks.isEmpty()) {
                        agentState = AgentState.EXPLORING;
                    }
                }

            case EXPLORING:
                if(getPosition().distanceTo(currentRegion.location) > 30){
                    currentRegion = new AreaScan();
                    
                }
                scanCells(view);
                if (getChargeLevel() <= 150) {
                    System.out.println("shit im hungry");
                    agentState = AgentState.MOVETOCHARGER;
                    Cell closestRecharge = closestPoint(rechargingStations);
                    return new MoveTowardsAction(closestRecharge.getPoint());
                } else if (this.currentTask == null) {
                    initCurrentTask();
                    if (this.explorationLocation.equals(getPosition())) {
                        this.explorationLocation = new Point(this.originalPoint.getX() + r.nextInt(100), this.originalPoint.getY() + r.nextInt(100));
                    }
                    System.out.println("im looking for " + this.explorationLocation.toString());
                    return new MoveTowardsAction(this.explorationLocation);
                }

            case MOVETOLITTERBIN:
                if (this.currentTask.getClass().toString().equals(WASTETASK)) {
                    if (getCurrentCell(view) instanceof WasteBin && (getCurrentCell(view).getPoint().equals(this.getPosition()))) {
                        System.out.println("I must feed on filth");
                        agentState = AgentState.LITTERDISPOSAL;
                        return new LoadAction(this.currentTask);
                    } else {
                        System.out.println("On my way to get waste");
                        agentState = AgentState.MOVETOLITTERBIN;
                        if(wasteTasks.isEmpty() && recyclingTasks.isEmpty()){
                            System.out.println("Looks like nothing is about");
                            agentState = AgentState.EXPLORING;
                        } else if(forageForWaste){
                            this.currentTask = evaluateTask(wasteTasks, this.currentTask);
                            return new MoveTowardsAction(this.currentTask.getPosition());
                        } else {
                            this.currentTask = superiorTask(this.currentTask, view);
                            return new MoveTowardsAction(this.currentTask.getPosition());
                        }
                    }
                } else if (this.currentTask.getClass().toString().equals(RECYCLINGTASK)) {
                    if (getCurrentCell(view) instanceof RecyclingBin &&getCurrentCell(view).getPoint().equals(this.getPosition())) {
                        System.out.println("I must feed on recyclable material");
                        agentState = AgentState.LITTERDISPOSAL;
                        return new LoadAction(this.currentTask);
                    } else {
                        System.out.println("On my way to get recycling");
                        agentState = AgentState.MOVETOLITTERBIN;
                        if(wasteTasks.isEmpty() && recyclingTasks.isEmpty()){
                            System.out.println("Looks like nothing is about");
                            agentState = AgentState.EXPLORING;
                        } else if(forageForRecycling){
                            this.currentTask = evaluateTask(recyclingTasks, this.currentTask);
                            return new MoveTowardsAction(this.currentTask.getPosition());
                        } else {
                            this.currentTask = superiorTask(this.currentTask, view);
                            return new MoveTowardsAction(this.currentTask.getPosition());
                        }
                    }
                }

            case MOVETOCHARGER:
                if (getCurrentCell(view) instanceof RechargePoint) {
                    if (getChargeLevel() == MAX_CHARGE) {
                        if(forageForRecycling || forageForWaste){
                            agentState = AgentState.FORAGING;
                        } else {
                            agentState = AgentState.EXPLORING;
                        }
                    } else {
                        System.out.println("that is delicious");
                        return new RechargeAction();
                    }
                } else {
                    Cell closestRecharge = closestPoint(rechargingStations);
                    System.out.println("fuck i hope i get there quick");
                    return new MoveTowardsAction(closestRecharge.getPoint());
                }

            case FORAGING:
                scanCells(view);
                if (forageForWaste) {
                    System.out.println("that wasn't enough, I hunger for more waste");
                    wasteTasks.remove(this.currentTask);
                    if(!wasteTasks.isEmpty()){
                        this.currentTask = null;
                        System.out.println("finding a new task");
                        this.currentTask = closestTask(wasteTasks);
                        if(getPosition().distanceTo(closestPoint(wasteStations).getPoint())/getPosition().distanceTo(this.currentTask.getPosition()) > 0.75){
                            System.out.println("Gonna dump this shit real quick");
                            forageForRecycling = false;
                            agentState = AgentState.LITTERDISPOSAL;
                        } else {
                            agentState = AgentState.MOVETOLITTERBIN;
                            return new MoveTowardsAction(this.currentTask.getPosition());
                        }
                    } else {
                        System.out.println("guess we gotta dump the litter");
                        if (getRecyclingLevel() != 0) {
                            forageForWaste = false;
                            agentState = AgentState.LITTERDISPOSAL;
                        } else {
                            agentState = AgentState.EXPLORING;
                            return new MoveTowardsAction(explorationLocation);
                        }
                    }
                } else if (forageForRecycling) {
                    System.out.println("that wasn't enough, I hunger for more recycling");
                    recyclingTasks.remove(this.currentTask);
                    if(!recyclingTasks.isEmpty()){
                        this.currentTask = null;
                        System.out.println("finding a new task");
                        this.currentTask = closestTask(recyclingTasks);
                        if(getPosition().distanceTo(closestPoint(recyclingStations).getPoint())/getPosition().distanceTo(this.currentTask.getPosition()) > 0.75){
                            System.out.println("Gonna dump this shit real quick");
                            forageForRecycling = false;
                            agentState = AgentState.LITTERDISPOSAL;
                        } else {
                            agentState = AgentState.MOVETOLITTERBIN;
                            return new MoveTowardsAction(this.currentTask.getPosition());
                        }
                    } else {
                        System.out.println("guess we gotta dump the recycling");
                        if(getRecyclingLevel()!= 0){
                            forageForRecycling = false;
                            agentState = AgentState.LITTERDISPOSAL;
                        } else {
                            agentState = AgentState.EXPLORING;
                            return new MoveTowardsAction(explorationLocation);
                        }
                    }

                }

            case LITTERDISPOSAL:
                scanCells(view);
                if (this.currentTask.getClass().toString().equals(WASTETASK)) {
                    if(getWasteLevel() < MAX_LITTER/4 && !wasteTasks.isEmpty()){
                        agentState = AgentState.FORAGING;
                        forageForRecycling = false;
                        forageForWaste = true;
                    } else if (getCurrentCell(view) instanceof WasteStation && getWasteLevel() != 0) {
                        dealWithTask();
                        System.out.println("Thank god that waste is gone");
                        forageForRecycling = false;
                        forageForWaste = false;
                        agentState = AgentState.EXPLORING;
                        return new DisposeAction();
                    } else if(getWasteLevel() != 0){
                        Cell closestLitterDisposal = closestPoint(wasteStations);
                        System.out.println("Need to grab that waste");
                        return new MoveTowardsAction(closestLitterDisposal.getPoint());
                    } else {
                        agentState = AgentState.EXPLORING;
                        return new MoveAction(0);
                    }
                } else if (this.currentTask.getClass().toString().equals(RECYCLINGTASK)) {
                    if(getRecyclingLevel() < MAX_LITTER/4 && !recyclingTasks.isEmpty()){
                        agentState = AgentState.FORAGING;
                        forageForRecycling = true;
                        forageForWaste = false;
                    } else if (getCurrentCell(view) instanceof RecyclingStation && getRecyclingLevel() != 0) {
                        dealWithTask();
                        System.out.println("Thank god that recycling is gone");
                        agentState = AgentState.EXPLORING;
                        forageForRecycling = false;
                        forageForWaste = false;
                        return new DisposeAction();
                    } else if(getRecyclingLevel() != 0){
                        Cell closestRecyclingDisposal = closestPoint(recyclingStations);
                        System.out.println("Need to grab that recycling");
                        return new MoveTowardsAction(closestRecyclingDisposal.getPoint());
                    } else {
                        agentState = AgentState.EXPLORING;
                        return new MoveAction(0);
                    }
                }
            default:
                return new MoveAction(0);
        }
    }

    private void dealWithTask() {
        if (this.currentTask.getClass().toString().equals(WASTETASK)) {
            wasteTasks.remove(this.currentTask);
            this.currentTask = null;
            if(forageForWaste){
                this.currentTask = closestTask(wasteTasks);
            } else {
                initCurrentTask();
            }
            initCurrentTask();
        } else if (this.currentTask.getClass().toString().equals(RECYCLINGTASK)) {
            recyclingTasks.remove(this.currentTask);
            this.currentTask = null;
            if(forageForRecycling) {
                this.currentTask = closestTask(recyclingTasks);
            } else {
                initCurrentTask();
            }
            System.out.println("that recycling bin is empty for now");
        }
    }
}
