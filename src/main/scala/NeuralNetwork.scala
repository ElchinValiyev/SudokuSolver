import java.io.File

import org.bytedeco.javacpp.opencv_core.Size
import org.bytedeco.javacpp.opencv_imgcodecs._
import org.bytedeco.javacpp.opencv_imgproc._
import org.deeplearning4j.eval.Evaluation
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.layers.{DenseLayer, OutputLayer}
import org.deeplearning4j.nn.conf.{MultiLayerConfiguration, NeuralNetConfiguration, Updater}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction

class NeuralNetwork {

  val inputImageWidth = 10
  val inputImageHeight = 10
  val numberOfClasses = 10

  val savedNetworkFile = new File("trained_model.nn")
  val network = initializeNetwork() // neural network for classification

  /** Reads training dataset from files */
  def getDataSetFromFolder(folderName: String): DataSet = {

    val exampleFolder = new File(folderName)
    require(exampleFolder.isDirectory)

    val numberOfExamples = countFilesInDirectory(exampleFolder)

    val input = Nd4j.zeros(numberOfExamples, inputImageWidth * inputImageHeight)
    val output = Nd4j.zeros(numberOfExamples, numberOfClasses)

    val digitFolders = exampleFolder.listFiles()
    for ((folder, index) <- digitFolders.zipWithIndex if folder.isDirectory) {

      val files = folder.listFiles()
      for (file <- files if file.isFile) {
        var subMatrix = imread(file.getPath, IMREAD_GRAYSCALE)
        resize(subMatrix, subMatrix, new Size(inputImageWidth, inputImageHeight), 0, 0, INTER_NEAREST) // make image smaller
        subMatrix = subMatrix.reshape(1, inputImageWidth * inputImageHeight) // preparing to pass to neural network

        val bytes = new Array[Byte](inputImageWidth * inputImageHeight)
        subMatrix.ptr().get(bytes) // transform mat to array

        for (j <- bytes.indices) // transform array to binary
          bytes(j) = if (bytes(j) < 0) 1 else 0

        val target = folder.getName()(0) - '0'
        input.putRow(index, Nd4j.create(bytes.map(_.toDouble)))
        output.putRow(index, Nd4j.create(Array.tabulate(numberOfClasses)(i => if (i == target) 1.0 else 0.0)))
      }
    }
    println("Dataset is ready!")
    new DataSet(input, output)
  }

  private def countFilesInDirectory(directory: File): Int = {
    var count = 0
    for (file <- directory.listFiles) {
      if (file.isFile) count += 1
      if (file.isDirectory) count += countFilesInDirectory(file)
    }
    count
  }

  /** Create configuration for new neural network */
  def initializeConfiguration(): MultiLayerConfiguration = {
    val inputNum = inputImageWidth * inputImageHeight
    val hiddenNum = inputNum / 2
    val outputNum = numberOfClasses
    new NeuralNetConfiguration.Builder()
      .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
      .iterations(1)
      .learningRate(0.01)
      .updater(Updater.ADADELTA)
      .miniBatch(true)
      .list()
      .layer(0, new DenseLayer.Builder()
        .nIn(inputNum)
        .nOut(hiddenNum)
        .activation("tanh")
        .weightInit(WeightInit.XAVIER)
        .build())
      .layer(1, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
        .nIn(hiddenNum)
        .nOut(outputNum)
        .activation("softmax")
        .weightInit(WeightInit.XAVIER)
        .build())
      .pretrain(false)
      .backprop(true)
      .build()
  }

  /** Tries to load saved network from file, if fails - creates new network */
  def initializeNetwork(): MultiLayerNetwork = {
    if (savedNetworkFile.exists()) {
      println("Model is loaded from file")
      ModelSerializer.restoreMultiLayerNetwork(savedNetworkFile)
    } else {
      println("Creating network")
      val conf = initializeConfiguration()
      val model = new MultiLayerNetwork(conf)
      model.init()
      println("Model is initialized!")
      model
    }
  }

  /** Trains network and saves afterwards */
  def train(numberOfEpochs: Int): Unit = {
    println("Train model....")
    val dataSet = getDataSetFromFolder("train_data")
    for (i <- 0 until numberOfEpochs)
      network.fit(dataSet)
  }

  /** Trains network and saves it at the peak of its accuracy */
  def trainWithEarlyStopping(
                              maxEpochs: Int,
                              trainDataFolder: String = "train_data",
                              testDataFolder: String = "test_data"
                            ): Unit = {
    val ds = getDataSetFromFolder(trainDataFolder)
    val ts = getDataSetFromFolder(testDataFolder)

    var maxAccuracy = 0.0
    for (i <- 0 until maxEpochs) {
      val acc = evaluate(ts, printStats = false)
      if (acc > maxAccuracy) {
        maxAccuracy = acc
        saveNetwork(network)
      }
      network.fit(ds)
    }
    evaluate(ts)
  }

  /** Saves network to the file */
  def saveNetwork(net: MultiLayerNetwork): Unit = {
    ModelSerializer.writeModel(net, savedNetworkFile, true)
  }

  /** returns the accuracy on passed dataset */
  def evaluate(dataSet: DataSet, printStats: Boolean = true): Double = {
    val modelOutput = network.output(dataSet.getFeatureMatrix)
    val eval = new Evaluation(10)
    eval.eval(dataSet.getLabels, modelOutput)
    if (printStats)
      println(eval.stats())
    eval.accuracy()
  }

  /** predicts to which class belongs image */
  def predict(bytes: Array[Byte]): Int = {
    val prediction = network.output(Nd4j.create(bytes.map(x => if (x == 0) 0.0 else 1.0)))
    Nd4j.argMax(prediction, 1).getInt(0)
  }
}
