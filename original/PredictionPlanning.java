import java.util.*;
import java.lang.*;
import java.io.*;
  
// import org.json.simple.JSONArray; 
// import org.json.simple.JSONObject; 
// import org.json.simple.parser.*; 

class PredictionsManager {

	public ActionObjectTable aoTable;
    public ExtraItemTable extraItemsTable;
    public PredictionTree myPredictionTree;

    private int numObjectsTracking;
    private int numExtraItemsTracking;
    private double contextWeight;

    PredictionsManager(int numObjects, int numExtraItems) {
        this.numObjectsTracking = numObjectsTracking;
        this.numExtraItemsTracking = numExtraItemsTracking;
        this.contextWeight = 0.50;
        this.aoTable = new ActionObjectTable();
        this.extraItemsTable = new ExtraItemTable();
        this.myPredictionTree = new PredictionTree(numObjects, numExtraItems);
    }

    public void setContextWeight(double weight) {
        this.contextWeight = 0.50;
    }

    public FullSemantic getFullSemantic() {
        return this.myPredictionTree.getTopSemantic();
    }
}

// TODO: update algorithm to account for getting objects wrong
class PredictionTree {

    // Predicting is halted if response is definitely already defined
    private boolean haltPredictions;

    // These will represent the widths of the tree at its two levels
    private int numObjectsTracking;
    private int numExtraItemsTracking;

    private Action action;

    // The action that will actually be taken if our predicted full semantic 
    //   matches the real semantic. 
    private HashMap<FullSemantic, String> physicalResponses;

    PredictionTree(int numObjects, int numExtraItems) {
        this.numObjectsTracking = numObjects;
        this.numExtraItemsTracking = numExtraItems;
        this.haltPredictions = false;
        this.action = null;
        this.physicalResponses = new HashMap<FullSemantic, String>();
    }

    void setAction(Action someAction) {
        this.action = someAction;
    }

    Action getAction() {
        return this.action;
    }

    void haltPredictions() {
        this.haltPredictions = true;
    }

    // will eventually return an array of most likely semantics
    FullSemantic getTopSemantic() {
        return this.action.getTopSemantic();
    }
}

class ActionObjectTable {

    private HashMap<String, Action> allActions;

    // TODO: worry about dynamically updating weights later, lamda func stuff
    // void updatePriors(function, Action action);
    ActionObjectTable() {
        this.allActions = new HashMap<String, Action>();
    }

    // Difference between two functions is that getLikelyObjects() only returns
    //  the top "numObjectsTracking" objects for the given action.
    ActionObjectPair[] getLikelyObjects(String action) {
        return this.allActions.get(action).getLikelyObjects();
    }

    void addAction(Action someAction){
        this.allActions.put(someAction.getName(), someAction);
    }

    Action getAction(String someAction) {
        return this.allActions.get(someAction);
    }
}

class ExtraItemTable {

    private HashMap<String, FullSemantic> allFullSemantics;

    ExtraItemTable() {
        this.allFullSemantics = new HashMap<String, FullSemantic>();
    }

    FullSemantic getMostLikelySemantic(String object) {
        return this.allFullSemantics.get(object);
    }

    void addSemantic(FullSemantic someSemantic) {
        this.allFullSemantics.put(someSemantic.getExtraItemWords(), someSemantic);
    }

    FullSemantic getFullSemantic(String someSemantic) {
        return this.allFullSemantics.get(someSemantic);
    }
}

class Action {

	private ArrayList<Object> likelyObjects;
    private HashMap<String, Double> objects;

    private String name;
    private int numObjectsTracking;

    Action(String name, int numObjects) {
        this.name = name;
        this.numObjectsTracking =  numObjects;
        this.likelyObjects = new ArrayList<Object>(numObjects);
        this.objects = new HashMap<String, Double>();
    }

    ActionObjectPair[] getLikelyObjects() {
        return this.likelyObjects.toArray(new ActionObjectPair[0]);
    }

    double getObjectProb(String object) {
        return this.objects.get(object).getProb();
    }

    String getName() {
        return this.name;
    }

    void addObject(String objectName, double prob) {
        //TODO: possibly add to likelyObjects
        this.objects.put(objectName, prob);
    }
}

class Object {
    private String name;

    ActionObjectPair(String name) {
        this.name = name;
    }

    String getName() {
        return this.name;
    }

}

class FullSemantic {

    private String extraItemWords;

    FullSemantic(String extraItemWords) {
        this.extraItemWords = extraItemWords;
    }

    String getExtraItemWords() {
        return this.extraItemWords;
    }
}

class PredictionPlanning {

    public static void main(String[] args) {
        final int NUM_PREDICTIONS = 3;
        PredictionsManager pm = new PredictionsManager(3, 3);
        Action act = new Action("pickup", NUM_PREDICTIONS);
        act.addObject();
        pm.aoTable.addAction(act);
	}
}
