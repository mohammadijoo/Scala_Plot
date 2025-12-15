//> using scala "3.3.3"
//> using dep "org.knowm.xchart:xchart:3.8.8"

import org.knowm.xchart.*
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle
import org.knowm.xchart.CategorySeries.CategorySeriesRenderStyle
import org.knowm.xchart.internal.chartpart.Chart
import org.knowm.xchart.style.Styler
import org.knowm.xchart.style.lines.SeriesLines
import org.knowm.xchart.style.markers.SeriesMarkers

import scala.jdk.CollectionConverters.*
import scala.util.Random
import scala.io.{Source, Codec}
import java.nio.charset.StandardCharsets
import javax.swing.JFrame

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.imageio.IIOImage
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.FileImageOutputStream
import java.awt.Graphics2D

object Line:

  private type AnyChart = Chart[?, ?]

  private val rng = new Random(42)

  // Export settings
  private val ExportDpi: Int = 300
  private val TickPattern: String = "#0.0"

  // ----------------------------
  // Utilities
  // ----------------------------
  private def linspace(start: Double, end: Double, n: Int = 100): Array[Double] =
    require(n >= 2, "linspace requires n >= 2")
    val step = (end - start) / (n - 1).toDouble
    Array.tabulate(n)(i => start + i * step)

  private def randn(n: Int, mu: Double = 0.0, sigma: Double = 1.0): Array[Double] =
    Array.fill(n)(mu + sigma * rng.nextGaussian())

  private def applyOneDecimalTicks(chart: XYChart): Unit =
    chart.getStyler.setXAxisDecimalPattern(TickPattern)
    chart.getStyler.setYAxisDecimalPattern(TickPattern)

  private def applyOneDecimalTicks(chart: CategoryChart): Unit =
    chart.getStyler.setYAxisDecimalPattern(TickPattern)

  private def maybeShowSingle[T <: AnyChart](chart: T, show: Boolean, windowTitle: String): Unit =
    if !show then return
    val frame: JFrame = new SwingWrapper[T](chart).displayChart()
    frame.setTitle(windowTitle)

  private def maybeShowMatrix(charts: Seq[AnyChart], rows: Int, cols: Int, title: String, show: Boolean): Unit =
    if !show then return
    val jl: java.util.List[AnyChart] = charts.map(_.asInstanceOf[AnyChart]).asJava
    val frame: JFrame = new SwingWrapper[AnyChart](jl, rows, cols).displayChartMatrix()
    frame.setTitle(title)

  // ----------------------------
  // High-quality saving (300 DPI)
  // ----------------------------
  private def savePng300(chart: AnyChart, baseNameNoExt: String): Unit =
    // Produces: <baseNameNoExt>.png
    BitmapEncoder.saveBitmapWithDPI(chart, baseNameNoExt, BitmapFormat.PNG, ExportDpi)

  // Write PNG with pHYs DPI metadata (useful for print/layout tools)
  private def writePngWithDpi(image: BufferedImage, filePath: String, dpi: Int): Unit =
    val writer = ImageIO.getImageWritersByFormatName("png").next()
    val ios = new FileImageOutputStream(new java.io.File(filePath))
    try
      writer.setOutput(ios)
      val param = writer.getDefaultWriteParam
      val typeSpecifier = javax.imageio.ImageTypeSpecifier.createFromBufferedImageType(image.getType)
      val metadata = writer.getDefaultImageMetadata(typeSpecifier, param)

      val ppm = Math.round(dpi * 39.3700787).toInt // pixels per meter
      val root = new IIOMetadataNode("javax_imageio_png_1.0")
      val phys = new IIOMetadataNode("pHYs")
      phys.setAttribute("pixelsPerUnitXAxis", ppm.toString)
      phys.setAttribute("pixelsPerUnitYAxis", ppm.toString)
      phys.setAttribute("unitSpecifier", "meter")
      root.appendChild(phys)

      metadata.mergeTree("javax_imageio_png_1.0", root)
      val iio = new IIOImage(image, null, metadata)
      writer.write(null, iio, param)
    finally
      try ios.close() catch case _: Throwable => ()
      try writer.dispose() catch case _: Throwable => ()

  // Save a chart matrix as a single high-res PNG where fonts/lines/markers scale consistently.
  private def saveMatrixPng300(charts: Seq[AnyChart], rows: Int, cols: Int, baseNameNoExt: String): Unit =
    require(charts.nonEmpty, "charts is empty")
    require(rows * cols >= charts.size, "rows*cols must be >= number of charts")

    val w = charts.head.getWidth
    val h = charts.head.getHeight

    // XChart DPI scaling convention: 72 is the baseline DPI
    val scaleFactor = ExportDpi / 72.0

    val outW = (w * cols * scaleFactor).toInt
    val outH = (h * rows * scaleFactor).toInt

    val image = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB)
    val g0 = image.createGraphics().asInstanceOf[Graphics2D]

    val at = g0.getTransform
    at.scale(scaleFactor, scaleFactor)
    g0.setTransform(at)

    charts.zipWithIndex.foreach { (chart, idx) =>
      val r = idx / cols
      val c = idx % cols
      val g = g0.create().asInstanceOf[Graphics2D]
      try
        g.translate(c * w, r * h)
        chart.paint(g, w, h)
      finally
        g.dispose()
    }

    g0.dispose()
    writePngWithDpi(image, s"$baseNameNoExt.png", ExportDpi)

  // ----------------------------
  // CSV reader
  // ----------------------------
  // Local file example:
  // val raw = Source.fromFile("C:/path/to/file.csv")(Codec(StandardCharsets.UTF_8)).mkString
  private def readCsv(urlOrPath: String): Vector[Array[String]] =
    val codec = Codec(StandardCharsets.UTF_8)

    val raw =
      if urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://") then
        Source.fromURL(urlOrPath)(codec).mkString.trim
      else
        Source.fromFile(urlOrPath)(codec).mkString.trim

    val rows =
      if raw.contains("\n") || raw.contains("\r") then raw.split("\\r?\\n+").toVector
      else raw.split("\\s+").toVector

    rows.filter(_.nonEmpty).map(_.trim.split(",", -1).map(_.trim))

  // ----------------------------
  // Main
  // ----------------------------
  def main(args: Array[String]): Unit =
    val show = args.contains("--show")

    val csvUrlOrPath =
      val i = args.indexOf("--csv")
      if i >= 0 && i + 1 < args.length then args(i + 1)
      else "https://raw.githubusercontent.com/mohammadijoo/Datasets/refs/heads/main/iris.csv"

    val pi = Math.PI

    // 1) Multiple line plots on same axes
    {
      val x = linspace(0, 2 * pi, 120)
      val noise = randn(x.length, 0.0, 0.03)
      val y1 = x.indices.map(i => Math.sin(x(i)) + noise(i)).toArray
      val y2 = y1.map(v => -v)
      val y3 = x.map(v => v / pi - 1.0)

      val chart = new XYChartBuilder()
        .width(1200).height(800)
        .title("Multiple line plots")
        .xAxisTitle("x").yAxisTitle("y")
        .build()

      applyOneDecimalTicks(chart)
      chart.getStyler.setLegendPosition(Styler.LegendPosition.InsideNE)

      val s1 = chart.addSeries("sin(x) + noise", x, y1)
      s1.setLineStyle(SeriesLines.SOLID)
      s1.setMarker(SeriesMarkers.CIRCLE)

      val s2 = chart.addSeries("-sin(x) - noise", x, y2)
      s2.setLineStyle(SeriesLines.DASH_DASH)
      s2.setMarker(SeriesMarkers.CROSS)

      val s3 = chart.addSeries("x/pi - 1", x, y3)
      s3.setLineStyle(SeriesLines.DOT_DOT)
      s3.setMarker(SeriesMarkers.DIAMOND)

      val seqY = Array(1.0, 0.7, 0.4, 0.0, -0.4, -0.7, -1.0)
      val seqX = Array.tabulate(seqY.length)(i => i.toDouble)
      val s4 = chart.addSeries("sequence", seqX, seqY)
      s4.setLineStyle(SeriesLines.SOLID)
      s4.setMarker(SeriesMarkers.NONE)

      maybeShowSingle(chart, show, "line_1")
      savePng300(chart, "line_1")
    }

    // 2) Plot from set of vectors
    {
      val Y: Seq[Array[Double]] = Seq(
        Array(16, 5, 9, 4),
        Array(2, 11, 7, 14),
        Array(3, 10, 6, 15),
        Array(13, 8, 12, 1)
      ).map(_.map(_.toDouble))

      val chart = new XYChartBuilder()
        .width(1200).height(800)
        .title("Multiple line plots (set of vectors)")
        .xAxisTitle("x").yAxisTitle("y")
        .build()

      applyOneDecimalTicks(chart)
      chart.getStyler.setLegendPosition(Styler.LegendPosition.InsideNE)

      Y.zipWithIndex.foreach { (y, i) =>
        val x = Array.tabulate(y.length)(k => k.toDouble)
        val s = chart.addSeries(s"series ${i + 1}", x, y)
        s.setMarker(SeriesMarkers.CIRCLE)
      }

      maybeShowSingle(chart, show, "line_2")
      savePng300(chart, "line_2")
    }

    // 3) Sin function line plots
    {
      val x = linspace(0, 2 * pi, 200)
      val y1 = x.map(Math.sin)
      val y2 = x.map(v => Math.sin(v - 0.25))
      val y3 = x.map(v => Math.sin(v - 0.5))

      val chart = new XYChartBuilder()
        .width(1200).height(800)
        .title("Sin() function line plots")
        .xAxisTitle("x").yAxisTitle("y")
        .build()

      applyOneDecimalTicks(chart)
      chart.getStyler.setLegendPosition(Styler.LegendPosition.InsideNE)

      val a = chart.addSeries("sin(x)", x, y1)
      a.setLineStyle(SeriesLines.SOLID)
      a.setMarker(SeriesMarkers.NONE)

      val b = chart.addSeries("sin(x-0.25)", x, y2)
      b.setLineStyle(SeriesLines.DASH_DASH)
      b.setMarker(SeriesMarkers.NONE)

      val c = chart.addSeries("sin(x-0.5)", x, y3)
      c.setLineStyle(SeriesLines.DOT_DOT)
      c.setMarker(SeriesMarkers.NONE)

      maybeShowSingle(chart, show, "line_3")
      savePng300(chart, "line_3")
    }

    // 4) Sin function line plots with markers
    {
      val x = linspace(0, 2 * pi, 140)
      val jitter = randn(x.length, 0.0, 0.02)
      val y1 = x.indices.map(i => Math.sin(x(i)) + jitter(i)).toArray
      val y2 = x.map(v => Math.sin(v - 0.25))
      val y3 = x.map(v => Math.sin(v - 0.5))

      val chart = new XYChartBuilder()
        .width(1200).height(800)
        .title("Sin() function line plots with markers")
        .xAxisTitle("x").yAxisTitle("y")
        .build()

      applyOneDecimalTicks(chart)
      chart.getStyler.setLegendPosition(Styler.LegendPosition.InsideNE)

      val a = chart.addSeries("sin(x) + jitter", x, y1)
      a.setLineStyle(SeriesLines.SOLID)
      a.setMarker(SeriesMarkers.CIRCLE)

      val b = chart.addSeries("sin(x-0.25)", x, y2)
      b.setLineStyle(SeriesLines.DASH_DASH)
      b.setMarker(SeriesMarkers.DIAMOND)

      val c = chart.addSeries("sin(x-0.5)", x, y3)
      c.setLineStyle(SeriesLines.SOLID)
      c.setMarker(SeriesMarkers.SQUARE)

      maybeShowSingle(chart, show, "line_4")
      savePng300(chart, "line_4")
    }

    // 5) Simple tiledlayout example (2x1 matrix)
    {
      val x = linspace(0, 3, 200)
      val y1 = x.map(v => Math.sin(5 * v) + 0.02 * rng.nextGaussian())
      val y2 = x.map(v => Math.sin(15 * v) + 0.02 * rng.nextGaussian())

      val top = new XYChartBuilder()
        .width(800).height(450)
        .title("Top Plot")
        .xAxisTitle("x").yAxisTitle("sin(5x)")
        .build()
      applyOneDecimalTicks(top)
      val sTop = top.addSeries("sin(5x)", x, y1)
      sTop.setMarker(SeriesMarkers.NONE)

      val bottom = new XYChartBuilder()
        .width(800).height(450)
        .title("Bottom Plot")
        .xAxisTitle("x").yAxisTitle("sin(15x)")
        .build()
      applyOneDecimalTicks(bottom)
      val sBottom = bottom.addSeries("sin(15x)", x, y2)
      sBottom.setMarker(SeriesMarkers.NONE)

      val charts: Seq[AnyChart] = Seq(top, bottom)
      maybeShowMatrix(charts, 2, 1, "line_5 (2x1)", show)
      saveMatrixPng300(charts, 2, 1, "line_5")
    }

    // 6) 3x2 subplots (6 charts in a matrix)
    {
      // A) sin(x) with marker indices
      val xA = linspace(0, 10, 100)
      val yA = xA.map(Math.sin)
      val chartA =
        new XYChartBuilder().width(600).height(450).title("sin(x) with markers").xAxisTitle("x").yAxisTitle("y").build()
      applyOneDecimalTicks(chartA)

      val lineA = chartA.addSeries("sin(x)", xA, yA)
      lineA.setMarker(SeriesMarkers.NONE)

      val markerIdx = (0 until xA.length by 5).toArray
      val xMarks = markerIdx.map(xA)
      val yMarks = markerIdx.map(yA)
      val marks = chartA.addSeries("markers", xMarks, yMarks)
      marks.setXYSeriesRenderStyle(XYSeriesRenderStyle.Scatter)
      marks.setMarker(SeriesMarkers.CIRCLE)

      // B) nonlinear
      val xB = linspace(-pi, +pi, 40)
      val yB = xB.map(v => Math.tan(Math.sin(v)) - Math.sin(Math.tan(v)))
      val chartB =
        new XYChartBuilder().width(600).height(450).title("Nonlinear combo").xAxisTitle("x").yAxisTitle("y").build()
      applyOneDecimalTicks(chartB)
      val sB = chartB.addSeries("f(x)", xB, yB)
      sB.setLineStyle(SeriesLines.DASH_DASH)
      sB.setMarker(SeriesMarkers.DIAMOND)

      // C) cos(5x)
      val xC = linspace(0, 10, 150)
      val yC = xC.map(v => Math.cos(5 * v))
      val chartC =
        new XYChartBuilder().width(600).height(450).title("2-D Line Plot").xAxisTitle("x").yAxisTitle("cos(5x)").build()
      applyOneDecimalTicks(chartC)
      val sC = chartC.addSeries("cos(5x)", xC, yC)
      sC.setMarker(SeriesMarkers.NONE)

      // D) time plot via CategoryChart (string x labels)
      val tLabels = List("00:00", "00:30", "01:00", "01:30", "02:00", "02:30", "03:00")
      val yD = List(0.8, 0.9, 0.1, 0.9, 0.6, 0.1, 0.3)
      val chartD =
        new CategoryChartBuilder().width(600).height(450).title("Time Plot").xAxisTitle("Time").yAxisTitle("Value").build()
      applyOneDecimalTicks(chartD)
      chartD.getStyler.setDefaultSeriesRenderStyle(CategorySeriesRenderStyle.Line)
      chartD.getStyler.setYAxisMin(0.0)
      chartD.getStyler.setYAxisMax(1.0)
      chartD.addSeries("signal", tLabels.asJava, yD.map(Double.box).asJava)

      // E) sin(5x)
      val xE = linspace(0, 3, 200)
      val yE = xE.map(v => Math.sin(5 * v))
      val chartE = new XYChartBuilder().width(600).height(450).title("sin(5x)").xAxisTitle("x").yAxisTitle("y").build()
      applyOneDecimalTicks(chartE)
      val sE = chartE.addSeries("sin(5x)", xE, yE)
      sE.setMarker(SeriesMarkers.NONE)

      // F) circle
      val r = 2.0
      val xc = 4.0
      val yc = 3.0
      val theta = linspace(0, 2 * pi, 240)
      val xF = theta.map(t => r * Math.cos(t) + xc)
      val yF = theta.map(t => r * Math.sin(t) + yc)
      val chartF = new XYChartBuilder().width(600).height(450).title("Circle").xAxisTitle("x").yAxisTitle("y").build()
      applyOneDecimalTicks(chartF)
      chartF.getStyler.setXAxisMin((xc - r) - 0.5)
      chartF.getStyler.setXAxisMax((xc + r) + 0.5)
      chartF.getStyler.setYAxisMin((yc - r) - 0.5)
      chartF.getStyler.setYAxisMax((yc + r) + 0.5)
      val sF = chartF.addSeries("circle", xF, yF)
      sF.setMarker(SeriesMarkers.NONE)

      val charts: Seq[AnyChart] = Seq(chartA, chartB, chartC, chartD, chartE, chartF)
      maybeShowMatrix(charts, 3, 2, "line_6 (3x2)", show)
      saveMatrixPng300(charts, 3, 2, "line_6")
    }

    // 7) Real-world CSV plot (Iris): petal_length vs petal_width grouped by classification
    {
      val rows = readCsv(csvUrlOrPath)
      val header = rows.head

      val ixPetalLen = header.indexOf("petal_length")
      val ixPetalWid = header.indexOf("petal_width")
      val ixClass = header.indexOf("classification")

      require(ixPetalLen >= 0 && ixPetalWid >= 0 && ixClass >= 0, "Required columns not found in CSV header.")

      val dataRows = rows.tail.filter(r => r.length > Math.max(ixClass, Math.max(ixPetalLen, ixPetalWid)))

      val grouped =
        dataRows.foldLeft(Map.empty[String, Vector[(Double, Double)]]) { (acc, r) =>
          val cls = r(ixClass)
          val x = r(ixPetalLen).toDouble
          val y = r(ixPetalWid).toDouble
          acc.updated(cls, acc.getOrElse(cls, Vector.empty) :+ (x, y))
        }

      val chart = new XYChartBuilder()
        .width(1200).height(800)
        .title("Iris CSV: petal_length vs petal_width")
        .xAxisTitle("petal_length").yAxisTitle("petal_width")
        .build()

      applyOneDecimalTicks(chart)
      chart.getStyler.setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Scatter)
      chart.getStyler.setLegendPosition(Styler.LegendPosition.InsideNE)

      grouped.toSeq.sortBy(_._1).foreach { (cls, pts) =>
        val xs = pts.map(_._1).toArray
        val ys = pts.map(_._2).toArray
        val s = chart.addSeries(cls, xs, ys)
        s.setMarker(SeriesMarkers.CIRCLE)
      }

      maybeShowSingle(chart, show, "line_7_csv")
      savePng300(chart, "line_7_csv")
    }

