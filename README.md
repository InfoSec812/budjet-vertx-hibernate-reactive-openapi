# OpenAPI Contract-First with Vert.x and Hibernate Reactive

## NOTICE
**WARNING:** YOU **MUST** at least run `mvn generate-sources` before IDEs will be able to make sense
of this project. A lot of the code needs to be generated by [OpenAPI Generator](https://openapi-generator.tech)
before the correct classes/entities/services will exist.

Most generated code will be placed under it's associated module in the `src/gen/java` directories

## Overview

The purpose of this project is to demonstrate how you could create a Contract-First API
with [Vert.x](https://vertx.io) using [OpenAPI Generator](https://openapi-generator.tech) and [Hibernate Reactive](http://hibernate.org/reactive/). This API is designed to be the backend-for-frontend which complements this [VueJS UI](https://github.com/InfoSec812/budjet)

## Build Application

```bash
mvn clean verify
```

## Run application locally

```bash
mvn -pl modules/models clean package install
mvn -pl modules/api clean compile vertx:run
```



### NOTES

This application was bootstrapped using the [OpenAPI Vert.x Maven Archetype](https://github.com/redhat-appdev-practice/openapi-vertx-archetype) created by the [Red Hat Cloud-Native Runtimes
Practice](https://appdev.consulting.redhat.com/). 

