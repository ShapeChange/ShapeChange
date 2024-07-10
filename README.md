# ShapeChange
Processing application schemas for geographic information. See the [wiki](https://github.com/ShapeChange/ShapeChange/wiki) for information on how to set up the development environment and the [technical documentation](https://shapechange.github.io/ShapeChange/) on how to use ShapeChange. 

NOTE: You can use the [issue tracker](https://github.com/ShapeChange/ShapeChange/issues) to report issues - bugs, enhancement requests, but also general questions - with the code as well as the documentation.


# Modules

Version 4.0.0 of ShapeChange introduced a major refactoring of the codebase. The maven project was refactored into three maven modules:

* `shapechange-core`: Contains the core logic for executing ShapeChange workflows, including model transformations, model validators, and a wide range of encoding targets.
* `shapechange-ea` (depends on core): Contains classes for interacting with _Sparx Systems Enterprise Architect_ (EA) repositories, including the code for reading a model from an EA repository, as well as encoding targets.
* `shapechange-app` (depends on core and ea): Contains the main class for invoking ShapeChange from the command line. Also includes the technical documentation as well as integration tests. The ShapeChange distribution package is built from this module.

The new project structure is better suited for integration of ShapeChange in other software:

* Programs that do not need EA-related functionality just import `shapechange-core`.
* If interaction with EA repositories is needed, also import `shapechange-ea`. In this case, the jar for the EA Java API needs to be installed in the local maven repository, because the jar is not (allowed to be made) available in a public maven repository.

NOTE: The refactoring led to renaming of java packages. Thus, also the qualified names of ShapeChange Java classes changed. Since qualified names for model, model validator, transformer and target classes are used in ShapeChange configurations, the renaming introduced breaking changes. ShapeChange configurations created for ShapeChange 3.1.0 and below need to be updated in order to work with ShapeChange 4.0.0. The ShapeChange wiki contains [mappings](https://github.com/ShapeChange/ShapeChange/wiki/Migration-to-ShapeChange-v4.0.0) for relevant (qualified) class names as well as Java packages. The class mappings can be used to update ShapeChange configurations, while the package mappings can be used by ShapeChange extension developers to update the import statements in their Java code.




