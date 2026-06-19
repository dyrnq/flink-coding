package sample

import org.apache.flink.api.common.RuntimeExecutionMode
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala._

import scala.math.random

/**
 * Flink Pi — Monte Carlo estimation using DataStream API
 *
 * Equivalent to Spark project: SparkPi.scala
 *
 * Generates random points in a unit square, counts those inside the unit circle,
 * and estimates π ≈ 4 * count_inside / total_points.
 */
object FlinkPi {

  def main(args: Array[String]): Unit = {

    val params = ParameterTool.fromArgs(args)
    val slices = params.getInt("slices", 10)
    val n = math.min(100000L * slices, Int.MaxValue).toInt

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setRuntimeMode(RuntimeExecutionMode.BATCH)

    val count = env
      .fromSequence(1, n)
      .map { _ =>
        val x = random() * 2 - 1
        val y = random() * 2 - 1
        if (x * x + y * y <= 1) 1 else 0
      }
      .reduce(_ + _)
      .executeAndCollect()
      .next()

    println(s"Pi is roughly ${4.0 * count / n}")
  }
}
