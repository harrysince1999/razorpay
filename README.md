# Razorpay

A Spring Boot payment-domain project that models the core data layer for a Razorpay-like payment system. The current codebase focuses on JPA entities and domain relationships for merchants, customers, API keys, orders, payments, refunds, settlements, webhooks, and card vaulting.

## Purpose

This project is structured as the foundation for a payment gateway backend. It captures the main business objects needed to support:

- Merchant onboarding and KYC status tracking
- API key management across sandbox and production environments
- Customer records under merchants
- Order creation and payment attempts
- Payment state transitions and audit logs
- Refund processing
- Card vaulting and reusable card tokens
- Merchant webhook delivery and dead-letter handling
- Settlement calculation and settlement-payment mapping

At the moment, the repository contains the Spring Boot application bootstrap, entity model, enum definitions, configuration, and a basic context-load test. Controllers, services, repositories, authentication, and real payment gateway integrations can be built on top of this model.

## Tech Stack

- Java 21
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- Hibernate
- PostgreSQL for runtime persistence
- H2 for tests
- Lombok
- Maven Wrapper

## Project Structure

```text
src/main/java/com/harikesh/razorpay
├── RazorpayApplication.java
├── common
│   ├── entity          Shared embeddable value objects
│   ├── enums           Domain enums for statuses, events, roles, and methods
│   └── exceptions      Reserved for shared exception types
├── merchant
│   └── entity          Merchant, user, customer, API key, and webhook config models
├── operations
│   └── entity          Settlement, webhook delivery, and DLQ models
├── payment
│   └── entity          Order, payment, refund, and transition log models
└── vault
    └── entity          Card vault and token models
```

The ER diagram is available at:

```text
src/main/resources/Razorpay_ER_Diagram.webp
```

## Domain Modules

### Common

The `common` package contains shared building blocks used by the rest of the model.

- `Money` is an embeddable value object with `amountUnits` and `currency`.
- Enums define allowed domain states such as payment status, refund status, settlement status, merchant status, user role, payment method, and webhook status.

Important enums:

- `PaymentStatus`: `CREATED`, `AUTHORIZING`, `AUTHORIZED`, `CAPTURING`, `CAPTURED`, `FAILED`, `CANCELLED`, `REFUNDED`, `PARTIALLY_REFUNDED`, `SETTLED`, `AUTH_EXPIRED`
- `PaymentEvent`: `AUTHORIZE_ATTEMPT`, `AUTHORIZE_SUCCESS`, `AUTHORIZE_FAIL`, `CAPTURE_REQUEST`, `CAPTURE_SUCCESS`, `CAPTURE_FAIL`, `REFUND_INIT`, `REFUND_COMPLETE`, `SETTLE`, `CANCEL`, `CAPTURE_TIMEOUT`
- `OrderStatus`: `CREATED`, `PAID`, `FAILED`, `REFUNDED`
- `PaymentMethod`: `CARD`, `NETBANKING`, `UPI`, `WALLET`
- `RefundStatus`: `PENDING`, `PROCESSING`, `PROCESSED`, `FAILED`
- `SettlementStatus`: `INITIATED`, `PROCESSED`, `FAILED`
- `WebhookEventStatus`: `PENDING`, `DELIVERED`, `FAILED`, `DEAD`

### Merchant

The `merchant` package models platform users and merchant-owned configuration.

- `Merchant` stores business identity, contact details, KYC identifiers, merchant status, and settlement bank account details.
- `ApiKey` belongs to a merchant and stores a `keyId`, hashed secret, environment, and enabled flag.
- `AppUser` stores login-related user data and role information.
- `Customer` stores a merchant-scoped customer profile.
- `MerchantWebhookConfig` stores the merchant webhook target URL, webhook secret hash, enabled flag, and subscribed event types.

### Payment

The `payment` package models checkout and money movement state.

- `OrderRecord` represents a merchant order with amount, status, attempt count, notes, and expiry time.
- `Payment` belongs to an order and records merchant ID, amount, idempotency key, payment method, method details, references, errors, and lifecycle timestamps.
- `PaymentTransitionLog` records state changes for a payment, including previous status, event, next status, actor, and time.
- `Refund` belongs to a payment and records refund amount, refund status, bank reference, error details, notes, and processing time.

### Vault

The `vault` package models saved card storage.

- `VaultCard` stores card metadata such as BIN, last four digits, brand, expiry, holder name, encrypted PAN, encrypted DEK, and soft deletion time.
- `CardToken` maps a generated token to a vault card, customer, and merchant. It also supports revocation through `revokedAt`.

### Operations

The `operations` package models asynchronous operational flows.

- `WebhookEvent` stores an outbound webhook payload, target URL, signature, delivery status, retry details, response details, and delivery time.
- `DlqEvent` stores webhook events that permanently failed and were moved to a dead-letter queue.
- `Settlement` stores merchant settlement totals such as gross amount, refund amount, fee, GST, and net amount.
- `SettlementPayment` links payments to settlements through an embedded composite ID.

## Project Flow

The entity model supports the following high-level flow.

```text
Merchant onboarding
    -> API keys and webhook configuration
    -> Customer creation
    -> Order creation
    -> Payment attempt
    -> Payment authorization/capture
    -> Payment transition logs
    -> Webhook event creation and delivery
    -> Optional refund
    -> Settlement generation
    -> Settlement-payment mapping
```

### 1. Merchant Onboarding Flow

A merchant is created first. The `Merchant` entity stores the business profile, contact information, KYC identifiers, status, and settlement bank details.

Typical lifecycle:

```text
PENDING_KYC -> PENDING_VERIFICATION -> ACTIVE
```

Once active, the merchant can receive API keys and process payments.

Related entities:

- `Merchant`
- `AppUser`
- `ApiKey`
- `MerchantWebhookConfig`

### 2. API Access Flow

Each merchant can have API keys for different environments.

```text
Merchant
    -> ApiKey(environment = SANDBOX or PRODUCTION)
    -> keyId + keySecretHash
    -> enabled/disabled access
```

The model stores only a hashed secret through `keySecretHash`, which is the right direction for real-world secret handling.

### 3. Customer Flow

A merchant can create customer records. Customers are merchant-scoped, so the same end user could exist under different merchants.

```text
Merchant
    -> Customer
    -> name/email/contact
    -> optional deletedAt for soft deletion
```

Customers can later be connected to saved cards through card tokens.

### 4. Order Flow

An order represents the merchant's intent to collect a specific amount.

```text
Merchant
    -> OrderRecord
    -> amount + currency
    -> status = CREATED
    -> attempts = 0
    -> expiresAt
```

The order can hold extra merchant metadata in `notes`, stored as PostgreSQL `jsonb`.

Expected status movement:

```text
CREATED -> PAID
CREATED -> FAILED
PAID -> REFUNDED
```

### 5. Payment Flow

A payment is created against an order. It records how the customer is attempting to pay and tracks the gateway lifecycle.

```text
OrderRecord
    -> Payment
    -> method = CARD / UPI / NETBANKING / WALLET
    -> status = CREATED
    -> idempotencyKey
    -> methodDetails jsonb
```

Expected payment state flow:

```text
CREATED
    -> AUTHORIZING
    -> AUTHORIZED
    -> CAPTURING
    -> CAPTURED
    -> SETTLED
```

Failure and cancellation paths:

```text
AUTHORIZING -> FAILED
AUTHORIZED -> AUTH_EXPIRED
CREATED -> CANCELLED
CAPTURING -> FAILED
```

Refund-related paths:

```text
CAPTURED -> PARTIALLY_REFUNDED
CAPTURED -> REFUNDED
PARTIALLY_REFUNDED -> REFUNDED
```

For each important state change, `PaymentTransitionLog` can store:

- Source status
- Triggering event
- Destination status
- Actor responsible for the change
- Timestamp

This creates an audit trail for debugging, reconciliation, and support.

### 6. Card Vaulting Flow

When a customer chooses to save a card, the model separates card storage from reusable tokens.

```text
Raw card input
    -> encrypted PAN stored in VaultCard
    -> encrypted DEK stored in VaultCard
    -> token generated in CardToken
    -> token linked to customer and merchant
```

The payment system should use `CardToken.token` for future payments instead of exposing card details. `VaultCard` keeps only the encrypted sensitive card data plus display metadata such as BIN, last four, brand, expiry, and holder name.

Revocation flow:

```text
CardToken active
    -> revokedAt set
    -> token should no longer be accepted
```

### 7. Refund Flow

A refund belongs to an existing payment.

```text
Payment(CAPTURED or PARTIALLY_REFUNDED)
    -> Refund(PENDING)
    -> PROCESSING
    -> PROCESSED or FAILED
```

The `Refund` entity stores amount, bank reference, notes, error details, and `processedAt`. Once refunds are processed, the owning payment can move to either `PARTIALLY_REFUNDED` or `REFUNDED` depending on total refunded amount.

### 8. Webhook Flow

Merchants configure webhook delivery through `MerchantWebhookConfig`.

```text
MerchantWebhookConfig
    -> targetUrl
    -> webhookSecretHash
    -> enabled
    -> eventTypes
```

When a domain event happens, such as payment captured or refund processed, the system can create a `WebhookEvent`.

```text
Domain event
    -> WebhookEvent(PENDING)
    -> deliver to targetUrl with signature
    -> DELIVERED
```

Retry path:

```text
PENDING or FAILED
    -> attempts incremented
    -> lastAttemptAt updated
    -> lastResponseCode/body stored
    -> nextRetryAt scheduled
```

Dead-letter path:

```text
WebhookEvent repeatedly fails
    -> status = DEAD
    -> DlqEvent created
    -> finalError and payload retained
```

The `DlqEvent` model also supports replay tracking through `replayedAt`.

### 9. Settlement Flow

Settlements represent payouts from the platform to merchants after payments are captured and eligible for settlement.

```text
Captured payments
    -> grouped by merchant
    -> grossAmount calculated
    -> refundAmount deducted
    -> feeAmount deducted
    -> gstAmount deducted
    -> netAmount paid out
    -> Settlement status tracked
```

Status flow:

```text
INITIATED -> PROCESSED
INITIATED -> FAILED
```

`SettlementPayment` links individual payment IDs to a settlement so reconciliation can answer:

- Which payments were included in this settlement?
- Which settlement paid out a particular payment?
- What was the bank reference for the payout?

## Data Storage Notes

- Runtime database: PostgreSQL
- Test database dependency: H2
- JSON fields use Hibernate JSON mapping and PostgreSQL `jsonb`
- Primary IDs are UUID-based
- Hibernate DDL mode is currently `update`
- SQL logging is enabled with `spring.jpa.show-sql: true`

## Configuration

The application reads database settings from environment variables, with defaults defined in `src/main/resources/application.yaml`.

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/razorpay
export SPRING_DATASOURCE_USERNAME=your_username
export SPRING_DATASOURCE_PASSWORD=your_password
```

Current application configuration:

```yaml
spring:
  application:
    name: razorpay
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/razorpay}
    username: ${SPRING_DATASOURCE_USERNAME:}
    password: ${SPRING_DATASOURCE_PASSWORD:}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

## Prerequisites

- JDK 21
- PostgreSQL
- Maven is optional because the repository includes `mvnw`

## Run Locally

Create the local database if it does not already exist:

```bash
createdb razorpay
```

Set database credentials:

```bash
export SPRING_DATASOURCE_USERNAME=your_username
export SPRING_DATASOURCE_PASSWORD=your_password
```

Start the application:

```bash
./mvnw spring-boot:run
```

## Test

Run the test suite:

```bash
./mvnw test
```

The current test suite contains a Spring Boot context-load test.

## Build

Create a packaged build:

```bash
./mvnw clean package
```

The generated artifact will be written under `target/`.

## Suggested Next Steps

Useful additions on top of the current entity model:

- Repository interfaces for each aggregate
- Service layer for merchant onboarding, order creation, payment state changes, refunds, webhook delivery, and settlement generation
- REST controllers and DTOs
- Validation for state transitions
- Database migrations through Flyway or Liquibase
- Authentication and API key verification
- Test coverage for entity mappings and payment flow rules
- Webhook signature generation and verification
- Idempotency enforcement for payment creation and capture
