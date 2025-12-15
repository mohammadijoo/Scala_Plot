//> using scala "3.3.3"
//> using dep "org.knowm.xchart:xchart:3.8.8"

import org.knowm.xchart.*
import org.knowm.xchart.BitmapEncoder.BitmapFormat
import org.knowm.xchart.internal.chartpart.Chart
import org.knowm.xchart.style.Styler
import org.knowm.xchart.CategorySeries.CategorySeriesRenderStyle

import scala.jdk.CollectionConverters.*
import scala.util.{Random, Try}
import scala.io.{Source, Codec}
import java.nio.charset.StandardCharsets
import javax.swing.JFrame

import java.awt.Graphics2D
import java.awt.image.BufferedImage
import javax.imageio.{ImageIO, IIOImage}
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.FileImageOutputStream

object Histogram:

  private type AnyChart = Chart[?, ?]

  private val rng = new Random(7)

  // Export settings
  private val ExportDpi: Int = 300
  private val TickPattern: String = "#0.0"

  // Force English digits/decimal point for any string labels we create
  private val LocaleUS: java.util.Locale = java.util.Locale.US

  // ----------------------------
  // Main
  // ----------------------------
  def main(args: Array[String]): Unit =
    val show = args.contains("--show")

    val csvUrlOrPath =
      val i = args.indexOf("--csv")
      if i >= 0 && i + 1 < args.length then args(i + 1)
      else "https://raw.githubusercontent.com/mohammadijoo/Datasets/refs/heads/main/diabetes.csv"

    // 1) Simple histogram of standard normal data
    {
      val x1 = randn(n = 10000, mean = 0.0, std = 1.0)
      val bins = sturgesBins(x1.length)

      val chart = histogramBarChart(
        width = 1200,
        height = 800,
        title = "Histogram of standard normal data",
        xLabel = "Value",
        yLabel = "Frequency",
        data = x1,
        numBins = bins,
        normalization = None
      )

      println(s"Histogram with $bins bins")

      maybeShowSingle(chart, show, "histogram_1")
      savePng300(chart, "histogram_1")
    }

    // 2) Compare different binning rules (2x3 matrix)
    {
      val x2 = randn(n = 10000, mean = 0.0, std = 1.0)

      val configs = Seq(
        ("Automatic binning", automaticBins(x2)),
        ("Scott's rule", scottBins(x2)),
        ("Freedman-Diaconis rule", freedmanDiaconisBins(x2)),
        ("Integers rule", integersBins(x2)),
        ("Sturges' rule", sturgesBins(x2.length)),
        ("Square root rule", sqrtBins(x2.length))
      )

      val charts = configs.map { (ttl, b) =>
        histogramBarChart(
          width = 700,
          height = 450,
          title = ttl,
          xLabel = "Value",
          yLabel = "Frequency",
          data = x2,
          numBins = b,
          normalization = None
        )
      }

      maybeShowMatrix(charts, rows = 2, cols = 3, title = "Histogram binning rules", show = show)
      saveMatrixPng300(charts, rows = 2, cols = 3, baseNameNoExt = "histogram_2_matrix_2x3")
    }

    // 3) Change number of bins (two outputs)
    {
      val x3 = randn(n = 1000, mean = 0.0, std = 1.0)

      val b1 = automaticBins(x3)
      val c1 = histogramBarChart(
        width = 1200,
        height = 800,
        title = s"$b1 bins",
        xLabel = "Value",
        yLabel = "Frequency",
        data = x3,
        numBins = b1,
        normalization = None
      )
      maybeShowSingle(c1, show, "histogram_3_initial")
      savePng300(c1, "histogram_3_initial")

      val b2 = 50
      val c2 = histogramBarChart(
        width = 1200,
        height = 800,
        title = s"$b2 bins",
        xLabel = "Value",
        yLabel = "Frequency",
        data = x3,
        numBins = b2,
        normalization = None
      )
      maybeShowSingle(c2, show, "histogram_3_50bins")
      savePng300(c2, "histogram_3_50bins")
    }

    // 4) Custom bin edges + count-density normalization
    {
      val x4 = randn(n = 10000, mean = 0.0, std = 1.0)

      val edges = Array(
        -10.0, -2.0, -1.75, -1.5, -1.25,
        -1.0, -0.75, -0.5, -0.25, 0.0,
        0.25, 0.5, 0.75, 1.0, 1.25,
        1.5, 1.75, 2.0, 10.0
      )

      val (centers, density) = histogramWithEdges(x4, edges, normalization = "count_density")
      val labels = centers.map(format1).toList.asJava

      val chart = categoryChart(1200, 800, "Histogram with custom bin edges", "Bin center", "Count density")
      chart.getStyler.setLegendVisible(false)
      chart.getStyler.setOverlapped(false)
      chart.getStyler.setAvailableSpaceFill(0.85)

      chart.addSeries("count density", labels, density.map(java.lang.Double.valueOf).toList.asJava)

      maybeShowSingle(chart, show, "histogram_4")
      savePng300(chart, "histogram_4")
    }

    // 5) Categorical histogram (bar chart)
    {
      val categories = Seq(
        "no", "no",  "yes",       "yes",       "yes", "no",  "no",
        "no", "no",  "undecided", "undecided", "yes", "no",  "no",
        "no", "yes", "no",        "yes",       "no",  "yes", "no",
        "no", "no",  "yes",       "yes",       "yes", "yes"
      )

      val counts = categories.groupBy(identity).view.mapValues(_.size).toMap
      val keys = counts.keys.toSeq.sorted
      val vals = keys.map(k => counts(k).toDouble)

      val chart = categoryChart(1200, 800, "Histogram of categorical responses", "Category", "Count")
      chart.getStyler.setLegendVisible(false)
      chart.getStyler.setAvailableSpaceFill(0.5)

      chart.addSeries("count", keys.toList.asJava, vals.map(java.lang.Double.valueOf).toList.asJava)

      maybeShowSingle(chart, show, "histogram_5")
      savePng300(chart, "histogram_5")
    }

    // 6) Overlay normalized histograms (probability)
    {
      val x5 = randn(n = 2000, mean = 0.0, std = 1.0)
      val y5 = randn(n = 5000, mean = 1.0, std = 1.0)

      val binWidth = 0.25

      val (centers, p1) = histogramFixedWidth(x5, binWidth, normalization = "probability")
      val (_, p2) = histogramFixedWidth(y5, binWidth, normalization = "probability", fixedCenters = centers)

      val labels = centers.map(format1).toList.asJava

      val chart = categoryChart(1200, 800, "Overlaid normalized histograms", "Bin center", "Probability")
      chart.getStyler.setOverlapped(true)
      chart.getStyler.setLegendPosition(Styler.LegendPosition.InsideNE)
      chart.getStyler.setAvailableSpaceFill(0.85)

      chart.addSeries("N(0,1)", labels, p1.map(java.lang.Double.valueOf).toList.asJava)
      chart.addSeries("N(1,1)", labels, p2.map(java.lang.Double.valueOf).toList.asJava)

      maybeShowSingle(chart, show, "histogram_6")
      savePng300(chart, "histogram_6")
    }

    // 7) PDF-normalized histogram + theoretical normal PDF
    {
      val x6 = randn(n = 5000, mean = 5.0, std = 2.0)
      val binWidth = 0.25

      val (centers, pdfHist) = histogramFixedWidth(x6, binWidth, normalization = "pdf")
      val mu = 5.0
      val sigma = 2.0
      val pdfCurve = centers.map(t => normalPdf(t, mu, sigma))

      val labels = centers.map(format1).toList.asJava

      val chart = categoryChart(1200, 800, "Histogram with theoretical normal PDF", "Bin center", "Probability density")
      chart.getStyler.setLegendPosition(Styler.LegendPosition.InsideNE)
      chart.getStyler.setOverlapped(false)
      chart.getStyler.setAvailableSpaceFill(0.85)

      val histSeries = chart.addSeries("Histogram (pdf)", labels, pdfHist.map(java.lang.Double.valueOf).toList.asJava)
      histSeries.setChartCategorySeriesRenderStyle(CategorySeriesRenderStyle.Bar)

      val pdfSeries = chart.addSeries("Normal PDF", labels, pdfCurve.map(java.lang.Double.valueOf).toList.asJava)
      pdfSeries.setChartCategorySeriesRenderStyle(CategorySeriesRenderStyle.Line)

      maybeShowSingle(chart, show, "histogram_7")
      savePng300(chart, "histogram_7")
    }

    // 8) Real-world CSV histogram (default: diabetes.csv, column: Glucose)
    {
      val rows = readCsv(csvUrlOrPath)
      val header = rows.headOption.getOrElse(sys.error("CSV is empty"))
      val ix = header.indexOf("Glucose")
      require(ix >= 0, "Column 'Glucose' not found in CSV header.")

      val values =
        rows.tail
          .flatMap(r => if r.length > ix then toDoubleOpt(r(ix)) else None)
          .toArray

      val bins = automaticBins(values)

      val chart = histogramBarChart(
        width = 1200,
        height = 800,
        title = "CSV: Glucose distribution",
        xLabel = "Glucose",
        yLabel = "Frequency",
        data = values,
        numBins = bins,
        normalization = None
      )

      maybeShowSingle(chart, show, "histogram_8_csv")
      savePng300(chart, "histogram_8_csv")
    }

  // ----------------------------
  // Chart builders
  // ----------------------------
  private def categoryChart(width: Int, height: Int, title: String, xLabel: String, yLabel: String): CategoryChart =
    val chart =
      new CategoryChartBuilder()
        .width(width)
        .height(height)
        .title(title)
        .xAxisTitle(xLabel)
        .yAxisTitle(yLabel)
        .build()

    chart.getStyler.setAntiAlias(true)
    chart.getStyler.setYAxisDecimalPattern(TickPattern)
    chart

  private def histogramBarChart(
    width: Int,
    height: Int,
    title: String,
    xLabel: String,
    yLabel: String,
    data: Array[Double],
    numBins: Int,
    normalization: Option[String]
  ): CategoryChart =
    val bins = math.max(2, numBins)

    val h = new org.knowm.xchart.Histogram(data.map(java.lang.Double.valueOf).toList.asJava, bins)
    val centers = h.getxAxisData.asScala.map(_.doubleValue()).toArray
    val counts = h.getyAxisData.asScala.map(_.doubleValue()).toArray

    val y: Array[Double] =
      normalization match
        case None => counts
        case Some("probability") =>
          val n = data.length.toDouble
          counts.map(_ / n)
        case Some("pdf") =>
          val n = data.length.toDouble
          val bw = if centers.length >= 2 then centers(1) - centers(0) else 1.0
          counts.map(c => c / (n * bw))
        case _ => counts

    val labels = centers.map(format1).toList.asJava

    val chart = categoryChart(width, height, title, xLabel, yLabel)
    chart.getStyler.setLegendVisible(false)
    chart.getStyler.setOverlapped(false)
    chart.getStyler.setAvailableSpaceFill(0.85)
    chart.addSeries("hist", labels, y.map(java.lang.Double.valueOf).toList.asJava)

    chart

  // ----------------------------
  // Display helpers
  // ----------------------------
  private def maybeShowSingle(chart: AnyChart, show: Boolean, windowTitle: String): Unit =
    if !show then return
    val frame: JFrame = new SwingWrapper[AnyChart](chart).displayChart()
    frame.setTitle(windowTitle)

  private def maybeShowMatrix(charts: Seq[AnyChart], rows: Int, cols: Int, title: String, show: Boolean): Unit =
    if !show then return
    val jl: java.util.List[AnyChart] = charts.map(_.asInstanceOf[AnyChart]).asJava
    val frame: JFrame = new SwingWrapper[AnyChart](jl, rows, cols).displayChartMatrix()
    frame.setTitle(title)

  // ----------------------------
  // High-quality exports (300 DPI)
  // ----------------------------
  private def savePng300(chart: AnyChart, baseNameNoExt: String): Unit =
    BitmapEncoder.saveBitmapWithDPI(chart, baseNameNoExt, BitmapFormat.PNG, ExportDpi)

  private def writePngWithDpi(image: BufferedImage, filePath: String, dpi: Int): Unit =
    val writer = ImageIO.getImageWritersByFormatName("png").next()
    val ios = new FileImageOutputStream(new java.io.File(filePath))
    try
      writer.setOutput(ios)
      val param = writer.getDefaultWriteParam
      val typeSpecifier = javax.imageio.ImageTypeSpecifier.createFromBufferedImageType(image.getType)
      val metadata = writer.getDefaultImageMetadata(typeSpecifier, param)

      val ppm = Math.round(dpi * 39.3700787).toInt
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

  private def saveMatrixPng300(charts: Seq[AnyChart], rows: Int, cols: Int, baseNameNoExt: String): Unit =
    require(charts.nonEmpty, "charts is empty")
    require(rows * cols >= charts.size, "rows*cols must be >= number of charts")

    val w = charts.head.getWidth
    val h = charts.head.getHeight
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
  // Histogram math helpers
  // ----------------------------
  private def randn(n: Int, mean: Double, std: Double): Array[Double] =
    Array.fill(n)(mean + std * rng.nextGaussian())

  // 1-digit formatting with forced English digits
  private def format1(x: Double): String =
    val v = if math.abs(x) < 0.0000001 then 0.0 else x
    String.format(LocaleUS, "%.1f", java.lang.Double.valueOf(v))

  private def automaticBins(data: Array[Double]): Int = sturgesBins(data.length)

  private def sqrtBins(n: Int): Int =
    math.max(2, math.ceil(math.sqrt(n.toDouble)).toInt)

  private def sturgesBins(n: Int): Int =
    math.max(2, math.ceil(1.0 + (math.log(n.toDouble) / math.log(2.0))).toInt)

  private def scottBins(data: Array[Double]): Int =
    val n = data.length.toDouble
    val s = stddev(data)
    val w = 3.5 * s / math.cbrt(n)
    binsFromWidth(data, w)

  private def freedmanDiaconisBins(data: Array[Double]): Int =
    val n = data.length.toDouble
    val iqr = percentile(data, 0.75) - percentile(data, 0.25)
    val w = 2.0 * iqr / math.cbrt(n)
    binsFromWidth(data, w)

  private def integersBins(data: Array[Double]): Int =
    val mn = data.min
    val mx = data.max
    val span = math.ceil(mx) - math.floor(mn)
    math.max(2, span.toInt)

  private def binsFromWidth(data: Array[Double], width: Double): Int =
    val w = if width.isFinite && width > 0 then width else 1.0
    val mn = data.min
    val mx = data.max
    math.max(2, math.ceil((mx - mn) / w).toInt)

  private def stddev(x: Array[Double]): Double =
    val m = x.sum / x.length.toDouble
    val v = x.map(a => (a - m) * (a - m)).sum / (x.length.toDouble - 1.0)
    math.sqrt(v)

  private def percentile(x: Array[Double], p: Double): Double =
    val s = x.sorted
    val idx = (p * (s.length - 1)).toInt
    s(math.max(0, math.min(s.length - 1, idx)))

  private def histogramWithEdges(data: Array[Double], edges: Array[Double], normalization: String): (Array[Double], Array[Double]) =
    val e = edges.sorted
    val counts = Array.fill(e.length - 1)(0.0)

    data.foreach { v =>
      val i = findBin(e, v)
      if i >= 0 && i < counts.length then counts(i) += 1.0
    }

    val centers = counts.indices.map(i => 0.5 * (e(i) + e(i + 1))).toArray

    val y =
      normalization match
        case "count_density" =>
          counts.indices.map { i =>
            val w = e(i + 1) - e(i)
            counts(i) / w
          }.toArray
        case _ => counts

    (centers, y)

  private def histogramFixedWidth(
    data: Array[Double],
    binWidth: Double,
    normalization: String,
    fixedCenters: Array[Double] = Array.emptyDoubleArray
  ): (Array[Double], Array[Double]) =
    val centers: Array[Double] =
      if fixedCenters.nonEmpty then fixedCenters
      else
        val mn = data.min
        val mx = data.max
        val start = math.floor(mn / binWidth) * binWidth + 0.5 * binWidth
        val end = math.ceil(mx / binWidth) * binWidth - 0.5 * binWidth
        val nBins = math.max(2, math.round((end - start) / binWidth).toInt + 1)
        Array.tabulate(nBins)(i => start + i * binWidth)

    val edges = centers.map(c => c - 0.5 * binWidth) :+ (centers.last + 0.5 * binWidth)
    val counts = Array.fill(centers.length)(0.0)

    data.foreach { v =>
      val i = findBin(edges, v)
      if i >= 0 && i < counts.length then counts(i) += 1.0
    }

    val y =
      normalization match
        case "probability" =>
          val n = data.length.toDouble
          counts.map(_ / n)
        case "pdf" =>
          val n = data.length.toDouble
          counts.map(c => c / (n * binWidth))
        case _ => counts

    (centers, y)

  private def findBin(edges: Array[Double], v: Double): Int =
    var lo = 0
    var hi = edges.length - 2
    while lo <= hi do
      val mid = (lo + hi) >>> 1
      if v < edges(mid) then hi = mid - 1
      else if v >= edges(mid + 1) then lo = mid + 1
      else return mid
    -1

  private def normalPdf(x: Double, mu: Double, sigma: Double): Double =
    val z = (x - mu) / sigma
    (1.0 / (sigma * math.sqrt(2.0 * math.Pi))) * math.exp(-0.5 * z * z)

  // ----------------------------
  // CSV reader
  // ----------------------------
  // Local file example:
  // val raw = Source.fromFile("C:/path/to/diabetes.csv")(Codec(StandardCharsets.UTF_8)).mkString
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

    rows
      .filter(_.nonEmpty)
      .map(_.trim.split(",", -1).map(_.trim))
      .map(r => if r.nonEmpty then r.updated(0, stripBom(r(0))) else r)

  private def stripBom(s: String): String =
    if s.nonEmpty && s.charAt(0) == '\uFEFF' then s.substring(1) else s

  private def toDoubleOpt(s: String): Option[Double] =
    val t = s.trim
    if t.isEmpty then None
    else
      val cleaned = t.replace(",", ".")
      Try(cleaned.toDouble).toOption
