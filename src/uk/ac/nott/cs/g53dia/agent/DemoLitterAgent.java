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

enum Direction {
    NORTH, EAST, SOUTH, WEST;

    private static Direction[] vals = values();
    public Direction next()
    {
        return vals[(this.ordinal()+1) % vals.length];
    }
}

public class DemoLitterAgent extends LitterAgent {
    private static final String WASTEBIN = "class uk.ac.nott.cs.g53dia.library.WasteBin";
    private static final String WASTESTATION = "class uk.ac.nott.cs.g53dia.library.WasteStation";
    private static final String RECYCLINGBIN = "class uk.ac.nott.cs.g53dia.library.RecyclingBin";
    private static final String RECYCLINGSTATION = "class uk.ac.nott.cs.g53dia.library.RecyclingStation";
    private static final String RECHARGEPOINT = "class uk.ac.nott.cs.g53dia.library.RechargePoint";
    private static final String WASTETASK = "class uk.ac.nott.cs.g53dia.library.WasteTask";
    private static final String RECYCLINGTASK = "class uk.ac.nott.cs.g53dia.library.RecyclingTask";
    private AgentState agentState;
    private AgentState previousState;
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

    public Task evaluateTask(ArrayList<Task> list, Task currentTask) {
        int newScore, currentScore, distanceToTask;
        int distanceToNewTask;
        if(currentTask == null){
            agentState = AgentState.EXPLORING;
        }
        Task bestTask = currentTask;
        for (Object task : list) {
            Task potentialTask = (Task) task;
            newScore = potentialTask.getRemaining();
            distanceToNewTask = potentialTask.getPosition().distanceTo(getPosition());
            currentScore = bestTask.getRemaining();
            distanceToTask = bestTask.getPosition().distanceTo(getPosition());

            if(currentScore == 0) {currentScore = 1;}
            if(distanceToTask == 0){distanceToTask = 1;}
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
    evaluateRegion(view);
        if(forageForWaste) {
            currentTask = closestTask(currentRegion.wasteTasks);
        } else if(forageForRecycling){
            currentTask = closestTask(currentRegion.recyclingTasks);
        } else if(currentTask == null){
            agentState = AgentState.EXPLORING;
        }
        Task currentBest = currentTask;
        if (!currentRegion.recyclingTasks.isEmpty()) {
            //System.out.println("trying to find the best recycling");
            currentBest = evaluateTask(currentRegion.recyclingTasks, currentBest);
            if(forageForRecycling){
                return currentBest;
            }
        }
        if (!currentRegion.wasteTasks.isEmpty()) {
            //System.out.println("trying to find the best waste");
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

    public Point closestPointOfAll(ArrayList<?> list) {
        switch (list.getClass().toString()) {
            case RECHARGEPOINT:
                ArrayList<Point> rechargingStations = new ArrayList<>();
                for (AreaScan region : regions) {
                    rechargingStations.add(closestPoint(region.rechargingStations).getPoint());
                }
                return closestPoint(rechargingStations).getPoint();
            case RECYCLINGBIN:
                ArrayList<Point> recyclingBins = new ArrayList<>();
                for (AreaScan region : regions) {
                    recyclingBins.add(closestPoint(region.recyclingBins).getPoint());
                }
                return closestPoint(recyclingBins).getPoint();
            case RECYCLINGSTATION:
                ArrayList<Point> recyclingStations = new ArrayList<>();
                for (AreaScan region : regions) {
                    recyclingStations.add(closestPoint(region.recyclingStations).getPoint());
                }
                return closestPoint(recyclingStations).getPoint();
            case WASTEBIN:
                ArrayList<Point> wasteBins = new ArrayList<>();
                for (AreaScan region : regions) {
                    wasteBins.add(closestPoint(region.wasteBins).getPoint());
                }
                return closestPoint(wasteBins).getPoint();
            case WASTESTATION:
                ArrayList<Point> wasteStations = new ArrayList<>();
                for (AreaScan region : regions) {
                    wasteStations.add(closestPoint(region.wasteStations).getPoint());
                }
                return closestPoint(wasteStations).getPoint();
            default:
                return RECHARGE_POINT_LOCATION;
        }
    }

    public Point closestRecharge(){
        Point closestPoint, newPoint;
        if(!currentRegion.rechargingStations.isEmpty()){
            closestPoint = closestPoint(currentRegion.rechargingStations).getPoint();
            return closestPoint;
        } else {
            closestPoint = RECHARGE_POINT_LOCATION;
            for (AreaScan region : regions) {
                if (!region.rechargingStations.isEmpty()) {
                    newPoint = closestPoint(region.rechargingStations).getPoint();
                    if (newPoint.distanceTo(getPosition()) < closestPoint.distanceTo(getPosition())) {
                        closestPoint = newPoint;
                    }
                } else {
                    System.out.println("Nothing in this region");
                }
            }
        }
        return closestPoint;
    }

    private Task currentTask;

    public Action senseAndAct(Cell[][] view, long timestep) {
        if(timestep > 9500){
            timestep = timestep;
        }
        if (getChargeLevel()!= MAX_CHARGE && abs(getPosition().distanceTo(closestRecharge())-getChargeLevel()) < 5) {
            System.out.println("shit im hungry");
            previousState = agentState;
            agentState = AgentState.MOVETOCHARGER;
        }
        switch (agentState) {
            case INIT:
                forageForRecycling = false;
                forageForWaste = false;
                currentRegion = new AreaScan(getPosition());
                currentRegion.scanCells(view);
                regions.add(currentRegion);
                this.originalPoint = getPosition();
                this.explorationLocation = new Point(this.originalPoint.getX() +30, this.originalPoint.getY() + 30 + r.nextInt(60));
                if (currentRegion.wasteTasks.isEmpty()) {
                    if (currentRegion.recyclingTasks.isEmpty()) {
                        agentState = AgentState.EXPLORING;
                    }
                }

            case EXPLORING:
             evaluateRegion(view);
              if (this.currentTask == null) {
                    initCurrentTask();
                    if (this.explorationLocation.equals(getPosition())) {
                        this.explorationLocation = new Point(this.originalPoint.getX() + r.nextInt(100), this.originalPoint.getY() + r.nextInt(100));
                        System.out.println("Into the unknown!");
                    }
                    System.out.println("im looking for " + this.explorationLocation.toString());
                    return new MoveTowardsAction(this.explorationLocation);
              } else {
                  agentState = AgentState.MOVETOLITTERBIN;
              }

            case MOVETOLITTERBIN:
                if (this.currentTask.getClass().toString().equals(WASTETASK)) {
                    if (getCurrentCell(view) instanceof WasteBin && getPosition().equals(this.currentTask.getPosition())) {
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
                    if (getCurrentCell(view) instanceof RecyclingBin && getPosition().equals(this.currentTask.getPosition())) {
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
                    agentState = AgentState.EXPLORING;
                    if (getChargeLevel() == MAX_CHARGE) {
                        System.out.println("that was delicious");
                        agentState = previousState;
                    } else {
                        agentState = AgentState.EXPLORING;
                        System.out.println("that is delicious");
                        if(getLitterLevel()!=0){
                            agentState = AgentState.LITTERDISPOSAL;
                        } else if(getRecyclingLevel() != 0){
                            agentState = AgentState.LITTERDISPOSAL;
                        }
                        return new RechargeAction();
                    }
                } else {
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
                        if(getPosition().distanceTo(closestPoint(currentRegion.wasteStations).getPoint())/
                                closestPointOfAll(currentRegion.wasteTasks).distanceTo(this.currentTask.getPosition()) > 0.5){
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
                        if(getPosition().distanceTo(closestPoint(currentRegion.recyclingStations).getPoint())/
                                closestPointOfAll(currentRegion.recyclingTasks).distanceTo(this.currentTask.getPosition()) > 0.5){
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
                    if(getWasteLevel() < MAX_LITTER/20 && !currentRegion.wasteTasks.isEmpty()){
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
                        Point closestLitterDisposal;
                        try {
                            closestLitterDisposal = closestPoint(currentRegion.wasteStations).getPoint();
                        } catch (NullPointerException e) {
                            closestLitterDisposal = closestPointOfAll(currentRegion.wasteStations);
                        }
                        System.out.println("Need to grab that waste");
                        return new MoveTowardsAction(closestLitterDisposal);
                    } else {
                        agentState = AgentState.EXPLORING;
                    }
                } else if (this.currentTask.getClass().toString().equals(RECYCLINGTASK)) {
                    if(getRecyclingLevel() < MAX_LITTER/20 && !currentRegion.recyclingTasks.isEmpty()){
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
                        Point closestRecyclingDisposal;
                        try {
                            closestRecyclingDisposal = closestPoint(currentRegion.recyclingStations).getPoint();
                        } catch (NullPointerException e) {
                            closestRecyclingDisposal = closestPointOfAll(currentRegion.recyclingStations);
                        }
                        System.out.println("Need to grab that recycling");
                        return new MoveTowardsAction(closestRecyclingDisposal);
                    } else {
                        agentState = AgentState.EXPLORING;
                    }
                }
            default:
                return null;
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