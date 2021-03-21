## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!

### Notes from challenge
* Fun to start using Kotlin, I've never worked with it before so this is going to be fun! (and you might see some Java traces here and there).
* Needed to upgrade gradle to be able to build project.
* _First commit_
* Started to look into different classes and how Javalin is structured and came across the billingservice and the other classes under core package.
* The challenge is about to bill on the 1st every month so I thought that the BillingService would act like a scheduler.
* BillingService should own the logic for two different jobs.
 ** Monthly billing job which fetches all invoices which are in PENDING state and tries to charge all and if it fails. Sets the status to FAILED. - should run 1st of month at 00:00.
 ** Retry job which fetches all invoices which are in FAILED state and retries charge these. - should run everyday.
* Added functionality to InvoiceService to fetch all invoices with given status and updated the DAL accordingly.
* Added functioanlity to InvoiceService and DAL to update a invoice.
* _Second commit_
* Started to write on the JobScheduler, to separate this logic from the BillingService.
 ** Wanted the jobs to be represented as enums.
* _Third commit_
* I realize that we need a FAILED status on the invoice to be handle to handle logic explained above.
* _Fourth commit_
* Creates the `init` function. I know for sure that I want to have two scheduled jobs triggered in the init so I start with a failing testcase where I assert that with mockk this has actually happened. In the implementation I am working with the `Calendar` and `Date` classes and separates two different functions separately handling setting up the different jobs. These functions I plan to use after each job has finished also to trigger next run.
* Implementing and writes tests for `chargeInvoices` function which should take a list of invoices and iterate through them and `charge`. Depending of the outcome it updates the invoice accordingly.
* Implementing and writing tests for the actual logic of the jobs now is fairly easy when I already have `fetchAllWithStatus` in InvoiceService, charging the invoices and the scheduling done.
* Extends construction of `BillingService` in `AntaeusApp` with required args.
* _Fifth commit_ (Too large, I know.. got carried away)

### Improvements
* The billing/charging should not be in this service. This REST service should only return the invoices related to our customers. If I'd had the chance I would definetely implement this as a AWS Lambda which would be periodically triggered with a Cloudwatch event - so that this REST Service can be scalable to many more nodes when traffic increases. With this implementation we are limited to running a single node.