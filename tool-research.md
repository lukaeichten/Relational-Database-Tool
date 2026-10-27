# DB modeling tools

## Tools

I did not cover the AI capabilities of some tools because it is not relevant for the thesis.

Online comparaisons:
- [ChartDB Blog](https://chartdb.io/blog/best-free-erd-tools)
- [Trevor](https://trevor.io/blog/top-7-entity-relationship-diagram-tools#erdrawmax)

### ChartDB

[Docs](https://docs.chartdb.io/docs/welcome)

[GitHub](https://github.com/chartdb/chartdb)

- Uses [DBML](#dbml) and is a graphical wrapper around it
- Supports `1:1` and `1:N` relationship cardinalities

> Limitations: ChartDB does not support the selection of cardinality coefficients other than `1:1` and `1:N`. It is not possible to define functional dependencies. Cardinality coefficients and constraints are not enforced by the generated SQL code.

### dbdiagram.io

[Homepage](https://dbdiagram.io/)

[Docs](https://docs.dbdiagram.io/)

- Uses [DBML](#dbml) and is a graphical wrapper around it
- Supports all DBML relationship cardinalities

> Limitations: It is not possible to define functional dependencies. Cardinality coefficients and constraints are not enforced by the generated SQL code.

### DBML

"DBML (Database Markup Language) is an open-source DSL designed to define and document database schemas and structures. It is designed to be simple, consistent and highly-readable.", copied from the documentation.

[Homepage/Docs](https://dbml.dbdiagram.io/home)

[GitHub](https://github.com/holistics/dbml)

- No real 3 phase design but there is a destinction between tables and relationships (see the [documentation](https://dbml.dbdiagram.io/docs#relationships--foreign-key-definitions)).
- Supports `1:N`, `N:1`, `1:1`, and `N:N` relationships
- [Toolchain/Ecosystem](https://dbml.dbdiagram.io/ecosystem) for SQL to DBML, DBML to SQL, validation, inspection, documentation generation, and much more.

### SqlDBM

[Homepage](https://sqldbm.com/)

[Docs](https://support.sqldbm.com/hc/en-us/categories/34520372680845-Knowledge-Base)

- Bad documentation (it is not well accessible and incomplete, proper navigation is missing)
- Explicitly mentions ["Conceptual, logical, and physical modeling"](https://sqldbm.com/) but does no separation of those layers (concluded based on images and demo).

> Limitations: It is not possible to define cardinalitiy constraints or coefficients, but nullable relationships are possible.

### DrawSQL

[Homepage](https://drawsql.app/)

- No public documentation
- Cardinality coefficients limited to `1:1`, `1:N`, `N:1`

> Limitations: Not possible to generate SQL code after schema creation. It is not possible to define any cardinalitiy constraints or coefficients freely, only limited to `1:1`, `1:N`, and `N:1`. Functional dependencies cannot be defined.

### Azimutt

[Homepage](https://azimutt.app/)

[Docs](https://azimutt.app/docs)

[GitHub](https://github.com/azimuttapp/azimutt)

- Uses [AML](https://azimutt.app/aml) DSL
- Splits between tables and relations
- `1:N`, `N:N`, and `1:1` relationships are supported
- [**Custom type system**](https://azimutt.app/docs/aml#types)

> Limitations: Export to SQL is a paid feature. It is not possible to define any cardinalitiy constraints or coefficients freely, only limited to `1:1`, `1:N`, and `N:1`. Functional dependencies cannot be defined.

### QuickDBD

[Homepage](https://quickdatabasediagrams.com)

- Supports many relationship cardinalities: `1:1`, `1:N`, `N:1`, `M:N`, `1:0/1`, `0/1:1`, `0/1:0/1`, `1:0/N`, `0/N:1`

> Limitations: SQL export does not contain cardinality constraint implementations. Functional dependencies cannot be defined.

### eraser

[Examples](https://docs.eraser.io/erd-examples)

[Docs](https://docs.eraser.io/erd-syntax)

> Warning: The web application does not work correctly, I was unable to test the tool.

### ERDPlus

[Homepage](https://erdplus.com/)

[App (trial)](https://erdplus.com/trial)

- only faces the conceptual design layer

> Limitations: Only faces the conceptual design layer, no SQL export or more.

### Lucidchart

[Homepage](https://lucid.co/lucidchart)

> Warning: The web application is not accessible without payment; I was unable to test the tool.

### DbSchema

[Homepage](https://dbschema.com/)

- Bugs on Linux: I was unable to close the program, conflicts with VS Code

> Limitations: SQL export can only create a basic initialization script and migrations. Those are incomplete.

### Vertabelo/Redgate Data Modeler

[Homepage](https://www.red-gate.com/products/redgate-data-modeler/)

> Warning: The web application is not accessible without payment; I was unable to test the tool.

### Creately

[Homepage](https://creately.com/)

> Warning: The web application does not offer explorable examples and I was unable to create a new diagram (limited by free plan); I was unable to test the tool.

- ERD tool which only implements the first conceptual design layer
- Based on the example images, I assume that it is possible to define some cardinality coefficients but I could not derive what precision they offer

### bigER

[GitHub](https://github.com/borkdominik/bigER)

[Docs](https://github.com/borkdominik/bigER/wiki)

- The user is warned about some missing constraints or similar such as no identifying attribute for an entity.

> Limitations: Generated SQL code does not contain all constraint implementations. Logical layer is missing.

## Comparative table

| **Name** | **Open Source** | **Conceptual layer** | **Logical layer** | **Physical layer** | **Input Style** | **Export** | **Collaboration** |  **Distribution** | **License** | **Purpose** | **Engineering direction** | **Cardinality coefficients** | **Functional Dependencies** | **Decision Tracing** |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| [**ChartDB**](#chartdb) | ✅ | ✗ | ✗ | ✅ | Graphical, SQL, DBML | SQL, DBML | Cloud based | Web, Docker, NodeJS | AGPL-3.0 | Documentation, design | Forward, reverse | ✓ | ❌ | ❌ |
| [**dbdiagram.io**](#dbdiagramio) | ❌ | ✗ | ✗ | ✅ | Graphical, DBML | SQL, DBML | Possibly behind paywall | Web | *Proprietary* | Documentation, design | Forward, reverse | ✓ | ❌ | ❌ |
| [**DBML**](#dbml) | ✅ | ✗ | ✗ | ✅ | DBML | SQL | Possible with VCS | `npm` | Apache-2.0 | Documentation, design | Forward, reverse | ✓ | ❌ | ❌ |
| [**SqlDBM**](#sqldbm) | ❌ | ✗ | ❌ | ✅ | Graphical | | Cloud based | Web | *Proprietary* | Documentation, design, AI | Forward, reverse | ❌ | ❌ | ❌ |
| [**DrawSQL**](#drawsql) | ❌ | ❌ | ❌ | ✅ | Graphical, SQL, DBML, JSON | ? | Cloud based | Web | *Proprietary* | (Fast) Design, AI, Laravel, collaboration | Forward, reverse | ✓ | ❌ | ❌ |
| [**Azimutt**](#azimutt) | ✅ | ✗ | ❌ | ✅ | SQL, [AML](https://azimutt.app/aml) | SQL, [AML](https://azimutt.app/aml) | ❌ | Web, Docker, `npm` | MIT | Documentation, design, analysis, optimization | Forward, reverse | ✓ | ❌ | ❌ |
| [**QuickDBD**](#quickdbd) | ❌ | ❌ | ✗ | ✅ | Graphical, DSL | SQL, diagram as PDF | ❌ | Web | *Proprietary* | Documentation, design | Forward, reverse | ✓ | ❌ | ❌ |
| [**eraser**](#eraser) | ❌ | ✓ | ❌ | ✅ | Graphical, DSL | ? | ? | Web | *Proprietary* | Diagram, Design | ? | ? | ❌ | ❌ |
| [**ERDPlus**](#erdplus) | ❌ | ✅ | ❌ | ❌ | Graphical | PNG | ❌ | Web | *Proprietary* | ER-diagram | ❌ | ✅ | ❌ | ❌ |
| [**Lucidchart**](#lucidchart) | ❌ | ? | ? | ? | Graphical, ? | ? | Cloud based | Web | *Proprietary*| ER-diagram | ? | ? | ❌ | ❌ |
| [**DbSchema**](#dbschema) | ❌ | ✗ | ✗ | ✅ | Graphical, binary file format | SQL, binary file format | ❌ | App | *Proprietary* | DB management | Forward, reverse | ✅ | ❌ | ❌ |
| [**Vertabelo/Redgate Data Modeler**](#vertabeloredgate-data-modeler) | ❌ | ? | ? | ? | Graphical, ? | ? | Cloud based | Web | *Proprietary* | Design | ? | ? | ❌ | ❌ |
| [**Creately**](#creately) | ❌ | ✅ | ❌ | ❌ | Graphical | ? | Cloud based | Web | *Proprietary* | Design | ? | ? | ❌ | ❌ |
| [**bigER**](#biger) | ✅ | ✅ | ✗ | ✓ | Graphical, DSL | SQL, `.erd` file | Possible with VCS | VS Code Extension | MIT | Design, modeling | Forwards, reverse | ✅ | ❌ | ✗ |

- `✅` indicates that the criteria is fully met.
- `✓` indicates that the criteria is mostly met (check the notes for more information).
- `✗` indicates that the criteria is partially met (check the notes for more information).
- `❌` indicates that the criteria is not met at all.

Field values that are `?` indicate that this information could not be found. Check the notes in the [Tools](#tools) section.

## Conclusion

Many database tools exist, but they lack the power to work with all 3 design layers/phases.
Most of the tools work purely on the physical model; a simple tool that has no notion of relationships and entities but only tables and foreign keys.
There exist a few tools which are open sourced.
All tools support a GUI to facilitate the DB modeling process (with the only exception of DBML which is a toolkit to represent a database model as a human readable file).
Most apps are cloud based and run in the web.
No tool supports the notion of functional dependencies or implements design decision taceing (for example the implementation of the safeguards of a domain of postal codes).

One can see a clear pattern; most of the tools do not offer the design of a database by using multiple design phases.
They focus on the quick generation of SQL/DDL code which is easier when only exposing the physical implementation directly where the notation of entities and releationships does not exist anymore.
Other tools only offer the generation of visual diagramms for documentation purpose but are not ment to generate artifacts that can be used in the related developped software.

To sum up, no tool supports all of the following functional requirements:
- Separation and implementation of the conceptual, logical, and physical database design layer,
- generation of SQL/DDL code,
- cardinality coefficients without templates or constraints (so 2:5 is allowed),
- validation of the built model and warnings/popups if something is not correct or not implemented (such as the implementation of domain constraints or cardinality coefficients),
- functional dependencies,
- and normalization.

## Problems and research questions

- Unite conceptual, logical, and physical design layers in a streamlined way from *conceptual* to *logical* to *physical* without mixing the layers itself
  > How can a design decisions in one layer propagate to and influence the previous layer(s)?

  > How can a tool store the design decisions and their impact of the 3 different layers?

  > What happens in the three design layers if the model changes?
- Analyse functional dependencies and compute normalization level
  > How must the user interact with the tool to communicate functional dependencies?
- Serialization
  > How can the tool store the DB model in a human readable way without risking the loss of information and by using a structured non-binary language?

# Technology overview

## From scratch

- VS Code Extension with language server
  > Benefits: Well documented pattern, easy installation for the INFO0009 students, everything packages in one installation (no dependencies to resolve manually), natural integration in the development workflow, integration into existing UI (less work)

  > Possible issues: VS Code performance, slow execution
- Java FX app
  > Benefits: Well documented, "fat Jar" as dependency package (no dependencies to resolve manually)

  > Issues: A lot of work because one needs to implement a whole application from scratch
- Compiler and DSL
  > Benefits: Compiler creation is a well understood pattern, *conceptual* to *logical* to *physical* layer feels like a multi-stage compiler and each stage can report its own errors

  > Issues: Cross-compatibility on all devices (INFO0009 students likely use Windows, Linux, and MacOS), GUI is not obvious for a compiler/transpiler
- Web-based app (static website)
  > Benefits: Easy to ship (for example GitHub pages)

  > Issues: Everything runs with JavaScript (possibly slow) or WASM (hard to do), storage on the client device
- Docker-based app
  > Benefits: Backend can be implemented in any language, a lot of freedom

  > Issues: Both backend and client must be implemented

In collaboration with professor Debruyne, we came up with the following research question: "**How can we facilitate database design at every level (conceptual, logical, and physical) by propagating the effects of each informed decision from one level to the others?**"

## Reuse of existing solutions

- bigER
  > Benefits: 2 phases are nearly completely implemented (less work to do), I like the syntax of the modelling language

  > Issues: Inactive GitHub repository (last commit one year ago, not much issues and PRs), logical design phase is completely missing, a lot of changes must be made to the modelling language to introduce the logical design layer
- ChartDB or DBML
  > Benefits: Built to run completely in the browser (JS/TS), packaging as a VS Code extension may be possible, DBML toolchain is extensible

  > Issues: DBML is not built around the 3 design phases -> the implementation of a 3 phase design will add a lot on new elements in the UI and DBML
- Azimutt
  > same as above with the exception that AML is used instead of DBML

We agreed on forking [BigER](#biger) to test the implementation of additional features such that we can test out the extensibility of the extension.

### DSL

Here is a feature list of a DSL for database modelling:
- (weak) entities
- (identifying) relationships, roles, and cardinality coefficients
- (derived/identifying, unique, multi-value, composite, required or nullable) attributes and domains (null as domain implementation or as attribute modifier?)
- functional dependencies and normalization
- domain to DB type mapping (or algorithm to do it automatically)
- choice of index type (hash table or B+ tree)
- absorb relationships or junction table
- decision tracing (with annotations?)
- *sub-classes* and *super-classes*
- aggregations?
- views? (additional feature, same as derived attribute)

Possible additional language features:
- SQL dialect
- name style (Camel case, Pascal case, Snake case, ...)
