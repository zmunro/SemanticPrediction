class PredictionsManager {
	// Number of most likely objects/extraItems to keep track of.
	// 	Will be provided in the constructor. 
	//	At any time, the most likely objects/extraItems can be got in O(1) time
	private int numObjectsTracking;
	private int numExtraItemsTracking;

	private ActionObjectTable aoTable;
	private ExtraItemTable extraItemsTable;
    private PredictionTree myPredictionTree;
    private float contextWeight;

    PredictionsManager(int numObjects, int numExtraItems) {
        this.numObjectsTracking = numObjectsTracking;
        this.numExtraItemsTracking = numExtraItemsTracking
        this.contextWeight = 0.50;
        this.aoTable = new ActionObjectTable();
        this.extraItemsTable = new ExtraItemTable();
        this.myPredictionTree = new PredictionTree();
    }

    public void setContextWeight(float weight) {
        this.contextWeight = 0.50;
    }

    public Object[] getFullSemantics(String action) {

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

    void addObject(ActionObjectPair aoPair) {
        this.objects.add(aoPair);
    }

    void haltPredictions() {
        this.haltPredictions = True;
    }

    PredictionTree(int numObjects, int numExtraItems) {
        this.numObjectsTracking = numObjects;
        this.numExtraItemsTracking = numExtraItems;
        this.haltPredictions = False;
        this.action = NULL;
        this.objects = new ArrayList<ActionObjectPair>(numObjects);
        this.physicalResponses = new HashMap<FullSemantic, String>();
    }
        

    // Predicting is halted if response is definitely already defined
    private boolean haltPredictions;

    // These will represent the widths of the tree at its two levels
    private int numObjectsTracking;
    private int numExtraItemsTracking;

    private Action action;
    private ArrayList<ActionObjectPair> objects;

    // The action that will actually be taken if our predicted full semantic 
    //   matches the real semantic. 
    private HashMap<FullSemantic, String> physicalResponses;
}

class ActionObjectTable {
    // Difference between two functions is that getLikelyObjects() only returns
    //  the top "numObjectsTracking" objects for the given action.
    Object[] getAllObjects(Action action);
    Object[] getLikelyObjects(Action action);

    void updatePriors(function, Action action);

    private HashMap<String, Action> allActions;
    private int numObjectsTracking;
}


class ExtraItemTable {
	FullSemantic[] getAllExtraItems(ActionObjectPair aoPair);
	FullSemantic[] getLikelyExtraItems(ActionObjectPair aoPair);

	void updateExtraItems(function, ActionObjectPair aoPair);

	private HashMap<ActionObjectPair, FullSemantic> FullSemantics;
	private int numFullSemanticsTracking;
}

class Action {
	ActionObjectPair[] getAllObjects() {
        return this.objects.values().toArray();
    }

	ActionObjectPair[] getLikelyObjects() {
        return this.likelyObjects.toArray();
    }

	float getObjectProb(String object) {
        return this.objects.get(object).getProb();
    }

	String getName() {
        return this.name;
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
    FullSemantic[] getAllExtraItems() {
        return this.allExtraItems.values().toArray();
    }

    FullSemantic[] getLikelyExtraItems() {
        return this.likelyExtraItems.toArray();
    }

    String getActionName() {
        return this.action;
    }

    String getObjectName() {
        return this.object;
    }

    void setDynamicProb(float prob) {
        this.dynamicProbability = prob;
    }
    
    float getProb() {
        return this.staticProbability * (1.0 - this.contextWeight) +
        this.dynamicProbability * this.contextWeight;
    }

    ActionObjectPair(
        String action, String object, int numExtraItems,
        float weight, float static, float dynamic) {

        this.allExtraItems = new HashMap<String, FullSemantic>();
        this.likelyExtraItems = new ArrayList<FullSemantic>(numExtraItems);
        this.action = action;
        this.object = object;
        this.contextWeight = weight;
        this.staticProbability = static;
        this.dynamicProbability = dynamic;
        this.numExtraItemsTracking = numExtraItems;
    }

    private ArrayList<FullSemantic> likelyExtraItems;
    private HashMap<String, FullSemantic> allExtraItems;
    private String action;
    private String object;
    private float contextWeight;
    private float staticProbability;
    private float dynamicProbability;
    private int numExtraItemsTracking;
}

class FullSemantic {
    ActionObjectPair getAOPair() {
        return this.aoPair;
    }
	
    float getProb() {
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
        ActionObjectPair aoPair, String extraItemWords, float static,
        float dynamic, float weight) {

        this.aoPair = aoPair;
        this.extraItemWords = extraItemWords;
        this.contextWeight = weight;
        this.staticProbability = static;
        this.dynamicProbability = dynamic;
    }

    private ActionObjectPair aoPair;	
	private String extraItemWords;
	private float staticProb;
    private float dynamicProb;
    private float contextWeight;
    private String responseUtterance;
    private File actionScript;
}