import java.util.*
import java.lang.*
import java.awt.Desktop.Action
import java.io.*
import java.math.*
// Stores the weight of a vertex between object/action or aoPair/extra item
internal class ProbTuple {
  var staticProb:Double = 0.toDouble()
  var dynamicProb:Double = 0.toDouble()
  
  // This can be modified to weight context higher or lower
  fun weight():Double {
    return (this.staticProb + 5 * this.dynamicProb) / 6.0
  }

  constructor(staticProb:Double) {
    this.staticProb = staticProb
    this.dynamicProb = 0.5
  }

  constructor(staticProb:Double, dynamicProb:Double) {
    this.staticProb = staticProb
    this.dynamicProb = dynamicProb
  }
}

internal class ActionObjectTable(aoTable:HashMap<String, HashMap<String, ProbTuple>>) {
  //<actionName, map of aoPairs to probabilities>
  var aoTable:HashMap<String, HashMap<String, ProbTuple>>
  
  fun getProb(actionName:String, objectName:String):Double {
    val map = this.aoTable.get(actionName)
    val probs = map.get(objectName)
    return probs.weight()
  }

  fun printProbs() {
    val it = aoTable.entries.iterator()
    while (it.hasNext())
    {
      val pair = it.next() as Entry<*, *>
      println(pair.key)
      val map = aoTable.get(pair.key)
      val objIt = map.entries.iterator()
      while (objIt.hasNext())
      {
        val objPair = objIt.next() as Entry<*, *>
        val prob = Math.round(map.get(objPair.key as String).weight() * 1000) / 1000.0
        println(" " + objPair.key + ": " + java.lang.Double.toString(prob))
      }
    }
  }

  fun getTopObjects(numGetting:Int, actionName:String):ArrayList<ObjectLevel> {
    val maxes = PriorityQueue<ObjectLevel>(SortByProbAO())
    val it = aoTable.get(actionName).entries.iterator()
    while (it.hasNext())
    {
      val pair = it.next() as Entry<*, *>
      val value = aoTable.get(actionName).get(pair.key)
      val objlev = ObjectLevel(
        pair.key.toString(),
        value
      )
      if (maxes.size() < numGetting)
      {
        maxes.add(objlev)
      }
      else if (value.weight() > maxes.peek().probability.weight())
      {
        maxes.poll()
        maxes.add(objlev)
      }
      it.remove() // avoids a ConcurrentModificationException
    }
    return ArrayList(Arrays.asList<ObjectLevel>(*maxes.toArray<ObjectLevel>(arrayOfNulls<ObjectLevel>(numGetting))))
  }
  init{
    this.aoTable = aoTable
  }

  // biases the weight of objects
  // args:
  // object: name of object/property to bias
  // strength: percent to multiply current dynamicProb by
  // override: if true then dynamicProb is set to strength provided 
  fun bias(myObject:String, strength:Double, override:Boolean) {
    val it = aoTable.entries.iterator()
    // Go through each action and see if it is connected to the object
    while (it.hasNext())
    {
      val pair = it.next() as Entry<*, *>
      val map = aoTable.get(pair.key)
      // bias unchopped object down
      if (map.containsKey(myObject))
      {
        val oldTuple = map.get(myObject)
        val newTuple:ProbTuple
        if (override)
        {
          newTuple = ProbTuple(oldTuple.staticProb, strength)
        }
        else
        {
          newTuple = ProbTuple(oldTuple.staticProb, oldTuple.dynamicProb * strength)
        }
        map.put(myObject, newTuple)
        aoTable.put(pair.key as String, map)
      }
    }
  }
}

internal class ExtraItemTable(eiTable:HashMap<String, HashMap<String, ProbTuple>>) {
  //<aoPair, map of ExtraItem strings to probabilities>
  var eiTable:HashMap<String, HashMap<String, ProbTuple>>
  fun getProb(myObject:String, extraItem:String):Double {
    val map = this.eiTable.get(myObject)
    val probs = map.get(extraItem)
    return probs.weight()
  }
  fun getTopExtraItems(numGetting:Int, myObject:String):ArrayList<ExtraItemLevel> {
    val maxes = PriorityQueue<ExtraItemLevel>(SortByProbEI())
    val it = eiTable.get(myObject).entries.iterator()
    while (it.hasNext())
    {
      val pair = it.next() as Entry<*, *>
      val value = eiTable.get(myObject).get(pair.key)
      val eil = ExtraItemLevel(
        pair.key as String,
        value
      )
      if (maxes.size() < numGetting)
      {
        maxes.add(eil)
      }
      else if (value.weight() > maxes.peek().probability.weight())
      {
        maxes.poll()
        maxes.add(eil)
      }
      it.remove() // avoids a ConcurrentModificationException
    }
    return ArrayList(Arrays.asList<ExtraItemLevel>(*maxes.toArray<ExtraItemLevel>(arrayOfNulls<ExtraItemLevel>(numGetting))))
  }
  init{
    this.eiTable = eiTable
  }
}

internal class SortByProbEI:Comparator<ExtraItemLevel> {
  public override fun compare(a:ExtraItemLevel, b:ExtraItemLevel):Int {
    return ((b.probability.weight() - a.probability.weight()) * 100).toInt()
  }
}

internal class SortByProbAO:Comparator<ObjectLevel> {
  public override fun compare(a:ObjectLevel, b:ObjectLevel):Int {
    return ((b.probability.weight() - a.probability.weight()) * 100).toInt()
  }
}

// Only to be used in the Prediction Tree
internal class ExtraItemLevel(extraItem:String, prob:ProbTuple) {
  var probability:ProbTuple
  var extraItem:String
  fun comparator():Double {
    return this.probability.weight()
  }
  init{
    this.extraItem = extraItem
    this.probability = prob
  }
}

// Only to be used in the Prediction Tree
internal class ObjectLevel(myObject:String, prob:ProbTuple) {
  var extraItems:ArrayList<ExtraItemLevel> // always sorted
  var objectName:String
  var probability:ProbTuple
  fun topExtraItem():ExtraItemLevel {
    return this.extraItems.get(0)
  }
  fun generatePredictions(eiTable:ExtraItemTable, numGetting:Int) {
    this.extraItems = eiTable.getTopExtraItems(numGetting, objectName)
  }
  init{
    this.objectName = myObject
    this.probability = prob
    this.extraItems = ArrayList<ExtraItemLevel>()
  }
}

// Only to be used in the Prediction Tree
internal class ActionLevel(action:String) {
  var objects:ArrayList<ObjectLevel> // always sorted
  var actionName:String
  fun topObject():ObjectLevel {
    return objects.get(0)
  }
  fun generatePredictions(
    aoTable:ActionObjectTable, eiTable:ExtraItemTable, numGetting:Int) {
    this.objects = aoTable.getTopObjects(numGetting, actionName)
    for (i in this.objects.indices)
    {
      objects.get(i).generatePredictions(eiTable, numGetting)
    }
  }
  init{
    this.actionName = action
  }
}

internal class FullSemantic {
  var action:ActionLevel
  var myObject:ObjectLevel
  var extraItem:ExtraItemLevel
  // When you instantiate a FullSemantic, it generates the predicted semantic
  constructor(action:ActionLevel) {
    this.action = action
    this.myObject = action.topObject()
    this.extraItem = this.myObject.topExtraItem()
  }
  constructor(action:ActionLevel, myObject:ObjectLevel) {
    this.action = action
    this.myObject = myObject
    this.extraItem = myObject.topExtraItem()
  }
}

internal class PredictionTree {
  var aoTable:ActionObjectTable
  var eiTable:ExtraItemTable
  var correctObject:ObjectLevel
  var action:ActionLevel
  val fullSemantic:FullSemantic
  get() {
    if (correctObject == null)
    {
      val fs = FullSemantic(this.action)
      return fs
    }
    else
    {
      val fs = FullSemantic(this.action, this.correctObject)
      return fs
    }
  }
  fun generatePredictions(numGetting:Int) {
    this.action.generatePredictions(
      this.aoTable, this.eiTable, numGetting)
  }
  init{
    this.correctObject = null
  }
}

internal object SemanticPrediction {
  @JvmStatic fun main(args:Array<String>) {
    val file = File("C:\\Users\\User\\Documents\\HRI\\prediction\\input.txt")
    val sc:Scanner = null
    try
    {
      sc = Scanner(file)
    }
    catch (ex:FileNotFoundException) {
      // insert code to run when exception occurs
      println(ex)
    }
    val aoTable = HashMap<String, HashMap<String, ProbTuple>>()
    val objectProbMap = HashMap<String, ProbTuple>()
    val eiTable = HashMap<String, HashMap<String, ProbTuple>>()
    val extraItemProbMap = HashMap<String, ProbTuple>()
    val st:String
    val currentAction:String = null
    val currentObject:String = null
    // Kinda sloppy input processing algorithm
    while (sc.hasNextLine())
    {
      st = sc.nextLine().replace(("\\s+").toRegex(), "")
      // System.out.println(st);
      if (st.get(0) == '-')
      {
        st = sc.nextLine().replace(("\\s+").toRegex(), "")
      }
      else if (st.get(0) == '!')
      {
        // System.out.println("found end");
        aoTable.put(currentAction, objectProbMap)
        objectProbMap = HashMap<String, ProbTuple>()
        eiTable.put(currentObject, extraItemProbMap)
        extraItemProbMap = HashMap<String, ProbTuple>()
        break
      }
      val lastChar = st.get(st.length - 1)
      if (lastChar == ':')
      {
        // is an action
        val action = st.split((":").toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]
        // System.out.println("action: " + action);
        if (currentAction != null)
        {
          aoTable.put(currentAction, objectProbMap)
          objectProbMap = HashMap<String, ProbTuple>()
        }
        currentAction = action
      }
      else if (lastChar == ';')
      {
        // is an object
        val myObject = st.split((",").toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]
        // System.out.println(" object: " + object);
        if (currentObject != null)
        {
          eiTable.put(currentObject, extraItemProbMap)
          extraItemProbMap = HashMap<String, ProbTuple>()
        }
        currentObject = myObject
        val prob = 0.0
        try
        {
          prob = java.lang.Double.valueOf(st.substring(0, st.length - 1).split((",").toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1])
        }
        catch (e:Exception) {
          println("Exception occurred")
          println(st)
          return
        }
        val probability:ProbTuple
        if (myObject.contains("(cooked)") || myObject.contains("(chopped)"))
        {
          probability = ProbTuple(prob, 0.0)
        }
        else
        {
          probability = ProbTuple(prob)
        }
        objectProbMap.put(myObject, probability)
      }
      else
      {
        // is an extra item
        val extraItem = st.split((",").toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[0]
        // System.out.println(" ei: " + extraItem);
        val prob = java.lang.Double.valueOf(st.split((",").toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[1])
        val probability = ProbTuple(prob)
        extraItemProbMap.put(extraItem, probability)
      }
    }
    val actionObjTable = ActionObjectTable(aoTable)
    val extraItemTable = ExtraItemTable(eiTable)
    val pt = PredictionTree()
    pt.aoTable = actionObjTable
    pt.eiTable = extraItemTable
    // actionObjTable.printProbs();
    test(pt)
  }
  // Test out the prediction of a full semantic given an action
  fun test(pt:PredictionTree) {
    val scanner = Scanner(System.`in`)
    val newAction:ActionLevel = null
    val invalidAction = true
    val userActionName = ""
    while (invalidAction)
    {
      try
      {
        println("Enter action: ")
        userActionName = scanner.nextLine()
        newAction = ActionLevel(userActionName)
        pt.action = newAction
        pt.generatePredictions(3)
        invalidAction = false
      }
      catch (e:Exception) {
        invalidAction = true
        println("Not a valid action!!")
        println("------------------")
      }
    }
    scanner.close()
    val fs = pt.fullSemantic
    print(fs.action.actionName + " ")
    print(fs.myObject.objectName + " ")
    println(fs.extraItem.extraItem)
  }
}