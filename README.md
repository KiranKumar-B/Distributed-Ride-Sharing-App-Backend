# Distributed Ride-Sharing Backend

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot 3.2](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg?style=flat&logo=spring)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-Event%20Driven-black.svg?style=flat&logo=apachekafka)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-Geospatial-red.svg?style=flat&logo=redis)](https://redis.io/)
[![PostgreSQL & PostGIS](https://img.shields.io/badge/PostgreSQL-PostGIS-blue.svg?style=flat&logo=postgresql)](https://postgis.net/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Minikube-blue.svg?style=flat&logo=kubernetes)](https://kubernetes.io/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED.svg?style=flat&logo=docker)](https://www.docker.com/)

A highly scalable, production-ready microservices-based distributed backend for a ride-sharing platform engineered to handle real-time driver matching, geospatial indexing, event-driven trip workflows, and high-throughput concurrent trip requests under strict consistency guarantees.

---

## 🏗️ Architectural Overview & Core Services

The system is broken down into independent domain-driven microservices communicating synchronously via non-blocking HTTP (`WebClient`) and asynchronously via event streams (`Apache Kafka`).

* **Rider Service**: Manages rider profiles, ride requests, and active trip tracking.
* **Driver Service**: Handles driver onboarding, verification, and live status toggling.
* **Trip Service**: Orchestrates the core matching lifecycle, fare calculation, and state transitions.
* **Geospatial Tracking Engine**: Built on **Redis GEO (`GEOADD`, `GEORADIUS`)** for lightning-fast sub-millisecond driver radius searches.
* **Event Streaming Bus**: **Apache Kafka** decouples core workflows (`trip-events`) ensuring reliable downstream audit projections and notification triggers.

---

## 🛠️ Technology Stack

* **Core Language:** Java 21 (Virtual Threads ready, pattern matching, record types)
* **Framework:** Spring Boot 3.2, Spring Cloud, Spring Data JPA / Hibernate
* **Databases:** PostgreSQL 15 with **PostGIS** extension for spatial geometries and indexing
* **In-Memory Store:** Redis (Cluster-ready configuration, Geospatial Indexing, ZSets)
* **Messaging & Streaming:** Apache Kafka (Topic partitions, consumer groups: `driver-availability-group`, `rider-group`, `audit-query-group`)
* **Resilience & Fault Tolerance:** Resilience4j (Circuit breakers, rate limiters, bulkheads)
* **Containerization & Orchestration:** Docker, Docker Compose, Kubernetes (Minikube deployments via custom YAML manifests)

---

## 💡 Key Engineering Challenges & Solutions

### 1. High-Concurrency Trip Acceptance Race Conditions
* **Problem:** Multiple drivers attempting to accept the exact same ride request simultaneously leads to double-booking anomalies.
* **Solution:** Implemented **JPA Optimistic Locking (`@Version`)** coupled with strict database constraints and atomic state transitions in the Trip Service (`REQUESTED` $\rightarrow$ `ACCEPTED`). If a concurrent transaction tries to update an outdated version, it rolls back gracefully with a retry policy.

### 2. Sub-Millisecond Nearby Driver Discovery
* **Problem:** Traditional SQL bounding box queries (`lat/long` range checks) across millions of active drivers degrade severely under high read QPS.
* **Solution:** Leveraged **Redis Geospatial Data Structures (`opsForGeo().radius`)** backed by Geohash algorithms. Driver coordinates update in real-time as they navigate, allowing instant lookup of available drivers within a 5km radius in $O(N + \log(M))$ time.

### 3. Distributed Consistency & Fault Tolerance
* **Problem:** Network partitions between Trip creation and Driver assignment could cause orphaned records or inconsistent states.
* **Solution:** Implemented the **Saga Pattern (Choreography-based)** using Kafka event logs with compensating rollback transactions if downstream validation fails. Configured **Resilience4j Circuit Breakers** on inter-service WebClient calls to isolate failures and prevent cascading degradation.

---

## 🚀 Getting Started Locally

### Prerequisites
* **Java Development Kit (JDK) 21+**
* **Docker & Docker Compose**
* **Maven 3.9+**

### 1. Clone the Repository
```bash
git clone [https://github.com/KiranKumar-B/Distributed-Ride-Sharing-App-Backend.git](https://github.com/KiranKumar-B/Distributed-Ride-Sharing-App-Backend.git)
cd Distributed-Ride-Sharing-App-Backend
