# 3 phase research

This page contains research about the 3 database design phases: conceptual, logical, and physical.
This page also includes what information and which constraints they add and how those aspects are conserved or lost when going from one phase to another.

## Conceptual phase

The book `Fundamentals of Database Systems 6th Edition` uses the same notation as seen in the INFO0009 course but also mentions an alternative UML style.

- high level data model and relationships
  > From: `Fundamentals of Database Systems 6th Edition`, Chapter 7 Data Modeling Using the Entity-Relationship (ER) Model; page 199 & 200
- specialization, generalization, inheritance, and union types (categories)
  > From: `Fundamentals of Database Systems 6th Edition`, Chapter 7 Data Modeling Using the Entity-Relationship (ER) Model; page 199 & 200
- Entities & weak entities , attributes (composite & atomic, single-valued & multi-valued, stored & derived, NULL) & key attributes
  > From: `Fundamentals of Database Systems 6th Edition`, Chapter 7.3 Entity Types, Entity Sets, Attributes, and Keys; page 203 - 206 & 208 - 209
  > From: `Fundamentals of Database Systems 6th Edition`, Chapter 7.5 Weak Entity Types; page 219
- no concept of primary key (so multiple key attributes are possible and each key attribute is a key on its own)
  > From: `Fundamentals of Database Systems 6th Edition`, Chapter 7.3 Entity Types, Entity Sets, Attributes, and Keys; page 209
- Attribute domains *(also called value sets in the book)*
  > From: `Fundamentals of Database Systems 6th Edition`, Chapter 7.3.1 Entities and Attributes & Chapter 7.3.2 Entity Types, Entity Sets, and Value Sets; page 209
- Relationships (binary & ternary & ..., roles & participants, cardinality ratios & coefficients & constraints) and relationship attributes
  > From: `Fundamentals of Database Systems 6th Edition`, Chapter 7.4 Relationship Types, Relationship Sets, Roles, and Structural Constraints; page 212 - 218

  > **Note**: The source uses the term "Structural Constrains" for cardinality coefficients.
- Aggregations *(relationship between a whole object and its component parts)*
  > From: `Fundamentals of Database Systems 6th Edition`, Chapter 7.8 Example of Other Notation: UML Class Diagrams; page 227
- Subclasses, Superclasses, Inheritance (Specialization and Generalization) and their constraints
  > From: `Fundamentals of Database Systems 6th Edition`, Chapter 8.1 Subclasses, Superclasses, and Inheritance & Chapter 8.2 Specialization and Generalization & Chapter 8.3 Constraints and Characteristics of Specialization and Generalization Hierarchies; page 246 & 248 & 251

Output of the conceptual design phase is the ER diagram.

## Logical phase

- transformation from the high level conceptual model into the implementation data model
  > From: `Fundamentals of Database Systems 6th Edition`, Chapter 7.1 Data Modeling Using the Entity-Relationship (ER) Model; page 202
- normalization and functional dependencies
  > From: `Fundamentals of Database Systems 6th Edition`, Chapter 15 Functional Dependencies and Normalization of Relational Databases, page 501 - 536

Algorithm to apply to go from the conceptual model to the relational model (logical phase):
1. Entities are mapped to relations, including all attributes where composite attributes are "flattened" and a composite key attribute forms a set of primary keys.
2. Weak entities are mapped to relations the same way as entities but they include a foreign key attributes all primary key attributes of the identifying relation. Note that if a weak entity is identified by a second weak entity, the second weak entity must be mapped first.
3. There are **three ways of mapping binary 1:1 relationships** (decision to take and to document):
     - absorb the relationship in one of the two participants
     - merge the two entities into one relation
     - create a relationship relation/lookup table which contains both primary keys of both relations
4. There are **two possible approaches of mapping binary 1:N relationships** (decision to take and to document):
     - add the primary keys of the relation that has the 1 participation to the relation that has the N participation
     - create a relationship relation which contains both primary keys of both relations
5. To map binary M:N relationship types, we must use a lookup table. **The `CASCADE` option should be defined for this relation in the physical implementation** because the lookup table depends on both sides of the relationship.
6. Multivalued attributes are mapped to a new relation. A composite multivalued attribute is flattened. It includes a foreign key to the primary key of the owning entity. **The `CASCADE` option should be defined for this relation in the physical implementation** because the relation is strictly bound to the owning entity. **Additional constraints such as the number of minumum and maximum number of multivalued attribute instances are lost**.
7. N-ary relationships are mapped to new relations. The primary keys of each participating entity are added to the new relation and form the primary key of the relation. This relation includes additional attributes of the relationship. **Relations with participation of 1 may absorb the relation** (decision to take and to document).
> From: `Fundamentals of Database Systems 6th Edition`, Chapter 9.1 ER-to-Relational Mapping Algorithm; page 286 - 292
8. There are **four possible ways of mapping specialization and generalization to relations** (decision to take and to document), **constraints are lost and must be implemented elsewhere**:
   - Create a relation for the generalizing superclass with all attributes of the superclass entity including keys. Create a relation for each of the subclasses with all attributes of the subclass in addition to a foreign key to the superclass relation. The foreign key is part of the primary key of the subclass relation. **This approach works best for any kind of specialization**.
   - Create only relations for the subclass entities where the attributes of the superclass entity are duplicated in each subclass relation. **This approach only works for disjoint subclasses and creates redundancy if attributes are used in several relations**.
   - Create only a single relation which contains the attributes of the superclass entity, the attributes of all subclass entities, and one additional type attribute whose value is used to indicate the subclass type. **This only works for disjoint subclasses**.
   - Create only a single relation which contains the attributes of the superclass entity, the attributes of all subclass entities, and additional type boolean attributes whose trueness indicates that the subclass is used.
9. The mapping of union types (superclasses with different keys) requires the use of a surrogate key. The surrogate key requires all superclass relations to implement a new foreign key to a new artificial relation. This new artificial relation contains only a primary key and is used in the union type's relationships.
> From: `Fundamentals of Database Systems 6th Edition`, Chapter 9.2 Mapping EER Model Constructs to Relations; page 294 - 299

The book does not mention how to treat relationships with precise cardinality coefficients such as 2:5.

Output of the logical design phase is a relational schema, an alternative database model which is similar to the conceptual ER diagram but may differ after applying normalization.

## Physical phase

- "internal storage structures, file organizations, indexes, access paths, and physical design parameters for the database files are specified"
  > From: `Fundamentals of Database Systems 6th Edition`, Chapter 7.1 Data Modeling Using the Entity-Relationship (ER) Model; page 202
- implementation of stored procedures
  > From: `Fundamentals of Database Systems 6th Edition`, Chapter 7.1 Data Modeling Using the Entity-Relationship (ER) Model; page 202
- choice of indexing: when to use which index
  > From: `Fundamentals of Database Systems 6th Edition`, Cgapter 20.1.2 Physical Database Design Decisions; page 730 & 731
- denormalization: some queries/operations on the data might benefit from removing normalization, which aims to reduze redundancy, to get rid of extra processing overhead
  > From: `Fundamentals of Database Systems 6th Edition`, Cgapter 20.1.2 Physical Database Design Decisions; page 731

The output of the physical database design phase is a DDL "script" which can be used to initialize or migrate a database. This script may be fine tuned for a specific DBMS such as PostgreSQL, MySQL, MariaDB, and Microsoft SQL Server. The constraints of the relational schema ([logical design phase](#logical-phase)) must be enforced by the DDL script. Any violation of these constrains is prohibited.