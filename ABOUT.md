# SQL-injection detection plugin

## Why?
Obligatorily, the all-famous xkcd comic: https://xkcd.com/327/



A lot of SQL-related plugins and guides contain information about the dangers of SQL-injections.
However, none actually proactively warn a user if they construct their SQL queries in an injection-prone way.

This plugin strives to be that missing component: actively check the composition of queries to warn the user
about potential SQL-injections.

## Usage
The usage is simple: load the module, and if you do anything that looks like an injection, the plugin will inform you!

## Use cases

This functionality would be particularly useful in the following situations:
- Students/novice programmers who are new to SQL (but already have some experience with string formatting) might be inclined to format queries like they would usually format strings. They might not even know what SQL-injection is, and thus a proactive warning explaining that they are doing something is very useful.
- Those who make quick prototypes (for example startups), might not think about security too much and just create some "quick and dirty" code that works. If an IDE does not warn about an SQL-injection at this stage (and proposes alternative syntax) this code might make it into production with potentially destructive consequences. Getting insecure code out of a project from the get go can thus be hugely beneficial.
- In some applications, the composition of queries is not so simple and may consist of a concatenation of several strings. Here, IDEs may not even be aware that the code composes a SQL query, and for the programmer the risk to unintentionally introduce a SQL-injection becomes a lot higher. By performing a deeper analysis on what the resulting concatenated string(s) will become, the plugin can warn the developer if this concatenation poses a risk for SQL-injection.

## Features and potential extensions.

### 1: Simple cases of injection

These are mostly relevant for novice programmers, but even experienced programmers may get into the fallacy of creating queries like these!
The following example uses Python.

*Intended usage:*
```sql
SELECT name, home_address, phone_nr
FROM customer
WHERE customer_id = 1234;
```

*Bad (!) implementation:*

```python
customer_id = input("What is your ID?")

query_str = """
    SELECT name, home_address, phone_nr
    FROM customer
    WHERE customer_id = {x};
""".format(customer_id)

db.query(query_str)
```

*Safe alternative:*
```python
customer_id = input("What is your ID?")

query_str = """
    SELECT name, home_address, phone_nr
    FROM customer
    WHERE customer_id = ?;
"""

db.query(query_str, params=[customer_id])
```

The detection here can be done as follows: the variable `query_str` is structured like a query, but has curly braces `{}`.
In Python, these are called "replacement fields", which can be filled with arbitrary strings. 
The right way to do this is to SQL-friendly parameters instead (such as `?` or `$` in some SQL dialects). 
This is not possible in all cases; tables or columns cannot be specified in this way. However, when tables are parametrised
that way, this might be indicative of another problem with the query/database structure and might still have to be reported to the user.

### 2. Composite strings

Similar to the first example, rather than having a replacement inside the string, SQL injections can also occur as a result of string concatenation. 
As these gaps can be arbitrarily complex, identifying such patterns as a potential SQL string may seem more complicated. An example:

```py 
person = get_person()
person_table = "employee" if person.works() else "student" 

query = "SELECT name, age, address" + person_table + "WHERE city = ?;"
db.query(query_str, params=[person.town])
```

By performing a regex validation on the "known" components (constants) within the concatenated string,
and replacing the remaining components with a wildcard-like character, the regex can determine if the string may _potentially_
be a valid SQL string. Note that this does not guarantee that any string is indeed SQL (the non-constant values must be known for that),
however a vast amount of strings can be thrown out for not complying with the format (for example by not starting with one of the SQL keywords).



### 3. Reliability of external sources
It must be noted that in practice, SQL-injection is only unsafe if the input to the SQL string comes from an "untrusted" source.

For that reason, the _severity_ of the warning should depend on the "reliability" of the sources.
If a string originates from an if-else statement with strings created by the programmer, this can be assumed to be "trusted".
However, if the string directly originates from user input (or anything that can be controlled by the user), 
then this is most likely SQL-injectable.

### 4. Distinguishing value parametrisation from tables, columns, and keywords

Parametrised SQL-queries have their limitations: they only work for matching **literals**. 
There may be legit use cases where a non-literal has to be inserted to a query, without this being possible through parametrisation.

An example of this would be an online store where one user might just want to filter on "product type", 
but another user also wants to filter on "price", "brand", "rating", "store", "delivery time", "size", and "weight" (or any combination thereof).

If one would avoid generating these queries through a script, this would mean needing 2^8 = 256 separate strings.
Nevertheless, this needs to be done safely and without the risk of SQL-injection.

Therefore, the following distinction needs to be made:
- If a query is structured in such a way that replacement fields or concatenations can be replaced by parameters (i.e. they are used to insert literals), then report this as a SQL-injection warning. If possible, also suggest how the code can be transformed.
- If the concatenation/replacement field is used for any other components of the query, do the following:
  - If filled by constants specified in the project (which have no non-constant concatenations or replacement fields themselves), display no warning as this is safe. 
  - If filled by a "trusted" (but opaque) source, display a soft warning stating that the "injectability risk" of the source cannot be verified.
  - In all other cases (so if filled by an "untrusted" source), display a normal warning. This forces the user to think about whether their written code may result in a SQL-injection. If their source is for example an external module which is actually "trusted", the user can perform a _context action_ inside their IDE to mark this source as trusted. 


### Potential extensions

#### Backtrace database execution calls

Rather than analysing whether any given string is meant to be a SQL-string, another approach would be to determine which function calls in a program query a database.
Given the vast amount of libraries that offer database possibilities, this may not be possible to do in conventional ways outside of the Python standard library.
Instead, an OpenAI-classifier could be built that determines whether a function call is (likely) a query to a database or not.
If such a call is found, the strings put in to this call can then be traced back to see if there is any string formatters used at any moment of the creation of this string.

#### Advanced composite string classification
The composite string classification described in the previous section works on an "exclusionary" basis: 
it eliminates concatenations which are "certainly" not an SQL string, but (threshold aside) does not eliminate entries that are unlikely to be SQL but still happen to match the pattern.
This is where language models really can excel in their usage. 
The BERT transformer is trained by having gaps in written text (including code) and being forced to predict what fills that gap.

In this situation, we have a string which is interrupted due to concatenation, which is filled by an unknown string.
A language model that is trained using this method will therefore likely be very performant at such a task.

This might require a dedicate language model that is specifically trained on SQL-detection, but that is unfortunately out of the scope for the time being.
From our limited testing, GPT-4 is however already rather performant at this classification task without dedicated training on our side.

#### Suggestions to improve database structuring
If code is structured in such a way that string concatenations for queries often seem a necessity,
this may indicate that the database created by the programmer is violating some SQL design guidelines.

A possible future extension could be to somehow inform the user about design principles of database 
if a user's queries depict dubious SQL.

#### Tight integration with JetBrains' (paid) SQL support
The professional (paid) versions of JetBrains IDEs have a rather deep integration with most database systems. 
For example, syntax is automatically highlighted, the SQL dialect can be chosen, and the IDE can even use the schema of the database to determine whether the programming might be making a mistake with naming.

Due to reasons beyond our control, it has not been possible yet to combine this functionality with our plugin.
However, it would obviously be a great benefit for users if the "database experience" in their IDE is harmonised!
SQL-injections are very impactful, and in our opinions IDEs should make it as easy as possible to guard against them.
