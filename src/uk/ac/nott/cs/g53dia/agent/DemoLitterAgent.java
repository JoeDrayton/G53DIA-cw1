package uk.ac.nott.cs.g53dia.agent;

import javafx.scene.shape.MoveTo;
import uk.ac.nott.cs.g53dia.library.*;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Random;

import static java.lang.Math.abs;
import static java.lang.Math.floorMod;

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
//    public ArrayList<Task> wasteTasks = new ArrayList<>();
  //  public ArrayList<Task> recyclingTasks = new ArrayList<>();
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

    public Cell closestPointOfAll(ArrayList<?> list) {
        int distance, closestDistance;
        closestDistance = 1000;
        Cell closestPointOfAll = null;
        for (Object element : list) {
            Cell point = (Cell) element;
            distance = point.getPoint().distanceTo(getPosition());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPointOfAll = point;
            }
        }
        return closestPointOfAll;
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
        //scanCells(view);
        if(forageForWaste) {
            currentTask = closestTask(currentRegion.wasteTasks);
        } else if(forageForRecycling){
            currentTask = closestTask(currentRegion.recyclingTasks);
        } else if(currentTask == null){
            agentState = AgentState.EXPLORING;
        }
        Task currentBest = currentTask;
        if (!currentRegion.recyclingTasks.isEmpty()) {
            System.out.println("trying to find the best recycling");
            currentBest = evaluateTask(currentRegion.recyclingTasks, currentBest);
            if(forageForRecycling){
                return currentBest;
            }
        }
        if (!currentRegion.wasteTasks.isEmpty()) {
            System.out.println("trying to find the best waste");
            if(forageForWaste){
                return currentBest;
            }
        }
        currentBest = evaluateTask(currentRegion.wasteTasks, currentBest);

        return currentBest;
    }

    public void initCurrentTask() {
        if (!currentRegion.wasteTasks.isEmpty()) {
            this.currentTask = closestTask(currentRegion.wasteTasks);
        }
        if (!currentRegion.recyclingTasks.isEmpty()) {
            this.currentTask = closestTask(currentRegion.recyclingTasks);
        }

    }

    public void evaluateRegion(Cell[][] view){
        if(getPosition().distanceTo(currentRegion.location) > 30){
            currentRegion = new AreaScan(getPosition());
            currentRegion.scanCells(view);
            regions.add(currentRegion);
        } else {
            currentRegion.scanCells(view);
        }
    }

    public void regionSelect(){
        AreaScan bestRegion = currentRegion;
        int combinedPotential, wastePotential, recyclingPotential, distanceToRegion;
        int regionScore = 0;
        for(AreaScan region : regions){
            wastePotential = region.wasteTasks.size();
            recyclingPotential = region.recyclingTasks.size();
            combinedPotential = wastePotential + recyclingPotential;
            distanceToRegion = getPosition().distanceTo(region.location);
            int newRegionScore = combinedPotential/distanceToRegion;
            if(newRegionScore > regionScore){
                regionScore = newRegionScore;
                bestRegion = region;
            }
        }
        this.currentRegion = bestRegion;
    }

    public Point closestPointOfAllOfAll(ArrayList<?> list) {
        switch (list.getClass().toString()) {
            case RECHARGEPOINT:
                return closestPointOfAll(currentRegion.rechargingStations).getPoint();
            case RECYCLINGBIN:
                return closestPointOfAll(currentRegion.recyclingBins).getPoint();
            case RECYCLINGSTATION:
                return closestPointOfAll(currentRegion.recyclingStations).getPoint();
            case WASTEBIN:
                return closestPointOfAll(currentRegion.wasteStations).getPoint();
            case WASTESTATION:
                return closestPointOfAll(currentRegion.wasteStations).getPoint();
            default:
                return RECHARGE_POINT_LOCATION;
        }
    }
    public Point closestRecharge(){
        Point closestPointOfAll, newPoint;
        if(!currentRegion.rechargingStations.isEmpty()){
            closestPointOfAll = closestPointOfAll(currentRegion.rechargingStations).getPoint();
            return closestPointOfAll;
        } else {
            closestPointOfAll = RECHARGE_POINT_LOCATION;
            for (AreaScan region : regions) {
                if (!region.rechargingStations.isEmpty()) {
                    newPoint = closestPointOfAll(region.rechargingStations).getPoint();
                    if (newPoint.distanceTo(getPosition()) < closestPointOfAll.distanceTo(getPosition())) {
                        closestPointOfAll = newPoint;
                    }
                } else {
                    System.out.println("Nothing in this region");
                }
            }
        }
        return closestPointOfAll;
    }

    private Task currentTask;

    public Action senseAndAct(Cell[][] view, long timestep) {
        switch (agentState) {
            case INIT:
                forageForRecycling = false;
                forageForWaste = false;
                currentRegion = new AreaScan(getPosition());
                currentRegion.scanCells(view);
                regions.add(currentRegion);
                this.originalPoint = getPosition();
                this.explorationLocation = new Point(this.originalPoint.getX() + 30 + r.nextInt(60), this.originalPoint.getY() + 30 + r.nextInt(60));

                if (currentRegion.wasteTasks.isEmpty()) {
                    if (currentRegion.recyclingTasks.isEmpty()) {
                        agentState = AgentState.EXPLORING;
                    }
                }

            case EXPLORING:
                evaluateRegion(view);
                if (getChargeLevel() <= 150) {
                    System.out.println("shit im hungry");
                    agentState = AgentState.MOVETOCHARGER;
                    return new MoveTowardsAction(closestRecharge());
                } else if (this.currentTask == null) {
                    initCurrentTask();
                    if (this.explorationLocation.equals(getPosition())) {
                        if(timestep%2 == 0 ){
                            regionSelect();
                            this.explorationLocation = currentRegion.location;
                            System.out.println("I'm returning to previously charted territory");
                        } else {
                            this.explorationLocation = new Point(this.originalPoint.getX() + r.nextInt(100), this.originalPoint.getY() + r.nextInt(100));
                            System.out.println("Into the unknown!");
                        }
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
                        if(currentRegion.wasteTasks.isEmpty() && currentRegion.recyclingTasks.isEmpty()){
                            System.out.println("Looks like nothing is about");
                            agentState = AgentState.EXPLORING;
                        } else if(forageForWaste){
                            this.currentTask = evaluateTask(currentRegion.wasteTasks, this.currentTask);
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
                        if(currentRegion.wasteTasks.isEmpty() && currentRegion.recyclingTasks.isEmpty()){
                            System.out.println("Looks like nothing is about");
                            agentState = AgentState.EXPLORING;
                        } else if(forageForRecycling){
                            this.currentTask = evaluateTask(currentRegion.recyclingTasks, this.currentTask);
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
                    Cell closestRecharge = closestPointOfAll(currentRegion.rechargingStations);
                    System.out.println("fuck i hope i get there quick");
                    return new MoveTowardsAction(closestRecharge());
                }

            case FORAGING:
                evaluateRegion(view);
                if (forageForWaste) {
                    System.out.println("that wasn't enough, I hunger for more waste");
                    currentRegion.wasteTasks.remove(this.currentTask);
                    if(!currentRegion.wasteTasks.isEmpty()){
                        this.currentTask = null;
                        System.out.println("finding a new task");
                        this.currentTask = closestTask(currentRegion.wasteTasks);
                        if(getPosition().distanceTo(closestPointOfAll(currentRegion.wasteStations).getPoint())/
                                closestPointOfAllOfAll(currentRegion.wasteTasks).distanceTo(this.currentTask.getPosition()) > 0.75){
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
                    currentRegion.recyclingTasks.remove(this.currentTask);
                    if(!currentRegion.recyclingTasks.isEmpty()){
                        this.currentTask = null;
                        System.out.println("finding a new task");
                        this.currentTask = closestTask(currentRegion.recyclingTasks);
                        if(getPosition().distanceTo(closestPointOfAll(currentRegion.recyclingStations).getPoint())/closestPointOfAllOfAll(currentRegion.recyclingTasks).distanceTo(this.currentTask.getPosition()) > 0.75){
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
                evaluateRegion(view);
                if (this.currentTask.getClass().toString().equals(WASTETASK)) {
                    if(getWasteLevel() < MAX_LITTER/5 && !currentRegion.wasteTasks.isEmpty()){
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
                        Cell closestLitterDisposal = closestPointOfAll(currentRegion.wasteStations);
                        System.out.println("Need to grab that waste");
                        return new MoveTowardsAction(closestLitterDisposal.getPoint());
                    } else {
                        agentState = AgentState.EXPLORING;
                        return new MoveAction(0);
                    }
                } else if (this.currentTask.getClass().toString().equals(RECYCLINGTASK)) {
                    if(getRecyclingLevel() < MAX_LITTER/5 && !currentRegion.recyclingTasks.isEmpty()){
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
                        Cell closestRecyclingDisposal = closestPointOfAll(currentRegion.recyclingStations);
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
            currentRegion.wasteTasks.remove(this.currentTask);
            this.currentTask = null;
            if(forageForWaste){
                this.currentTask = closestTask(currentRegion.wasteTasks);
            } else {
                initCurrentTask();
            }
            initCurrentTask();
        } else if (this.currentTask.getClass().toString().equals(RECYCLINGTASK)) {
            currentRegion.recyclingTasks.remove(this.currentTask);
            this.currentTask = null;
            if(forageForRecycling) {
                this.currentTask = closestTask(currentRegion.recyclingTasks);
            } else {
                initCurrentTask();
            }
            System.out.println("that recycling bin is empty for now");
        }
    }
}
