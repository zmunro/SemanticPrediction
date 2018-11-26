public static final int NUM_OBJECTS = 5;
public static final int NUM_ACTIONS = 5;

class PredictionPlanning {
    public static void main(String[] args) {
        PredictionsManager pm = new PredictionsManager(3, 3);
        
        ObjectMapper mapper = new ObjectMapper();
		InputStream is = Test.class.getResourceAsStream("/input.json");
		testObj = mapper.readValue(is, Test.class);
		System.out.println(testObj.actions[0].name);
	}
}


class PredictionsManager {
    // Number of most likely objects/extraItems to keep track of.
    //  Will be provided in the constructor. 
    //  At any time, the most likely objects/extraItems can be got in O(1) time
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
        this.myPredictionTree = new PredictionTree(numObjects, numExtraItems);
    }

    public void setContextWeight(float weight) {
        this.contextWeight = 0.50;
    }

    public FullSemantic getFullSemantic() {
        return this.myPredictionTree.getFullSemantic();
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

    FullSemantic getFullSemantic() {
        return this.action.getFullSemantic();
    }

    PredictionTree(int numObjects, int numExtraItems) {
        this.numObjectsTracking = numObjects;
        this.numExtraItemsTracking = numExtraItems;
        this.haltPredictions = False;
        this.action = NULL;
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
    FullSemantic[] getLikelyExtraItems(String object) {
        return this.allFullSemantics.get(object).getLikelyExtraItems();
    }

    void addSemantic(FullSemantic someSemantic) {
        this.allFullSemantics.put(someSemantic.getObjectName(), someSemantic);
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

    ActionObjectPair mostLikelyObject() {
        int maxProbability = 0;
        ActionObjectPair mostLikelyObject;
        for(int i = 0; i < this.likelyObjects.length(), i++) {
            int prob = this.objects[i].getProb();
            if (prob > maxProbability) {
                mostLikelyObject = this.objects[i];
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
    FullSemantic[] getAllExtraItems() {
        return this.allExtraItems.values().toArray();
    }

    FullSemantic[] getLikelyExtraItems() {
        return this.likelyExtraItems.toArray();
    }

    FullSemantic getMostLikelySemantic() {
        int maxProbability = 0;
        FullSemantic mostLikelySemantic;
        for(int i = 0; i < this.likelyExtraItems.length(), i++) {
            int prob = this.likelyExtraItems[i].getProb();
            if (prob > maxProbability) {
                mostLikelySemantic = this.likelyExtraItems[i];
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