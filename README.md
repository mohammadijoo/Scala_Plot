<div style="font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; line-height: 1.6;">

  <h1 style="margin-bottom: 0.2em; text-align: center !important;">Scala Plotting Playground</h1>

  <p style="font-size: 0.95rem; max-width: 820px; margin: 0 auto;">
    A minimal, extensible <strong>Scala 3</strong> project showcasing plotting with
    <strong>XChart</strong>, starting with <strong>line plots</strong> and <strong>histograms</strong>.
    The examples are implemented in <code>src/main/scala/line.scala</code> and
    <code>src/main/scala/histogram.scala</code>.
    The repository is intentionally structured so you can add new plot modules (pie, scatter, bar, etc.) by
    adding a new Scala file and a new README section.
  </p>

  <p style="font-size: 1rem; color: #666; margin-top: 0.5em; text-align: center !important;">
    Built with Scala 3, sbt / scala-cli, and XChart (Swing backend).
  </p>

</div>

<hr />

<!-- ========================================================= -->
<!-- Table of Contents                                        -->
<!-- ========================================================= -->

<ul style="list-style: none; padding-left: 0; font-size: 0.95rem;">
  <li><a href="#about-this-repository">About this repository</a></li>
  <li><a href="#scala-and-dependency-management-quick-primer">Scala and dependency management (quick primer)</a></li>
  <li><a href="#xchart-library">XChart library</a></li>
  <li><a href="#prerequisites">Prerequisites</a></li>
  <li><a href="#installing-scala-3-sbt-and-scala-cli">Installing Scala 3, sbt, and scala-cli</a></li>
  <li><a href="#running-the-examples-quick-start-scala-cli">Running the examples (Quick Start: scala-cli)</a></li>
  <li><a href="#option-a-sbt-workflow-recommended-for-repos">Option A: sbt workflow (recommended for repos)</a></li>
  <li><a href="#option-b-compile--run-with-scalacscala-manual-classpath">Option B: Compile + run with scalac/scala (manual classpath)</a></li>
  <li><a href="#running-in-vs-code">Running in VS Code</a></li>
  <li><a href="#running-without-vs-code-cmd-powershell-git-bash">Running without VS Code (cmd / PowerShell / Git Bash)</a></li>
  <li><a href="#repository-structure-and-key-files">Repository structure and key files</a></li>
  <li><a href="#common-cli-options-used-in-this-repo">Common CLI options used in this repo</a></li>
  <li><a href="#line-plots-module-linescala">Line plots module (<code>line.scala</code>)</a></li>
  <li><a href="#histogram-plots-module-histogramscala">Histogram plots module (<code>histogram.scala</code>)</a></li>
  <li><a href="#troubleshooting">Troubleshooting</a></li>
  <li><a href="#implementation-tutorial-video">Implementation tutorial video</a></li>
</ul>

---

## About this repository

This repository demonstrates how to generate publication-quality plots in <strong>Scala 3</strong> using
<a href="https://knowm.org/open-source/xchart/" target="_blank">XChart</a> (a small Java charting library).

Current examples:

- <code>src/main/scala/line.scala</code> — line plots (multi-series, markers, tiled layouts, CSV-based scatter, mixed chart types).
- <code>src/main/scala/histogram.scala</code> — histograms (binning rules, normalization modes, overlays, categorical histograms, CSV-based histograms).

Outputs:

- High-resolution PNG images are written into your working directory (e.g., <code>line_1.png</code>, <code>histogram_6.png</code>).
- Optional interactive windows can be displayed with Swing by adding the <code>--show</code> argument.

---

## Scala and dependency management (quick primer)

### Installing Scala vs. running Scala code

There are three common ways to compile/run Scala code (this repo supports all three):

1) <strong>scala-cli</strong>  
   Best for single-file scripts and quick runs. Dependencies can be declared at the top of a file using <code>//&gt; using ...</code> directives.

2) <strong>sbt</strong> (Scala Build Tool)  
   The most common repo workflow. Dependencies and Scala version are declared in <code>build.sbt</code>, and sources live under <code>src/main/scala</code>.

3) <strong>scalac/scala</strong> directly  
   A manual approach where you manage the classpath yourself (works, but more tedious).

### Importing packages in Scala

Scala imports work similarly to Java:

- Import a package (or multiple definitions): <code>import org.knowm.xchart.*</code>
- Import specific members: <code>import scala.util.Random</code>
- For Java interop conversions (used heavily in XChart): <code>import scala.jdk.CollectionConverters.*</code>

This repo uses XChart (a Java library), so you will see conversions like:

- <code>List(...).asJava</code>
- <code>java.lang.Double.valueOf(x)</code>

---

## XChart library

### What is XChart?

XChart is a lightweight Java charting library that can render charts using Swing and export to images.
In this repository it is used for:

- <strong>XYChart</strong> (continuous x/y data: line plots, scatter plots)
- <strong>CategoryChart</strong> (categorical x-axis labels: histograms, bar charts, time-category plots)

### Dependency

This repository targets:

- Scala: <code>3.3.3</code>
- XChart: <code>3.8.8</code>

You can use XChart in two ways here:

- <strong>sbt</strong> via <code>libraryDependencies</code> in <code>build.sbt</code>
- <strong>scala-cli</strong> via file directives in each <code>.scala</code> file

---

## Prerequisites

- <strong>Java (JDK)</strong>: a modern JDK is recommended (e.g., Java 17+).
- <strong>Scala</strong>: Scala 3 (the repo is configured for <code>3.3.3</code>).
- One of:
  - <strong>scala-cli</strong> (fastest to get started)
  - <strong>sbt</strong> (standard repo workflow)
  - <strong>scalac/scala</strong> (manual classpath)

Optional (recommended for development):

- <strong>VS Code</strong> with the <strong>Scala (Metals)</strong> extension.

---

## Installing Scala 3, sbt, and scala-cli

Below are practical install paths. Use whichever matches your environment.

### 1) Install Scala 3

Common approaches:

- Install via an installer/package manager for your OS, or
- Use a version manager (recommended if you switch versions often)

After installation, verify:

```bash
scala -version
scalac -version
```

### 2) Install sbt

After installation, verify:

```bash
sbt --version
```

This repository pins an sbt version using <code>project/build.properties</code> (see the file explanation below).

### 3) Install scala-cli

After installation, verify:

```bash
scala-cli version
```

---

## Running the examples (Quick Start: scala-cli)

This is the simplest workflow and matches the file-level dependency directives at the top of each file:

- <code>//&gt; using scala "3.3.3"</code>
- <code>//&gt; using dep "org.knowm.xchart:xchart:3.8.8"</code>

From the repository root:

```bash
# Run line plots (headless image export only)
scala-cli run src/main/scala/line.scala

# Run line plots and show Swing windows
scala-cli run src/main/scala/line.scala -- --show

# Run histograms (headless image export only)
scala-cli run src/main/scala/histogram.scala

# Run histograms and show Swing windows
scala-cli run src/main/scala/histogram.scala -- --show
```

You can also run the short form (if your working directory is the file’s directory):

```bash
scala-cli run line.scala -- --show
scala-cli run histogram.scala -- --show
```

---

## Option A: sbt workflow (recommended for repos)

sbt is the standard “compile + run” workflow for Scala repositories.

### Folder structure (already used here)

```text
Scala_Plot/
  build.sbt
  project/
    build.properties
  src/main/scala/
    line.scala
    histogram.scala
```

### build.sbt

This repository uses:

- Scala version: <code>3.3.3</code>
- XChart dependency: <code>3.8.8</code>

Example (matches this repo):

```scala
ThisBuild / scalaVersion := "3.3.3"

libraryDependencies += "org.knowm.xchart" % "xchart" % "3.8.8"
```

### Run with sbt

From the project root:

```bash
# Compile and run (sbt may ask you to pick which main to run)
sbt run
```

To explicitly run a specific module (recommended):

```bash
sbt "runMain Line --show"
sbt "runMain Histogram --show"
```

Notes:

- The main entry points are <code>object Line</code> and <code>object Histogram</code>.
- Both accept optional arguments (e.g., <code>--show</code>, <code>--csv ...</code>).

---

## Option B: Compile + run with scalac/scala (manual classpath)

This is the manual alternative to sbt. You must put the XChart JAR on your classpath.

### 1) Obtain the XChart JAR

Download or locate:

- <code>xchart-3.8.8.jar</code>

(If you use sbt once, you can often find cached JARs under your dependency cache; otherwise download from a Maven repository.)

### 2) Compile

Example (adjust paths to your system):

```bash
scalac -classpath "path/to/xchart-3.8.8.jar" src/main/scala/histogram.scala
```

### 3) Run

Example (classpath separator differs by OS)

- Windows typically uses <code>;</code>
- macOS/Linux typically uses <code>:</code>

```bash
# Windows example:
scala -classpath ".;path/to/xchart-3.8.8.jar" Histogram --show

# macOS/Linux example:
scala -classpath ".:path/to/xchart-3.8.8.jar" Histogram --show
```

Most users prefer sbt or scala-cli to avoid classpath management.

---

## Running in VS Code

Two good options are supported.

### Option A: VS Code + scala-cli (simplest)

1) Install the <strong>Scala (Metals)</strong> extension in VS Code.
2) Open the repository folder in VS Code.
3) In the integrated terminal:

```bash
scala-cli run src/main/scala/histogram.scala -- --show
```

#### Optional: VS Code Task (tasks.json)

You can create a VS Code task so you run a module from the Command Palette:

<strong>Terminal → Configure Tasks → Create tasks.json</strong>

Example task:

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Run histogram (scala-cli)",
      "type": "shell",
      "command": "scala-cli run src/main/scala/histogram.scala -- --show",
      "group": "build",
      "problemMatcher": []
    }
  ]
}
```

### Option B: VS Code + sbt project (best long-term for a repo)

1) Install the <strong>Scala (Metals)</strong> extension.
2) Open the repository root folder (the folder containing <code>build.sbt</code>).
3) Metals will detect <code>sbt</code> and import the build.
4) Run from terminal:

```bash
sbt "runMain Histogram --show"
```

You can also use Metals run/debug code lenses above <code>main</code> entry points when available.

---

## Running without VS Code (cmd / PowerShell / Git Bash)

This section focuses on CLI usage only. You can use <strong>cmd</strong>, <strong>PowerShell</strong>, or <strong>Git Bash</strong>—the key requirement is that your Scala tooling is on <code>PATH</code>.

### Approach 1: scala-cli (recommended for quick runs)

From repo root:

```bash
scala-cli run src/main/scala/line.scala -- --show
scala-cli run src/main/scala/histogram.scala -- --show
```

### Approach 2: sbt

From repo root:

```bash
sbt "runMain Line --show"
sbt "runMain Histogram --show"
```

### Approach 3: scalac/scala (manual classpath)

See <a href="#option-b-compile--run-with-scalacscala-manual-classpath">Option B</a> above.

---

## Repository structure and key files

This repo uses a standard sbt layout, while still supporting scala-cli “single-file” execution.

### <code>build.sbt</code>

What it is:
- The sbt build definition for the project.

Why it is necessary:
- Declares which Scala version to compile against.
- Declares dependencies (XChart) so sbt can download and add them to the classpath automatically.

How it can be created:
- Manually (recommended; it is a simple text file).
- Or via shell scripting / PowerShell (useful when scaffolding a repo).

Configured in this repo:

- Scala: <code>3.3.3</code>
- Dependency: <code>org.knowm.xchart:xchart:3.8.8</code>

### <code>project/build.properties</code>

What it is:
- A configuration file used by sbt to pin the sbt launcher version for this project.

Why it is necessary:
- Keeps sbt behavior consistent across machines and CI.
- Ensures the sbt version used matches the repo’s expectations.

How it can be created:
- Manually by creating the <code>project</code> directory and adding <code>build.properties</code>.
- Or via scripting (PowerShell, Bash, etc.).

Configured in this repo:

- <code>sbt.version=1.11.7</code>

### <code>src/main/scala/line.scala</code>

What it is:
- A Scala 3 module that defines <code>object Line</code> with a <code>main</code> entry point.

Why it is necessary:
- Implements line-plot examples (XY charts, category/time plot, subplot matrix rendering, CSV-based scatter).
- Demonstrates exporting charts as high-quality 300 DPI PNGs.

How it can be created:
- Manually (recommended).
- Or generated as a template file when scaffolding future plot modules.

Also note:
- The file includes scala-cli directives (<code>//&gt; using ...</code>) so it can run without sbt.

### <code>src/main/scala/histogram.scala</code>

What it is:
- A Scala 3 module that defines <code>object Histogram</code> with a <code>main</code> entry point.

Why it is necessary:
- Implements histogram examples (binning rules, normalization, overlays, categorical histograms, CSV histogram).
- Includes helper functions for bin selection and normalization logic.

How it can be created:
- Manually.
- Or generated as part of a standard module template.

Also note:
- The file includes scala-cli directives for dependency resolution without sbt.

---

## Common CLI options used in this repo

Both modules accept optional arguments:

- <code>--show</code>  
  Displays Swing windows via XChart’s <code>SwingWrapper</code>.  
  If omitted, the code still exports PNG images.

- <code>--csv &lt;path-or-url&gt;</code>  
  Overrides the default CSV source used by the “real-world” examples.

Examples:

```bash
# Show windows and load a local CSV
scala-cli run src/main/scala/line.scala -- --show --csv "C:/data/iris.csv"

# Run headless export and load a remote CSV
scala-cli run src/main/scala/histogram.scala -- --csv "https://example.com/data.csv"
```

---

<section id="line-plots-module-linescala">

## Line plots module (<code>line.scala</code>)

This section documents the <strong>line plot</strong> examples in <code>src/main/scala/line.scala</code>.  
It is designed to be <strong>modular</strong>, so you can later add new sections (pie plots, scatter, etc.) without restructuring the README.

### What this module uses (imports and why)

Core XChart imports:

- <code>org.knowm.xchart.*</code>  
  Brings in chart builders (<code>XYChartBuilder</code>, <code>CategoryChartBuilder</code>), chart types (<code>XYChart</code>, <code>CategoryChart</code>), and <code>SwingWrapper</code>.

- <code>org.knowm.xchart.BitmapEncoder.BitmapFormat</code>  
  Used for exporting images as PNG.

- <code>org.knowm.xchart.XYSeries.XYSeriesRenderStyle</code> and
  <code>org.knowm.xchart.CategorySeries.CategorySeriesRenderStyle</code>  
  Used to switch between render styles (line, scatter, bar) depending on the chart.

- <code>org.knowm.xchart.style.Styler</code>,
  <code>org.knowm.xchart.style.lines.SeriesLines</code>,
  <code>org.knowm.xchart.style.markers.SeriesMarkers</code>  
  Used to configure legend placement and line/marker styles.

Scala/Java interop & utilities:

- <code>scala.jdk.CollectionConverters.*</code>  
  Converts Scala collections to Java lists required by XChart.

- <code>scala.util.Random</code>  
  Deterministic random generation (seeded) for repeatable plots.

- <code>scala.io.Source</code> and <code>java.nio.charset.StandardCharsets</code>  
  Load CSV data from a local file or an HTTP(S) URL.

Swing + image export utilities:

- <code>javax.swing.JFrame</code>  
  Used to set titles on display windows.

- <code>java.awt.image.BufferedImage</code>, <code>javax.imageio.ImageIO</code>, and related classes  
  Used for exporting a multi-chart matrix as a single high-resolution PNG with DPI metadata.

### Internal structure (key methods)

The module is structured as:

- <strong>Data utilities</strong>
  - <code>linspace(start, end, n)</code>: evenly spaced grid for x-values.
  - <code>randn(n, mu, sigma)</code>: Gaussian noise generation.

- <strong>Styling utilities</strong>
  - <code>applyOneDecimalTicks(...)</code>: forces 1-decimal tick labels (via XChart styler patterns).
  - Legend position and marker/line styles are configured per-series.

- <strong>Display helpers</strong>
  - <code>maybeShowSingle(chart, show, windowTitle)</code>: shows a Swing window only if <code>--show</code> is present.
  - <code>maybeShowMatrix(charts, rows, cols, title, show)</code>: displays a grid of charts in a Swing matrix.

- <strong>High-quality exports</strong>
  - <code>savePng300(chart, baseNameNoExt)</code>: exports PNG with 300 DPI via XChart.
  - <code>saveMatrixPng300(...)</code>: renders multiple charts into a single high-res image so fonts/lines scale consistently.

- <strong>CSV reader</strong>
  - <code>readCsv(urlOrPath)</code>: reads a CSV from local disk or a URL and returns tokenized rows.

### What the <code>main</code> function generates (example set)

When you run the module, it generates multiple outputs:

1) <strong>Multiple lines on the same axes</strong>  
   Demonstrates multiple series with different line styles and markers.  
   Output: <code>line_1.png</code>

2) <strong>Plot from a set of vectors</strong>  
   Builds multiple series from a sequence of arrays.  
   Output: <code>line_2.png</code>

3) <strong>Sin() function line plots</strong>  
   Multiple phase-shifted sin curves with different styles.  
   Output: <code>line_3.png</code>

4) <strong>Sin() function plots with markers</strong>  
   Uses markers to differentiate curves on shared axes.  
   Output: <code>line_4.png</code>

5) <strong>Simple tiled layout (2×1)</strong>  
   Builds two XY charts and exports a single combined matrix image.  
   Output: <code>line_5.png</code>

6) <strong>3×2 subplot grid</strong>  
   A chart matrix showcasing mixed content, including:
   - sin curve with marker indices (scatter overlay)
   - nonlinear function
   - cosine plot
   - category/time plot using string labels
   - a parametric circle
   Output: <code>line_6.png</code>

7) <strong>CSV-based plot (Iris)</strong>  
   Loads the Iris dataset and plots <code>petal_length</code> vs <code>petal_width</code> grouped by <code>classification</code>.  
   Output: <code>line_7_csv.png</code>

To display windows as the plots are generated:

```bash
scala-cli run src/main/scala/line.scala -- --show
```

To supply your own CSV file or URL:

```bash
scala-cli run src/main/scala/line.scala -- --show --csv "path/or/url/to/iris.csv"
```

</section>

---

<section id="histogram-plots-module-histogramscala">

## Histogram plots module (<code>histogram.scala</code>)

This section documents the <strong>histogram</strong> examples in <code>src/main/scala/histogram.scala</code>.  
It is structured to be <strong>modular</strong> so you can later extend it (e.g., KDE plots, distribution overlays, violin plots).

### What this module uses (imports and why)

Core XChart imports:

- <code>org.knowm.xchart.*</code>  
  Category charts, builders, histogram helper class, and SwingWrapper.

- <code>org.knowm.xchart.BitmapEncoder.BitmapFormat</code>  
  PNG exports.

- <code>org.knowm.xchart.style.Styler</code> and
  <code>org.knowm.xchart.CategorySeries.CategorySeriesRenderStyle</code>  
  Used to control legend placement and to render a histogram as bars and an overlay curve as a line when needed.

Scala/Java interop & utilities:

- <code>scala.jdk.CollectionConverters.*</code>  
  Required because XChart accepts Java collections.

- <code>scala.util.{Random, Try}</code>  
  Random generation and safe numeric parsing.

CSV utilities and encoding:

- <code>scala.io.Source</code>, <code>scala.io.Codec</code>, and <code>StandardCharsets</code>  
  Read CSV from file or URL using UTF-8.

High-quality export utilities:

- Same approach as <code>line.scala</code> for matrix export (<code>saveMatrixPng300</code>) and for embedding DPI metadata.

### Histogram-specific helpers (math and normalization)

This module includes helper functions to control histogram construction beyond “default bins”:

- <strong>Binning rules</strong>
  - <code>sturgesBins(n)</code>
  - <code>sqrtBins(n)</code>
  - <code>scottBins(data)</code>
  - <code>freedmanDiaconisBins(data)</code>
  - <code>integersBins(data)</code>
  - <code>automaticBins(data)</code> (configured as Sturges in this repo)

- <strong>Histogram builders</strong>
  - <code>histogramBarChart(...)</code>  
    Uses XChart’s <code>Histogram</code> class to compute bin centers and counts, then renders a <code>CategoryChart</code> bar chart.

- <strong>Normalization</strong>
  - <code>None</code>: raw counts
  - <code>probability</code>: counts / N
  - <code>pdf</code>: counts / (N * binWidth)
  - <code>count_density</code> (custom edges): counts / binWidth

- <strong>Fixed-width / custom edges</strong>
  - <code>histogramWithEdges(data, edges, normalization)</code>
  - <code>histogramFixedWidth(data, binWidth, normalization, fixedCenters)</code>

### What the <code>main</code> function generates (example set)

When you run the module, it generates multiple histogram-related figures:

1) <strong>Simple histogram (normal data)</strong>  
   Uses Sturges’ rule for bin selection.  
   Output: <code>histogram_1.png</code>

2) <strong>Compare binning rules (2×3 matrix)</strong>  
   Visual comparison of different bin rules in a chart matrix.  
   Output: <code>histogram_2_matrix_2x3.png</code>

3) <strong>Changing the number of bins</strong>  
   Creates one histogram with automatic bins, then one with 50 bins.  
   Outputs: <code>histogram_3_initial.png</code>, <code>histogram_3_50bins.png</code>

4) <strong>Custom bin edges + count-density normalization</strong>  
   Demonstrates non-uniform bin edges and density normalization.  
   Output: <code>histogram_4.png</code>

5) <strong>Categorical histogram (bar chart)</strong>  
   Counts string categories and plots them as bars.  
   Output: <code>histogram_5.png</code>

6) <strong>Overlay normalized histograms (probability)</strong>  
   Two datasets overlaid on the same chart using consistent bin centers.  
   Output: <code>histogram_6.png</code>

7) <strong>PDF-normalized histogram + theoretical normal PDF</strong>  
   Builds a PDF-like histogram and overlays the theoretical normal density as a line series.  
   Output: <code>histogram_7.png</code>

8) <strong>CSV-based histogram (default: diabetes.csv, column: Glucose)</strong>  
   Loads CSV data, extracts a column, and builds a histogram with automatic bins.  
   Output: <code>histogram_8_csv.png</code>

To display windows as the plots are generated:

```bash
scala-cli run src/main/scala/histogram.scala -- --show
```

To supply your own CSV file or URL:

```bash
scala-cli run src/main/scala/histogram.scala -- --show --csv "path/or/url/to/diabetes.csv"
```

</section>

---

## Troubleshooting

### Swing window does not appear

- Ensure you are running on a machine with GUI support.
- If you are on a headless environment (some servers/containers), omit <code>--show</code> and rely on PNG exports.

### Compilation errors involving Java/Scala collections

- Confirm the conversion import is present:
  - <code>import scala.jdk.CollectionConverters.*</code>

### sbt finds multiple main classes

- Prefer running explicitly with <code>runMain</code>:

```bash
sbt "runMain Line --show"
sbt "runMain Histogram --show"
```

---

## Implementation tutorial video

Replace the placeholder YouTube ID below with your own video ID after uploading the tutorial.

<a href="https://www.youtube.com/watch?v=UW6AGWX11bY" target="_blank">
  <img
    src="https://i.ytimg.com/vi/UW6AGWX11bY/maxresdefault.jpg"
    alt="Scala Plotting with XChart - Implementation Tutorial"
    style="max-width: 100%; border-radius: 8px; box-shadow: 0 4px 16px rgba(0,0,0,0.15); margin-top: 0.5rem;"
  />
</a>
