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

### dbdiagram.io

[Homepage](https://dbdiagram.io/)

[Docs](https://docs.dbdiagram.io/)

- Supports all DBML relationship cardinalities

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

### DrawSQL

[Homepage](https://drawsql.app/)

- No public documentation
- Cardinality coefficients limited to `1:1`, `1:N`, `N:1`

### Azimutt

[Homepage](https://azimutt.app/)

[Docs](https://azimutt.app/docs)

[GitHub](https://github.com/azimuttapp/azimutt)

- Uses [AML](https://azimutt.app/aml) DSL
- Splits between tables and relations
- `1:N`, `N:N`, and `1:1` relationships are supported
- [**Custom type system**](https://azimutt.app/docs/aml#types)

### QuickDBD

[Homepage](https://quickdatabasediagrams.com)

- Supports many relationship cardinalities: `1:1`, `1:N`, `N:1`, `M:N`, `1:0/1`, `0/1:1`, `0/1:0/1`, `1:0/N`, `0/N:1`

### eraser

[Examples](https://docs.eraser.io/erd-examples)

[Docs](https://docs.eraser.io/erd-syntax)

> Warning: The web application does not work correctly, I was unable to test the tool.

### ERDPlus

[Homepage](https://erdplus.com/)

[App (trial)](https://erdplus.com/trial)

- only faces the conceptual design layer

### Lucidchart

[Homepage](https://lucid.co/lucidchart)

> Warning: The web application is not accessible without payment; I was unable to test the tool.

### DbSchema

[Homepage](https://dbschema.com/)

- Bugs on Linux: I was unable to close the program, conflicts with VS Code

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

## Comparative table

| **Name** | **Open Source** | **Conceptual layer** | **Logical layer** | **Physical layer** | **Input Style** | **Export** | **Collaboration** |  **Distribution** | **License** | **Purpose** | **Engineering direction** | **Cardinality coefficients** | **Functional Dependencies** | **Decision Tracing** |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| [**ChartDB**](#chartdb) | ✅ | ✗ | ✗ | ✅ | Graphical, SQL, DBML | SQL, DBML | Cloud based | Web, Docker, NodeJS | AGPL-3.0 | Documentation, design | Forward, reverse | ✓ | ❌ | ❌ |
| [**dbdiagram.io**](#dbdiagramio) | ❌ | ✗ | ✗ | ✅ | Graphical, DBML | SQL, DBML | Possibly behind paywall | Web | *Proprietary* | Documentation, design | Forward, reverse | ✓ | ❌ | ❌ |
| [**DBML**](#dbml) | ✅ | ✗ | ✗ | ✅ | DBML | SQL | Possible with VCS | `npm` | Apache-2.0 | Documentation, design | Forward, reverse | ✓ | ❌ | ❌ |
| [**SqlDBM**](#sqldbm) | ❌ | ✗ | ❌ | ✅ | Graphical | | Cloud based | Web | *Proprietary* | Documentation, design, AI | Forward, reverse | ❌ | ❌ | ❌ |
| [**DrawSQL**](#drawsql) | ❌ | ❌ | ❌ | ✅ | Graphical, SQL, DBML, JSON | SQL, JSON, Laravel* | Cloud based | Web | *Proprietary* | (Fast) Design, AI, Laravel, collaboration | Forward, reverse | ✓ | ❌ | ❌ |
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

  > How can the tool enforce the use of the three design phases?

  > What happens in the three design layers if the model changes?
- Analyse functional dependencies and compute normalization level
  > How must the user interact with the tool to communicate functional dependencies?
- Human readable storage of the DB model
  > How can the tool store the DB model in a human readable way without risking the loss of information and by using a structured non-binary language?
