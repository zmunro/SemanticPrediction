import java.util.*;
import java.lang.*;
import java.io.*;

class PredictionsManager {
    // Number of most likely objects/extraItems to keep track of.
    //  Will be provided in the constructor. 
    //  At any time, the most likely objects/extraItems can be got in O(1) time
    private int numObjectsTracking;
    private int numExtraItemsTracking;

    private ActionObjectTable aoTable;
    private ExtraItemTable extraItemsTable;
    private PredictionTree myPredictionTree;
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

    PredictionTree(int numObjects, int numExtraItems) {
        this.numObjectsTracking = numObjects;
        this.numExtraItemsTracking = numExtraItems;
        this.haltPredictions = false;
        this.action = null;
        this.physicalResponses = new HashMap<FullSemantic, String>();
    }
        

    // Predicting is halted if response is definitely already defined
    private boolean haltPredictions;

    // These will represent the widths of the tree at its two levels
    private int numObjectsTracking;
    private int numExtraItemsTracking;

    private Action action;

    // The action that will actually be taken if our predicted full semantic 
    //   matches the real semantic. 
    private HashMap<FullSemantic, String> physicalResponses;
}

class ActionObjectTable {
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

    // TODO: worry about dynamically updating weights later, lamda func stuff
    // void updatePriors(function, Action action);
    ActionObjectTable() {
        this.allActions = new HashMap<String, Action>();
    }

    private HashMap<String, Action> allActions;
}

class ExtraItemTable {
    FullSemantic getMostLikelySemantic(String object) {
        return this.allFullSemantics.get(object);
    }

    void addSemantic(FullSemantic someSemantic) {
        this.allFullSemantics.put(someSemantic.getExtraItemWords(), someSemantic);
    }

    FullSemantic getFullSemantic(String someSemantic) {
        return this.allFullSemantics.get(someSemantic);
    }

    ExtraItemTable() {
        this.allFullSemantics = new HashMap<String, FullSemantic>();
    }

    // TODO: worry about dynamically updating weights later, lamda func stuff
    // void updateExtraItems(function, ActionObjectPair aoPair);

    private HashMap<String, FullSemantic> allFullSemantics;
}

class Action {

    ActionObjectPair[] getLikelyObjects() {
        return this.likelyObjects.toArray(new ActionObjectPair[0]);
    }

    double getObjectProb(String object) {
        return this.objects.get(object).getProb();
    }

    String getName() {
        return this.name;
    }

    FullSemantic getTopSemantic() {
        return this.getTopObject().getTopSemantic();
    }

    ActionObjectPair getTopObject() {
        int maxProbability = 0;
        ActionObjectPair mostLikelyObject = null;
        for(int i = 0; i < this.likelyObjects.size(); i++) {
            double prob = this.objects.get(i).getProb();
            if (prob > maxProbability) {
                mostLikelyObject = this.objects.get(i);
            }
        }
        return mostLikelyObject;
    }

    Action(String action, int numObjects) {
        this.name = action;
        this.numObjectsTracking =  numObjects;
        this.likelyObjects = new ArrayList<ActionObjectPair>(numObjects);
    }

    private ArrayList<ActionObjectPair> likelyObjects;
    private HashMap<String, ActionObjectPair> objects;
    private String name;
    private int numObjectsTracking;
}

class ActionObjectPair {

    FullSemantic[] getLikelyExtraItems() {
        return this.likelyExtraItems.toArray(new FullSemantic[0]);
    }

    FullSemantic getTopSemantic() {
        int maxProbability = 0;
        FullSemantic mostLikelySemantic = null;
        for(int i = 0; i < this.likelyExtraItems.size(); i++) {
            double prob = this.likelyExtraItems.get(i).getProb();
            if (prob > maxProbability) {
                mostLikelySemantic = this.likelyExtraItems.get(i);
            }
        }
        return mostLikelySemantic;
    }

    String getActionName() {
        return this.action;
    }

    String getObjectName() {
        return this.object;
    }

    void setDynamicProb(double prob) {
        this.dynamicProbability = prob;
    }
    
    double getProb() {
        return this.staticProbability * (1.0 - this.contextWeight) +
        this.dynamicProbability * this.contextWeight;
    }

    ActionObjectPair(
        String action, String object, int numExtraItems,
        double weight, double staticP, double dynamicP) {

        this.allExtraItems = new HashMap<String, FullSemantic>();
        this.likelyExtraItems = new ArrayList<FullSemantic>(numExtraItems);
        this.action = action;
        this.object = object;
        this.contextWeight = weight;
        this.staticProbability = staticP;
        this.dynamicProbability = dynamicP;
        this.numExtraItemsTracking = numExtraItems;
    }

    private ArrayList<FullSemantic> likelyExtraItems;
    private HashMap<String, FullSemantic> allExtraItems;
    private String action;
    private String object;
    private double contextWeight;
    private double staticProbability;
    private double dynamicProbability;
    private int numExtraItemsTracking;
}

class FullSemantic {
    ActionObjectPair getAOPair() {
        return this.aoPair;
    }
    
    double getProb() {
        return this.staticProbability * (1.0 - this.contextWeight) +
        this.dynamicProbability * this.contextWeight;
    }

    String getExtraItemWords() {
        return this.extraItemWords;
    }

    void setAction(String fileName){
        this.actionScript = fileName;
    }

    void setResponse(String utt) {
        this.responseUtterance = utt;
    }

    FullSemantic(
        ActionObjectPair aoPair, String extraItemWords, double staticP,
        double dynamicP, double weight) {

        this.aoPair = aoPair;
        this.extraItemWords = extraItemWords;
        this.contextWeight = weight;
        this.staticProbability = staticP;
        this.dynamicProbability = dynamicP;
    }

    private ActionObjectPair aoPair;    
    private String extraItemWords;
    private double staticProbability;
    private double dynamicProbability;
    private double contextWeight;
    private String responseUtterance;
    private String actionScript;
}

class PredictionPlanning {
    public static void main(String[] args) {
        PredictionsManager pm = new PredictionsManager(3, 3);
        
        // ObjectMapper mapper = new ObjectMapper();
		// InputStream is = Test.class.getResourceAsStream("/input.json");
		// testObj = mapper.readValue(is, Test.class);
		// System.out.println(testObj.actions[0].name);
	}
}