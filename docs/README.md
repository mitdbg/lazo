## Lazo - High-Dimensional Data Search

Lazo is a library for approximate high-dimensional search. It can index large
volumes of high-dimensional points, search among them efficiently, and give an
approximate answer with high accuracy. Right now Lazo supports two different
search metrics, Jaccard similarity and Jaccard containment; more metrics are
under development and will be made available soon. 

We are using Lazo within the context of data discovery, and in particular,
Aurum. For more information take a look at 
[this project](http://mitdbg.github.io/aurum-datadiscovery)
and the relevant papers in [my webpage](http://www.raulcastrofernandez.com).

### When should I use Lazo?

This is a non-exhaustive list of situations where you may benefit from Lazo:

- When you are trying to solve an approximate nearest neighbor problem and your
metric of interest is Jaccard similarity (Lazo implements MinHash/LSH) or
Jaccard containment.

- When you want to find all-pairs of elements within a set N with a Jaccard
similarity or containment beyond a given threshold, and you cannot afford
actually comparing every pair, but you are ok with approximate answers. 

### Quick Start

#### Building Project

Lazo will be made available through a Maven repository in the future. For the
time being, it must be built locally, which is simple:

From the root directory of the project execute the following command:

`
$> ./gradlew build
`

this builds the library and runs the tests. The output will be in a repository
*build* in the root directory of the project. For convenience, this command
creates a **jar** file, which you can easily link to your project.

**Note: ** Lazo uses the Gradle build tool. The project includes the [gradle
wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html), which
means you don't need to install anything: the wrapper will download all
the necessary files of the build system.

#### Basic use of the library

With Lazo you can index efficiently very high numbers of sets (N) and then quickly
search for those that are similar to a query set (q). The workflow for that is
1) to create a *sketch* (a succint summary of the data) of each set you want to
search; 2) insert the sketch into the Lazo index; and 3) query the index with
the sketch for q.

**Creating a sketch of a set: **Creating a sketch of a set of *values*:

`
LazoSketch sketch = new LazoSketch();
for (String value : values) {
    sketch.update(value);
}
`

the method `update` takes a string as input parameter. You can update the sketch
as data becomes available if that's necessary.

**Indexing a sketch: **To index the *sketch* in the Lazo Index is as simple as:

`
LazoIndex index = new LazoIndex();
index.insert(<key>, sketch);
`

Note the *key* argument of the insert function can be any Java Object.

**Querying the index: **To query the index, i.e., find all sketches similar to
an input sketch *q*:

`
Set<LazoCandidate> results = index.querySimilarity(q, <similarity_threshold>);
Set<LazoCandidate> results = index.queryContainment(q, <containment_threshold>);
` 

The *similarity_* and *containment_threshold* must be a floating number in the
range [0,1]. The object LazoCandidate contains the <key> used to identify the
sketch when it was inserted as well as the specific Jaccard similarity and
containment with respect to the input query sketch, q.

### Support or Contact

Docs are being built, if you are interested in contributing to this project, you
can reach me at raulcf@csail.mit.edu

