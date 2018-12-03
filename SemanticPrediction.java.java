import java.util.*;
import java.lang.*;
import java.io.*;

class ProbTuple{
    double staticProb;
    double dynamicProb;

    weight() {
        return (probs.staticProb + probs.dynamicProb) / 2.0;
    }
}

class ActionObjectPair {
    String action;
    String object;
}

class ActionObjectTable {
    
    //<actionName, map of aoPairs>
    HashMap<String, HashMap> aoTable;

    double getProb (String actionName, String objectName) {
        HashMap map = this.aoTable.get(actionName);
        ProbTuple probs = map.get(objectName);
        return probs.weight();
    }

    ObjectLevel[] getTopObjects(int numGetting, String actionName) {
        PriorityQueue<ObjectLevel> maxes = new 
            PriorityQueue<ObjectLevel>(new SortByProbAO());
        Iterator it = aoTable.get(actionName).entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            ObjectLevel objlev = new ObjectLevel(
                pair.getKey(),
                pair.getValue()
            );
            if (maxes.size() < numGetting) {
                maxes.add(objlev);
            } else if (pair.getValue() > maxes.get(0).probability) {
                maxes.poll();
                maxes.add(objlev);
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        return maxes.toArray(new ObjectLevel[numGetting]);
    }
}

class ExtraItemTable {
    
    HashMap<ActionObjectPair, HashMap> eiTable;

    double getProb (ActionObjectPair aoPair, String extraItem) {
        HashMap map = this.eiTable.get(aoPair);
        ProbTuple probs = map.get(extraItem);
        return probs.weight();
    }

    ExtraItemLevel[] getTopExtraItems(int numGetting, ActionObjectPair aoPair) {
        PriorityQueue<ExtraItemLevel> maxes = new 
            PriorityQueue<ExtraItemLevel>(new SortByProbEI());
        Iterator it = eiTable.get(aoPair).entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            ExtraItemLevel eil = new ExtraItemLevel(
                pair.getKey(),
                pair.getValue()
            );
            if (maxes.size() < numGetting) {
                maxes.add(eil);
            } else if (pair.getValue() > maxes.get(0).probability) {
                maxes.poll();
                maxes.add(eil);
            }
            it.remove(); // avoids a ConcurrentModificationException
        }
        return maxes.toArray(new ExtraItemLevel[numGetting]);
    }
}

class SortByProbEI implements Comparator<ExtraItemLevel> { 
    public int compare(ExtraItemLevel a, ExtraItemLevel b) { 
        return a.probability - b.probability;
    } 
}

class SortByProbAO implements Comparator<ObjectLevel> { 
    public int compare(ObjectLevel a, ObjectLevel b) { 
        return a.probability - b.probability;
    }
}

class ExtraItemLevel {
    double probability;
    String extraItem;

    public double comparator() {
        return this.probability;
    }

    ExtraItemLevel(String extraItem, double prob) {
        this.extraItem = extraItem;
        this.probability = prob;
    }
}

class ObjectLevel {
    ExtraItemLevel[] extraItems; // always sorted
    String objectName;

    double probability;

    ExtraItemLevel topExtraItem() {
        return this.extraItems.get(0);
    }

    void generatePredictions(ExtraItemTable eiTable, int numGetting) {
        this.extraItems = eiTable.getTopExtraItems(numGetting);
    }

    ObjectLevel(String object, double prob) {
        this.objectName = object;
        this.probability = prob;
        this.extraItems = new ArrayList<ExtraItemLevel>;
    }
}

class ActionLevel {

    ObjectLevel[] objects; // always sorted
    String actionName;
    
    ObjectLevel topObject() {
        return objects.get(0);
    }

    void generatePredictions( 
        ActionObjectTable aoTable, ExtraItemTable eiTable, int numGetting) {  
        this.objects = aoTable.getTopObjects(numGetting);
        for(int i = 0; i < ObjectLevel.size(); i++) {
            objects[i].generatePredictions(eiTable, numGetting);
        }
    }
}

class FullSemantic {
    ActionLevel action;
    ObjectLevel object;
    ExtraItemLevel extraItem;

    // When you instantiate a FullSemantic, it generates the predicted semantic
    FullSemantic(ActionLevel action) {
        this.action = action;
        this.object = action.topObject();
        this.extraItem = this.object.topExtraItem();
    }
}


class PredictionTree {
    ActionObjectTable aoTable;
    ExtraItemTable eiTable;

    ActionLevel action;

    void generatePredictions(int numGetting) {
        this.action.generatePredictions(
            this.aoTable, this.eiTable, numGetting);
    }

    String getFullSemantic() {
        FullSemantic fs = new FullSemantic(this.action);
    }
}

class PredictionPlanning {

    public static void main(String[] args) {
        HashMap<String, Double> pickupMap = new HashMap<String, Double>();   
	}
}
