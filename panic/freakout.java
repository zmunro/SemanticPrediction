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

}

class ExtraItemTable {
    
    HashMap<ActionObjectPair, HashMap> eiTable;
    double getProb (ActionObjectPair aoPair, String extraItem) {
        HashMap map = this.eiTable.get(aoPair);
        ProbTuple probs = map.get(extraItem);
        return probs.weight();
    }
}



class PredictionPlanning {

    public static void main(String[] args) {
        HashMap<String, Double> pickupMap = new HashMap<String, Double>();
        
	}
}
