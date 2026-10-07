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
- Cardinality constraints limited to `1:1`, `1:N`, `N:1`

### Azimutt

[Homepage](https://azimutt.app/)

[Docs](https://azimutt.app/docs)

[GitHub](https://github.com/azimuttapp/azimutt)

- Uses [AML](https://azimutt.app/aml) DSL
- Splits between tables and relations
- `1:N`, `N:N`, and `1:1` relationships are supported
- [**Custom type system**](https://azimutt.app/docs/aml#types)

### QuickDBD

[Homepage](quickdatabasediagrams.com)

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
- Based on the example images, I assume that it is possible to define some cardinality constraints but I could not derive what precision they offer

## Comparative table

| **Name** | **Open Source** | **3 layer separation** | **Input Style** | **Export** | **Collaboration** |  **Distribution** | **License** | **Purpose** | **Engineering direction** | **Cardinality constraints** |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| [**ChartDB**](#chartdb) | ✅ | Partially (see [DBML](#dbml)) | Graphical, SQL, DBML | SQL, DBML | Cloud based | Web, Docker, NodeJS | AGPL-3.0 | Documentation, design | Forward, reverse |✅* |
| [**dbdiagram.io**](#dbdiagramio) | ❌ | Partially (see [DBML](#dbml)) | Graphical, DBML | SQL, DBML | Possibly behind paywall | Web | *Proprietary* | Documentation, design | Forward, reverse |✅* |
| [**DBML**](#dbml) | ✅ | Partially* | DBML | SQL | Possible with VCS | `npm` | Apache-2.0 | Documentation, design | Forward, reverse | ✅* |
| [**SqlDBM**](#sqldbm) | ❌ | ❌ | Graphical | | Cloud based | Web | *Proprietary* | Documentation, design, AI | Forward, reverse | ❌ |
| [**DrawSQL**](#drawsql) | ❌ | ❌ | Graphical, SQL, DBML, JSON | SQL, JSON, Laravel* | Cloud based | Web | *Proprietary* | (Fast) Design, AI, Laravel, collaboration | Forward, reverse | ✅* |
| [**Azimutt**](#azimutt) | ✅ | Partially* | SQL, [AML](https://azimutt.app/aml) | SQL, [AML](https://azimutt.app/aml) | ❌ | Web, Docker, `npm` | MIT | Documentation, design, analysis, optimization | Forward, reverse | ✅* |
| [**QuickDBD**](#quickdbd) | ❌ | ❌ | Graphical, DSL | SQL, diagram as PDF | ❌ | Web | *Proprietary* | Documentation, design | Forward, reverse | ✅* |
| [**eraser**](#eraser) | ❌ | ❌ | Graphical, DSL | ? | ? | Web | *Proprietary* | Diagram, Design | ? | ? |
| [**ERDPlus**](#erdplus) | ❌ | ❌* | Graphical | PNG | ❌ | Web | *Proprietary* | ER-diagram | ❌ | ✅ |
| [**Lucidchart**](#lucidchart) | ❌ | ? | Graphical, ? | ? | Cloud based | Web | *Proprietary*| ER-diagram | ? | ? |
| [**DbSchema**](#dbschema) | ❌ | ❌ | Graphical, binary file format | SQL, binary file format | ❌ | App | *Proprietary* | DB management | Forward, reverse | ✅ |
| [**Vertabelo/Redgate Data Modeler**](#vertabeloredgate-data-modeler) | ❌ | ? | Graphical, ? | ? | Cloud based | Web | *Proprietary* | Design | ? | ? |
| [**Creately**](#creately) | ❌ | ❌* | Graphical | ? | Cloud based | Web | *Proprietary* | Design | ? | ?* |

Field values with a star (`*`) indicate that one should check the notes in the [Tools](#tools) section.

Field values that are `?` indicate that this information could not be found. Check the notes in the [Tools](#tools) section.
