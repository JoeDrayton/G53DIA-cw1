package uk.ac.nott.cs.g53dia.agent;

import uk.ac.nott.cs.g53dia.library.*;

import java.util.ArrayList;

public class AreaScan {
    private static final String WASTEBIN = "class uk.ac.nott.cs.g53dia.library.WasteBin";
    private static final String WASTESTATION = "class uk.ac.nott.cs.g53dia.library.WasteStation";
    private static final String RECYCLINGBIN = "class uk.ac.nott.cs.g53dia.library.RecyclingBin";
    private static final String RECYCLINGSTATION = "class uk.ac.nott.cs.g53dia.library.RecyclingStation";
    private static final String RECHARGEPOINT = "class uk.ac.nott.cs.g53dia.library.RechargePoint";
    private static final String EMPTYCELL = "class uk.ac.nott.cs.g53dia.library.EmptyCell";
    public ArrayList<WasteBin> wasteBins = new ArrayList<>();
    public ArrayList<WasteStation> wasteStations = new ArrayList<>();
    public ArrayList<RecyclingBin> recyclingBins = new ArrayList<>();
    public ArrayList<RecyclingStation> recyclingStations = new ArrayList<>();
    public ArrayList<RechargePoint> rechargingStations = new ArrayList<>();
    public ArrayList<Task> wasteTasks = new ArrayList<>();
    public ArrayList<Task> recyclingTasks = new ArrayList<>();
    public Point location;

    public void scanCells(Cell[][] view, Point position) {
        location = position;
        for (int i = 0; i < view.length; i++) {
            for (int j = 0; j < view.length; j++) {
                Cell currentCell = view[i][j];
                switch (view[i][j].getClass().toString()) {
                    case WASTEBIN:
                        if (!wasteBins.contains(currentCell)) {
                            WasteBin wasteBin = (WasteBin) currentCell;
                            if (wasteBin.getTask() != null) {
                                Task newTask = wasteBin.getTask();
                                wasteTasks.add(newTask);
                            }
                            wasteBins.add((WasteBin) view[i][j]);
                            //System.out.println("new cell is a waste bin");
                        }
                        break;
                    case WASTESTATION:
                        if (!wasteStations.contains(currentCell)) {
                            wasteStations.add((WasteStation) view[i][j]);
                            //System.out.println("new cell is a waste station");
                        }
                        break;
                    case RECYCLINGBIN:
                        if (!recyclingBins.contains(currentCell)) {
                            RecyclingBin recycleBin = (RecyclingBin) currentCell;
                            if (recycleBin.getTask() != null) {
                                Task newTask = recycleBin.getTask();
                                recyclingTasks.add(newTask);
                            }
                            recyclingBins.add((RecyclingBin) view[i][j]);
                            //System.out.println("new cell is a recycling bin");
                        }
                        break;
                    case RECYCLINGSTATION:
                        if (!rechargingStations.contains(currentCell)) {
                            recyclingStations.add((RecyclingStation) view[i][j]);
                            //System.out.println("new cell is a recycling station");
                        }
                        break;
                    case RECHARGEPOINT:
                        if (!rechargingStations.contains(currentCell)) {
                            rechargingStations.add((RechargePoint) view[i][j]);
                            //System.out.println("new cell is a recharging point");
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
}
